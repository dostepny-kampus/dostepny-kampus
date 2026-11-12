# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Testy nazwy budynku do pokazania: wszystkie postaci nazw z symbolem w `data/buildings/pwr-buildings.geojson`.

Uruchamia je workflow `backend`; lokalnie `python -X utf8 -m unittest discover -s data/scripts/tests`.
"""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))

from display_name import display_name  # noqa: E402  (ścieżka do skryptu dodana wyżej)

DATA = HERE.parents[1] / "buildings" / "pwr-buildings.geojson"


class DisplayNameTest(unittest.TestCase):
    def test_own_symbol_removed(self) -> None:
        cases = {
            ("A-1 / Budynek główny", "A-1"): "Budynek główny",
            ("P-22 - Hala Widowiskowo-Sportowa", "P-22"): "Hala Widowiskowo-Sportowa",
            ("T-7 Dom doktoranta", "T-7"): "Dom doktoranta",
            ('T-16 "Tower"', "T-16"): '"Tower"',
            ('Dom Studencki T-6 "Alcatraz"', "T-6"): 'Dom Studencki "Alcatraz"',
        }
        for (name, symbol), expected in cases.items():
            self.assertEqual(expected, display_name(name, [symbol]))

    def test_short_rest_keeps_name(self) -> None:
        self.assertEqual("M-6 bis", display_name("M-6 bis", ["M-6"]))

    def test_symbols_only_is_no_name(self) -> None:
        for name in ("C-3/C-4", "C-3", " T-22 ", "", None):
            self.assertIsNone(display_name(name, ["C-3", "C-4"]))

    def test_other_symbol_stays(self) -> None:
        self.assertEqual("A-1 / Budynek główny", display_name("A-1 / Budynek główny", ["T-18"]))
        self.assertEqual('T-16 "Tower"', display_name('T-16 "Tower"', ["T-1"]))
        self.assertEqual("Budynek testowy", display_name("Budynek testowy", ["C-13"]))

    def test_data_file_matches_rule(self) -> None:
        buildings = [
            feature["properties"]
            for feature in json.loads(DATA.read_text(encoding="utf-8"))["features"]
            if feature["properties"]["kind"] == "building"
        ]
        for building in buildings:
            expected = display_name(building["name"], building["symbols"])
            self.assertEqual(expected, building.get("display_name"), building["osm_id"])
            self.assertEqual(expected is not None, "display_name" in building, building["osm_id"])
        shown = [b["display_name"] for b in buildings if "display_name" in b]
        self.assertEqual(15, len(shown))
        repeated = [b["display_name"] for b in buildings if any(s in b.get("display_name", "") for s in b["symbols"])]
        self.assertEqual(["M-6 bis"], repeated)


if __name__ == "__main__":
    unittest.main()
