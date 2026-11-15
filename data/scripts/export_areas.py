# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — granice pięciu terenów PWr (BR-G5) do data/areas/pwr-areas.geojson."""

from __future__ import annotations

import argparse
import datetime
import json
from pathlib import Path

import numpy
import requests
import shapely
from shapely.strtree import STRtree

from export_buildings import OVERPASS_URL, SOURCE, TIMEOUT_SECONDS, USER_AGENT, elements, geometry, symbol_key, write_json

ROOT = Path(__file__).resolve().parent.parent
BUILDINGS = ROOT / "buildings" / "pwr-buildings.geojson"
OSM_AREAS = ROOT / "areas" / "osm-university-areas.geojson"
OUTPUT = ROOT / "areas" / "pwr-areas.geojson"
QUERY = """
[out:json][timeout:120][bbox:51.04,16.80,51.21,17.18];
(
  wr["amenity"="university"]["name"~"Politechnika Wrocławska"];
  wr["amenity"="university"]["operator"~"Politechnika Wrocławska"];
);
out geom;
"""
AREA_NAMES = {"A-1": "Kampus główny", "T-15": "Wittigowo", "P-14": "Bartoszowice", "M-3": "Długa", "T-9": "T-9 Atol"}
LINK_METERS = 600
GROW_METERS = 50
SHRINK_METERS = 20
SIMPLIFY_METERS = 10
RETRY_GROW_METERS = 5
POLYGONS = ("Polygon", "MultiPolygon")
EARTH_RADIUS_METERS = 6371008.8


def center(shapes: list[shapely.Geometry]) -> tuple[float, float]:
    """Środek prostokąta obejmującego kształty (długość, szerokość geograficzna)."""
    min_x, min_y, max_x, max_y = shapely.union_all(shapes).bounds
    return (min_x + max_x) / 2, (min_y + max_y) / 2


def project(shape: shapely.Geometry, origin: tuple[float, float], inverse: bool = False) -> shapely.Geometry:
    """Przelicza geometrię między stopniami a metrami w rzucie równoodległościowym wokół `origin`.

    Rzut jest lokalny: dla terenu o rozmiarze do ok. 2 km błąd odległości to rząd centymetrów.
    """
    lon0, lat0 = origin
    scale = EARTH_RADIUS_METERS * numpy.pi / 180
    stretch = numpy.cos(numpy.radians(lat0))

    def convert(coords: numpy.ndarray) -> numpy.ndarray:
        if inverse:
            return numpy.column_stack((lon0 + coords[:, 0] / (scale * stretch), lat0 + coords[:, 1] / scale))
        return numpy.column_stack(((coords[:, 0] - lon0) * stretch * scale, (coords[:, 1] - lat0) * scale))

    return shapely.transform(shape, convert)


def group(contours: list[shapely.Geometry]) -> list[list[int]]:
    """Dzieli kontury (w metrach) na grupy, w których każdy leży najwyżej 600 m od innego konturu grupy."""
    tree = STRtree(contours)
    parent = list(range(len(contours)))

    def root(index: int) -> int:
        while parent[index] != index:
            parent[index] = parent[parent[index]]
            index = parent[index]
        return index

    for index, contour in enumerate(contours):
        for near in tree.query(contour, predicate="dwithin", distance=LINK_METERS):
            parent[root(int(near))] = root(index)
    groups: dict[int, list[int]] = {}
    for index in range(len(contours)):
        groups.setdefault(root(index), []).append(index)
    return list(groups.values())


def boundary(contours: list[shapely.Geometry], osm_areas: list[shapely.Geometry]) -> tuple[shapely.Geometry, int, int]:
    """Granica terenu w metrach: kontury i przylegające obszary OSM, bufor +50 m, potem -20 m, uproszczenie 10 m.

    Obszar OSM należy do terenu, gdy po buforze +50 m styka się z konturem budynku terenu albo go przecina; inne pomija. Gdy któryś kontur
    nie leży całkowicie wewnątrz, bufor rośnie o 5 m; gdy dalej nie leży, funkcja zgłasza błąd.
    Zwraca granicę, liczbę użytych obszarów OSM i dodatkowy bufor w metrach.
    """
    buildings = shapely.union_all(contours)
    near = buildings.buffer(GROW_METERS)
    used = [area for area in osm_areas if area.intersects(near)]
    merged = shapely.union_all([buildings, *used])
    for extra in (0, RETRY_GROW_METERS):
        result = merged.buffer(GROW_METERS + extra).buffer(-SHRINK_METERS).simplify(SIMPLIFY_METERS, preserve_topology=True)
        if all(result.covers(contour) for contour in contours):
            return result, len(used), extra
    raise ValueError("kontur budynku poza granicą terenu mimo dodatkowego bufora")


def read_buildings(path: Path) -> list[tuple[list[str], shapely.Geometry]]:
    """Czyta kontury budynków: lista symboli i geometria w EPSG:4326."""
    data = json.loads(path.read_text(encoding="utf-8"))
    return [
        (f["properties"]["symbols"], shapely.geometry.shape(f["geometry"]))
        for f in data["features"]
        if f["properties"]["kind"] == "building"
    ]


def fetch_osm_areas(path: Path) -> None:
    """Pobiera obszary `amenity=university` PWr z Overpass API i zapisuje je razem z datą pobrania."""
    response = requests.post(OVERPASS_URL, data={"data": QUERY}, headers={"User-Agent": USER_AGENT}, timeout=TIMEOUT_SECONDS)
    response.raise_for_status()
    body = response.json()
    found = sorted((e for e in elements(body) if e.shape.geom_type in POLYGONS), key=lambda e: e.sort_key())
    items = [
        {"type": "Feature", "properties": {"osm_id": e.osm_id, "name": e.tags.get("name")}, "geometry": geometry(e.shape)}
        for e in found
    ]
    meta = {"retrieved": datetime.date.today().isoformat(), "osm_base": body["osm3s"]["timestamp_osm_base"]}
    write_json(path, {"type": "FeatureCollection"} | SOURCE | meta | {"features": items})


def area_name(symbols: set[str]) -> str:
    """Nazwa terenu po symbolu jego budynku z listy nazw; teren bez takiego budynku albo z dwoma to błąd."""
    names = [AREA_NAMES[s] for s in symbols if s in AREA_NAMES]
    if len(names) != 1:
        raise ValueError(f"teren {sorted(symbols, key=symbol_key)} ma nazw: {len(names)}")
    return names[0]


def areas(buildings: list[tuple[list[str], shapely.Geometry]], osm_areas: list[shapely.Geometry]) -> list[dict]:
    """Obiekty GeoJSON pięciu terenów w kolejności z `AREA_NAMES`; wypisuje użyte obszary OSM i dodatkowy bufor."""
    outlines = [shape for _, shape in buildings]
    result = {}
    for indexes in group([project(shape, center(outlines)) for shape in outlines]):
        symbols = {s for i in indexes for s in buildings[i][0]}
        origin = center([outlines[i] for i in indexes])
        shape, used, extra = boundary([project(outlines[i], origin) for i in indexes], [project(a, origin) for a in osm_areas])
        props = {"name": area_name(symbols), "symbols": sorted(symbols, key=symbol_key)}
        result[props["name"]] = {"type": "Feature", "properties": props, "geometry": geometry(project(shape, origin, inverse=True))}
        print(f"{props['name']}: budynków {len(indexes)}, obszarów OSM {used}, dodatkowy bufor {extra} m")
    if sorted(result) != sorted(AREA_NAMES.values()):
        raise ValueError(f"oczekiwano terenów {sorted(AREA_NAMES.values())}, jest {sorted(result)}")
    return [result[name] for name in AREA_NAMES.values()]


def check_rounded(found: list[dict], buildings: list[tuple[list[str], shapely.Geometry]]) -> None:
    """Sprawdza po zaokrągleniu do 7 miejsc, że kontur każdego budynku leży wewnątrz granicy jego terenu."""
    for feature in found:
        outline = shapely.geometry.shape(feature["geometry"])
        own = set(feature["properties"]["symbols"])
        if not all(outline.covers(shape) for symbols, shape in buildings if set(symbols) <= own):
            raise ValueError(f"po zaokrągleniu kontur poza granicą: {feature['properties']['name']}")


def main() -> None:
    """Liczy granice terenów z zapisanych obszarów OSM i konturów budynków; z `--fetch` najpierw odświeża obszary OSM."""
    parser = argparse.ArgumentParser(description="Granice pięciu terenów PWr do data/areas/pwr-areas.geojson.")
    parser.add_argument("--fetch", action="store_true", help="pobierz obszary uczelni z Overpass API zamiast czytać zapisane")
    args = parser.parse_args()
    if args.fetch:
        fetch_osm_areas(OSM_AREAS)
    buildings = read_buildings(BUILDINGS)
    osm_areas = [shapely.geometry.shape(f["geometry"]) for f in json.loads(OSM_AREAS.read_text(encoding="utf-8"))["features"]]
    found = areas(buildings, osm_areas)
    check_rounded(found, buildings)
    write_json(OUTPUT, {"type": "FeatureCollection"} | SOURCE | {"features": found})
    print(f"Zapisano {len(found)} terenów w {OUTPUT.name}")


if __name__ == "__main__":
    main()
