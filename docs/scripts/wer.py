# Dostępny Kampus
# Author: Myroslav Hryniuka
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — WER (Word Error Rate) dla par tekst wzorcowy / transkrypcja. Version: 0.1.0."""

from __future__ import annotations

import argparse
import csv
import re
import unicodedata
from collections.abc import Iterable
from pathlib import Path

COLUMNS = ("wzorzec", "transkrypcja")


def words(text: str) -> list[str]:
    """Słowa tekstu po normalizacji: postać NFC, małe litery, interpunkcja jako odstęp."""
    return re.sub(r"[^\w\s]", " ", unicodedata.normalize("NFC", text).lower()).split()


def edit_distance(expected: list[str], actual: list[str]) -> int:
    """Najmniejsza liczba zamian, usunięć i wstawień słów, które zmieniają `expected` w `actual`."""
    previous = list(range(len(actual) + 1))
    for i, expected_word in enumerate(expected, start=1):
        current = [i]
        for j, actual_word in enumerate(actual, start=1):
            substitution = previous[j - 1] + (expected_word != actual_word)
            current.append(min(substitution, previous[j] + 1, current[j - 1] + 1))
        previous = current
    return previous[-1]


def wer(pairs: Iterable[tuple[str, str]]) -> float:
    """WER zbioru par (wzorzec, transkrypcja): suma błędów podzielona przez sumę słów wzorców.

    Para z pustym wzorcem (cisza, szum) dodaje same wstawienia. Wynik może przekroczyć 1.
    Rzuca `ValueError`, gdy wzorce nie mają żadnego słowa.
    """
    errors = 0
    total = 0
    for reference, transcript in pairs:
        expected = words(reference)
        errors += edit_distance(expected, words(transcript))
        total += len(expected)
    if total == 0:
        raise ValueError("wzorce nie mają żadnego słowa")
    return errors / total


def read_pairs(path: Path) -> list[tuple[str, str]]:
    """Pary z pliku CSV w UTF-8 z kolumnami `wzorzec` i `transkrypcja`.

    Rzuca `ValueError`, gdy brakuje kolumny w nagłówku albo wartości w wierszu.
    """
    with path.open(encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        if not set(COLUMNS) <= set(reader.fieldnames or []):
            raise ValueError(f"nagłówek musi mieć kolumny: {', '.join(COLUMNS)}")
        pairs = []
        for row in reader:
            reference, transcript = (row[column] for column in COLUMNS)
            if reference is None or transcript is None:
                raise ValueError(f"wiersz {reader.line_num}: brak wartości")
            pairs.append((reference, transcript))
    return pairs


def main(argv: list[str] | None = None) -> int:
    """Wypisuje liczbę par i WER całego pliku; zwraca kod wyjścia."""
    parser = argparse.ArgumentParser(description="WER dla par wzorzec/transkrypcja z pliku CSV.")
    parser.add_argument("path", type=Path, help="plik CSV z kolumnami wzorzec i transkrypcja")
    path = parser.parse_args(argv).path
    try:
        pairs = read_pairs(path)
        print(f"Pary: {len(pairs)}, WER: {wer(pairs):.4f}")
    except (OSError, ValueError) as error:
        parser.error(f"{path}: {error}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
