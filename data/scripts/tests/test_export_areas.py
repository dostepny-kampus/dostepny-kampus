# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Testy liczenia granic terenów PWr na małych sztucznych danych w metrach (BR-G5).

Uruchamia je workflow `backend`; lokalnie `python -X utf8 -m unittest discover -s data/scripts/tests`.
"""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

import shapely
from shapely.geometry import Point, box

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))

import export_areas  # noqa: E402  (ścieżka do skryptu dodana wyżej)

DATA = HERE.parents[1] / "areas" / "pwr-areas.geojson"


def building(x: float, y: float = 0) -> shapely.Geometry:
    return box(x, y, x + 20, y + 20)


class GroupTest(unittest.TestCase):
    def test_chain_of_buildings_is_one_group(self) -> None:
        contours = [building(0), building(500), building(1000), building(5000)]
        groups = sorted(sorted(g) for g in export_areas.group(contours))
        self.assertEqual([[0, 1, 2], [3]], groups)


class BoundaryTest(unittest.TestCase):
    def test_buildings_inside(self) -> None:
        contours = [building(0), building(40)]
        result, used, extra = export_areas.boundary(contours, [])
        self.assertTrue(all(result.covers(c) for c in contours))
        self.assertEqual((0, 0), (used, extra))

    def test_gap_of_60_meters_is_filled(self) -> None:
        contours = [building(0), building(80)]
        result, _, _ = export_areas.boundary(contours, [])
        self.assertTrue(result.covers(Point(50, 10)))

    def test_empty_space_of_300_meters_is_not_filled(self) -> None:
        contours = [building(0), building(320)]
        result, _, _ = export_areas.boundary(contours, [])
        self.assertFalse(result.covers(Point(170, 10)))

    def test_building_without_osm_area_gets_boundary_from_outline(self) -> None:
        contour = building(0)
        result, used, _ = export_areas.boundary([contour], [box(5000, 5000, 5100, 5100)])
        self.assertEqual(0, used)
        self.assertTrue(result.covers(contour))
        self.assertTrue(result.covers(Point(10, 40)))
        self.assertFalse(result.covers(Point(10, 120)))


class AreaNameTest(unittest.TestCase):
    def test_name_comes_from_known_symbol(self) -> None:
        self.assertEqual("Wittigowo", export_areas.area_name({"T-15", "T-16"}))


class DataFileTest(unittest.TestCase):
    def test_five_areas_cover_all_buildings(self) -> None:
        areas = json.loads(DATA.read_text(encoding="utf-8"))["features"]
        self.assertEqual(list(export_areas.AREA_NAMES.values()), [a["properties"]["name"] for a in areas])
        buildings = export_areas.read_buildings(export_areas.BUILDINGS)
        listed = [s for a in areas for s in a["properties"]["symbols"]]
        self.assertEqual(len(listed), len(set(listed)))
        self.assertEqual({s for symbols, _ in buildings for s in symbols}, set(listed))
        export_areas.check_rounded(areas, buildings)


if __name__ == "__main__":
    unittest.main()
