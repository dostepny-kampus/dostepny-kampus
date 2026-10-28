# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — render diagramów PlantUML i BPMN do docs/_generated z manifestem źródeł. Author: Vladyslav Yevkov. Version: 0.2.0. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import signal
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path
from typing import Any

REPO = Path(__file__).resolve().parent.parent
FORMATS = ("svg", "png")
MANIFEST_NAME = "zrodla.sha256"
MOCKUP_DIR = Path("ux") / "makiety"
PLANTUML_TIMEOUT_S = 300
BPMN_BASE_TIMEOUT_S = 60
BPMN_PER_FILE_TIMEOUT_S = 20


class RenderError(RuntimeError):
    """Błąd renderu diagramu; komunikat jest gotowy do wypisania użytkownikowi."""


def node_modules_dir() -> Path:
    """Zwraca katalog node_modules z narzędziami: DK_NODE_MODULES, a bez tej zmiennej `node_modules` obok
    package.json (po `npm ci`). Nie sprawdza istnienia wyniku."""
    env = os.environ.get("DK_NODE_MODULES")
    return Path(env) if env else REPO / "node_modules"


def plantuml_jar() -> Path:
    """Zwraca ścieżkę do plantuml.jar ze zmiennej PLANTUML_JAR.

    Wyjątki: RenderError z poleceniem, co ustawić, gdy zmiennej brak albo plik nie istnieje.
    """
    env = os.environ.get("PLANTUML_JAR")
    if not env:
        raise RenderError("brak PlantUML: ustaw zmienną PLANTUML_JAR na ścieżkę do pliku plantuml.jar")
    jar = Path(env)
    if not jar.is_file():
        raise RenderError(f"PLANTUML_JAR wskazuje nieistniejący plik: {jar}")
    return jar


def find_sources(docs: Path, suffix: str) -> list[Path]:
    """Zwraca posortowaną listę plików `*<suffix>` w `docs` (rekurencyjnie) poza `_generated`.

    Wymaga: `suffix` z kropką, np. ".puml". Brak katalogu `docs` daje pustą listę.
    """
    if not docs.is_dir():
        return []
    return sorted(p for p in docs.rglob(f"*{suffix}") if "_generated" not in p.relative_to(docs).parts)


def ensure_unique_names(sources: list[Path]) -> None:
    """Sprawdza, że nazwy plików (bez rozszerzenia) są unikalne, bo wspólny katalog wyjściowy jest płaski.

    Wyjątki: RenderError z listą powtórzonych nazw.
    """
    counts = Counter(p.stem for p in sources)
    duplicates = sorted(name for name, n in counts.items() if n > 1)
    if duplicates:
        raise RenderError("powtórzone nazwy diagramów: " + ", ".join(duplicates))


def file_digest(path: Path) -> str:
    """Zwraca sha256 treści pliku po zamianie końców linii CRLF na LF (wynik nie zależy od systemu)."""
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def dependencies(source: Path, docs: Path) -> list[Path]:
    """Zwraca posortowane pliki `*.iuml`, od których może zależeć źródło PlantUML: z jego katalogu i z katalogu
    makiet. Źródło BPMN nie ma zależności."""
    if source.suffix != ".puml":
        return []
    found = set(source.parent.glob("*.iuml")) | set((docs / MOCKUP_DIR).glob("*.iuml"))
    return sorted(found, key=lambda p: p.relative_to(docs).as_posix())


def source_digest(source: Path, docs: Path) -> str:
    """Zwraca sha256 źródła połączony z sha256 jego zależności (ścieżki względne do `docs`, POSIX).

    Gwarantuje: wynik zależy tylko od treści i nazw plików, nie od systemu ani czasu modyfikacji.
    """
    combined = hashlib.sha256()
    for path in [source, *dependencies(source, docs)]:
        combined.update(f"{path.relative_to(docs).as_posix()}\t{file_digest(path)}\n".encode("utf-8"))
    return combined.hexdigest()


def current_manifest(docs: Path, sources: list[Path]) -> dict[str, str]:
    """Zwraca manifest bieżących źródeł: ścieżka względna do `docs` (POSIX) → source_digest."""
    return {s.relative_to(docs).as_posix(): source_digest(s, docs) for s in sources}


def read_manifest(path: Path) -> dict[str, str]:
    """Czyta manifest w formacie `<sha256>  <ścieżka>` (jak sha256sum). Brak pliku daje pusty słownik."""
    if not path.is_file():
        return {}
    entries = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        digest, _, name = line.partition("  ")
        if digest and name:
            entries[name.strip()] = digest.strip()
    return entries


def write_manifest(path: Path, entries: dict[str, str]) -> None:
    """Zapisuje manifest posortowany po ścieżce, z końcami linii LF."""
    lines = [f"{entries[name]}  {name}\n" for name in sorted(entries)]
    path.write_bytes("".join(lines).encode("utf-8"))


def outputs_of(source: Path, out_dir: Path) -> list[Path]:
    """Zwraca ścieżki plików wyjściowych źródła we wszystkich formatach."""
    return [out_dir / f"{source.stem}.{fmt}" for fmt in FORMATS]


def orphan_outputs(sources: list[Path], out_dir: Path) -> list[Path]:
    """Zwraca posortowane pliki SVG i PNG w `out_dir`, dla których nie ma źródła o tej samej nazwie."""
    if not out_dir.is_dir():
        return []
    stems = {s.stem for s in sources}
    return sorted(p for p in out_dir.iterdir() if p.suffix.lstrip(".") in FORMATS and p.stem not in stems)


def is_fresh(source: Path, docs: Path, out_dir: Path, recorded: dict[str, str]) -> bool:
    """Zwraca True, gdy manifest zgadza się z bieżącym źródłem i istnieją wszystkie pliki wyjściowe."""
    name = source.relative_to(docs).as_posix()
    if recorded.get(name) != source_digest(source, docs):
        return False
    return all(o.is_file() for o in outputs_of(source, out_dir))


def stale_report(docs: Path, out_dir: Path) -> list[str]:
    """Zwraca listę rozbieżności między źródłami a katalogiem wyników (pusta, gdy wszystko jest aktualne).

    Sprawdza: manifest wobec bieżących źródeł i zależności, brak wyników dla źródła, wyniki bez źródła.
    """
    sources = find_sources(docs, ".puml") + find_sources(docs, ".bpmn")
    manifest_path = out_dir / MANIFEST_NAME
    if not manifest_path.is_file():
        return [f"brak manifestu {MANIFEST_NAME}"]
    recorded = read_manifest(manifest_path)
    current = current_manifest(docs, sources)
    problems = [f"zmienione źródło lub zależność: {n}" for n in sorted(current) if n in recorded and recorded[n] != current[n]]
    problems += [f"źródło spoza manifestu: {n}" for n in sorted(current) if n not in recorded]
    problems += [f"w manifeście, ale bez źródła: {n}" for n in sorted(recorded) if n not in current]
    problems += [f"brak wyniku: {o.name}" for s in sources for o in outputs_of(s, out_dir) if not o.is_file()]
    problems += [f"wynik bez źródła: {o.name}" for o in orphan_outputs(sources, out_dir)]
    return problems


def kill_tree(proc: subprocess.Popen[Any]) -> None:
    """Kończy proces wraz z potomkami (np. Chromium uruchomionym przez Node)."""
    if os.name == "nt":
        subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)], capture_output=True, check=False)
    else:
        os.killpg(proc.pid, signal.SIGKILL)


def run_with_timeout(cmd: list[str], timeout_s: int, cwd: Path) -> tuple[int, str]:
    """Uruchamia polecenie i zwraca (kod wyjścia, połączone stdout+stderr).

    Gwarantuje: po przekroczeniu `timeout_s` albo po Ctrl+C całe drzewo procesów zostaje zabite.
    Wyjątki: RenderError przy przekroczeniu czasu lub braku programu.
    """
    flags = subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0
    try:
        proc = subprocess.Popen(
            cmd,
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            creationflags=flags,
            start_new_session=os.name != "nt",
        )
    except FileNotFoundError as exc:
        raise RenderError(f"nie znaleziono programu: {cmd[0]} (zainstaluj go albo ustaw JAVA lub NODE)") from exc
    try:
        out, _ = proc.communicate(timeout=timeout_s)
    except subprocess.TimeoutExpired as exc:
        kill_tree(proc)
        proc.communicate()
        raise RenderError(f"przekroczono limit {timeout_s} s: {' '.join(cmd[:3])} …") from exc
    except KeyboardInterrupt:
        kill_tree(proc)
        raise
    return proc.returncode, out.decode("utf-8", errors="replace")


def render_plantuml(sources: list[Path], out_dir: Path) -> None:
    """Renderuje pliki .puml do `out_dir/<nazwa>.svg` i `.png`.

    Wymaga: Javy w PATH (lub JAVA) i plantuml.jar (PLANTUML_JAR). Gwarantuje: nazwa wyniku = nazwa pliku źródłowego.
    Wyjątki: RenderError, gdy brak narzędzi, PlantUML zgłosi błąd składni, zabraknie pliku wyniku lub minie limit czasu.
    """
    if not sources:
        return
    jar = plantuml_jar()
    java = os.environ.get("JAVA", "java")
    for fmt in FORMATS:
        render_plantuml_format(java, jar, sources, out_dir, fmt)


def render_plantuml_format(java: str, jar: Path, sources: list[Path], out_dir: Path, fmt: str) -> None:
    """Renderuje wszystkie `sources` do jednego formatu jednym wywołaniem Javy przez katalog tymczasowy.

    Wyjątki: RenderError jak w render_plantuml.
    """
    with tempfile.TemporaryDirectory(prefix="dk-puml-") as tmp:
        tmp_dir = Path(tmp)
        cmd = [java, "-Djava.awt.headless=true", "-jar", str(jar), f"-t{fmt}", "-charset", "UTF-8", "-o", str(tmp_dir)]
        code, out = run_with_timeout(cmd + [str(s) for s in sources], PLANTUML_TIMEOUT_S, REPO)
        if code != 0:
            raise RenderError(f"PlantUML ({fmt}) zakończył się kodem {code}:\n{out.strip()}")
        for src in sources:
            collect_output(tmp_dir, src, out_dir, fmt)


def collect_output(tmp_dir: Path, src: Path, out_dir: Path, fmt: str) -> None:
    """Przenosi `tmp_dir/<src.stem>.<fmt>` do `out_dir`.

    Wyjątki: RenderError, gdy wyniku brak (np. nazwa po `@startuml` różna od nazwy pliku).
    """
    produced = tmp_dir / f"{src.stem}.{fmt}"
    if not produced.is_file():
        raise RenderError(f"{src}: brak wyniku {produced.name}; usuń nazwę po @startuml albo nazwij plik tak samo")
    shutil.move(str(produced), out_dir / produced.name)


def bpmn_cli() -> Path:
    """Zwraca ścieżkę do cli.js pakietu bpmn-to-image. Wyjątki: RenderError, gdy pakietu brak."""
    cli = node_modules_dir() / "bpmn-to-image" / "cli.js"
    if not cli.is_file():
        raise RenderError(f"brak bpmn-to-image: {cli} (uruchom npm ci w katalogu docs albo ustaw DK_NODE_MODULES)")
    return cli


def render_bpmn(sources: list[Path], out_dir: Path) -> None:
    """Renderuje pliki .bpmn do `out_dir/<nazwa>.svg` i `.png` jednym wywołaniem bpmn-to-image.

    Wymaga: Node w PATH (lub NODE) i pakietu bpmn-to-image. Pliki muszą mieć sekcję bpmndi.
    Gwarantuje: wywołanie nie trwa dłużej niż limit czasu (proces jest zabijany).
    Wyjątki: RenderError przy kodzie wyjścia ≠ 0, braku wyników lub przekroczeniu czasu.
    """
    if not sources:
        return
    node = os.environ.get("NODE", "node")
    cmd = [node, str(bpmn_cli()), "--no-footer"]
    for src in sources:
        outputs = ",".join(str(out_dir / f"{src.stem}.{fmt}") for fmt in FORMATS)
        cmd.append(f"{src}{os.pathsep}{outputs}")
    timeout_s = BPMN_BASE_TIMEOUT_S + BPMN_PER_FILE_TIMEOUT_S * len(sources)
    code, out = run_with_timeout(cmd, timeout_s, REPO)
    if code != 0:
        raise RenderError(f"bpmn-to-image zakończył się kodem {code}:\n{out.strip()}")
    missing = [f"{s.stem}.{f}" for s in sources for f in FORMATS if not (out_dir / f"{s.stem}.{f}").exists()]
    if missing:
        raise RenderError("bpmn-to-image nie utworzył: " + ", ".join(missing))


def render_all(docs: Path, out_dir: Path, force: bool) -> tuple[list[Path], list[Path]]:
    """Renderuje nieaktualne diagramy z `docs` do `out_dir`; zwraca (wyrenderowane źródła, usunięte wyniki bez źródła).

    Gwarantuje: `out_dir` istnieje po wywołaniu; aktualne według manifestu wyniki są pomijane, chyba że `force`;
    po udanym renderze wyniki bez źródła są usunięte, a manifest opisuje wszystkie bieżące źródła.
    Wyjątki: RenderError przy pierwszym nieudanym renderze lub powtórzonych nazwach (manifest zostaje bez zmian).
    """
    puml = find_sources(docs, ".puml")
    bpmn = find_sources(docs, ".bpmn")
    ensure_unique_names(puml + bpmn)
    out_dir.mkdir(parents=True, exist_ok=True)
    recorded = read_manifest(out_dir / MANIFEST_NAME)
    todo_puml = [p for p in puml if force or not is_fresh(p, docs, out_dir, recorded)]
    todo_bpmn = [p for p in bpmn if force or not is_fresh(p, docs, out_dir, recorded)]
    render_plantuml(todo_puml, out_dir)
    render_bpmn(todo_bpmn, out_dir)
    removed = orphan_outputs(puml + bpmn, out_dir)
    for path in removed:
        path.unlink()
    write_manifest(out_dir / MANIFEST_NAME, current_manifest(docs, puml + bpmn))
    return todo_puml + todo_bpmn, removed


def use_utf8_output() -> None:
    """Przełącza stdout i stderr na UTF-8, by polskie znaki nie były zamieniane na sekwencje ucieczki."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    """Parsuje argumenty wiersza poleceń."""
    parser = argparse.ArgumentParser(description="Renderuje docs/**/*.puml i *.bpmn do docs/_generated (SVG i PNG).")
    parser.add_argument("--docs", type=Path, default=REPO / "docs", help="katalog źródeł (domyślnie docs)")
    parser.add_argument("--out", type=Path, default=None, help="katalog wyników (domyślnie <docs>/_generated)")
    parser.add_argument("--force", action="store_true", help="renderuj także aktualne diagramy")
    parser.add_argument("--check", action="store_true", help="tylko sprawdź, czy wyniki i manifest są aktualne")
    return parser.parse_args(argv)


def check(docs: Path, out_dir: Path) -> int:
    """Tryb --check: wypisuje rozbieżności i zwraca 1, gdy są, w przeciwnym razie 0. Niczego nie renderuje."""
    problems = stale_report(docs, out_dir)
    for problem in problems:
        print(f"render: {problem}")
    if problems:
        print(f"render: obrazy nieaktualne ({len(problems)}), uruchom scripts/render.py i dodaj wyniki do commita")
        return 1
    print("render: obrazy aktualne")
    return 0


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 0 przy sukcesie, 1 przy błędzie renderu albo nieaktualnych obrazach (--check)."""
    use_utf8_output()
    args = parse_args(argv)
    docs = args.docs.resolve()
    out_dir = (args.out or docs / "_generated").resolve()
    if args.check:
        return check(docs, out_dir)
    try:
        done, removed = render_all(docs, out_dir, args.force)
    except RenderError as exc:
        print(f"render: BŁĄD: {exc}", file=sys.stderr)
        return 1
    for src in done:
        print(f"render: {src.relative_to(docs).as_posix()} -> {out_dir.name}/{src.stem}.{{svg,png}}")
    for path in removed:
        print(f"render: usunięto wynik bez źródła {out_dir.name}/{path.name}")
    print(f"render: OK ({len(done)} wyrenderowano)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
