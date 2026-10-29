# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Testy kontroli odwołań w wymaganiach ekranów (check_ids.screen_rows) na przykładowych plikach z katalogu `dane`."""

from __future__ import annotations

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


if __name__ == "__main__":
    unittest.main()
