# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — kontrola identyfikatorów (N, US, AC, UC, BR, NFR, R, RO, M, TB), macierz śledzenia,
odwołania wierszy w wymaganiach ekranów, znaczniki scenariuszy w testach Androida i pokrycie scenariuszy testami. Version: 0.5.0."""

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
COVERAGE_PAGE = Path("_generated") / "pokrycie-testami.md"
COVERAGE_HEADER = ["Plik", "Scenariusz", "Kryteria i reguły", "Test", "Stan"]
COVERAGE_COUNTS = (("Pełny test", "pełny"), ("Tylko częściowo", "częściowo"), ("Bez testu", "brak"))
COVERAGE_INTRO = """# Pokrycie scenariuszy testami

Jeden wiersz to jeden scenariusz z katalogu `specs/`. Kolumna „Test” wskazuje testy Androida ze znacznikiem
`// Scenariusz:` (moduł, klasa i funkcja testu). Stan „pełny” znaczy, że test sprawdza wszystkie kroki „Wtedy”,
„częściowo” — tylko część z nich, a „brak” — że scenariusz nie ma testu.

Stronę tworzy skrypt, nie edytuj jej ręcznie. Odśwież ją razem z macierzą śledzenia:
`python scripts/check_ids.py --matrix docs/wymagania/sledzenie.md`.

"""
SCREEN_REF_HEADER = "Odwołanie"
SCREEN_COMMON_REF = "wymagania wspólne"
REQUIREMENT_RE = re.compile(r"(?<![\w-])(?:US\d+(?:\.\d+)?|UC\d+|BR-G?\d+|NFR-\d+)(?![\w-])")
MARKER = re.compile(r"^\s*// Scenariusz( \(częściowo\))?: (\S[^:]*?\.feature):(?: (\S.*?))?\s*$")
MARKER_CONTINUATION = re.compile(r"^\s*//\s{2,}(\S.*?)\s*$")
MARKER_HINT = re.compile(r"^\s*//\s*scenariusz\b[^:]{0,20}:", re.IGNORECASE)
ANNOTATION = re.compile(r"@([\w.:]+)(\((?:[^()]|\([^()]*\))*\))?\s*")
TEST_ANNOTATIONS = {"Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate"}
FUNCTION = re.compile(r"^(?:[a-z]+\s+)*fun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(?:`([^`]+)`|(\w+))")
CLASS = re.compile(r"^\s*(?:@[\w.]+(?:\([^)]*\))?\s+)*(?:[a-z]+\s+)*(?:class|object)\s+(\w+)[^{]*\{")
CODE_TOKEN = re.compile(r"""//[^\n]*|/\*|\"\"\"[\s\S]*?\"\"\"|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n]){1,6}'""")
BLOCK_COMMENT_PART = re.compile(r"/\*|\*/")
TEST_SOURCE = re.compile(r"^(?!(?:.*/)?(?:build|\.gradle)/)(?:.*/)?src/(?:test|\w*Test)\w*/")


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
    feature: str = ""


@dataclass(frozen=True)
class Marker:
    """Znacznik `// Scenariusz: <plik>.feature: <nazwa>` w teście; `test` — stan funkcji tuż pod znacznikiem:
    „aktywny”, „wyłączony” (@Ignore, @Disabled…) albo pusty, gdy pod znacznikiem nie ma funkcji testu; `test_id` —
    stały identyfikator testu „moduł · Klasa.funkcja”, niezależny od numerów linii."""

    feature: str
    scenario: str
    partial: bool
    test: str
    location: Location
    test_id: str = ""
    function_line: int = 0


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
    features: set[str] = field(default_factory=set)
    markers: list[Marker] = field(default_factory=list)
    bad_markers: list[Location] = field(default_factory=list)

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


def scan_feature(path: Path, corpus: Corpus, feature: str = "") -> None:
    """Dodaje do `corpus` scenariusze i ich tagi-ID z pliku .feature (tagi funkcji i reguły są dziedziczone).

    `feature` to ścieżka pliku względna do katalogu scenariuszy, jak w znaczniku testu (domyślnie nazwa pliku).
    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    rel = relative(path)
    feature = feature or path.name
    corpus.feature_files += 1
    corpus.features.add(feature)
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
            scenario = Scenario(name or "(bez nazwy)", Location(rel, number), feature)
            corpus.all_scenarios.append(scenario)
            for tag in dict.fromkeys(feature_tags + rule_tags + tags):
                corpus.scenarios[tag].append(scenario)


def collect(docs: Path, specs: Path, android: Path | None = None) -> Corpus:
    """Zbiera korpus z `docs/**/*.md` (bez `_generated`), `specs/**/*.feature` i `android/**/*.kt`. Brak katalogów nie jest błędem.

    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu pliku.
    """
    corpus = Corpus()
    for path in files_in(docs, "*.md"):
        if "_generated" not in path.relative_to(docs).parts:
            scan_markdown(path, corpus)
    for path in files_in(specs, "*.feature"):
        scan_feature(path, corpus, path.relative_to(specs).as_posix())
    for path in files_in(android, "*.kt"):
        if TEST_SOURCE.match(path.relative_to(android).as_posix()):
            parts = path.relative_to(android).parts
            scan_kotlin(path, corpus, "/".join(parts[: parts.index("src")]))
    return corpus


def files_in(root: Path | None, pattern: str) -> list[Path]:
    """Zwraca pliki `pattern` z `root` (rekurencyjnie) posortowane po ścieżce POSIX względem `root`.

    Gwarantuje: ta sama kolejność na każdym systemie (bez porównań zależnych od wielkości liter w Windows);
    brak katalogu daje pustą listę.
    """
    return sorted(root.rglob(pattern), key=lambda p: p.relative_to(root).as_posix()) if root and root.is_dir() else []


def scan_kotlin(path: Path, corpus: Corpus, module: str = "") -> None:
    """Dodaje do `corpus` znaczniki scenariuszy z pliku testów Kotlin (`src/test*`, `src/*Test*`, bez build i .gradle).
    Linię podobną do znacznika, ale niezgodną ze wzorem, albo znacznik bez nazwy zapisuje jako zły znacznik.
    Identyfikator testu to `module` (katalog przed `src/`), łańcuch klas i obiektów z ciałem `{`, które obejmują
    znacznik (według wcięcia; bez nich — nazwa pliku), i nazwa funkcji testu.

    Wyjątki: OSError/UnicodeDecodeError przy błędzie odczytu.
    """
    lines, rel = code_only(path.read_text(encoding="utf-8")).splitlines(), relative(path)
    owners: list[tuple[int, str]] = []
    for index, line in enumerate(lines):
        marker = read_marker(lines, index)
        location = Location(rel, index + 1)
        indent = len(line) - len(line.lstrip())
        while line.strip() and owners and owners[-1][0] >= indent:
            owners.pop()
        if declared := CLASS.match(line):
            owners.append((indent, declared.group(1)))
        if marker and marker.scenario:
            state, function, line_number = test_state(lines, marker.end)
            owner = ".".join(name for _, name in owners) or path.stem
            test_id = f"{module} · {owner}.{function}" if module else f"{owner}.{function}"
            found = Marker(marker.feature, marker.scenario, marker.partial, state, location, test_id, line_number)
            corpus.markers.append(found)
        elif marker or MARKER_HINT.match(line):
            corpus.bad_markers.append(location)


def code_only(text: str) -> str:
    """Zwraca kod Kotlin z treścią komentarzy blokowych `/* */` (także zagnieżdżonych) i napisów (`"…"`, `\"\"\"…\"\"\"`,
    `'…'`) zamienioną na spacje, żeby znacznik, adnotacja ani nawias w nich nie były brane pod uwagę.

    Gwarantuje: liczba linii i komentarze `//` poza komentarzem blokowym i napisem zostają bez zmian.
    """
    parts, pos = [], 0
    while match := CODE_TOKEN.search(text, pos):
        end, depth = match.end(), 1
        while match.group(0) == "/*" and depth and (inner := BLOCK_COMMENT_PART.search(text, end)):
            depth, end = depth + (1 if inner.group(0) == "/*" else -1), inner.end()
        if match.group(0) == "/*" and depth:
            end = len(text)
        token = text[match.start() : end]
        parts += [text[pos : match.start()], token if token.startswith("//") else re.sub(r"[^\n]", " ", token)]
        pos = end
    return "".join(parts) + text[pos:]


@dataclass(frozen=True)
class MarkerText:
    """Znacznik odczytany z linii kodu: „częściowo”, plik, nazwa scenariusza i indeks pierwszej linii po znaczniku."""

    partial: bool
    feature: str
    scenario: str
    end: int


def read_marker(lines: list[str], index: int) -> MarkerText | None:
    """Zwraca znacznik zaczynający się w linii `index` albo None.

    Gwarantuje: nazwa to tekst po `<plik>.feature:` z tej linii i z kolejnych linii `//` z wcięciem co najmniej
    dwóch spacji po `//`, sklejony jedną spacją; pusta nazwa oznacza znacznik bez nazwy.
    """
    match = MARKER.match(lines[index])
    if not match:
        return None
    parts = [match.group(3)] if match.group(3) else []
    end = index + 1
    while end < len(lines) and (continuation := MARKER_CONTINUATION.match(lines[end])):
        parts.append(continuation.group(1))
        end += 1
    return MarkerText(bool(match.group(1)), match.group(2), " ".join(parts), end)


def test_state(lines: list[str], start: int) -> tuple[str, str, int]:
    """Zwraca (stan, nazwa funkcji, numer linii funkcji od 1). Stan to „aktywny”, gdy od linii `start` są kolejne
    znaczniki i adnotacje (wśród nich adnotacja testu z TEST_ANNOTATIONS), a po nich `fun` z dowolnymi modyfikatorami;
    „wyłączony”, gdy jest też @Ignore albo @Disabled…; inaczej pusty stan, pusta nazwa i 0. Linie są z code_only.

    Gwarantuje: pusta linia, komentarz albo inny kod przed `fun` daje pusty stan; argumenty adnotacji mogą mieć kilka linii.
    """
    names: set[str] = set()
    depth, index = 0, start
    while index < len(lines):
        text = lines[index].strip()
        index += 1
        if depth:
            depth = max(0, depth + text.count("(") - text.count(")"))
            continue
        if not text:
            return "", "", 0
        if marker := read_marker(lines, index - 1):
            index = marker.end
            continue
        while match := ANNOTATION.match(text):
            names.add(match.group(1).rsplit(".", 1)[-1])
            text = text[match.end() :]
            if text.startswith("("):
                depth, text = text.count("(") - text.count(")"), ""
        if (function := FUNCTION.match(text)) and names & TEST_ANNOTATIONS:
            disabled = any(n == "Ignore" or n.startswith("Disabled") for n in names)
            return "wyłączony" if disabled else "aktywny", function.group(1) or function.group(2), index
        if text:
            return "", "", 0
    return "", "", 0


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


def check_tests(corpus: Corpus, release: bool = False) -> list[Finding]:
    """Zwraca wyniki kontroli „scenariusz → test” (decyzja 67).

    Gwarantuje: zły znacznik, znacznik do nieistniejącego pliku .feature albo scenariusza (nazwa znak w znak)
    i znacznik, który nie stoi tuż nad funkcją testu, to „BŁĄD” z miejscem w teście; znacznik nad testem wyłączonym
    to „UWAGA”. Żaden z nich nie pokrywa scenariusza. Dwa scenariusze o tej samej nazwie w jednym pliku to „BŁĄD”
    (znacznik byłby niejednoznaczny). Scenariusz bez testu i scenariusz tylko z testami „częściowo” to „UWAGI”
    w dwóch osobnych listach, a w trybie `release` — „BŁĘDY”. Ten sam identyfikator testu u dwóch różnych funkcji
    to „BŁĄD” (wiersz tabeli pokrycia byłby niejednoznaczny).
    """
    form = "znacznik niezgodny ze wzorem „// Scenariusz: <plik>.feature: <nazwa>”"
    findings = [Finding("BŁĄD", str(loc), form) for loc in corpus.bad_markers]
    first: dict[tuple[str, str], Scenario] = {}
    for scenario in corpus.all_scenarios:
        if (twin := first.setdefault((scenario.feature, scenario.name), scenario)) is not scenario:
            line = twin.location.line
            message = f"scenariusz o tej samej nazwie co w linii {line}, znacznik byłby niejednoznaczny: {scenario.name}"
            findings.append(Finding("BŁĄD", str(scenario.location), message))
    names = set(first)
    for marker in corpus.markers:
        key, severity = (marker.feature, marker.scenario), "BŁĄD"
        if marker.feature not in corpus.features:
            problem = f"znacznik wskazuje nieistniejący plik (ścieżka względem specs/): {marker.feature}"
        elif key not in names:
            problem = f"znacznik wskazuje nieistniejący scenariusz: {marker.feature}: {marker.scenario}"
        elif not marker.test:
            problem = "znacznik nie stoi tuż nad funkcją testu"
        elif marker.test == "wyłączony":
            severity, problem = "UWAGA", f"test wyłączony, nie pokrywa scenariusza: {marker.scenario}"
        else:
            continue
        findings.append(Finding(severity, str(marker.location), problem))
    functions: dict[str, tuple[str, int]] = {}
    for marker in corpus.markers:
        place = (marker.location.path, marker.function_line)
        if marker.test and functions.setdefault(marker.test_id, place) != place:
            other = ":".join(map(str, functions[marker.test_id]))
            message = f"identyfikator testu „{marker.test_id}” ma też inna funkcja ({other}); zmień nazwę funkcji albo klasy"
            findings.append(Finding("BŁĄD", str(marker.location), message))
    level, states = "BŁĄD" if release else "UWAGA", coverage_states(corpus)
    missing = [s for s in corpus.all_scenarios if states[s][1] == "brak"]
    partial = [s for s in corpus.all_scenarios if states[s][1] == "częściowo"]
    findings += [Finding(level, str(s.location), f"scenariusz bez testu: {s.name}") for s in missing]
    return findings + [Finding(level, str(s.location), f"scenariusz tylko z testem częściowym: {s.name}") for s in partial]


def coverage_states(corpus: Corpus) -> dict[Scenario, tuple[list[Marker], str]]:
    """Zwraca dla każdego scenariusza znaczniki, które go pokrywają, i stan: „pełny”, „częściowo” albo „brak”.

    Gwarantuje: pokrywa tylko znacznik nad aktywnym testem; stan „pełny”, gdy choć jeden znacznik nie jest „częściowo”.
    """
    covering = defaultdict(list)
    for marker in corpus.markers:
        if marker.test == "aktywny":
            covering[(marker.feature, marker.scenario)].append(marker)
    states = {}
    for scenario in corpus.all_scenarios:
        markers = covering.get((scenario.feature, scenario.name), [])
        state = "brak" if not markers else "częściowo" if all(m.partial for m in markers) else "pełny"
        states[scenario] = (markers, state)
    return states


def render_coverage(corpus: Corpus) -> str:
    """Zwraca stronę pokrycia: liczby stanów i tabelę [plik, scenariusz, kryteria i reguły, testy, stan] w kolejności
    plików i scenariuszy. Test to jego stały identyfikator (Marker.test_id), bez numeru linii."""
    tags: dict[Scenario, list[str]] = defaultdict(list)
    for tag, items in corpus.scenarios.items():
        for scenario in items if AC_RE.match(tag) or BR_RE.match(tag) else []:
            tags[scenario].append(tag)
    rows = []
    for scenario, (markers, state) in coverage_states(corpus).items():
        tests = [f"`{test_id}`".replace("|", "\\|") for test_id in sorted(dict.fromkeys(m.test_id for m in markers))]
        name = scenario.name.replace("|", "\\|")
        rows.append([f"`{scenario.feature}`", name, join(sorted(tags[scenario], key=natural_key)), join(tests, "<br>"), state])
    counts = [[label, str(sum(row[-1] == state for row in rows))] for label, state in COVERAGE_COUNTS]
    tables = [*plain_table(["Stan", "Liczba"], counts), "", *plain_table(COVERAGE_HEADER, rows)]
    return COVERAGE_INTRO + "\n".join(tables) + "\n"


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


def plain_table(header: list[str], rows: list[list[str]]) -> list[str]:
    """Zwraca linie tabeli Markdown bez wyrównania kolumn: nowy wiersz nie zmienia pozostałych linii."""
    return ["| " + " | ".join(row) + " |" for row in [header, ["---"] * len(header), *rows]]


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


def report_matrix_state(target: Path, stale: list[Path]) -> int:
    """Wypisuje wynik kontroli aktualności macierzy w `target` i zwraca kod wyjścia: 0 aktualna, 1 nieaktualna."""
    if not stale:
        print(f"check_ids: macierz w {target} i strona pokrycia testami są aktualne")
        return 0
    names = ", ".join(str(path) for path in stale)
    print(f"check_ids: BŁĄD: nieaktualne albo brak: {names}; odśwież: python scripts/check_ids.py --matrix {target}")
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
    parser = argparse.ArgumentParser(description="Kontrola ID w docs/**/*.md, specs/**/*.feature i testach Androida.")
    parser.add_argument("--docs", type=Path, default=REPO / "docs", help="katalog dokumentacji")
    parser.add_argument("--specs", type=Path, default=ROOT / "specs", help="katalog plików .feature")
    parser.add_argument("--android", type=Path, default=ROOT / "android", help="katalog z testami Androida (.kt)")
    target = parser.add_mutually_exclusive_group()
    target.add_argument("--matrix", type=Path, help="zapisz macierz N → US → AC → UC → scenariusz do pliku")
    target.add_argument("--check-matrix", type=Path, help="tylko sprawdź, czy macierz w pliku jest aktualna")
    parser.add_argument("--strict", action="store_true", help="kod wyjścia 1, gdy są błędy")
    parser.add_argument("--partial", action="store_true", help="błędy klas ID bez żadnej definicji jako uwagi")
    parser.add_argument("--release", action="store_true", help="na wydanie: scenariusz bez pełnego testu jest błędem")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    """Punkt wejścia. Zwraca 1 przy błędach w trybie --strict, 2 przy błędzie odczytu/zapisu, w pozostałych razach 0.

    Gwarantuje: --matrix zapisuje też stronę pokrycia testami (COVERAGE_PAGE w katalogu dokumentacji). Z --check-matrix
    tylko porównuje macierz i stronę pokrycia z wygenerowanymi z bieżących plików, bez raportu kontroli ID; zwraca 0,
    gdy obie są aktualne, 1, gdy któraś jest nieaktualna albo jej brak, 2 przy błędzie odczytu.
    """
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    try:
        corpus = collect(args.docs, args.specs, args.android)
        coverage = args.docs / COVERAGE_PAGE
        if args.check_matrix:
            stale = [] if matrix_is_current(args.check_matrix, render_blocks(corpus)) else [args.check_matrix]
            if not coverage.is_file() or coverage.read_text(encoding="utf-8") != render_coverage(corpus):
                stale.append(coverage)
            return report_matrix_state(args.check_matrix, stale)
        if args.matrix:
            write_matrix(args.matrix, render_blocks(corpus))
            coverage.parent.mkdir(parents=True, exist_ok=True)
            coverage.write_text(render_coverage(corpus), encoding="utf-8", newline="\n")
            print(f"check_ids: macierz zapisana do {args.matrix}, pokrycie testami do {coverage}")
        findings = check(corpus, args.partial) + check_tests(corpus, args.release) + check_screen_page(args.docs)
    except (OSError, UnicodeDecodeError) as exc:
        print(f"check_ids: BŁĄD: {exc}", file=sys.stderr)
        return 2
    print_report(corpus, findings)
    has_errors = any(f.level == "BŁĄD" for f in findings)
    return 1 if args.strict and has_errors else 0


if __name__ == "__main__":
    sys.exit(main())
