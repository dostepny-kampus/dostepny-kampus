# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — kontrola identyfikatorów (N, US, AC, UC, BR, NFR, R, RO, M, TB), macierz śledzenia
i odwołania wierszy w wymaganiach ekranów. Version: 0.3.0."""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
ROOT = REPO.parent
ID_BODY = r"US\d+\.\d+|US\d+|UC\d+|BR-G\d+|BR-\d+|NFR-\d+|TB-\d+|RO-[A-Z]{3}|R-\d+|M-\d+|N\d+"
ID_RE = re.compile(rf"(?<![\w-])(?:{ID_BODY})(?![\w°-]|\.\d)")
DEFINITION_END = re.compile(r"[\s:.*_\]`]|$")
LEGACY_AC = re.compile(r"(?<![\w-])AC\d+(?![\w-])")
ID_FULL = re.compile(rf"(?:{ID_BODY})")
AC_RE = re.compile(r"^US\d+\.\d+$")
US_RE = re.compile(r"^US\d+$")
UC_RE = re.compile(r"^UC\d+$")
N_RE = re.compile(r"^N\d+$")
BR_RE = re.compile(r"^BR-G?\d+$")
HEADING = re.compile(r"^(#{1,6})\s+(.*)$")
FENCE = re.compile(r"^\s*(```|~~~)")
MERMAID_FENCE = re.compile(r"^\s*(```|~~~)\s*mermaid(?![\w-])")
LINK_TARGET = re.compile(r"\]\([^)]*\)")
URL = re.compile(r"https?://\S+")
INLINE_CODE = re.compile(r"`[^`]*`")
HTML_COMMENT = re.compile(r"<!--.*?-->")
LEADING_NOISE = re.compile(r"^[\s*_\[`<>]*(?:<[^>]+>)?[\s*_\[`]*")
SEPARATOR_CELL = re.compile(r"^:?-{3,}:?$")
TAG_RE = re.compile(r"@([\w.\-]+)")
CLASS_PART = re.compile(r"\d+|(?<=RO-)[A-Z]+")
PARTIAL_NOTE = " (klasa ID jeszcze bez definicji)"
BLOCK_NAMES = ("podsumowanie", "potrzeby", "macierz", "reguly", "dziury")
BLOCK_MARKER = re.compile(r"<!-- ([a-z]+):(start|end) -->")
VALE_OFF = "<!-- vale off -->"
VALE_ON = "<!-- vale on -->"
FEATURE_KEYWORDS = ("Feature", "Funkcja", "Funkcjonalność", "Właściwość", "Aspekt", "Potrzeba biznesowa")
RULE_KEYWORDS = ("Rule", "Reguła", "Zasada")
SCENARIO_KEYWORDS = ("Scenario Outline", "Scenario Template", "Scenario", "Example", "Scenariusz", "Szablon scenariusza", "Przykład")
EXAMPLES_KEYWORDS = ("Examples", "Scenarios", "Przykłady")
SCREEN_PAGE = Path("ux") / "wymagania-ekranow.md"
SCREEN_REF_HEADER = "Odwołanie"
SCREEN_COMMON_REF = "wymagania wspólne"
REQUIREMENT_RE = re.compile(r"(?<![\w-])(?:US\d+(?:\.\d+)?|UC\d+|BR-G?\d+|NFR-\d+)(?![\w-])")


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
    legacy_ac: list[tuple[str, Location]] = field(default_factory=list)
    links: dict[str, set[str]] = field(default_factory=lambda: defaultdict(set))
    scenarios: dict[str, list[Scenario]] = field(default_factory=lambda: defaultdict(list))
    all_scenarios: list[Scenario] = field(default_factory=list)
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
    """Zwraca ścieżkę w formacie POSIX względną do katalogu dokumentacji, a poza nim — do korzenia monorepo.

    Gwarantuje: plik spoza monorepo dostaje ścieżkę bezwzględną.
    """
    for base in (REPO, ROOT):
        try:
            return path.resolve().relative_to(base).as_posix()
        except ValueError:
            continue
    return path.resolve().as_posix()


def clean_line(line: str) -> str:
    """Zwraca linię bez komentarzy HTML, kodu w linii, adresów linków i URL-i, by nie łapać w nich fałszywych ID."""
    for pattern in (HTML_COMMENT, INLINE_CODE, LINK_TARGET, URL):
        line = pattern.sub(" ", line)
    return line


def leading_id(text: str) -> str | None:
    """Zwraca ID stojące na początku tekstu (po formatowaniu Markdown) albo None.

    Gwarantuje: po ID jest koniec tekstu, odstęp, dwukropek, kropka albo znacznik formatowania;
    zakres („N5–N9”), wyliczenie („N1, N2”) i ciąg dalszy („N10…”) nie dają ID, bo nie są definicją.
    """
    body = LEADING_NOISE.sub("", text, count=1)
    match = ID_RE.match(body)
    if not match or not DEFINITION_END.match(body, match.end()):
        return None
    return match.group(0)


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
    """Zwraca (numer linii, linia) bez bloków kodu, front matter i bloków wygenerowanych przez ten skrypt (BLOCK_NAMES).

    Gwarantuje: treść bloków ```mermaid jest zwracana, bo diagram jest częścią strony i jego odwołania się sprawdza.
    """
    result: list[tuple[int, str]] = []
    fence = "text"
    in_block = False
    lines = text.splitlines()
    start = front_matter_end(lines)
    for number, line in enumerate(lines[start:], start=start + 1):
        marker = BLOCK_MARKER.search(line)
        if FENCE.match(line):
            fence = next_fence(line, fence)
        elif marker and marker.group(1) in BLOCK_NAMES:
            in_block = marker.group(2) == "start"
        elif not (fence == "code" or in_block):
            result.append((number, line))
    return result


def next_fence(line: str, current: str) -> str:
    """Zwraca stan po linii ogrodzenia bloku: „text” (poza blokiem), „mermaid” albo „code”."""
    if current != "text":
        return "text"
    return "mermaid" if MERMAID_FENCE.match(line) else "code"


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
        corpus.legacy_ac += [(m.group(0), location) for m in LEGACY_AC.finditer(line)]


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
            corpus.all_scenarios.append(scenario)
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


def id_class(identifier: str) -> str:
    """Zwraca klasę ID z liczbami zastąpionymi znakiem `#` (US3.2 → US#.#, BR-G1 → BR-G#, RO-GOS → RO-#)."""
    return CLASS_PART.sub("#", identifier)


def error_level(corpus: Corpus, required: str, partial: bool) -> tuple[str, str]:
    """Zwraca (poziom, dopisek) dla błędu, którego usunięcie wymaga definicji klasy `required`.

    Gwarantuje: w trybie `partial` błąd staje się „UWAGĄ” tylko wtedy, gdy klasa `required` nie ma jeszcze
    żadnej definicji (jej strona nie istnieje); w każdym innym razie pozostaje „BŁĘDEM”.
    """
    if partial and all(id_class(i) != required for i in corpus.definitions):
        return "UWAGA", PARTIAL_NOTE
    return "BŁĄD", ""


def check(corpus: Corpus, partial: bool = False) -> list[Finding]:
    """Zwraca wyniki kontroli: niezdefiniowane odwołania, US bez N, AC i BR bez scenariusza, UC bez US, wielokrotne
    definicje, kryteria w starym formacie „ACn” (decyzja 30: kryteria zapisuje się jako USx.y) i pozostałe dziury
    śledzenia (patrz untraced).

    Gwarantuje: AC i BR bez scenariusza są „BŁĘDEM” tylko wtedy, gdy istnieje choć jeden plik .feature; inaczej „UWAGĄ”.
    W trybie `partial` błędy zależne od klasy ID bez żadnej definicji są „UWAGAMI” (patrz error_level).
    Każda dziura z trace_gaps daje tu wynik, więc strona śledzenia z dziurami nie przejdzie kontroli `--strict`.
    """
    findings = undefined_references(corpus, partial) + legacy_criteria(corpus) + untraced(corpus, partial)
    defined = sorted(corpus.definitions, key=natural_key)
    first = {i: str(corpus.definitions[i][0]) for i in defined}
    ac_level = "BŁĄD" if corpus.feature_files else "UWAGA"
    for ident in defined:
        if US_RE.match(ident) and not has_need(corpus, ident):
            level, note = error_level(corpus, "N#", partial)
            findings.append(Finding(level, first[ident], f"{ident}: historyjka bez potrzeby N{note}"))
        if AC_RE.match(ident) and not corpus.scenarios.get(ident):
            findings.append(Finding(ac_level, first[ident], f"{ident}: kryterium bez scenariusza (tag @{ident})"))
        if BR_RE.match(ident) and not corpus.scenarios.get(ident):
            findings.append(Finding(ac_level, first[ident], f"{ident}: reguła bez scenariusza (tag @{ident})"))
        if UC_RE.match(ident) and not has_story(corpus, ident):
            level, note = error_level(corpus, "US#", partial)
            findings.append(Finding(level, first[ident], f"{ident}: przypadek użycia bez historyjki US{note}"))
        if len(corpus.definitions[ident]) > 1:
            places = ", ".join(str(loc) for loc in corpus.definitions[ident][1:])
            findings.append(Finding("UWAGA", first[ident], f"{ident}: zdefiniowane ponownie w {places}"))
    return findings


def untraced(corpus: Corpus, partial: bool = False) -> list[Finding]:
    """Zwraca błędy dla N bez US, US bez AC i scenariuszy bez tagu kryterium USx.y ani reguły BR.

    Gwarantuje: scenariusz bez takiego tagu jest zawsze „BŁĘDEM”; N bez US i US bez AC są „UWAGAMI” tylko
    w trybie `partial`, gdy klasa US# albo US#.# nie ma jeszcze definicji (patrz error_level).
    """
    findings: list[Finding] = []
    for need in defined_ids(corpus, N_RE):
        if not neighbours_matching(corpus, need, US_RE):
            level, note = error_level(corpus, "US#", partial)
            findings.append(Finding(level, str(corpus.definitions[need][0]), f"{need}: potrzeba bez historyjki US{note}"))
    for us in defined_ids(corpus, US_RE):
        if not criteria_of(corpus, us):
            level, note = error_level(corpus, "US#.#", partial)
            findings.append(Finding(level, str(corpus.definitions[us][0]), f"{us}: historyjka bez kryteriów akceptacji{note}"))
    served = served_scenarios(corpus)
    message = "scenariusz bez tagu kryterium USx.y i reguły BR"
    return findings + [Finding("BŁĄD", str(s.location), f"{message}: {s.name}") for s in corpus.all_scenarios if s not in served]


def served_scenarios(corpus: Corpus) -> set[Scenario]:
    """Zwraca scenariusze z tagiem kryterium USx.y albo reguły BR (także odziedziczonym z funkcji lub reguły)."""
    return {s for tag, items in corpus.scenarios.items() if AC_RE.match(tag) or BR_RE.match(tag) for s in items}


def has_need(corpus: Corpus, us: str) -> bool:
    """Zwraca True, gdy historyjka `us` jest powiązana z co najmniej jedną potrzebą N."""
    return any(N_RE.match(n) for n in corpus.links.get(us, set()))


def has_story(corpus: Corpus, uc: str) -> bool:
    """Zwraca True, gdy przypadek użycia `uc` jest powiązany z historyjką US albo z jej kryterium."""
    return any(US_RE.match(n) or AC_RE.match(n) for n in corpus.links.get(uc, set()))


def undefined_references(corpus: Corpus, partial: bool = False) -> list[Finding]:
    """Zwraca błędy dla odwołań do ID bez definicji (każde miejsce osobno, bez powtórzeń); poziom jak w error_level."""
    seen: set[tuple[str, Location]] = set()
    findings: list[Finding] = []
    for ref, location in corpus.references:
        if ref not in corpus.definitions and (ref, location) not in seen:
            seen.add((ref, location))
            level, note = error_level(corpus, id_class(ref), partial)
            findings.append(Finding(level, str(location), f"odwołanie do niezdefiniowanego ID: {ref}{note}"))
    return findings


def legacy_criteria(corpus: Corpus) -> list[Finding]:
    """Zwraca uwagi dla odwołań do kryteriów w starym formacie „US9 AC3” zamiast „US9.3”."""
    return [Finding("UWAGA", str(loc), f"{ac}: stary format kryterium, zapisz jako USx.y") for ac, loc in corpus.legacy_ac]


def neighbours_matching(corpus: Corpus, ident: str, pattern: re.Pattern[str]) -> list[str]:
    """Zwraca posortowane, zdefiniowane ID powiązane z `ident` i pasujące do `pattern`."""
    found = {n for n in corpus.links.get(ident, set()) if pattern.match(n) and n in corpus.definitions}
    return sorted(found, key=natural_key)


def defined_ids(corpus: Corpus, pattern: re.Pattern[str]) -> list[str]:
    """Zwraca posortowane zdefiniowane ID pasujące do `pattern`."""
    return sorted((i for i in corpus.definitions if pattern.match(i)), key=natural_key)


def criteria_of(corpus: Corpus, us: str) -> list[str]:
    """Zwraca posortowane kryteria akceptacji historyjki `us`."""
    return [ac for ac in defined_ids(corpus, AC_RE) if parent_us(ac) == us]


def need_rows(corpus: Corpus) -> list[list[str]]:
    """Zwraca wiersze tabeli [N, historyjki]; potrzeba bez historyjki ma „—”."""
    return [[n, join(neighbours_matching(corpus, n, US_RE))] for n in defined_ids(corpus, N_RE)]


def matrix_rows(corpus: Corpus) -> list[list[str]]:
    """Zwraca wiersze macierzy [N, US, AC, UC, scenariusze]; jeden wiersz na AC albo na US bez AC.

    Gwarantuje: każda zdefiniowana US pojawia się co najmniej raz, w kolejności numerów; kolumna N wymienia
    wszystkie potrzeby historyjki, a US bez N ma w niej „—”. Kolumna UC łączy przypadki historyjki i kryterium.
    """
    rows: list[list[str]] = []
    for us in defined_ids(corpus, US_RE):
        needs = join(neighbours_matching(corpus, us, N_RE))
        story_ucs = set(neighbours_matching(corpus, us, UC_RE))
        criteria = criteria_of(corpus, us)
        if not criteria:
            rows.append([needs, us, "—", join(sorted(story_ucs, key=natural_key)), scenario_list(corpus, us)])
        for ac in criteria:
            ucs = sorted(story_ucs | set(neighbours_matching(corpus, ac, UC_RE)), key=natural_key)
            rows.append([needs, us, ac, join(ucs), scenario_list(corpus, ac)])
    return rows


def rule_rows(corpus: Corpus) -> list[list[str]]:
    """Zwraca wiersze tabeli pokrycia reguł [BR, scenariusze]: najpierw BR-n, potem BR-G*, według numerów."""
    return [[br, scenario_list(corpus, br)] for br in defined_ids(corpus, BR_RE)]


def trace_gaps(corpus: Corpus) -> list[list[str]]:
    """Zwraca dziury śledzenia [miejsce, opis]: N bez US, US bez N, US bez AC, AC bez scenariusza, UC bez US,
    BR bez scenariusza i scenariusz bez tagu kryterium USx.y ani reguły BR (nie służy żadnemu wymaganiu).

    Gwarantuje: kolejność grup jak w tym opisie, w grupie według numerów ID; dla scenariusza miejscem jest
    plik i numer linii.
    """
    gaps = [[n, "potrzeba bez historyjki"] for n in defined_ids(corpus, N_RE) if not neighbours_matching(corpus, n, US_RE)]
    for us in defined_ids(corpus, US_RE):
        if not has_need(corpus, us):
            gaps.append([us, "historyjka bez potrzeby"])
        if not criteria_of(corpus, us):
            gaps.append([us, "historyjka bez kryteriów akceptacji"])
    gaps += [[ac, "kryterium bez scenariusza"] for ac in defined_ids(corpus, AC_RE) if not corpus.scenarios.get(ac)]
    gaps += [[uc, "przypadek użycia bez historyjki"] for uc in defined_ids(corpus, UC_RE) if not has_story(corpus, uc)]
    gaps += [[br, "reguła bez scenariusza"] for br in defined_ids(corpus, BR_RE) if not corpus.scenarios.get(br)]
    served = served_scenarios(corpus)
    gaps +=[[str(s.location), f"scenariusz bez kryterium i reguły: {s.name}"] for s in corpus.all_scenarios if s not in served]
    return gaps


def summary_rows(corpus: Corpus, gaps: list[list[str]]) -> list[list[str]]:
    """Zwraca wiersze podsumowania [element, liczba] wyliczone z korpusu i listy dziur `gaps` (patrz trace_gaps)."""
    counts = [
        ("Potrzeby (N)", len(defined_ids(corpus, N_RE))),
        ("Historyjki (US)", len(defined_ids(corpus, US_RE))),
        ("Kryteria akceptacji (USx.y)", len(defined_ids(corpus, AC_RE))),
        ("Przypadki użycia (UC)", len(defined_ids(corpus, UC_RE))),
        ("Reguły biznesowe (BR)", len(defined_ids(corpus, BR_RE))),
        ("Scenariusze", len(corpus.all_scenarios)),
        ("Dziury", len(gaps)),
    ]
    return [[name, str(count)] for name, count in counts]


def join(items: list[str], separator: str = ", ") -> str:
    """Łączy elementy separatorem; pusta lista daje „—”."""
    return separator.join(items) if items else "—"


def scenario_list(corpus: Corpus, ident: str) -> str:
    """Zwraca scenariusze z tagiem `ident` jako „nazwa (`plik.feature`)” rozdzielone znacznikiem `<br>` albo „—”.

    Gwarantuje: nazwa pliku jest kodem w linii, więc skróty ze słownika nie podkreślają w niej słów.
    """
    items = [f"{s.name} (`{Path(s.location.path).name}`)" for s in corpus.scenarios.get(ident, [])]
    return join([i.replace("|", "\\|") for i in items], "<br>")


def block_markers(name: str) -> tuple[str, str]:
    """Zwraca znaczniki początku i końca bloku `name`, np. `<!-- macierz:start -->`."""
    return f"<!-- {name}:start -->", f"<!-- {name}:end -->"


def wrap_block(name: str, lines: list[str]) -> str:
    """Zwraca linie `lines` otoczone znacznikami bloku `name` i wyłączeniem Vale.

    Gwarantuje: Vale nie sprawdza treści bloku, bo to lista nazw scenariuszy i ID, a nie zdania; tekst pisany
    ręcznie poza blokami Vale sprawdza nadal.
    """
    start, end = block_markers(name)
    return "\n".join([start, VALE_OFF, "", *lines, "", VALE_ON, end]) + "\n"


def render_blocks(corpus: Corpus) -> dict[str, str]:
    """Zwraca wygenerowane bloki strony śledzenia (nazwa → blok ze znacznikami) w kolejności BLOCK_NAMES."""
    gaps = trace_gaps(corpus)
    bodies = {
        "podsumowanie": aligned_table(["Element", "Liczba"], summary_rows(corpus, gaps)),
        "potrzeby": aligned_table(["Potrzeba", "Historyjki"], need_rows(corpus)),
        "macierz": aligned_table(["N", "US", "AC", "UC", "Scenariusz"], matrix_rows(corpus)),
        "reguly": aligned_table(["Reguła", "Scenariusz"], rule_rows(corpus)),
        "dziury": aligned_table(["Miejsce", "Dziura"], gaps) if gaps else ["Brak dziur."],
    }
    return {name: wrap_block(name, bodies[name]) for name in BLOCK_NAMES}


def aligned_table(header: list[str], rows: list[list[str]]) -> list[str]:
    """Zwraca linie tabeli Markdown z kolumnami wyrównanymi spacjami (zgodnie z regułą markdownlint MD060)."""
    widths = [max(3, *(len(r[i]) for r in [header, *rows])) for i in range(len(header))]
    return [table_line(header, widths), table_line(["-" * w for w in widths], widths), *[table_line(r, widths) for r in rows]]


def table_line(cells: list[str], widths: list[int]) -> str:
    """Zwraca wiersz tabeli Markdown z komórkami dopełnionymi spacjami do `widths`."""
    return "| " + " | ".join(c.ljust(w) for c, w in zip(cells, widths)) + " |"


def updated_content(current: str, blocks: dict[str, str]) -> str:
    """Zwraca treść pliku po wstawieniu bloków `blocks` (nazwa → blok ze znacznikami).

    Gwarantuje: istniejący blok między swoimi znacznikami jest zastępowany; blok, którego znaczników brak,
    jest dopisywany na końcu; pusta treść daje same bloki. Reszta treści pozostaje bez zmian.
    """
    content = current
    for name, block in blocks.items():
        start_marker, end_marker = block_markers(name)
        start, end = content.find(start_marker), content.find(end_marker)
        if 0 <= start < end:
            content = content[:start] + block.rstrip("\n") + content[end + len(end_marker) :]
        elif content:
            content = content.rstrip("\n") + "\n\n" + block
        else:
            content = block
    return content


def write_matrix(target: Path, blocks: dict[str, str]) -> None:
    """Zapisuje bloki do `target` według updated_content; brakujący plik zawiera tylko bloki.

    Wyjątki: OSError przy błędzie odczytu albo zapisu.
    """
    current = target.read_text(encoding="utf-8") if target.is_file() else ""
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(updated_content(current, blocks), encoding="utf-8", newline="\n")


def matrix_is_current(target: Path, blocks: dict[str, str]) -> bool:
    """Zwraca True, gdy plik `target` istnieje i jego bloki są takie jak `blocks` (końce linii CRLF/LF bez znaczenia).

    Wyjątki: OSError przy błędzie odczytu.
    """
    if not target.is_file():
        return False
    current = target.read_text(encoding="utf-8")
    return current == updated_content(current, blocks)


def report_matrix_state(target: Path, current: bool) -> int:
    """Wypisuje wynik kontroli aktualności macierzy w `target` i zwraca kod wyjścia: 0 aktualna, 1 nieaktualna."""
    if current:
        print(f"check_ids: macierz w {target} jest aktualna")
        return 0
    print(f"check_ids: BŁĄD: macierz w {target} jest nieaktualna; odśwież: python scripts/check_ids.py --matrix {target}")
    return 1


def screen_rows(path: Path) -> list[Finding]:
    """Zwraca błędy dla wierszy tabel z kolumną „Odwołanie”, w których ta kolumna nie ma ID US, AC, BR, NFR ani UC.

    Gwarantuje: wiersz przechodzi, gdy kolumna ma choć jedno takie ID albo odsyłacz „wymagania wspólne”; same
    „decyzja N”, „PW-x”, „WCAG …” czy „próg zespołu” dają „BŁĄD” z numerem linii. Wiersz z mniejszą
    liczbą komórek, który nie sięga tej kolumny, też jest błędem. Tabele bez tej kolumny i bloki kodu są
    pomijane. Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    findings: list[Finding] = []
    column: int | None = None
    for number, raw in markdown_lines(path.read_text(encoding="utf-8")):
        if not raw.strip().startswith("|"):
            column = None
            continue
        cells = table_cells(raw)
        if cells is None:
            continue
        if column is None:
            column = cells.index(SCREEN_REF_HEADER) if SCREEN_REF_HEADER in cells else -1
            continue
        if column < 0:
            continue
        location = str(Location(relative(path), number))
        if column >= len(cells):
            message = f"wiersz bez kolumny „{SCREEN_REF_HEADER}”: {len(cells)} komórek, a kolumna jest {column + 1}."
            findings.append(Finding("BŁĄD", location, message))
            continue
        cell = cells[column]
        if REQUIREMENT_RE.search(clean_line(cell)) or SCREEN_COMMON_REF in cell:
            continue
        message = f"wiersz bez odwołania do US, AC, BR, NFR ani UC: „{cell or '—'}”"
        findings.append(Finding("BŁĄD", location, message))
    return findings


def check_screen_page(docs: Path) -> list[Finding]:
    """Zwraca wyniki screen_rows dla strony wymagań ekranów w `docs`; brak strony nie jest błędem."""
    page = docs / SCREEN_PAGE
    return screen_rows(page) if page.is_file() else []


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
    parser.add_argument("--specs", type=Path, default=ROOT / "specs", help="katalog plików .feature")
    target = parser.add_mutually_exclusive_group()
    target.add_argument("--matrix", type=Path, help="zapisz macierz N → US → AC → UC → scenariusz do pliku")
    target.add_argument("--check-matrix", type=Path, help="tylko sprawdź, czy macierz w pliku jest aktualna")
    parser.add_argument("--strict", action="store_true", help="kod wyjścia 1, gdy są błędy")
    parser.add_argument("--partial", action="store_true", help="błędy klas ID bez żadnej definicji jako uwagi")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 1 przy błędach w trybie --strict, 2 przy błędzie odczytu/zapisu, w pozostałych razach 0.

    Gwarantuje: z --check-matrix tylko porównuje macierz w pliku z wygenerowaną z bieżących dokumentów, bez raportu
    kontroli ID; zwraca 0, gdy jest aktualna, 1, gdy jest nieaktualna albo pliku brak, 2 przy błędzie odczytu.
    """
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    try:
        corpus = collect(args.docs, args.specs)
        if args.check_matrix:
            return report_matrix_state(args.check_matrix, matrix_is_current(args.check_matrix, render_blocks(corpus)))
        if args.matrix:
            write_matrix(args.matrix, render_blocks(corpus))
            print(f"check_ids: macierz zapisana do {args.matrix}")
        findings = check(corpus, args.partial) + check_screen_page(args.docs)
    except (OSError, UnicodeDecodeError) as exc:
        print(f"check_ids: BŁĄD: {exc}", file=sys.stderr)
        return 2
    print_report(corpus, findings)
    has_errors = any(f.level == "BŁĄD" for f in findings)
    return 1 if args.strict and has_errors else 0


if __name__ == "__main__":
    sys.exit(main())
