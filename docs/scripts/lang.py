"""Dostępny Kampus — kontrola języka polskiego (LanguageTool, pl-PL) w plikach Markdown. Author: Vladyslav Yevkov. Version: 0.1.1. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import re
import sys
from bisect import bisect_right
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from check_ids import BLOCK_MARKER, BLOCK_NAMES, ID_BODY

REPO = Path(__file__).resolve().parent.parent
GLOSSARY = REPO / "docs" / "biznes" / "slownik.md"
IGNORE_FILE = REPO / "scripts" / "lang-ignore.txt"
IGNORE_RULE_PREFIX = "reguła:"
LANGUAGE = "pl-PL"
SEGMENT_GAP = "\n\n"
ID_RE = re.compile(rf"(?<![\w-])(?:{ID_BODY})(?![\w-])")
CODE = re.compile(r"(?<![\w-])(?:[A-ZĄĆĘŁŃÓŚŹŻ]{1,6}-?\d+(?:\.\d+)*|[A-Z]{1,6}-[A-Z]{2,})(?![\w-])")
PLACEHOLDER = "X"
ACRONYM_MAX = 6
FENCE = re.compile(r"^\s*(```|~~~)")
HEADING = re.compile(r"^#{1,6}\s+")
HEADING_LEVEL = re.compile(r"^(#{1,6})\s")
SOURCES_HEADING = re.compile(r"^#{1,6}\s+(?:Źródła|Bibliografia|Literatura)\s*$", re.IGNORECASE)
LIST_MARKER = re.compile(r"^\s*(?:[-*+]|\d+[.)])\s+(?:\[[ xX]\]\s+)?")
QUOTE = re.compile(r"^\s*>\s?")
SEPARATOR_CELL = re.compile(r"^:?-{3,}:?$")
SKIP_LINE = re.compile(r"^\s*(?:\*\[[^\]]+\]:|\[\^?[^\]]+\]:|--8<--|!!!|\?\?\?|<!--|-->|\{:|---\s*$)")
IMAGE = re.compile(r"!\[[^\]]*\]\([^)]*\)")
LINK = re.compile(r"\[([^\]]*)\]\([^)]*\)")
REF_LINK = re.compile(r"\[([^\]]*)\]\[[^\]]*\]")
FOOTNOTE = re.compile(r"\[\^[^\]]+\]")
URL = re.compile(r"<?https?://[^\s>)]+>?")
FILE_OR_DOMAIN = re.compile(
    r"(?<![\w.])[\w-]+(?:\.[\w-]+)*\.(?:pl|com|org|net|eu|io|edu|gov|pdf|md|ya?ml|json|geojson|csv|png|svg|bpmn|puml)(?![\w.])",
    re.IGNORECASE,
)
INLINE_CODE = re.compile(r"`[^`]*`")
HTML = re.compile(r"<!--.*?-->|<[^>]+>")
ATTR_LIST = re.compile(r"\{[:#.][^}]*\}")
EMPHASIS = re.compile(r"\*\*|__|(?<!\w)[*_]|[*_](?!\w)")
SPACES = re.compile(r"[ \t]{2,}")
SPACE_BEFORE_PUNCT = re.compile(r"\s+([,.;:!?)])")
FRAGMENT_RULES = frozenset({"UPPERCASE_SENTENCE_START", "PUNCTUATION_PARAGRAPH_END"})


@dataclass(frozen=True)
class Segment:
    """Fragment tekstu do sprawdzenia: numer linii źródła, tekst po oczyszczeniu i czy to fragment (komórka, punkt listy)."""

    line: int
    text: str
    fragment: bool


@dataclass(frozen=True)
class Issue:
    """Błąd językowy: plik, linia, reguła, komunikat i fragment tekstu."""

    path: str
    line: int
    rule: str
    message: str
    excerpt: str

    def __str__(self) -> str:
        return f"{self.path}:{self.line}: {self.rule}: {self.message} — „{self.excerpt}”"


def strip_inline(text: str) -> str:
    """Usuwa z linii elementy, których nie sprawdza się językowo: kod, obrazy, adresy, HTML; z linków zostaje tekst.

    Gwarantuje: ID, kody (np. US1.2, BR-G1, V3, A-1, RO-GOS), nazwy plików i domen są zastąpione słowem
    PLACEHOLDER, by nie psuć interpunkcji; wynik nie ma podwójnych spacji ani spacji przed znakami interpunkcyjnymi.
    """
    for pattern, repl in (
        (INLINE_CODE, " "),
        (IMAGE, " "),
        (LINK, r"\1"),
        (REF_LINK, r"\1"),
        (FOOTNOTE, ""),
        (URL, " "),
        (FILE_OR_DOMAIN, PLACEHOLDER),
        (HTML, " "),
        (ATTR_LIST, " "),
        (ID_RE, PLACEHOLDER),
        (CODE, PLACEHOLDER),
        (EMPHASIS, ""),
    ):
        text = pattern.sub(repl, text)
    text = SPACE_BEFORE_PUNCT.sub(r"\1", SPACES.sub(" ", text))
    return text.strip()


def table_cells(line: str) -> list[str] | None:
    """Zwraca komórki wiersza tabeli (pusta lista dla separatora) albo None, gdy to nie wiersz tabeli."""
    stripped = line.strip()
    if not stripped.startswith("|"):
        return None
    cells = [c.strip() for c in re.split(r"(?<!\\)\|", stripped.strip("|"))]
    if all(SEPARATOR_CELL.match(c.replace(" ", "")) for c in cells):
        return []
    return cells


def line_segments(number: int, line: str) -> list[Segment]:
    """Zamienia jedną linię Markdown na segmenty do sprawdzenia (zero lub więcej).

    Gwarantuje: komórki tabel i punkty list są oznaczone jako fragmenty; puste segmenty są pomijane.
    """
    if not line.strip() or SKIP_LINE.match(line):
        return []
    cells = table_cells(line)
    if cells is not None:
        parts = [(strip_inline(c), True) for c in cells]
    else:
        is_item = bool(LIST_MARKER.match(line))
        body = LIST_MARKER.sub("", HEADING.sub("", QUOTE.sub("", line)))
        parts = [(strip_inline(body), is_item or bool(HEADING.match(line)))]
    return [Segment(number, text, fragment) for text, fragment in parts if re.search(r"\w", text)]


def extract_segments(text: str) -> list[Segment]:
    """Zwraca segmenty z całego dokumentu z pominięciem front matter, bloków kodu, bloków wygenerowanych przez
    check_ids (macierz śledzenia) i sekcji „Źródła” (tytuły źródeł są w języku oryginału)."""
    lines = text.splitlines()
    start = front_matter_end(lines)
    segments: list[Segment] = []
    in_code = in_block = False
    sources: int | None = None
    for number, line in enumerate(lines[start:], start=start + 1):
        marker = BLOCK_MARKER.search(line)
        if FENCE.match(line):
            in_code = not in_code
        elif marker and marker.group(1) in BLOCK_NAMES:
            in_block = marker.group(2) == "start"
        elif not (in_code or in_block):
            sources = sources_level(line, sources)
            segments += line_segments(number, line) if sources is None else []
    return segments


def sources_level(line: str, current: int | None) -> int | None:
    """Zwraca poziom nagłówka otwartej sekcji źródeł po przeczytaniu `line` albo None poza taką sekcją.

    Gwarantuje: sekcja trwa do najbliższego nagłówka tego samego lub wyższego poziomu.
    """
    heading = HEADING_LEVEL.match(line)
    if not heading:
        return current
    level = len(heading.group(1))
    if current is not None and level > current:
        return current
    return level if SOURCES_HEADING.match(line) else None


def front_matter_end(lines: list[str]) -> int:
    """Zwraca indeks pierwszej linii po front matter YAML albo 0, gdy go nie ma."""
    if not lines or lines[0].strip() != "---":
        return 0
    for index, line in enumerate(lines[1:], start=1):
        if line.strip() == "---":
            return index + 1
    return 0


def join_segments(segments: list[Segment]) -> tuple[str, list[int]]:
    """Łączy segmenty w jeden tekst (rozdzielony pustą linią) i zwraca go razem z przesunięciami początków segmentów."""
    offsets: list[int] = []
    position = 0
    for segment in segments:
        offsets.append(position)
        position += len(segment.text) + len(SEGMENT_GAP)
    return SEGMENT_GAP.join(s.text for s in segments), offsets


def glossary_words(path: Path) -> list[str]:
    """Zwraca słowa z kolumny „Termin” słownika pojęć (do pominięcia w kontroli pisowni). Brak pliku = pusta lista."""
    if not path.is_file():
        return []
    words: set[str] = set()
    header_seen = False
    for line in path.read_text(encoding="utf-8").splitlines():
        cells = table_cells(line)
        if cells is None:
            header_seen = False
        elif cells and header_seen:
            words.update(re.findall(r"[^\W\d_]{2,}", strip_inline(cells[0])))
        elif cells:
            header_seen = strip_inline(cells[0]).lower() == "termin"
    return sorted(words)


def load_ignore(path: Path) -> tuple[frozenset[str], list[str]]:
    """Zwraca (reguły, słowa) z pliku wyjątków LanguageTool.

    Wymaga: UTF-8; linia `reguła: ID` wyłącza regułę, linia od `#` to opis, każda inna niepusta linia to słowo.
    Gwarantuje: brak pliku daje puste zbiory. Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    if not path.is_file():
        return frozenset(), []
    rules: set[str] = set()
    words: list[str] = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line.lower().startswith(IGNORE_RULE_PREFIX):
            rules.add(line[len(IGNORE_RULE_PREFIX) :].strip())
        elif line and not line.startswith("#"):
            words.append(line)
    return frozenset(rules), words


def match_attr(match: Any, *names: str) -> Any:
    """Zwraca pierwszy istniejący atrybut obiektu dopasowania (zgodność nazw między wersjami language_tool_python)."""
    for name in names:
        if hasattr(match, name):
            return getattr(match, name)
    raise AttributeError(names[0])


def check_file(tool: Any, path: Path, disabled: frozenset[str], known: frozenset[str]) -> list[Issue]:
    """Sprawdza jeden plik i zwraca listę błędów z numerami linii źródła.

    Wymaga: działającego obiektu LanguageTool. Gwarantuje: reguły z `disabled` są pomijane, a reguły typowe dla
    zdań (wielka litera, kropka na końcu) nie są zgłaszane dla komórek tabel, punktów list i nagłówków;
    błędy, których fragmentem jest słowo z `known` (małe litery: terminy słownika i wyjątki), są pomijane.
    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    segments = extract_segments(path.read_text(encoding="utf-8"))
    if not segments:
        return []
    text, offsets = join_segments(segments)
    issues: list[Issue] = []
    for match in tool.check(text):
        rule = match_attr(match, "rule_id", "ruleId")
        offset = match_attr(match, "offset")
        length = match_attr(match, "error_length", "errorLength")
        segment = segments[bisect_right(offsets, offset) - 1]
        excerpt = text[offset : offset + length] or segment.text[:40]
        if is_ignored(rule, excerpt, segment, disabled, known):
            continue
        issues.append(Issue(display_path(path), segment.line, rule, match_attr(match, "message"), excerpt))
    return issues


def is_ignored(rule: str, excerpt: str, segment: Segment, disabled: frozenset[str], known: frozenset[str]) -> bool:
    """Zwraca True dla dopasowań do pominięcia: reguła wyłączona, termin słownika lub wyjątek (bez względu na wielkość
    liter), sam zastępnik ID, skrótowiec wielkimi literami w regule pisowni albo reguła zdaniowa we fragmencie."""
    if rule in disabled or excerpt.lower() in known:
        return True
    if not re.sub(rf"{PLACEHOLDER}|[\W_]", "", excerpt):
        return True
    if rule.startswith("MORFOLOGIK") and excerpt.isupper() and len(excerpt) <= ACRONYM_MAX:
        return True
    return segment.fragment and rule in FRAGMENT_RULES


def display_path(path: Path) -> str:
    """Zwraca ścieżkę względną do repozytorium (lub bezwzględną poza nim) w formacie POSIX."""
    try:
        return path.resolve().relative_to(REPO).as_posix()
    except ValueError:
        return path.resolve().as_posix()


def open_tool(extra_words: list[str]) -> Any:
    """Uruchamia lokalny serwer LanguageTool dla pl-PL (pierwsze wywołanie pobiera serwer, wymaga Javy).

    Gwarantuje: słowa z `extra_words` nie są zgłaszane jako literówki i nie są trwale zapisywane.
    Wyjątki: ImportError bez pakietu language_tool_python; błędy pakietu przy braku Javy lub sieci.
    """
    import language_tool_python

    return language_tool_python.LanguageTool(LANGUAGE, new_spellings=extra_words or None, new_spellings_persist=False)


def run(paths: list[Path], disabled: frozenset[str], extra_words: list[str]) -> list[Issue]:
    """Sprawdza pliki z `paths` (pomijając nieistniejące) i zwraca wszystkie błędy.

    Gwarantuje: terminy słownika i `extra_words` nie są zgłaszane. Wyjątki: jak open_tool i check_file.
    """
    files = [p for p in paths if p.is_file()]
    if not files:
        return []
    words = glossary_words(GLOSSARY) + extra_words
    known = frozenset(w.lower() for w in words)
    tool = open_tool(words)
    try:
        return [issue for path in files for issue in check_file(tool, path, disabled, known)]
    finally:
        tool.close()


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    """Parsuje argumenty wiersza poleceń."""
    parser = argparse.ArgumentParser(description="Sprawdza polszczyznę w plikach .md przez lokalny LanguageTool.")
    parser.add_argument("files", nargs="*", type=Path, help="pliki .md do sprawdzenia")
    parser.add_argument("--disable", default="", help="dodatkowe reguły do pominięcia, rozdzielone przecinkami")
    parser.add_argument("--ignore-file", type=Path, default=IGNORE_FILE, help="plik wyjątków (reguły i słowa)")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 0 bez błędów, 1 gdy są błędy językowe, 2 gdy LanguageTool nie działa."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    try:
        rules, words = load_ignore(args.ignore_file)
        disabled = rules | {r.strip() for r in args.disable.split(",") if r.strip()}
        issues = run(args.files, frozenset(disabled), words)
    except Exception as exc:
        print(f"lang: BŁĄD LanguageTool: {exc}", file=sys.stderr)
        return 2
    for issue in issues:
        print(issue)
    print(f"lang: {len(args.files)} plików, błędów {len(issues)}")
    return 1 if issues else 0


if __name__ == "__main__":
    sys.exit(main())
