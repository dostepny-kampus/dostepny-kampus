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


if __name__ == "__main__":
    unittest.main()
