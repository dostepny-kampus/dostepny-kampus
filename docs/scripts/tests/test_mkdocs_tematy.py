# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Testy słownika tematów (mkdocs_tematy.read, mkdocs_tematy.render) na plikach tymczasowych i na pliku z repozytorium."""
from __future__ import annotations
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
import mkdocs_tematy  # noqa: E402  (ścieżka do skryptu dodana wyżej)

HEADER = "nazwa\trodzaj\ttermin\topis\n"


def written(text: str) -> Path:
    """Zapisuje treść do pliku tymczasowego i zwraca jego ścieżkę."""
    handle = tempfile.NamedTemporaryFile("w", suffix=".tsv", delete=False, encoding="utf-8")
    with handle:
        handle.write(text)
    return Path(handle.name)


class ReadTest(unittest.TestCase):
    def test_repository_dictionary_is_valid(self) -> None:
        entries = mkdocs_tematy.read(HERE.parents[1] / "docs" / mkdocs_tematy.SOURCE)
        self.assertIn("submission", [e["nazwa"] for e in entries], "słownik nie zawiera tematu zgłoszenia")

    def test_rejects_forbidden_name(self) -> None:
        path = written(HEADER + "utils\ttemat\tnarzędzia\tRóżne funkcje.\n")
        with self.assertRaises(mkdocs_tematy.DictionaryError, msg="zakazana nazwa przeszła"):
            mkdocs_tematy.read(path)

    def test_rejects_unsorted_names(self) -> None:
        path = written(HEADER + "photo\ttemat\tzdjęcie\tZdjęcie.\narea\ttemat\tteren\tTeren.\n")
        with self.assertRaises(mkdocs_tematy.DictionaryError, msg="nieposortowane nazwy przeszły"):
            mkdocs_tematy.read(path)

    def test_rejects_unknown_kind(self) -> None:
        path = written(HEADER + "photo\twarstwa\tzdjęcie\tZdjęcie.\n")
        with self.assertRaises(mkdocs_tematy.DictionaryError, msg="nieznany rodzaj przeszedł"):
            mkdocs_tematy.read(path)

    def test_rejects_plural_like_uppercase_name(self) -> None:
        path = written(HEADER + "Photos\ttemat\tzdjęcie\tZdjęcie.\n")
        with self.assertRaises(mkdocs_tematy.DictionaryError, msg="nazwa z wielką literą przeszła"):
            mkdocs_tematy.read(path)


class RenderTest(unittest.TestCase):
    def test_renders_one_row_per_entry(self) -> None:
        page = mkdocs_tematy.render([{"nazwa": "sync", "rodzaj": "temat", "termin": "synchronizacja", "opis": "W tle."}])
        self.assertIn("| `sync` | temat | synchronizacja | W tle. |", page, "brak wiersza tematu na stronie")


if __name__ == "__main__":
    unittest.main()
