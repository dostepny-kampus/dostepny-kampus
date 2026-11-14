#!/usr/bin/env python3
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — zaczep MkDocs: strona słownika tematów powstaje z pliku tematy.tsv przy każdej budowie."""
from __future__ import annotations
import csv
import re
from pathlib import Path

SOURCE = Path("architektura") / "tematy.tsv"
PAGE = Path("_generated") / "slownik-tematow.md"
HEADER = ["nazwa", "rodzaj", "termin", "opis"]
KINDS = {"temat", "ścieżka"}
NAME = re.compile(r"[a-z]+")
FORBIDDEN = {"util", "utils", "common", "helper", "misc", "base", "impl"}


class DictionaryError(ValueError):
    """Błąd pliku słownika tematów; komunikat wskazuje wiersz i powód."""


def read(path: Path) -> list[dict[str, str]]:
    """Czyta słownik tematów.

    Gwarantuje: wiersze w kolejności alfabetycznej nazw, nazwy unikalne, małe litery łacińskie, spoza
    listy zakazanych; rodzaj „temat” albo „ścieżka”; termin i opis niepuste.
    Wyjątki: DictionaryError przy naruszeniu; OSError przy błędzie odczytu.
    """
    with path.open(encoding="utf-8", newline="") as source:
        rows = list(csv.reader(source, delimiter="\t"))
    if not rows or rows[0] != HEADER:
        raise DictionaryError(f"{path}:1: nagłówek musi być {HEADER}")
    result = []
    for number, row in enumerate(rows[1:], start=2):
        if len(row) != len(HEADER):
            raise DictionaryError(f"{path}:{number}: wiersz musi mieć {len(HEADER)} kolumny")
        entry = dict(zip(HEADER, row))
        if not NAME.fullmatch(entry["nazwa"]) or entry["nazwa"] in FORBIDDEN:
            raise DictionaryError(f"{path}:{number}: niedozwolona nazwa „{entry['nazwa']}”")
        if entry["rodzaj"] not in KINDS:
            raise DictionaryError(f"{path}:{number}: rodzaj musi być jednym z {sorted(KINDS)}")
        if not entry["termin"].strip() or not entry["opis"].strip():
            raise DictionaryError(f"{path}:{number}: termin i opis nie mogą być puste")
        if result and entry["nazwa"] <= result[-1]["nazwa"]:
            raise DictionaryError(f"{path}:{number}: nazwy muszą być unikalne i ułożone alfabetycznie")
        result.append(entry)
    return result


def render(entries: list[dict[str, str]]) -> str:
    """Zwraca stronę Markdown z tabelą słownika w kolejności wierszy pliku."""
    lines = [
        "# Słownik tematów",
        "",
        "Nazwy tematów i ścieżek użytkownika w pakietach serwera i aplikacji. Ten sam temat ma tę samą nazwę",
        "w każdym module. Źródło: `docs/docs/architektura/tematy.tsv`; zasady: [Struktura modułów](../architektura/moduly.md).",
        "",
        "| Pakiet | Rodzaj | Termin | Co obejmuje |",
        "| --- | --- | --- | --- |",
    ]
    lines += [f"| `{e['nazwa']}` | {e['rodzaj']} | {e['termin']} | {e['opis']} |" for e in entries]
    return "\n".join(lines) + "\n"


def on_pre_build(config: object) -> None:
    """Zapisuje stronę słownika z pliku źródłowego, zanim MkDocs zbierze pliki dokumentacji.

    Gwarantuje: strona odpowiada plikowi `tematy.tsv`; nie jest przechowywana w repozytorium.
    Wyjątki: DictionaryError przy błędnym pliku, OSError przy błędzie odczytu albo zapisu — zatrzymują budowę.
    """
    docs = Path(config["docs_dir"])
    page = docs / PAGE
    page.parent.mkdir(parents=True, exist_ok=True)
    page.write_text(render(read(docs / SOURCE)), encoding="utf-8")
