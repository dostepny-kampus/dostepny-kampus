"""Dostępny Kampus — pełna kontrola repozytorium dokumentacji (render, skróty, Vale, markdownlint, LanguageTool, ID, macierz śledzenia, MkDocs). Author: Vladyslav Yevkov. Version: 0.2.0. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import functools
from contextlib import contextmanager
import os
import posixpath
import re
import subprocess
import sys
import tempfile
import threading
from dataclasses import dataclass
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Callable, Iterator

import yaml

from render import kill_tree, node_modules_dir

REPO = Path(__file__).resolve().parent.parent
SCRIPTS = REPO / "scripts"
DOCS = REPO / "docs"
CONFIG = REPO / "mkdocs.yml"
SITE = REPO / "site"
MATRIX_PAGE = DOCS / "wymagania" / "sledzenie.md"
DEFAULT_VALE = Path("tools/vale/vale.exe")
EXCLUDED_DIRS = ("includes", "_generated")
SCREEN_WIDTHS = (1280, 390)
SCREEN_HEIGHT = 900
STEP_TIMEOUT_S = 900
TIMEOUT_CODE = 124
CHECK_IDS_SUMMARY = re.compile(r"check_ids: błędów (\d+), uwag (\d+)")
LINK_NOT_FOUND = re.compile(r"Doc file '([^']+)' contains a link (?:to )?'([^']+)'.*not found")
PASS, FAIL, WARN, SKIP = "PASS", "FAIL", "WARN", "SKIP"


@dataclass(frozen=True)
class StepResult:
    """Wynik jednego kroku kontroli: nazwa, status (PASS/FAIL/WARN/SKIP) i krótki opis."""

    name: str
    status: str
    detail: str = ""


@dataclass(frozen=True)
class Options:
    """Ustawienia uruchomienia verify."""

    files: list[Path]
    lang_all: bool
    partial: bool
    screens: Path | None


def run_command(cmd: list[str], capture: bool = False, timeout_s: int = STEP_TIMEOUT_S) -> tuple[int, str]:
    """Uruchamia polecenie w katalogu repozytorium i zwraca (kod wyjścia, wyjście).

    Gwarantuje: bez `capture` wyjście trafia na bieżąco na konsolę, a zwracany tekst jest pusty;
    z `capture` wyjście jest zwracane i również wypisywane. Brak programu daje kod 127 i komunikat.
    Po `timeout_s` sekundach albo po Ctrl+C całe drzewo procesów jest zabijane; przekroczenie czasu daje kod 124.
    """
    pipe = subprocess.PIPE if capture else None
    try:
        proc = subprocess.Popen(
            cmd, cwd=REPO, stdout=pipe, stderr=subprocess.STDOUT if capture else None,
            text=True, encoding="utf-8", errors="replace", start_new_session=os.name != "nt",
        )
    except FileNotFoundError:
        return 127, f"nie znaleziono programu: {cmd[0]}"
    try:
        output = proc.communicate(timeout=timeout_s)[0] or ""
    except subprocess.TimeoutExpired:
        kill_tree(proc)
        proc.communicate()
        print(f"verify: przekroczono limit {timeout_s} s: {Path(cmd[0]).name}")
        return TIMEOUT_CODE, ""
    except KeyboardInterrupt:
        kill_tree(proc)
        raise
    print(output, end="" if output.endswith("\n") or not output else "\n")
    return proc.returncode, output


def python_script(name: str, *args: str) -> list[str]:
    """Zwraca polecenie uruchamiające skrypt z katalogu scripts bieżącym interpreterem Pythona."""
    return [sys.executable, str(SCRIPTS / name), *args]


def doc_files() -> list[Path]:
    """Zwraca posortowane pliki docs/**/*.md z pominięciem katalogów includes i _generated."""
    if not DOCS.is_dir():
        return []
    return sorted(p for p in DOCS.rglob("*.md") if not set(p.relative_to(DOCS).parts) & set(EXCLUDED_DIRS))


def git_lines(*args: str) -> list[str]:
    """Zwraca niepuste linie wyjścia polecenia git albo pustą listę, gdy git zakończy się błędem."""
    proc = subprocess.run(["git", *args], cwd=REPO, capture_output=True, text=True, encoding="utf-8", check=False)
    return [line.strip() for line in proc.stdout.splitlines() if line.strip()] if proc.returncode == 0 else []


def changed_doc_files() -> list[Path]:
    """Zwraca pliki dokumentacji zmienione względem punktu odejścia od `main` (commity, zmiany robocze i nowe pliki).

    Gwarantuje: tylko istniejące pliki z doc_files(); bez gałęzi `main` porównuje z HEAD.
    """
    base = (git_lines("merge-base", "HEAD", "main") or ["HEAD"])[0]
    names = git_lines("diff", "--name-only", "--diff-filter=d", base) + git_lines("ls-files", "--others", "--exclude-standard")
    candidates = {(REPO / n).resolve() for n in names}
    return [p for p in doc_files() if p.resolve() in candidates]


def step_render() -> StepResult:
    """Krok: render diagramów (scripts/render.py)."""
    code, _ = run_command(python_script("render.py"))
    return StepResult("render", PASS if code == 0 else FAIL, "" if code == 0 else f"kod {code}")


def step_abbr() -> StepResult:
    """Krok: generowanie skrótów ze słownika (scripts/abbr.py)."""
    code, _ = run_command(python_script("abbr.py"))
    return StepResult("abbr", PASS if code == 0 else FAIL, "" if code == 0 else f"kod {code}")


def step_vale() -> StepResult:
    """Krok: Vale dla wszystkich stron dokumentacji (ścieżka z VALE_BIN)."""
    files = doc_files()
    if not files:
        return StepResult("vale", SKIP, "brak plików .md")
    vale = os.environ.get("VALE_BIN", str(DEFAULT_VALE))
    code, _ = run_command([vale, *[str(f.relative_to(REPO)) for f in files]])
    return StepResult("vale", PASS if code == 0 else FAIL, f"{len(files)} plików" + ("" if code == 0 else f", kod {code}"))


def step_markdownlint() -> StepResult:
    """Krok: markdownlint-cli2 z konfiguracją repozytorium (node_modules z DK_NODE_MODULES)."""
    cli = node_modules_dir() / "markdownlint-cli2" / "markdownlint-cli2-bin.mjs"
    if not cli.is_file():
        return StepResult("markdownlint", FAIL, f"brak {cli}")
    code, _ = run_command([os.environ.get("NODE", "node"), str(cli), "docs/**/*.md"])
    return StepResult("markdownlint", PASS if code == 0 else FAIL, "" if code == 0 else f"kod {code}")


def lang_targets(options: Options) -> list[Path]:
    """Zwraca pliki do kontroli językowej: wszystkie (--lang-all), podane w argumentach albo zmienione."""
    if options.lang_all:
        return doc_files()
    if options.files:
        return [f.resolve() for f in options.files if f.suffix == ".md"]
    return changed_doc_files()


def step_lang(options: Options) -> StepResult:
    """Krok: LanguageTool (scripts/lang.py) dla plików z lang_targets.

    Gwarantuje: nigdy nie daje FAIL; znaleziska i niedziałający LanguageTool dają WARN, bo to podpowiedzi,
    a fałszywe alarmy wyłącza się w scripts/lang-ignore.txt.
    """
    files = lang_targets(options)
    if not files:
        return StepResult("lang", SKIP, "brak zmienionych lub podanych plików .md")
    code, _ = run_command(python_script("lang.py", *[str(f) for f in files]))
    if code == 0:
        return StepResult("lang", PASS, f"{len(files)} plików")
    reason = "znaleziska językowe (lista wyżej)" if code == 1 else f"LanguageTool nie działa, kod {code}"
    return StepResult("lang", WARN, f"{len(files)} plików — {reason}")


def step_check_ids(options: Options) -> StepResult:
    """Krok: kontrola ID (scripts/check_ids.py --strict, w trybie --partial także --partial).

    Gwarantuje: każdy błąd daje FAIL; same uwagi dają WARN. W trybie --partial błędy klas ID, które nie mają
    jeszcze żadnej definicji (strona jeszcze nie powstała), check_ids zgłasza jako uwagi.
    """
    flags = ["--strict", "--partial"] if options.partial else ["--strict"]
    code, output = run_command(python_script("check_ids.py", *flags), capture=True)
    summary = CHECK_IDS_SUMMARY.search(output)
    if code != 0:
        errors = summary.group(1) if summary else "?"
        return StepResult("check_ids", FAIL, f"błędów {errors}, kod {code}")
    warnings = int(summary.group(2)) if summary else 0
    return StepResult("check_ids", WARN, f"uwag {warnings}") if warnings else StepResult("check_ids", PASS)


def step_matrix(options: Options) -> StepResult:
    """Krok: aktualność macierzy śledzenia (scripts/check_ids.py --check-matrix docs/wymagania/sledzenie.md).

    Gwarantuje: macierz różna od wygenerowanej z bieżących dokumentów daje FAIL z poleceniem odświeżenia;
    brak strony daje SKIP w trybie --partial, a FAIL w pełnej kontroli; błąd odczytu daje FAIL z kodem wyjścia.
    """
    page = MATRIX_PAGE.relative_to(REPO).as_posix()
    if options.partial and not MATRIX_PAGE.is_file():
        return StepResult("matrix", SKIP, f"brak {page}")
    code, _ = run_command(python_script("check_ids.py", "--check-matrix", page))
    if code == 0:
        return StepResult("matrix", PASS)
    if code != 1:
        return StepResult("matrix", FAIL, f"błąd odczytu, kod {code}")
    return StepResult("matrix", FAIL, f"nieaktualna, odśwież: python scripts/check_ids.py --matrix {page}")


def tagged_loader() -> type[yaml.SafeLoader]:
    """Zwraca loader YAML, który czyta mkdocs.yml, zamieniając tagi `!!python/…` i `!ENV` na None."""

    class Loader(yaml.SafeLoader):
        """Bezpieczny loader YAML z obsługą tagów MkDocs."""

    def ignore(loader: yaml.SafeLoader, suffix: str, node: yaml.Node) -> None:
        """Zwraca None dla węzła z nieobsługiwanym tagiem."""
        return None

    Loader.add_multi_constructor("tag:yaml.org,2002:python/", ignore)
    Loader.add_multi_constructor("!", ignore)
    return Loader


def read_nav(config: Path) -> list[Any]:
    """Zwraca listę `nav` z pliku konfiguracji MkDocs (pusta lista, gdy jej brak).

    Wyjątki: OSError, yaml.YAMLError przy błędnym pliku.
    """
    data = yaml.load(config.read_text(encoding="utf-8"), Loader=tagged_loader()) or {}
    return data.get("nav") or []


def is_external(target: str) -> bool:
    """Zwraca True dla wpisów nawigacji będących adresami URL."""
    return bool(re.match(r"^[a-z][a-z0-9+.-]*://", target, re.IGNORECASE))


def prune_nav(items: list[Any], docs: Path, missing: set[str]) -> list[Any]:
    """Zwraca kopię nawigacji bez wpisów wskazujących na nieistniejące pliki i bez pustych sekcji.

    Gwarantuje: ścieżki usuniętych wpisów (względne do docs, POSIX) są dopisane do `missing`.
    """
    result: list[Any] = []
    for item in items:
        pruned = prune_item(item, docs, missing)
        if pruned is not None:
            result.append(pruned)
    return result


def prune_item(item: Any, docs: Path, missing: set[str]) -> Any:
    """Zwraca wpis nawigacji po przycięciu albo None, gdy nic z niego nie zostaje."""
    if isinstance(item, str):
        if is_external(item) or (docs / item).is_file():
            return item
        missing.add(item)
        return None
    if isinstance(item, dict):
        kept = {}
        for title, value in item.items():
            pruned = prune_nav(value, docs, missing) if isinstance(value, list) else prune_item(value, docs, missing)
            if pruned:
                kept[title] = pruned
        return kept or None
    return item


def partial_config(target: Path, missing: set[str]) -> None:
    """Zapisuje do `target` konfigurację dziedziczącą z mkdocs.yml z przyciętą nawigacją i `strict: false`.

    Gwarantuje: ścieżki docs_dir i site_dir są bezwzględne, więc plik może leżeć poza repozytorium.
    Wyjątki: OSError, yaml.YAMLError.
    """
    data = {
        "INHERIT": CONFIG.as_posix(),
        "docs_dir": DOCS.as_posix(),
        "site_dir": SITE.as_posix(),
        "strict": False,
        "nav": prune_nav(read_nav(CONFIG), DOCS, missing),
    }
    target.write_text(yaml.safe_dump(data, allow_unicode=True, sort_keys=False), encoding="utf-8")


def link_target(doc: str, link: str) -> str:
    """Zwraca ścieżkę celu linku względną do docs (POSIX), rozwiązaną względem strony `doc`, bez kotwicy."""
    target = link.split("#", 1)[0]
    return posixpath.normpath(posixpath.join(posixpath.dirname(doc), target))


def blocking_warnings(output: str, missing: set[str]) -> list[str]:
    """Zwraca linie WARNING/ERROR z wyjścia MkDocs poza ostrzeżeniami o linkach do planowanych, jeszcze nieistniejących stron."""
    result = []
    for line in output.splitlines():
        if not re.match(r"^\s*(WARNING|ERROR)\b", line):
            continue
        link = LINK_NOT_FOUND.search(line)
        if link and link_target(link.group(1), link.group(2)) in missing:
            continue
        result.append(line.strip())
    return result


def step_mkdocs(options: Options) -> StepResult:
    """Krok: `mkdocs build --strict`; w trybie --partial build z tymczasową konfiguracją (przycięta nawigacja).

    W trybie --partial ostrzeżenia o linkach do stron z nawigacji, które jeszcze nie istnieją, nie blokują wyniku.
    """
    if not options.partial:
        code, _ = run_command([sys.executable, "-m", "mkdocs", "build", "--strict", "-f", str(CONFIG)])
        return StepResult("mkdocs", PASS if code == 0 else FAIL, "" if code == 0 else f"kod {code}")
    missing: set[str] = set()
    with tempfile.TemporaryDirectory(prefix="dk-mkdocs-") as tmp:
        config = Path(tmp) / "mkdocs-partial.yml"
        partial_config(config, missing)
        code, output = run_command([sys.executable, "-m", "mkdocs", "build", "-f", str(config)], capture=True)
    blocking = blocking_warnings(output, missing)
    detail = f"pominięto w nawigacji: {len(missing)}"
    if code != 0 or blocking:
        return StepResult("mkdocs", FAIL, f"{detail}; ostrzeżeń: {len(blocking)}, kod {code}")
    return StepResult("mkdocs", PASS, detail)


def site_pages(site: Path) -> list[str]:
    """Zwraca ścieżki URL (względne) wszystkich stron HTML zbudowanego serwisu poza 404.html."""
    pages = []
    for html in sorted(site.rglob("*.html")):
        rel = html.relative_to(site).as_posix()
        if rel != "404.html":
            pages.append(rel[: -len("index.html")] if rel.endswith("index.html") else rel)
    return pages


def screenshot_name(url_path: str, width: int) -> str:
    """Zwraca nazwę pliku zrzutu, np. `biznes__wizja__1280.png` (strona główna: `index__1280.png`)."""
    slug = url_path.strip("/").removesuffix(".html").replace("/", "__") or "index"
    return f"{slug}__{width}.png"


@functools.cache
def quiet_handler(directory: str) -> type[SimpleHTTPRequestHandler]:
    """Zwraca klasę handlera HTTP serwującą `directory` bez logowania żądań."""

    class Handler(SimpleHTTPRequestHandler):
        """Handler plików statycznych z katalogu `directory`."""

        def __init__(self, *args: Any, **kwargs: Any) -> None:
            super().__init__(*args, directory=directory, **kwargs)

        def log_message(self, format: str, *args: Any) -> None:
            """Wyłącza logowanie żądań."""
            return None

    return Handler


@contextmanager
def serve(site: Path) -> Iterator[str]:
    """Menedżer kontekstu: uruchamia lokalny serwer HTTP dla `site` na wolnym porcie i udostępnia jego adres bazowy.

    Gwarantuje: serwer jest zatrzymywany przy wyjściu z bloku `with`, także po wyjątku.
    """
    server = ThreadingHTTPServer(("127.0.0.1", 0), quiet_handler(str(site)))
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        yield f"http://127.0.0.1:{server.server_address[1]}/"
    finally:
        server.shutdown()
        server.server_close()


def take_screenshots(site: Path, out_dir: Path) -> int:
    """Robi pełnostronicowe zrzuty każdej strony w szerokościach SCREEN_WIDTHS i zwraca liczbę plików.

    Wymaga: zbudowanego serwisu i przeglądarki Chromium dla Playwright. Wyjątki: błędy Playwright.
    """
    from playwright.sync_api import sync_playwright

    out_dir.mkdir(parents=True, exist_ok=True)
    pages = site_pages(site)
    count = 0
    with serve(site) as base, sync_playwright() as pw:
        browser = pw.chromium.launch()
        for width in SCREEN_WIDTHS:
            context = browser.new_context(viewport={"width": width, "height": SCREEN_HEIGHT})
            page = context.new_page()
            for url_path in pages:
                page.goto(base + url_path, wait_until="networkidle")
                page.screenshot(path=str(out_dir / screenshot_name(url_path, width)), full_page=True)
                count += 1
            context.close()
        browser.close()
    return count


def step_screens(out_dir: Path) -> StepResult:
    """Krok: zrzuty ekranu zbudowanego serwisu do katalogu `out_dir` (musi leżeć poza repozytorium)."""
    target = out_dir.resolve()
    if target == REPO or REPO in target.parents:
        return StepResult("screens", FAIL, "katalog zrzutów musi leżeć poza repozytorium")
    if not (SITE / "index.html").is_file() and not site_pages(SITE):
        return StepResult("screens", SKIP, "brak zbudowanych stron")
    try:
        count = take_screenshots(SITE, target)
    except Exception as exc:
        return StepResult("screens", FAIL, f"Playwright: {exc}")
    return StepResult("screens", PASS, f"{count} zrzutów w {target}")


def run_steps(options: Options) -> list[StepResult]:
    """Uruchamia wszystkie kroki po kolei (także po porażce lub wyjątku wcześniejszego) i zwraca ich wyniki.

    Gwarantuje: nieoczekiwany wyjątek kroku daje FAIL tego kroku zamiast przerwania całej kontroli.
    """
    steps: list[tuple[str, Callable[[], StepResult]]] = [
        ("render", step_render),
        ("abbr", step_abbr),
        ("vale", step_vale),
        ("markdownlint", step_markdownlint),
        ("lang", lambda: step_lang(options)),
        ("check_ids", lambda: step_check_ids(options)),
        ("matrix", lambda: step_matrix(options)),
        ("mkdocs", lambda: step_mkdocs(options)),
    ]
    if options.screens:
        steps.append(("screens", lambda: step_screens(options.screens)))
    results = []
    for name, step in steps:
        result = run_step(name, step)
        print(f"verify: {result.name}: {result.status} {result.detail}".rstrip())
        results.append(result)
    return results


def run_step(name: str, step: Callable[[], StepResult]) -> StepResult:
    """Wykonuje jeden krok i zwraca jego wynik; wyjątek (poza Ctrl+C) zamienia na FAIL z opisem."""
    try:
        return step()
    except Exception as exc:
        return StepResult(name, FAIL, f"wyjątek: {type(exc).__name__}: {exc}")


def print_summary(results: list[StepResult]) -> None:
    """Wypisuje tabelę wyników: krok, status, szczegóły."""
    width = max(len(r.name) for r in results)
    print("\n" + "=" * 60)
    print(f"{'Krok'.ljust(width)}  Wynik  Szczegóły")
    print("-" * 60)
    for r in results:
        print(f"{r.name.ljust(width)}  {r.status.ljust(5)}  {r.detail}")
    print("=" * 60)


def parse_args(argv: list[str] | None) -> Options:
    """Parsuje argumenty wiersza poleceń i zwraca Options."""
    parser = argparse.ArgumentParser(description="Uruchamia wszystkie kontrole dokumentacji i build MkDocs.")
    parser.add_argument("files", nargs="*", type=Path, help="pliki .md do kontroli językowej (domyślnie zmienione)")
    parser.add_argument("--lang-all", action="store_true", help="kontrola językowa wszystkich stron")
    parser.add_argument("--partial", action="store_true", help="pomiń w nawigacji nieistniejące strony")
    parser.add_argument("--screens", type=Path, help="katalog (poza repo) na zrzuty ekranu stron")
    args = parser.parse_args(argv)
    return Options(args.files, args.lang_all, args.partial, args.screens)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 0, gdy żaden krok nie ma statusu FAIL, w przeciwnym razie 1."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
    os.environ.setdefault("PYTHONIOENCODING", "utf-8")
    os.environ.setdefault("NO_MKDOCS_2_WARNING", "1")
    results = run_steps(parse_args(argv))
    print_summary(results)
    return 1 if any(r.status == FAIL for r in results) else 0


if __name__ == "__main__":
    sys.exit(main())
