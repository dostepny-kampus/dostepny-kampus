# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Testy kontroli odwołań w wymaganiach ekranów (check_ids.screen_rows) i kontroli „scenariusz → test”
(check_ids.check_tests) na przykładowych plikach z katalogu `dane`."""

from __future__ import annotations

import contextlib
import io
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))

import check_ids  # noqa: E402  (ścieżka do skryptu dodana wyżej)

DATA = HERE / "dane"


def lines_of(findings: list[check_ids.Finding]) -> list[int]:
    """Zwraca numery linii z wyników w kolejności wystąpienia."""
    return [int(f.location.rsplit(":", 1)[1]) for f in findings]


class ScreenRowsTest(unittest.TestCase):
    def test_correct_page_has_no_findings(self) -> None:
        self.assertEqual(check_ids.screen_rows(DATA / "ekrany-poprawne.md"), [])

    def test_rows_without_requirement_id_are_errors_with_line_numbers(self) -> None:
        findings = check_ids.screen_rows(DATA / "ekrany-bledne.md")
        self.assertEqual(lines_of(findings), [5, 6, 10, 14, 15, 17, 18])
        self.assertTrue(all(f.level == "BŁĄD" for f in findings))

    def test_message_quotes_the_reference_cell(self) -> None:
        findings = check_ids.screen_rows(DATA / "ekrany-bledne.md")
        self.assertIn("„decyzja 53”", findings[0].message)
        self.assertIn("„—”", findings[4].message)

    def test_row_shorter_than_reference_column_is_an_error(self) -> None:
        findings = check_ids.screen_rows(DATA / "ekrany-bledne.md")
        self.assertIn("bez kolumny „Odwołanie”", findings[-1].message)
        self.assertIn("2 komórek", findings[-1].message)

    def test_missing_screen_page_is_not_an_error(self) -> None:
        with tempfile.TemporaryDirectory() as docs:
            self.assertEqual(check_ids.check_screen_page(Path(docs)), [])

    def test_screen_page_is_checked_in_docs_directory(self) -> None:
        with tempfile.TemporaryDirectory() as docs:
            page = Path(docs) / check_ids.SCREEN_PAGE
            page.parent.mkdir(parents=True)
            page.write_text((DATA / "ekrany-bledne.md").read_text(encoding="utf-8"), encoding="utf-8")
            self.assertEqual(len(check_ids.check_screen_page(Path(docs))), 7)


def scenario_tests(android: str) -> list[tuple[str, int, str]]:
    """Zwraca wyniki check_tests dla `dane/scenariusze` i znaczników z `dane/<android>` jako (poziom, linia, komunikat)."""
    findings = check_ids.check_tests(check_ids.collect(DATA / "brak", DATA / "scenariusze", DATA / android))
    return [(f.level, line, f.message) for f, line in zip(findings, lines_of(findings))]


class ScenarioTestsTest(unittest.TestCase):
    def test_correct_markers_leave_only_missing_and_partial_lists(self) -> None:
        expected = [("UWAGA", 12, "scenariusz bez testu: Bez testu")]
        expected.append(("UWAGA", 8, "scenariusz tylko z testem częściowym: Tylko częściowo"))
        self.assertEqual(scenario_tests("android-poprawne"), expected)

    def test_wrong_markers_are_errors_with_test_line(self) -> None:
        errors = [(line, message) for level, line, message in scenario_tests("android-bledne") if level == "BŁĄD"]
        self.assertEqual([line for line, _ in errors], [31, 40, 10, 14, 18, 23, 35])
        self.assertTrue(all("niezgodny ze wzorem" in m for _, m in errors[:2]))
        self.assertIn('nieistniejący scenariusz: kampus.feature: Wyszukanie "A-1 329"', errors[2][1])
        self.assertIn("nieistniejący plik (ścieżka względem specs/): mapa.feature", errors[3][1])
        self.assertTrue(all("nie stoi tuż nad funkcją testu" in m for _, m in errors[4:6]))
        self.assertTrue(errors[6][1].endswith("kampus.feature: Zgłoszenie „Brak windy"))

    def test_disabled_test_is_a_warning_and_wrong_markers_cover_nothing(self) -> None:
        warnings = [(line, message) for level, line, message in scenario_tests("android-bledne") if level == "UWAGA"]
        self.assertEqual(warnings[0], (26, "test wyłączony, nie pokrywa scenariusza: Pełny test"))
        self.assertEqual([line for line, _ in warnings[1:]], [4, 6, 8, 10, 12, 14, 16])

    def test_only_test_source_sets_outside_build_are_scanned(self) -> None:
        self.assertEqual({level for level, _, _ in scenario_tests("brak")}, {"UWAGA"})
        with tempfile.TemporaryDirectory() as android:
            for folder in ("src/main", "src/testDebug", "src/androidTest", "build/x/src/test", "src/screenshotTest"):
                (Path(android) / folder).mkdir(parents=True)
                (Path(android) / folder / "A.kt").write_text("// scenariusz: brak sieci\n", encoding="utf-8")
            bad = check_ids.collect(DATA / "brak", DATA / "brak", Path(android)).bad_markers
        self.assertEqual(sorted(Path(loc.path).parent.name for loc in bad), ["androidTest", "screenshotTest", "testDebug"])

    def test_release_flag_turns_warnings_into_errors(self) -> None:
        args = ["--docs", str(DATA / "brak"), "--specs", str(DATA / "scenariusze"), "--android", str(DATA / "android-poprawne")]
        for flags, level in (([], "UWAGA"), (["--release"], "BŁĄD")):
            with contextlib.redirect_stdout(io.StringIO()) as output:
                check_ids.main([*args, *flags])
            self.assertEqual([row.split()[0] for row in output.getvalue().splitlines() if "scenariusz bez testu" in row], [level])


MARKER = "// Scenariusz: kampus.feature: Pełny test\n"


def kotlin_markers(source: str, newline: str = "\n") -> list[tuple[str, str]]:
    """Zwraca (nazwa scenariusza, stan testu) znaczników z pliku `src/test/A.kt` o treści `source` (końce linii `newline`)."""
    with tempfile.TemporaryDirectory() as android:
        path = Path(android) / "src" / "test" / "A.kt"
        path.parent.mkdir(parents=True)
        path.write_bytes(source.replace("\n", newline).encode("utf-8"))
        return [(m.scenario, m.test) for m in check_ids.collect(DATA / "brak", DATA / "brak", Path(android)).markers]


class KotlinParsingTest(unittest.TestCase):
    def test_markers_in_block_comments_and_strings_are_ignored(self) -> None:
        sources = [
            f"/*\n{MARKER}@Test\nfun a() = Unit\n*/\n",
            f"/* zewnętrzny /* wewnętrzny */\n{MARKER}@Test\nfun a() = Unit\n*/\n",
            f'val opis = """\n{MARKER}"""\n',
            'val opis = "// Scenariusz: kampus.feature: Pełny test"\n',
        ]
        for source in sources:
            self.assertEqual(kotlin_markers(source), [], source)

    def test_test_annotation_in_comment_or_string_does_not_count(self) -> None:
        for between in ("/* @Test */\n", "@DisplayName(\"@Test\")\n"):
            self.assertEqual(kotlin_markers(f"{MARKER}{between}fun a() = Unit\n"), [("Pełny test", "")], between)

    def test_only_listed_annotations_mark_a_test(self) -> None:
        for annotation, state in [("Test", "aktywny"), ("RepeatedTest(3)", "aktywny"), ("TestFactory", "aktywny"),
                                  ("TestTemplate", "aktywny"), ("SmallTest", ""), ("MediumTest", ""), ("LargeTest", "")]:
            self.assertEqual(kotlin_markers(f"{MARKER}@{annotation}\nfun a() = Unit\n"), [("Pełny test", state)], annotation)

    def test_parenthesis_inside_annotation_string_is_ignored(self) -> None:
        source = f'{MARKER}@DisplayName("sala (A-1")\n@ValueSource(\n    chars = [\'(\'],\n    strings = [")(", "("],\n)\n'
        self.assertEqual(kotlin_markers(source + "@ParameterizedTest\nfun a(s: String) = Unit\n"), [("Pełny test", "aktywny")])

    def test_any_modifiers_before_fun(self) -> None:
        for modifiers in ("internal inline", "private suspend", "tailrec", "public operator", "override"):
            self.assertEqual(kotlin_markers(f"{MARKER}@Test\n{modifiers} fun a() = Unit\n"), [("Pełny test", "aktywny")], modifiers)

    def test_crlf_line_endings(self) -> None:
        source = "// Scenariusz: kampus.feature: Zgłoszenie „Brak windy\n//   przy wejściu”\n@Test\nfun a() = Unit\n"
        self.assertEqual(kotlin_markers(source, "\r\n"), [("Zgłoszenie „Brak windy przy wejściu”", "aktywny")])


class KdocMarkersTest(unittest.TestCase):
    def test_full_and_partial_marker_in_multiline_kdoc(self) -> None:
        for partial in ("", " (częściowo)"):
            source = f"/**\n * Wiersz: opis.\n * Scenariusz{partial}: kampus.feature: Pełny test\n */\n@Test\nfun a() = Unit\n"
            self.assertEqual(kotlin_markers(source), [("Pełny test", "aktywny")], partial)

    def test_final_period_of_kdoc_marker_is_not_part_of_the_name(self) -> None:
        for source in (
            "/** Scenariusz: kampus.feature: Pełny test. */\n@Test\nfun a() = Unit\n",
            "/**\n * Scenariusz: kampus.feature: Pełny\n *   test.\n */\n@Test\nfun a() = Unit\n",
        ):
            self.assertEqual(kotlin_markers(source), [("Pełny test", "aktywny")], source)

    def test_marker_in_single_line_kdoc(self) -> None:
        source = "/** Scenariusz: kampus.feature: Pełny test */\n@Test\nfun a() = Unit\n"
        self.assertEqual(kotlin_markers(source), [("Pełny test", "aktywny")])

    def test_kdoc_continuation_extends_the_name(self) -> None:
        source = "/**\n * Scenariusz: kampus.feature: Zgłoszenie „Brak windy\n *   przy wejściu”\n */\n@Test\nfun a() = Unit\n"
        self.assertEqual(kotlin_markers(source), [("Zgłoszenie „Brak windy przy wejściu”", "aktywny")])

    def test_plain_kdoc_line_after_marker_is_not_a_continuation(self) -> None:
        source = "/**\n * Scenariusz: kampus.feature: Pełny test\n * Zwykły wiersz opisu.\n */\n@Test\nfun a() = Unit\n"
        self.assertEqual(kotlin_markers(source), [("Pełny test", "aktywny")])

    def test_kdoc_marker_without_test_has_empty_state(self) -> None:
        self.assertEqual(kotlin_markers("/** Scenariusz: kampus.feature: Pełny test */\nval a = 1\n"), [("Pełny test", "")])

    def test_ordinary_kdoc_and_block_comment_markers(self) -> None:
        self.assertEqual(kotlin_markers("/**\n * Opis bez znacznika.\n */\n@Test\nfun a() = Unit\n"), [])
        self.assertEqual(kotlin_markers("/* Scenariusz: kampus.feature: Pełny test */\n@Test\nfun a() = Unit\n"), [])

    def test_kdoc_marker_equals_line_comment_marker(self) -> None:
        line = kotlin_markers("// Scenariusz (częściowo): kampus.feature: A\n//   b\n@Test\nfun a() = Unit\n")
        kdoc = kotlin_markers("/**\n * Scenariusz (częściowo): kampus.feature: A\n *   b\n */\n@Test\nfun a() = Unit\n")
        self.assertEqual(line, kdoc)


def table_rows(page: str) -> list[list[str]]:
    """Zwraca komórki wierszy tabel Markdown ze strony `page` (bez nagłówków i separatorów)."""
    rows = [check_ids.table_cells(line) for line in page.splitlines()]
    return [r for r in rows if r and r[0] not in ("Warstwa", "Plik")]


class CoverageTest(unittest.TestCase):
    def test_duplicate_scenario_name_in_one_file_is_an_error(self) -> None:
        with tempfile.TemporaryDirectory() as specs:
            feature = "@aplikacja\nFunkcja: Dwa razy\n  Scenariusz: Taki sam\n    Wtedy a\n\n  Scenariusz: Taki sam\n    Wtedy b\n"
            (Path(specs) / "dwa.feature").write_text(feature, encoding="utf-8")
            corpus = check_ids.collect(DATA / "brak", Path(specs), DATA / "brak")
        errors = [f for f in check_ids.check_tests(corpus) if f.level == "BŁĄD"]
        self.assertEqual(lines_of(errors), [6])
        self.assertIn("tej samej nazwie co w linii 3", errors[0].message)

    def test_coverage_page_lists_counts_and_tests_of_each_scenario(self) -> None:
        corpus = check_ids.collect(DATA / "brak", DATA / "scenariusze", DATA / "android-poprawne")
        rows = table_rows(check_ids.render_coverage(corpus))
        self.assertEqual(rows[:2], [["Aplikacja", "5", "1", "1"], ["Serwer", "0", "0", "0"]])
        by_name = {r[1]: (r[0], r[4], r[5]) for r in rows[2:]}
        self.assertEqual(len(by_name), 7)
        both = "`PoprawneTest.calosc`<br>`PoprawneTest.wyszukanie`"
        self.assertEqual(by_name["Częściowo i w całości"], ("`kampus.feature`", both, "pełny"))
        self.assertEqual(by_name["Tylko częściowo"][1:], ("`PoprawneTest.czesciowo`", "częściowo"))
        self.assertEqual(by_name["Bez testu"][1:], ("—", "brak"))

    def test_test_id_is_module_class_and_function_without_line_number(self) -> None:
        source = f"package x\n\nclass FirstLaunchTest {{\n    {MARKER}    @Test\n    fun `pomoc do oznaczenia`() = Unit\n}}\n"
        with tempfile.TemporaryDirectory() as android:
            path = Path(android) / "core" / "data" / "src" / "test" / "kotlin" / "FirstLaunchTest.kt"
            path.parent.mkdir(parents=True)
            path.write_text(source, encoding="utf-8")
            (Path(android) / "src" / "test").mkdir(parents=True)
            (Path(android) / "src" / "test" / "Bez.kt").write_text(f"{MARKER}@Test\nfun bezKlasy() = Unit\n", encoding="utf-8")
            ids = [m.test_id for m in check_ids.collect(DATA / "brak", DATA / "brak", Path(android)).markers]
        self.assertEqual(ids, ["core/data · FirstLaunchTest.pomoc do oznaczenia", "Bez.bezKlasy"])

    def test_owner_follows_nested_classes_and_objects(self) -> None:
        source = (
            f"class OuterTest {{\n    data class Wiersz(val a: Int)\n\n    @Nested\n    inner class InnerTest {{\n"
            f"        {MARKER}        @Test\n        fun w() = Unit\n    }}\n\n"
            f"    {MARKER}    @Test\n    fun String.na() = Unit\n}}\n\n"
            f"object HelpersTest {{\n    {MARKER}    @Test\n    fun h() = Unit\n}}\n"
        )
        with tempfile.TemporaryDirectory() as android:
            path = Path(android) / "src" / "test" / "A.kt"
            path.parent.mkdir(parents=True)
            path.write_text(source, encoding="utf-8")
            ids = [m.test_id for m in check_ids.collect(DATA / "brak", DATA / "brak", Path(android)).markers]
        self.assertEqual(ids, ["OuterTest.InnerTest.w", "OuterTest.na", "HelpersTest.h"])

    def test_same_test_id_for_two_functions_is_an_error(self) -> None:
        source = f"class ATest {{\n    {MARKER}    @Test\n    fun a() = Unit\n\n    {MARKER}    @Test\n    fun a(x: Int) = Unit\n}}\n"
        with tempfile.TemporaryDirectory() as android:
            (Path(android) / "src" / "test").mkdir(parents=True)
            (Path(android) / "src" / "test" / "A.kt").write_text(source, encoding="utf-8")
            corpus = check_ids.collect(DATA / "brak", DATA / "scenariusze", Path(android))
        errors = [f for f in check_ids.check_tests(corpus) if f.level == "BŁĄD"]
        self.assertEqual(lines_of(errors), [6])
        self.assertIn("identyfikator testu „ATest.a” ma też inna funkcja", errors[0].message)

    def test_coverage_table_is_not_padded(self) -> None:
        page = check_ids.render_coverage(check_ids.collect(DATA / "brak", DATA / "scenariusze", DATA / "android-poprawne"))
        self.assertIn("| `kampus.feature` | Bez testu | — | aplikacja | — | brak |", page.splitlines())
        self.assertNotIn("  |", page)

    def test_check_matrix_does_not_need_the_coverage_page(self) -> None:
        with tempfile.TemporaryDirectory() as docs, contextlib.redirect_stdout(io.StringIO()):
            args = ["--docs", docs, "--specs", str(DATA / "scenariusze"), "--android", str(DATA / "android-poprawne")]
            page, coverage = str(Path(docs) / "sledzenie.md"), Path(docs) / check_ids.COVERAGE_PAGE
            check_ids.main([*args, "--matrix", page])
            self.assertTrue(coverage.is_file())
            coverage.unlink()
            self.assertEqual(check_ids.main([*args, "--check-matrix", page]), 0)

    def test_coverage_page_with_the_same_content_is_not_rewritten(self) -> None:
        with tempfile.TemporaryDirectory() as docs:
            corpus = check_ids.collect(DATA / "brak", DATA / "scenariusze", DATA / "android-poprawne")
            page = check_ids.write_coverage(Path(docs), corpus)
            written = page.stat().st_mtime_ns
            self.assertEqual(check_ids.write_coverage(Path(docs), corpus).stat().st_mtime_ns, written)
            self.assertEqual(page.read_text(encoding="utf-8"), check_ids.render_coverage(corpus))


LAYERED = (
    "Funkcja: Warstwy\n\n  @aplikacja\n  Scenariusz: W aplikacji\n    Wtedy a\n\n  @serwer\n  Scenariusz: Na serwerze\n"
    "    Wtedy b\n\n  @aplikacja @serwer\n  Scenariusz: W obu\n    Wtedy c\n\n  Scenariusz: Bez warstwy\n    Wtedy d\n"
)


def layered(tests: str, suite: str = "") -> check_ids.Corpus:
    """Zwraca korpus z plikiem `warstwy.feature` (LAYERED), testami Androida o treści `tests` i zestawem Cucumber
    serwera o treści `suite` (pusty: serwer bez zestawu)."""
    with tempfile.TemporaryDirectory() as root:
        (Path(root) / "backend" / "src" / "test" / "java").mkdir(parents=True)
        (Path(root) / "backend" / "src" / "test" / "java" / "ServerSpecs.java").write_text(suite, encoding="utf-8")
        (Path(root) / "specs").mkdir()
        (Path(root) / "specs" / "warstwy.feature").write_text(LAYERED, encoding="utf-8")
        (Path(root) / "android" / "src" / "test").mkdir(parents=True)
        (Path(root) / "android" / "src" / "test" / "WarstwyTest.kt").write_text(tests, encoding="utf-8")
        return check_ids.collect(DATA / "brak", Path(root) / "specs", Path(root) / "android", Path(root) / "backend")


SUITE = '@Suite\n@SelectFile("../specs/warstwy.feature")\npublic final class ServerSpecs {\n}\n'


def layer_marker(name: str, function: str) -> str:
    """Zwraca test Kotlin ze znacznikiem pełnego pokrycia scenariusza `name` z pliku `warstwy.feature`."""
    return f"// Scenariusz: warstwy.feature: {name}\n@Test\nfun {function}() = Unit\n\n"


class LayersTest(unittest.TestCase):
    def test_scenario_without_layer_tag_is_an_error_with_its_place(self) -> None:
        errors = [f for f in check_ids.check_tests(layered("")) if f.level == "BŁĄD"]
        self.assertEqual(lines_of(errors), [15])
        self.assertIn("scenariusz bez tagu warstwy (@aplikacja albo @serwer): Bez warstwy", errors[0].message)

    def test_summary_counts_each_layer_separately(self) -> None:
        rows = table_rows(check_ids.render_coverage(layered(layer_marker("W aplikacji", "a"))))
        self.assertEqual(rows[:2], [["Aplikacja", "1", "0", "2"], ["Serwer", "0", "0", "2"]])
        self.assertEqual([r[3] for r in rows[2:]], ["aplikacja", "serwer", "aplikacja, serwer", "—"])

    def test_two_layer_scenario_with_app_test_only_is_partial(self) -> None:
        rows = table_rows(check_ids.render_coverage(layered(layer_marker("W obu", "o"))))
        self.assertEqual({r[1]: r[5] for r in rows[2:]}["W obu"], "częściowo")
        self.assertEqual(rows[:2], [["Aplikacja", "1", "0", "2"], ["Serwer", "0", "0", "2"]])

    def test_scenario_run_by_server_suite_is_full_on_server(self) -> None:
        rows = table_rows(check_ids.render_coverage(layered("", SUITE)))
        self.assertEqual(rows[:2], [["Aplikacja", "0", "0", "3"], ["Serwer", "2", "0", "0"]])
        self.assertEqual({r[1]: (r[4], r[5]) for r in rows[2:]}["Na serwerze"], ("`backend · ServerSpecs`", "pełny"))

    def test_two_layer_scenario_needs_app_test_and_server_suite(self) -> None:
        only_server = table_rows(check_ids.render_coverage(layered("", SUITE)))
        both = table_rows(check_ids.render_coverage(layered(layer_marker("W obu", "o"), SUITE)))
        self.assertEqual({r[1]: r[5] for r in only_server[2:]}["W obu"], "częściowo")
        self.assertEqual({r[1]: r[5] for r in both[2:]}["W obu"], "pełny")

    def test_planned_scenario_is_not_run_by_server_suite(self) -> None:
        with tempfile.TemporaryDirectory() as root:
            (Path(root) / "specs").mkdir()
            text = LAYERED.replace("  @serwer\n  Scenariusz: Na serwerze", "  @serwer @do-zrobienia\n  Scenariusz: Na serwerze")
            (Path(root) / "specs" / "warstwy.feature").write_text(text, encoding="utf-8")
            (Path(root) / "src" / "test" / "java").mkdir(parents=True)
            (Path(root) / "src" / "test" / "java" / "ServerSpecs.java").write_text(SUITE, encoding="utf-8")
            corpus = check_ids.collect(DATA / "brak", Path(root) / "specs", DATA / "brak", Path(root))
        rows = table_rows(check_ids.render_coverage(corpus))
        self.assertEqual({r[1]: r[5] for r in rows[2:]}["Na serwerze"], "brak")

    def test_suite_of_another_file_does_not_cover(self) -> None:
        rows = table_rows(check_ids.render_coverage(layered("", SUITE.replace("warstwy", "inne"))))
        self.assertEqual(rows[1], ["Serwer", "0", "0", "2"])

    def test_android_marker_on_server_only_scenario_is_an_error(self) -> None:
        findings = check_ids.check_tests(layered(layer_marker("Na serwerze", "s")))
        found = [(f.level, f.message) for f in findings if "Androida" in f.message]
        self.assertEqual(found, [("BŁĄD", "znacznik testu Androida przy scenariuszu bez tagu @aplikacja: Na serwerze")])


OUTLINE = (
    "@aplikacja\nFunkcja: Szablon\n\n  Szablon scenariusza: Decyzja\n    Jeżeli osoba <decyzja>\n    Wtedy <wynik>\n\n"
    "    Przykłady:\n      | decyzja  | wynik |\n      | potwierdzi | A-1 |\n      | wybierze | B-1 |\n\n"
    "    Przykłady: poza budynkiem\n      | decyzja  | wynik |\n      | wybierze | poza |\n\n"
    "  Scenariusz: Zwykły\n    Wtedy a\n      | a | b |\n      | a | c |\n"
)


def outline(*markers: str) -> check_ids.Corpus:
    """Zwraca korpus z plikiem `szablon.feature` (OUTLINE) i testami Androida ze znacznikami o kwalifikatorach `markers`."""
    tests = "".join(f"// Scenariusz{m}\n@Test\nfun t{n}() = Unit\n\n" for n, m in enumerate(markers))
    with tempfile.TemporaryDirectory() as root:
        (Path(root) / "specs").mkdir()
        (Path(root) / "specs" / "szablon.feature").write_text(OUTLINE, encoding="utf-8")
        (Path(root) / "android" / "src" / "test").mkdir(parents=True)
        (Path(root) / "android" / "src" / "test" / "SzablonTest.kt").write_text(tests, encoding="utf-8")
        return check_ids.collect(DATA / "brak", Path(root) / "specs", Path(root) / "android")


def outline_state(*markers: str) -> str:
    """Zwraca stan szablonu „Decyzja” na stronie pokrycia dla znaczników `markers`."""
    rows = table_rows(check_ids.render_coverage(outline(*markers)))
    return {r[1]: r[5] for r in rows[2:]}["Decyzja"]


ROW = " (przykład: {}): szablon.feature: Decyzja"


class ExampleRowsTest(unittest.TestCase):
    def test_outline_is_full_when_every_row_has_a_full_marker(self) -> None:
        rows = [ROW.format("potwierdzi"), ROW.format("wybierze | B-1"), ROW.format("wybierze | poza")]
        self.assertEqual(outline_state(*rows), "pełny")
        self.assertEqual([f for f in check_ids.check_tests(outline(*rows)) if f.level == "BŁĄD"], [])

    def test_outline_is_partial_when_a_row_has_no_full_marker(self) -> None:
        self.assertEqual(outline_state(ROW.format("potwierdzi"), ROW.format("wybierze | B-1")), "częściowo")
        partial = " (częściowo, przykład: wybierze | poza): szablon.feature: Decyzja"
        self.assertEqual(outline_state(ROW.format("potwierdzi"), ROW.format("wybierze | B-1"), partial), "częściowo")

    def test_whole_scenario_marker_still_covers_an_outline(self) -> None:
        self.assertEqual(outline_state(": szablon.feature: Decyzja"), "pełny")

    def test_unknown_or_ambiguous_row_and_row_of_plain_scenario_are_errors(self) -> None:
        markers = [ROW.format("odrzuci"), ROW.format("wybierze"), " (przykład: a): szablon.feature: Zwykły"]
        errors = [f for f in check_ids.check_tests(outline(*markers)) if f.level == "BŁĄD"]
        self.assertEqual(lines_of(errors), [1, 5, 9])
        self.assertTrue(all("wiersz przykładów, którego nie ma albo który nie jest jednoznaczny" in f.message for f in errors))
        self.assertEqual(outline_state(*markers), "częściowo")


def drops(before: check_ids.Corpus, after: check_ids.Corpus) -> list[str]:
    """Zwraca komunikaty spadku pokrycia między korpusem `before` a korpusem `after`."""
    return [f.message for f in check_ids.coverage_drops(after, before)]


class CoverageDropTest(unittest.TestCase):
    def test_removed_full_marker_is_an_error(self) -> None:
        found = drops(layered(layer_marker("W aplikacji", "a")), layered(""))
        self.assertEqual(found, ["pokrycie scenariusza spadło z „pełny” na „brak”: W aplikacji"])

    def test_full_marker_turned_partial_is_an_error(self) -> None:
        partial = "// Scenariusz (częściowo): warstwy.feature: W aplikacji\n@Test\nfun a() = Unit\n"
        found = drops(layered(layer_marker("W aplikacji", "a")), layered(partial))
        self.assertEqual(found, ["pokrycie scenariusza spadło z „pełny” na „częściowo”: W aplikacji"])

    def test_new_marker_and_unchanged_state_are_not_errors(self) -> None:
        self.assertEqual(drops(layered(""), layered(layer_marker("W aplikacji", "a"))), [])
        self.assertEqual(drops(layered(""), layered("")), [])

    def test_scenario_absent_from_either_side_is_not_a_drop(self) -> None:
        covered = outline(": szablon.feature: Decyzja")
        self.assertEqual(drops(covered, layered("")), [])
        self.assertEqual(drops(layered(layer_marker("W aplikacji", "a")), outline()), [])

    def test_baseline_is_counted_from_files_of_the_revision(self) -> None:
        specs, android = check_ids.ROOT / "specs", check_ids.ROOT / "android"
        before = check_ids.baseline_corpus("HEAD", specs, android, check_ids.ROOT / "backend")
        self.assertIsNotNone(before)
        self.assertGreater(len(before.all_scenarios), 0)
        self.assertGreater(len(before.markers), 0)
        self.assertEqual(check_ids.coverage_drops(before, before), [])

    def test_directory_absent_from_revision_is_empty(self) -> None:
        root = check_ids.ROOT
        before = check_ids.baseline_corpus("HEAD", root / "specs", root / "nie-ma-takiego-katalogu", root / "backend")
        self.assertIsNotNone(before)
        self.assertEqual(before.markers, [])
        self.assertGreater(len(before.all_scenarios), 0)


def specs(feature: str, tests: str = "") -> check_ids.Corpus:
    """Zwraca korpus z plikiem `nowe.feature` o treści `feature` i testami Androida o treści `tests`."""
    with tempfile.TemporaryDirectory() as root:
        (Path(root) / "specs").mkdir()
        (Path(root) / "specs" / "nowe.feature").write_text(feature, encoding="utf-8")
        (Path(root) / "android" / "src" / "test").mkdir(parents=True)
        (Path(root) / "android" / "src" / "test" / "NoweTest.kt").write_text(tests, encoding="utf-8")
        return check_ids.collect(DATA / "brak", Path(root) / "specs", Path(root) / "android")


OLD = "@aplikacja\nFunkcja: Nowe\n\n  Scenariusz: Stary\n    Jeżeli a\n    Wtedy b\n"
NEW = OLD + "\n  Scenariusz: Nowy\n    Wtedy c\n"
NEW_TEST = "// Scenariusz: nowe.feature: Nowy\n@Test\nfun n() = Unit\n"


def unplanned(before: str, after: str, tests: str = "") -> list[str]:
    """Zwraca nazwy scenariuszy z błędem „nowy albo zmieniony” między treścią `before` a `after`."""
    findings = check_ids.unplanned_scenarios(specs(after, tests), specs(before))
    return [f.message.rsplit(": ", 1)[1] for f in findings if f.level == "BŁĄD"]


class NewScenarioTest(unittest.TestCase):
    def test_new_scenario_without_test_and_task_tag_is_an_error(self) -> None:
        self.assertEqual(unplanned(OLD, NEW), ["Nowy"])

    def test_new_scenario_with_full_test_passes(self) -> None:
        self.assertEqual(unplanned(OLD, NEW, NEW_TEST), [])
        self.assertEqual(unplanned(OLD, NEW, NEW_TEST.replace("Scenariusz:", "Scenariusz (częściowo):")), ["Nowy"])

    def test_new_scenario_with_task_tag_passes(self) -> None:
        tagged = NEW.replace("  Scenariusz: Nowy", "  # test w #5\n  @do-zrobienia\n  Scenariusz: Nowy")
        self.assertEqual(unplanned(OLD, tagged), [])

    def test_unchanged_scenario_without_test_is_not_an_error(self) -> None:
        self.assertEqual(unplanned(OLD, OLD), [])
        self.assertEqual(unplanned(OLD, OLD.replace("@aplikacja\n", "@aplikacja @US1.1\n")), [])

    def test_changed_steps_or_examples_make_the_scenario_new(self) -> None:
        self.assertEqual(unplanned(OLD, OLD.replace("Wtedy b", "Wtedy c")), ["Stary"])
        table = OLD.replace("Scenariusz: Stary", "Szablon scenariusza: Stary") + "    Przykłady:\n      | x |\n      | 1 |\n"
        self.assertEqual(unplanned(table, table.replace("| 1 |", "| 2 |")), ["Stary"])

    def test_unreadable_revision_gives_no_baseline(self) -> None:
        root = check_ids.ROOT
        self.assertIsNone(check_ids.baseline_corpus("nie-ma-takiej-rewizji", root / "specs", root / "android", root / "backend"))

    def test_scenario_keeps_its_requirement_tags_and_body(self) -> None:
        corpus = specs(OLD.replace("  Scenariusz: Stary", "  @US1.1\n  Scenariusz: Stary"))
        self.assertEqual([s.name for s in corpus.scenarios["US1.1"]], ["Stary"])
        self.assertEqual(corpus.bodies[("nowe.feature", "Stary")], ["Jeżeli a", "Wtedy b"])


if __name__ == "__main__":
    unittest.main()


def code_findings(source: str, name: str = "A.kt") -> list[str]:
    """Zwraca komunikaty kontroli linii `Wymagania:` dla jednego pliku kodu przy zdefiniowanych US1.1 i NFR-19."""
    corpus = check_ids.Corpus()
    for ident in ("US1.1", "NFR-19"):
        corpus.definitions[ident].append(check_ids.Location("docs/wymagania.md", 1))
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / name
        path.write_text(source, encoding="utf-8")
        check_ids.scan_code_requirements(path, corpus)
    return [f"{f.location.rsplit(':', 1)[1]} {f.message}" for f in check_ids.code_requirement_findings(corpus)]


class CodeRequirementsTest(unittest.TestCase):
    def test_defined_ids_pass(self) -> None:
        source = "/**\n * Opis.\n *\n * Wymagania: US1.1, NFR-19\n */\nfun a() = 1\n"
        self.assertEqual([], code_findings(source))

    def test_undefined_id_is_error(self) -> None:
        source = "/**\n * Opis.\n *\n * Wymagania: US1.1, US9.9\n */\nfun a() = 1\n"
        self.assertEqual(["4 linia Wymagania wskazuje niezdefiniowane ID: US9.9"], code_findings(source))

    def test_single_line_kdoc_is_read(self) -> None:
        self.assertEqual(
            ["1 linia Wymagania wskazuje niezdefiniowane ID: NFR-99"],
            code_findings("/** Opis. Wymagania: NFR-19, NFR-99 */\nfun a() = 1\n"),
        )

    def test_javadoc_paragraph_is_read(self) -> None:
        source = "/**\n * Opis.\n *\n * <p>Wymagania: BR-77</p>\n */\nclass A {}\n"
        self.assertEqual(["4 linia Wymagania wskazuje niezdefiniowane ID: BR-77"], code_findings(source, "A.java"))

    def test_non_id_item_is_error(self) -> None:
        source = "/**\n * Opis.\n *\n * Wymagania: US1.1, wymagania wspólne\n */\nfun a() = 1\n"
        self.assertEqual(["4 „wymagania wspólne” w linii Wymagania nie jest identyfikatorem"], code_findings(source))

    def test_prose_with_word_is_not_a_line(self) -> None:
        self.assertEqual([], code_findings("/**\n * Wymagania wspólne mówią o tym.\n */\nfun a() = 1\n"))
