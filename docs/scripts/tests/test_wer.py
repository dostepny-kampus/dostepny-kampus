# Dostępny Kampus
# Author: Myroslav Hryniuka
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Testy miary WER (wer.py) na wymyślonych zdaniach."""

from __future__ import annotations

import contextlib
import io
import sys
import tempfile
import unicodedata
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))

import wer  # noqa: E402  (ścieżka do skryptu dodana wyżej)


class WordsTest(unittest.TestCase):
    def test_ignores_case_punctuation_and_spacing(self) -> None:
        self.assertEqual(wer.words("  Hej,   jak SIĘ masz?! "), ["hej", "jak", "się", "masz"])

    def test_decomposed_polish_letters_stay_in_one_word(self) -> None:
        self.assertEqual(wer.words(unicodedata.normalize("NFD", "Wejście główne")), ["wejście", "główne"])


class WerTest(unittest.TestCase):
    def test_same_text_has_no_errors(self) -> None:
        self.assertEqual(wer.wer([("Winda jest zepsuta.", "winda jest zepsuta")]), 0)

    def test_counts_substitution_deletion_and_insertion(self) -> None:
        for transcript in ("za wąskie okno", "za drzwi", "za bardzo wąskie drzwi"):
            with self.subTest(transcript=transcript):
                self.assertAlmostEqual(wer.wer([("za wąskie drzwi", transcript)]), 1 / 3)

    def test_weights_pairs_by_reference_length(self) -> None:
        pairs = [("tak", "nie"), ("schody przy wejściu głównym", "schody przy wejściu głównym")]
        self.assertAlmostEqual(wer.wer(pairs), 1 / 5)

    def test_silence_adds_only_insertions(self) -> None:
        self.assertAlmostEqual(wer.wer([("anuluj", "anuluj"), ("", "no tak")]), 2.0)

    def test_rejects_references_without_words(self) -> None:
        with self.assertRaises(ValueError):
            wer.wer([("", "coś")])


class FileTest(unittest.TestCase):
    def setUp(self) -> None:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.path = Path(directory.name) / "pary.csv"

    def write(self, content: str) -> None:
        self.path.write_text(content, encoding="utf-8")

    def test_reads_quoted_values_and_byte_order_mark(self) -> None:
        self.write('﻿wzorzec,transkrypcja\n"Tak, wyślij",tak wyślij\nwróć,\n')
        self.assertEqual(wer.read_pairs(self.path), [("Tak, wyślij", "tak wyślij"), ("wróć", "")])

    def test_rejects_missing_column_and_short_row(self) -> None:
        for content in ("wzorzec\ntak\n", "wzorzec,transkrypcja\ntak\n"):
            with self.subTest(content=content):
                self.write(content)
                with self.assertRaises(ValueError):
                    wer.read_pairs(self.path)

    def test_prints_result_for_whole_file(self) -> None:
        self.write("wzorzec,transkrypcja\ntak,tak\nnie,tak\n")
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(wer.main([str(self.path)]), 0)
        self.assertEqual(output.getvalue(), "Pary: 2, WER: 0.5000\n")


if __name__ == "__main__":
    unittest.main()
