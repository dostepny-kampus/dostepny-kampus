"""Dostępny Kampus — render diagramów PlantUML i BPMN do docs/_generated. Author: Vladyslav Yevkov. Version: 0.1.0. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import os
import shutil
import signal
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MAIN_REPO = Path(".")
DEFAULT_PLANTUML = Path("tools/plantuml.jar")
FORMATS = ("svg", "png")
PLANTUML_TIMEOUT_S = 300
BPMN_BASE_TIMEOUT_S = 60
BPMN_PER_FILE_TIMEOUT_S = 20


class RenderError(RuntimeError):
    """Błąd renderu diagramu; komunikat jest gotowy do wypisania użytkownikowi."""


def node_modules_dir() -> Path:
    """Zwraca katalog node_modules z narzędziami.

    Gwarantuje: ścieżkę z DK_NODE_MODULES, a bez niej node_modules repo, jeśli istnieje,
    w przeciwnym razie node_modules głównego repozytorium. Nie sprawdza istnienia wyniku.
    """
    env = os.environ.get("DK_NODE_MODULES")
    if env:
        return Path(env)
    local = REPO / "node_modules"
    return local if local.is_dir() else MAIN_REPO / "node_modules"


def plantuml_jar() -> Path:
    """Zwraca ścieżkę do plantuml.jar z PLANTUML_JAR albo wartość domyślną. Nie sprawdza istnienia."""
    return Path(os.environ.get("PLANTUML_JAR", str(DEFAULT_PLANTUML)))


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


def is_fresh(source: Path, out_dir: Path) -> bool:
    """Zwraca True, gdy wszystkie formaty wyjściowe istnieją i są nowsze od źródła."""
    outputs = [out_dir / f"{source.stem}.{fmt}" for fmt in FORMATS]
    return all(o.exists() and o.stat().st_mtime >= source.stat().st_mtime for o in outputs)


def kill_tree(proc: subprocess.Popen[bytes]) -> None:
    """Kończy proces wraz z potomkami (np. Chromium uruchomionym przez Node)."""
    if os.name == "nt":
        subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)], capture_output=True, check=False)
    else:
        os.killpg(proc.pid, signal.SIGKILL)


def run_with_timeout(cmd: list[str], timeout_s: int, cwd: Path) -> tuple[int, str]:
    """Uruchamia polecenie i zwraca (kod wyjścia, połączone stdout+stderr).

    Gwarantuje: po przekroczeniu `timeout_s` całe drzewo procesów zostaje zabite.
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
        raise RenderError(f"nie znaleziono programu: {cmd[0]}") from exc
    try:
        out, _ = proc.communicate(timeout=timeout_s)
    except subprocess.TimeoutExpired as exc:
        kill_tree(proc)
        proc.communicate()
        raise RenderError(f"przekroczono limit {timeout_s} s: {' '.join(cmd[:3])} …") from exc
    return proc.returncode, out.decode("utf-8", errors="replace")


def render_plantuml(sources: list[Path], out_dir: Path) -> None:
    """Renderuje pliki .puml do `out_dir/<nazwa>.svg` i `.png`.

    Wymaga: Javy w PATH (lub JAVA) i plantuml.jar. Gwarantuje: nazwa wyniku = nazwa pliku źródłowego.
    Wyjątki: RenderError, gdy PlantUML zgłosi błąd składni, zabraknie pliku wyniku lub minie limit czasu.
    """
    if not sources:
        return
    jar = plantuml_jar()
    if not jar.is_file():
        raise RenderError(f"brak PlantUML: {jar} (ustaw PLANTUML_JAR)")
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
        raise RenderError(f"brak bpmn-to-image: {cli} (ustaw DK_NODE_MODULES)")
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
        cmd.append(f"{src};{outputs}")
    timeout_s = BPMN_BASE_TIMEOUT_S + BPMN_PER_FILE_TIMEOUT_S * len(sources)
    code, out = run_with_timeout(cmd, timeout_s, REPO)
    if code != 0:
        raise RenderError(f"bpmn-to-image zakończył się kodem {code}:\n{out.strip()}")
    missing = [f"{s.stem}.{f}" for s in sources for f in FORMATS if not (out_dir / f"{s.stem}.{f}").exists()]
    if missing:
        raise RenderError("bpmn-to-image nie utworzył: " + ", ".join(missing))


def render_all(docs: Path, out_dir: Path, force: bool) -> list[Path]:
    """Renderuje wszystkie nieaktualne diagramy z `docs` do `out_dir` i zwraca listę wyrenderowanych źródeł.

    Gwarantuje: `out_dir` istnieje po wywołaniu; aktualne wyniki są pomijane, chyba że `force`.
    Wyjątki: RenderError przy pierwszym nieudanym renderze lub powtórzonych nazwach.
    """
    puml = find_sources(docs, ".puml")
    bpmn = find_sources(docs, ".bpmn")
    ensure_unique_names(puml + bpmn)
    out_dir.mkdir(parents=True, exist_ok=True)
    todo_puml = [p for p in puml if force or not is_fresh(p, out_dir)]
    todo_bpmn = [p for p in bpmn if force or not is_fresh(p, out_dir)]
    render_plantuml(todo_puml, out_dir)
    render_bpmn(todo_bpmn, out_dir)
    return todo_puml + todo_bpmn


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
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 0 przy sukcesie, 1 przy błędzie renderu."""
    use_utf8_output()
    args = parse_args(argv)
    docs = args.docs.resolve()
    out_dir = (args.out or docs / "_generated").resolve()
    try:
        done = render_all(docs, out_dir, args.force)
    except RenderError as exc:
        print(f"render: BŁĄD: {exc}", file=sys.stderr)
        return 1
    for src in done:
        print(f"render: {src.relative_to(docs).as_posix()} -> {out_dir.name}/{src.stem}.{{svg,png}}")
    print(f"render: OK ({len(done)} wyrenderowano)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
