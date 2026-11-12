# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Nazwa budynku do pokazania obok symbolu, liczona raz dla aplikacji i serwera (US1.1, US2, NFR-18, WCAG 3.2.4)."""

from __future__ import annotations

import re

MIN_NAME_LETTERS = 4
SEPARATORS = " /-"
SYMBOLS_ONLY = re.compile(r"[A-Z]-\d+[a-z]?(\s*/\s*[A-Z]-\d+[a-z]?)*")


def display_name(name: str | None, symbols: list[str]) -> str | None:
    """Zwraca nazwę z OSM bez symboli swojego budynku, bo symbol stoi na ekranie tuż obok nazwy.

    Nazwa z samych symboli, np. `C-3/C-4`, to brak nazwy. Symbol znika razem z łącznikiem `/` albo `-` przy nim,
    także ze środka nazwy: `A-1 / Budynek główny` daje `Budynek główny`. Gdy po symbolu zostają mniej niż
    4 litery, np. `M-6 bis`, nazwa zostaje bez zmian. Cudzysłowy zostają jak w danych.
    """
    given = (name or "").strip()
    if not given or SYMBOLS_ONLY.fullmatch(given):
        return None
    rest = given
    for symbol in symbols:
        rest = re.sub(rf"\s*(?<![\w-]){re.escape(symbol)}(?![\w-])\s*[/-]?\s*", " ", rest)
    rest = rest.strip(SEPARATORS)
    return rest if sum(char.isalpha() for char in rest) >= MIN_NAME_LETTERS else given
