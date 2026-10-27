"""Dostępny Kampus — generator skrótów (tooltipów MkDocs) ze słownika pojęć. Author: Vladyslav Yevkov. Version: 0.1.0. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_GLOSSARY = REPO / "docs" / "biznes" / "slownik.md"
DEFAULT_OUTPUT = REPO / "docs" / "includes" / "slownik-skroty.md"
TERM_HEADER = "termin"
DEFINITION_HEADER = "definicja"
SEPARATOR_CELL = re.compile(r"^:?-{3,}:?$")
LINK = re.compile(r"!?\[([^\]]*)\]\([^)]*\)")
FOOTNOTE = re.compile(r"\[\^[^\]]+\]")
HTML_TAG = re.compile(r"<[^>]+>")
EMPHASIS = re.compile(r"(\*\*|__|\*|_|`)")
SPACES = re.compile(r"\s+")


def split_row(line: str) -> list[str] | None:
    """Dzieli wiersz tabeli Markdown na komórki.

    Gwarantuje: listę komórek bez skrajnych spacji albo None, gdy wiersz nie jest wierszem tabeli.
    Uwzględnia znak `\\|` jako pionową kreskę wewnątrz komórki.
    """
    stripped = line.strip()
    if not stripped.startswith("|"):
        return None
    body = stripped.strip("|")
    cells = re.split(r"(?<!\\)\|", body)
    return [c.strip().replace("\\|", "|") for c in cells]


def is_separator(cells: list[str]) -> bool:
    """Zwraca True, gdy komórki tworzą wiersz separatora `|---|---|`."""
    return bool(cells) and all(SEPARATOR_CELL.match(c.replace(" ", "")) for c in cells)


def plain_text(cell: str) -> str:
    """Zwraca tekst komórki bez linków, przypisów, znaczników HTML i formatowania, w jednej linii."""
    text = LINK.sub(r"\1", cell)
    text = FOOTNOTE.sub("", text)
    text = HTML_TAG.sub(" ", text)
    text = EMPHASIS.sub("", text)
    return SPACES.sub(" ", text).strip()


def find_columns(header: list[str]) -> tuple[int, int] | None:
    """Zwraca indeksy kolumn (Termin, Definicja) z nagłówka tabeli albo None, gdy ich brak."""
    names = [plain_text(c).lower() for c in header]
    if TERM_HEADER in names and DEFINITION_HEADER in names:
        return names.index(TERM_HEADER), names.index(DEFINITION_HEADER)
    return None


def parse_glossary(text: str) -> list[tuple[str, str]]:
    """Wyciąga pary (termin, definicja) ze wszystkich tabel z kolumnami `Termin` i `Definicja`.

    Gwarantuje: kolejność jak w pliku, bez powtórzeń terminów (wygrywa pierwsze wystąpienie),
    bez wierszy z pustym terminem lub definicją. Pomija bloki kodu.
    """
    pairs: dict[str, str] = {}
    columns: tuple[int, int] | None = None
    in_code = False
    for line in text.splitlines():
        if line.lstrip().startswith(("```", "~~~")):
            in_code = not in_code
            continue
        cells = None if in_code else split_row(line)
        if cells is None:
            columns = None
            continue
        if columns is None:
            columns = find_columns(cells) or (-1, -1)
            continue
        add_pair(pairs, cells, columns)
    return list(pairs.items())


def add_pair(pairs: dict[str, str], cells: list[str], columns: tuple[int, int]) -> None:
    """Dodaje parę z wiersza danych do `pairs`, jeśli wiersz należy do tabeli słownika i jest kompletny."""
    term_i, def_i = columns
    if term_i < 0 or is_separator(cells) or max(term_i, def_i) >= len(cells):
        return
    term, definition = plain_text(cells[term_i]), plain_text(cells[def_i])
    if term and definition and term not in pairs:
        pairs[term] = definition


def render_abbreviations(pairs: list[tuple[str, str]]) -> str:
    """Zwraca treść pliku skrótów: linie `*[Termin]: Definicja` zakończone znakiem nowej linii lub pusty tekst.

    Gwarantuje: nawiasy kwadratowe w terminie są zamienione na okrągłe, by nie łamać składni.
    """
    lines = [f"*[{t.replace('[', '(').replace(']', ')')}]: {d}" for t, d in pairs]
    return "".join(f"{line}\n" for line in lines)


def generate(glossary: Path, output: Path) -> int:
    """Generuje plik skrótów `output` ze słownika `glossary` i zwraca liczbę terminów.

    Gwarantuje: brak słownika daje pusty plik wynikowy (bez błędu); plik jest zapisywany tylko przy zmianie treści.
    Wyjątki: OSError przy problemach z odczytem lub zapisem, UnicodeDecodeError przy słowniku spoza UTF-8.
    """
    text = glossary.read_text(encoding="utf-8") if glossary.is_file() else ""
    pairs = parse_glossary(text)
    content = render_abbreviations(pairs)
    current = output.read_text(encoding="utf-8") if output.is_file() else None
    if current != content:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(content, encoding="utf-8", newline="\n")
    return len(pairs)


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    """Parsuje argumenty wiersza poleceń."""
    parser = argparse.ArgumentParser(description="Generuje docs/includes/slownik-skroty.md ze słownika pojęć.")
    parser.add_argument("--glossary", type=Path, default=DEFAULT_GLOSSARY, help="plik słownika")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUTPUT, help="plik wynikowy")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 0 przy sukcesie, 1 przy błędzie odczytu słownika lub zapisu wyniku."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    try:
        count = generate(args.glossary, args.out)
    except (OSError, UnicodeDecodeError) as exc:
        print(f"abbr: BŁĄD: {exc}", file=sys.stderr)
        return 1
    print(f"abbr: OK ({count} terminów -> {args.out.name})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
