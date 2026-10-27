"""Dostępny Kampus — kontrola identyfikatorów (N, US, AC, UC, BR, NFR, R, M, TB) i macierz śledzenia. Author: Vladyslav Yevkov. Version: 0.1.0. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
ID_BODY = r"US\d+\.\d+|US\d+|UC\d+|BR-G\d+|BR-\d+|NFR-\d+|TB-\d+|R-\d+|M-\d+|N\d+"
ID_RE = re.compile(rf"(?<![\w-])(?:{ID_BODY})(?![\w-]|\.\d)")
ID_FULL = re.compile(rf"(?:{ID_BODY})")
AC_RE = re.compile(r"^US\d+\.\d+$")
US_RE = re.compile(r"^US\d+$")
UC_RE = re.compile(r"^UC\d+$")
N_RE = re.compile(r"^N\d+$")
HEADING = re.compile(r"^(#{1,6})\s+(.*)$")
FENCE = re.compile(r"^\s*(```|~~~)")
LINK_TARGET = re.compile(r"\]\([^)]*\)")
URL = re.compile(r"https?://\S+")
INLINE_CODE = re.compile(r"`[^`]*`")
HTML_COMMENT = re.compile(r"<!--.*?-->")
LEADING_NOISE = re.compile(r"^[\s*_\[`<>]*(?:<[^>]+>)?[\s*_\[`]*")
SEPARATOR_CELL = re.compile(r"^:?-{3,}:?$")
TAG_RE = re.compile(r"@([\w.\-]+)")
MATRIX_START = "<!-- macierz:start -->"
MATRIX_END = "<!-- macierz:end -->"
FEATURE_KEYWORDS = ("Feature", "Funkcja", "Funkcjonalność", "Właściwość", "Aspekt", "Potrzeba biznesowa")
RULE_KEYWORDS = ("Rule", "Reguła", "Zasada")
SCENARIO_KEYWORDS = ("Scenario Outline", "Scenario Template", "Scenario", "Example", "Scenariusz", "Szablon scenariusza", "Przykład")
EXAMPLES_KEYWORDS = ("Examples", "Scenarios", "Przykłady")


@dataclass(frozen=True)
class Location:
    """Miejsce w pliku: ścieżka względna do repozytorium i numer linii (od 1)."""

    path: str
    line: int

    def __str__(self) -> str:
        return f"{self.path}:{self.line}"


@dataclass(frozen=True)
class Scenario:
    """Scenariusz Gherkin z nazwą i miejscem definicji."""

    name: str
    location: Location


@dataclass
class Corpus:
    """Zebrane definicje, odwołania, powiązania (nieskierowane) i scenariusze."""

    definitions: dict[str, list[Location]] = field(default_factory=lambda: defaultdict(list))
    references: list[tuple[str, Location]] = field(default_factory=list)
    links: dict[str, set[str]] = field(default_factory=lambda: defaultdict(set))
    scenarios: dict[str, list[Scenario]] = field(default_factory=lambda: defaultdict(list))
    feature_files: int = 0

    def link(self, owner: str, other: str) -> None:
        """Zapisuje powiązanie w obu kierunkach; pomija powiązanie ID z samym sobą."""
        if owner != other:
            self.links[owner].add(other)
            self.links[other].add(owner)


@dataclass(frozen=True)
class Finding:
    """Pojedynczy wynik kontroli: poziom („BŁĄD” albo „UWAGA”), miejsce i komunikat."""

    level: str
    location: str
    message: str


def natural_key(identifier: str) -> tuple[object, ...]:
    """Zwraca klucz sortowania, w którym liczby porównuje się numerycznie (US2 < US10)."""
    return tuple(int(p) if p.isdigit() else p for p in re.split(r"(\d+)", identifier))


def relative(path: Path) -> str:
    """Zwraca ścieżkę względną do repozytorium (lub bezwzględną, gdy plik leży poza nim) w formacie POSIX."""
    try:
        return path.resolve().relative_to(REPO).as_posix()
    except ValueError:
        return path.resolve().as_posix()


def clean_line(line: str) -> str:
    """Zwraca linię bez komentarzy HTML, kodu w linii, adresów linków i URL-i, by nie łapać w nich fałszywych ID."""
    for pattern in (HTML_COMMENT, INLINE_CODE, LINK_TARGET, URL):
        line = pattern.sub(" ", line)
    return line


def leading_id(text: str) -> str | None:
    """Zwraca ID stojące na początku tekstu (po formatowaniu Markdown) albo None."""
    match = ID_RE.match(LEADING_NOISE.sub("", text, count=1))
    return match.group(0) if match else None


def table_cells(line: str) -> list[str] | None:
    """Zwraca komórki wiersza tabeli albo None, gdy linia nie jest wierszem tabeli lub jest separatorem."""
    stripped = line.strip()
    if not stripped.startswith("|"):
        return None
    cells = [c.strip() for c in re.split(r"(?<!\\)\|", stripped.strip("|"))]
    if all(SEPARATOR_CELL.match(c.replace(" ", "")) for c in cells):
        return None
    return cells


def markdown_lines(text: str) -> list[tuple[int, str]]:
    """Zwraca (numer linii, linia) bez bloków kodu, front matter i bloku macierzy wygenerowanego przez ten skrypt."""
    result: list[tuple[int, str]] = []
    in_code = in_matrix = False
    lines = text.splitlines()
    start = front_matter_end(lines)
    for number, line in enumerate(lines[start:], start=start + 1):
        if FENCE.match(line):
            in_code = not in_code
        elif MATRIX_START in line:
            in_matrix = True
        elif MATRIX_END in line:
            in_matrix = False
        elif not (in_code or in_matrix):
            result.append((number, line))
    return result


def front_matter_end(lines: list[str]) -> int:
    """Zwraca indeks pierwszej linii po front matter YAML (`---` … `---`) albo 0, gdy go nie ma."""
    if not lines or lines[0].strip() != "---":
        return 0
    for index, line in enumerate(lines[1:], start=1):
        if line.strip() == "---":
            return index + 1
    return 0


def scan_markdown(path: Path, corpus: Corpus) -> None:
    """Dodaje do `corpus` definicje, odwołania i powiązania z jednego pliku Markdown.

    Definicja: ID na początku nagłówka albo pierwszej komórki wiersza tabeli. Pozostałe ID to odwołania,
    powiązane z definicją wiersza i ze wszystkimi otwartymi sekcjami nagłówków definiujących ID.
    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    rel = relative(path)
    sections: list[tuple[int, str | None]] = []
    for number, raw in markdown_lines(path.read_text(encoding="utf-8")):
        line = clean_line(raw)
        location = Location(rel, number)
        heading = HEADING.match(line)
        if heading:
            scan_heading(heading, location, sections, corpus)
            continue
        cells = table_cells(line)
        owner = scan_table_row(cells, location, [s for _, s in sections], corpus) if cells else None
        refs_text = " ".join(cells[1:]) if cells and owner else line
        record_references(refs_text, location, [owner] + [s for _, s in sections], corpus)


def scan_heading(heading: re.Match[str], location: Location, sections: list[tuple[int, str | None]], corpus: Corpus) -> None:
    """Obsługuje nagłówek: zamyka sekcje tego samego lub niższego poziomu, rejestruje definicję i odwołania."""
    level, text = len(heading.group(1)), heading.group(2)
    while sections and sections[-1][0] >= level:
        sections.pop()
    owner = leading_id(text)
    if owner:
        define(owner, location, [s for _, s in sections], corpus)
        text = text.replace(owner, " ", 1)
    record_references(text, location, [owner] + [s for _, s in sections], corpus)
    sections.append((level, owner))


def define(owner: str, location: Location, parents: list[str | None], corpus: Corpus) -> None:
    """Rejestruje definicję `owner` i wiąże ją z ID sekcji nadrzędnych (np. US w sekcji „## N1”)."""
    corpus.definitions[owner].append(location)
    for parent in parents:
        if parent:
            corpus.link(parent, owner)


def scan_table_row(cells: list[str], location: Location, parents: list[str | None], corpus: Corpus) -> str | None:
    """Rejestruje definicję z pierwszej komórki wiersza tabeli i zwraca jej ID (lub None).

    Definicja jest wiązana z `parents`; odwołania z reszty pierwszej komórki są zapisywane od razu.
    Wiersz z pustymi pozostałymi komórkami (np. szablon notatek) nie jest definicją.
    """
    owner = leading_id(cells[0])
    if not owner or not any(cells[1:]):
        return None
    define(owner, location, parents, corpus)
    record_references(cells[0].replace(owner, " ", 1), location, [owner], corpus)
    return owner


def record_references(text: str, location: Location, owners: list[str | None], corpus: Corpus) -> None:
    """Zapisuje każde ID z `text` jako odwołanie i wiąże je z każdym niepustym właścicielem z `owners`."""
    for match in ID_RE.finditer(text):
        ref = match.group(0)
        corpus.references.append((ref, location))
        for owner in owners:
            if owner:
                corpus.link(owner, ref)


def keyword_of(line: str, keywords: tuple[str, ...]) -> str | None:
    """Zwraca tekst po słowie kluczowym Gherkin zakończonym dwukropkiem albo None."""
    for keyword in keywords:
        if line.startswith(keyword + ":"):
            return line[len(keyword) + 1 :].strip()
    return None


def scan_feature(path: Path, corpus: Corpus) -> None:
    """Dodaje do `corpus` scenariusze i ich tagi-ID z pliku .feature (tagi funkcji i reguły są dziedziczone).

    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    rel = relative(path)
    corpus.feature_files += 1
    feature_tags: list[str] = []
    rule_tags: list[str] = []
    pending: list[tuple[str, Location]] = []
    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if line.startswith("@"):
            pending += [(t, Location(rel, number)) for t in TAG_RE.findall(line) if ID_FULL.fullmatch(t)]
            continue
        if line.startswith("#") or not line:
            continue
        tags = [t for t, _ in pending]
        corpus.references.extend(pending)
        pending = []
        if keyword_of(line, FEATURE_KEYWORDS) is not None:
            feature_tags, rule_tags = tags, []
        elif keyword_of(line, RULE_KEYWORDS) is not None:
            rule_tags = tags
        elif (name := keyword_of(line, SCENARIO_KEYWORDS)) is not None:
            scenario = Scenario(name or "(bez nazwy)", Location(rel, number))
            for tag in dict.fromkeys(feature_tags + rule_tags + tags):
                corpus.scenarios[tag].append(scenario)


def collect(docs: Path, specs: Path) -> Corpus:
    """Zbiera korpus z `docs/**/*.md` (bez `_generated`) i `specs/**/*.feature`. Brak katalogów nie jest błędem.

    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu pliku.
    """
    corpus = Corpus()
    for path in sorted(docs.rglob("*.md")) if docs.is_dir() else []:
        if "_generated" not in path.relative_to(docs).parts:
            scan_markdown(path, corpus)
    for path in sorted(specs.rglob("*.feature")) if specs.is_dir() else []:
        scan_feature(path, corpus)
    return corpus


def parent_us(ac: str) -> str:
    """Zwraca ID historyjki dla ID kryterium akceptacji (US3.2 → US3)."""
    return ac.split(".", 1)[0]


def check(corpus: Corpus) -> list[Finding]:
    """Zwraca wyniki kontroli: niezdefiniowane odwołania, US bez N, AC bez scenariusza, UC bez US, wielokrotne definicje.

    Gwarantuje: AC bez scenariusza jest „BŁĘDEM” tylko wtedy, gdy istnieje choć jeden plik .feature; inaczej „UWAGĄ”.
    """
    findings = undefined_references(corpus)
    defined = sorted(corpus.definitions, key=natural_key)
    first = {i: str(corpus.definitions[i][0]) for i in defined}
    ac_level = "BŁĄD" if corpus.feature_files else "UWAGA"
    for ident in defined:
        neighbours = corpus.links.get(ident, set())
        if US_RE.match(ident) and not any(N_RE.match(n) for n in neighbours):
            findings.append(Finding("BŁĄD", first[ident], f"{ident}: historyjka bez potrzeby N"))
        if AC_RE.match(ident) and not corpus.scenarios.get(ident):
            findings.append(Finding(ac_level, first[ident], f"{ident}: kryterium bez scenariusza (tag @{ident})"))
        if UC_RE.match(ident) and not any(US_RE.match(n) or AC_RE.match(n) for n in neighbours):
            findings.append(Finding("BŁĄD", first[ident], f"{ident}: przypadek użycia bez historyjki US"))
        if len(corpus.definitions[ident]) > 1:
            places = ", ".join(str(loc) for loc in corpus.definitions[ident][1:])
            findings.append(Finding("UWAGA", first[ident], f"{ident}: zdefiniowane ponownie w {places}"))
    return findings


def undefined_references(corpus: Corpus) -> list[Finding]:
    """Zwraca błędy dla odwołań do ID bez definicji (każde miejsce osobno, bez powtórzeń)."""
    seen: set[tuple[str, Location]] = set()
    findings: list[Finding] = []
    for ref, location in corpus.references:
        if ref not in corpus.definitions and (ref, location) not in seen:
            seen.add((ref, location))
            findings.append(Finding("BŁĄD", str(location), f"odwołanie do niezdefiniowanego ID: {ref}"))
    return findings


def neighbours_matching(corpus: Corpus, ident: str, pattern: re.Pattern[str]) -> list[str]:
    """Zwraca posortowane, zdefiniowane ID powiązane z `ident` i pasujące do `pattern`."""
    found = {n for n in corpus.links.get(ident, set()) if pattern.match(n) and n in corpus.definitions}
    return sorted(found, key=natural_key)


def matrix_rows(corpus: Corpus) -> list[list[str]]:
    """Zwraca wiersze macierzy [N, US, AC, UC, scenariusze]; jeden wiersz na AC (albo na US/N bez dzieci).

    Gwarantuje: każda zdefiniowana N i US pojawia się co najmniej raz; US bez N mają „—” w kolumnie N.
    """
    defined = sorted(corpus.definitions, key=natural_key)
    stories = [i for i in defined if US_RE.match(i)]
    rows: list[list[str]] = []
    for need in (i for i in defined if N_RE.match(i)):
        linked = neighbours_matching(corpus, need, US_RE)
        rows += [r for us in linked for r in story_rows(corpus, need, us)] or [[need, "—", "—", "—", "—"]]
    for us in stories:
        if not neighbours_matching(corpus, us, N_RE):
            rows += story_rows(corpus, "—", us)
    return rows


def story_rows(corpus: Corpus, need: str, us: str) -> list[list[str]]:
    """Zwraca wiersze macierzy dla jednej historyjki: po jednym na każde jej AC albo jeden, gdy AC brak."""
    criteria = sorted((i for i in corpus.definitions if AC_RE.match(i) and parent_us(i) == us), key=natural_key)
    story_ucs = set(neighbours_matching(corpus, us, UC_RE))
    if not criteria:
        return [[need, us, "—", join(sorted(story_ucs, key=natural_key)), scenario_list(corpus, us)]]
    rows = []
    for ac in criteria:
        ucs = sorted(story_ucs | set(neighbours_matching(corpus, ac, UC_RE)), key=natural_key)
        rows.append([need, us, ac, join(ucs), scenario_list(corpus, ac)])
    return rows


def join(items: list[str]) -> str:
    """Łączy elementy przecinkami; pusta lista daje „—”."""
    return ", ".join(items) if items else "—"


def scenario_list(corpus: Corpus, ident: str) -> str:
    """Zwraca listę scenariuszy oznaczonych tagiem `ident` w postaci „nazwa (plik.feature)” albo „—”."""
    items = [f"{s.name} ({Path(s.location.path).name})" for s in corpus.scenarios.get(ident, [])]
    return join([i.replace("|", "\\|") for i in items])


def render_matrix(corpus: Corpus) -> str:
    """Zwraca blok Markdown z tabelą macierzy otoczony znacznikami MATRIX_START i MATRIX_END."""
    header = ["Potrzeba", "Historyjka", "Kryterium", "Przypadek użycia", "Scenariusz"]
    return "\n".join([MATRIX_START, "", *aligned_table(header, matrix_rows(corpus)), "", MATRIX_END]) + "\n"


def aligned_table(header: list[str], rows: list[list[str]]) -> list[str]:
    """Zwraca linie tabeli Markdown z kolumnami wyrównanymi spacjami (zgodnie z regułą markdownlint MD060)."""
    widths = [max(3, *(len(r[i]) for r in [header, *rows])) for i in range(len(header))]
    return [table_line(header, widths), table_line(["-" * w for w in widths], widths), *[table_line(r, widths) for r in rows]]


def table_line(cells: list[str], widths: list[int]) -> str:
    """Zwraca wiersz tabeli Markdown z komórkami dopełnionymi spacjami do `widths`."""
    return "| " + " | ".join(c.ljust(w) for c, w in zip(cells, widths)) + " |"


def write_matrix(target: Path, block: str) -> None:
    """Zapisuje blok macierzy do `target`.

    Gwarantuje: istniejący blok między znacznikami jest zastępowany; plik bez znaczników dostaje blok na końcu;
    brakujący plik zawiera tylko blok. Reszta treści pliku pozostaje bez zmian.
    Wyjątki: OSError przy błędzie zapisu.
    """
    current = target.read_text(encoding="utf-8") if target.is_file() else ""
    start, end = current.find(MATRIX_START), current.find(MATRIX_END)
    if 0 <= start < end:
        content = current[:start] + block.rstrip("\n") + current[end + len(MATRIX_END) :]
    elif current:
        content = current.rstrip("\n") + "\n\n" + block
    else:
        content = block
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8", newline="\n")


def print_report(corpus: Corpus, findings: list[Finding]) -> None:
    """Wypisuje podsumowanie korpusu i listę wyników."""
    print(
        f"check_ids: definicji {len(corpus.definitions)}, odwołań {len(corpus.references)}, "
        f"scenariuszy z tagami ID {sum(len(v) for v in corpus.scenarios.values())}"
    )
    for finding in findings:
        print(f"{finding.level:5} {finding.location}  {finding.message}")
    errors = sum(f.level == "BŁĄD" for f in findings)
    print(f"check_ids: błędów {errors}, uwag {len(findings) - errors}")


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    """Parsuje argumenty wiersza poleceń."""
    parser = argparse.ArgumentParser(description="Kontrola ID w docs/**/*.md i specs/**/*.feature.")
    parser.add_argument("--docs", type=Path, default=REPO / "docs", help="katalog dokumentacji")
    parser.add_argument("--specs", type=Path, default=REPO / "specs", help="katalog plików .feature")
    parser.add_argument("--matrix", type=Path, help="zapisz macierz N → US → AC → UC → scenariusz do pliku")
    parser.add_argument("--strict", action="store_true", help="kod wyjścia 1, gdy są błędy")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 1 przy błędach w trybie --strict, 2 przy błędzie odczytu/zapisu, w pozostałych razach 0."""
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    try:
        corpus = collect(args.docs, args.specs)
        if args.matrix:
            write_matrix(args.matrix, render_matrix(corpus))
            print(f"check_ids: macierz zapisana do {args.matrix}")
    except (OSError, UnicodeDecodeError) as exc:
        print(f"check_ids: BŁĄD: {exc}", file=sys.stderr)
        return 2
    findings = check(corpus)
    print_report(corpus, findings)
    has_errors = any(f.level == "BŁĄD" for f in findings)
    return 1 if args.strict and has_errors else 0


if __name__ == "__main__":
    sys.exit(main())
