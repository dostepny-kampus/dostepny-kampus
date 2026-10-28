# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
"""Dostępny Kampus — eksport konturów budynków PWr i ich wejść z OpenStreetMap do data/buildings/pwr-buildings.geojson. Author: Vladyslav Yevkov. Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko."""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path

import osm2geojson
import requests
import shapely

OUTPUT = Path(__file__).resolve().parent.parent / "buildings" / "pwr-buildings.geojson"
OVERPASS_URL = "https://overpass-api.de/api/interpreter"
USER_AGENT = "dostepny-kampus-data (projekt studencki; https://github.com/dostepny-kampus/dostepny-kampus)"
TIMEOUT_SECONDS = 240
QUERY = """
[out:json][timeout:180][bbox:51.04,16.80,51.21,17.18];
(
  wr["amenity"="university"]["name"~"Politechnika Wrocławska"];
  wr["amenity"="university"]["operator"~"Politechnika Wrocławska"];
)->.kampusy;
.kampusy map_to_area->.obszar;
(
  wr["building"](area.obszar);
  wr["building"]["operator"~"Politechnika Wrocławska"];
)->.budynki;
(.budynki; way(r.budynki);)->.obrysy;
node(w.obrysy)["entrance"]->.wejscia;
.budynki out body geom;
.wejscia out body;
"""
PRECISION = 7
ON_OUTLINE = 1e-9
SYMBOL_IN_REF = re.compile(r"([A-Za-z])[- ]?(\d{1,3})([a-z]?)")
SYMBOL_IN_NAME = re.compile(r"(?<![\w-])([A-Z])-(\d{1,3})([a-z]?)(?![\w-])")
REF_SEPARATOR = re.compile(r"[;/]")
POLYGONS = frozenset({"Polygon", "MultiPolygon"})
SOURCE = {"source": "OpenStreetMap", "license": "ODbL-1.0", "attribution": "© OpenStreetMap contributors"}


@dataclass(frozen=True)
class Element:
    """Obiekt OSM z kształtem: identyfikator w postaci `way/123`, tagi i geometria w EPSG:4326."""

    osm_id: str
    tags: dict[str, str]
    shape: shapely.Geometry

    def sort_key(self) -> tuple[str, int]:
        """Klucz sortowania: typ obiektu OSM, potem numer."""
        kind, number = self.osm_id.split("/")
        return kind, int(number)


def fetch() -> dict:
    """Wysyła jedno zapytanie do Overpass API i zwraca odpowiedź JSON."""
    response = requests.post(
        OVERPASS_URL,
        data={"data": QUERY},
        headers={"User-Agent": USER_AGENT},
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def symbols(tags: dict[str, str]) -> list[str]:
    """Zwraca symbole PWr w postaci `C-13` z tagu `ref` (np. `C13`, `C3;C4`), a gdy go brak — z tagu `name`."""
    found = []
    for part in REF_SEPARATOR.split(tags.get("ref", "")):
        match = SYMBOL_IN_REF.fullmatch(part.strip())
        if match:
            found.append(match.groups())
    if not found:
        found = SYMBOL_IN_NAME.findall(tags.get("name", ""))
    return sorted({f"{letter.upper()}-{int(number)}{suffix}" for letter, number, suffix in found}, key=symbol_key)


def symbol_key(symbol: str) -> tuple[str, int, str]:
    """Klucz sortowania naturalnego: A-2 przed A-10, D-8 przed D-8a."""
    letter, rest = symbol.split("-")
    digits = rest.rstrip("abcdefghijklmnopqrstuvwxyz")
    return letter, int(digits), rest[len(digits) :]


def elements(response: dict) -> list[Element]:
    """Zamienia odpowiedź Overpass na obiekty z geometrią (wielokąty relacji składa osm2geojson); odrzuca odpowiedź z błędem wykonania (`remark`)."""
    if "remark" in response:
        raise ValueError(f"Overpass: {response['remark']}")
    found = []
    for item in osm2geojson.json2shapes(response, filter_used_refs=False):
        props = item["properties"]
        found.append(Element(f"{props['type']}/{props['id']}", props.get("tags", {}), item["shape"]))
    return found


def features(found: list[Element]) -> list[dict]:
    """Kontury z symbolem PWr, potem wejścia leżące na ich obrysie; każdy obiekt OSM raz, z listą `symbols`."""
    outlines = sorted(
        (e for e in found if "building" in e.tags and e.shape.geom_type in POLYGONS and symbols(e.tags)),
        key=Element.sort_key,
    )
    result = [
        feature(o, {"kind": "building", "symbols": symbols(o.tags), "name": o.tags.get("name"), "osm_id": o.osm_id})
        for o in outlines
    ]
    for e in sorted((e for e in found if "entrance" in e.tags and e.shape.geom_type == "Point"), key=Element.sort_key):
        owners = {s for o in outlines if e.shape.distance(o.shape.boundary) <= ON_OUTLINE for s in symbols(o.tags)}
        if owners:
            props = {"kind": "entrance", "symbols": sorted(owners, key=symbol_key), "osm_id": e.osm_id}
            result.append(feature(e, props | {k: e.tags.get(k) for k in ("entrance", "ref", "wheelchair")}))
    return result


def feature(element: Element, properties: dict) -> dict:
    """Obiekt GeoJSON z podanymi właściwościami i geometrią elementu."""
    return {"type": "Feature", "properties": properties, "geometry": geometry(element.shape)}


def geometry(shape: shapely.Geometry) -> dict:
    """Zwraca geometrię GeoJSON: współrzędne zaokrąglone do 7 miejsc, pierścień zewnętrzny przeciwnie do ruchu wskazówek zegara (RFC 7946)."""
    rounded = shapely.transform(shape, lambda coords: coords.round(PRECISION))
    if rounded.geom_type in POLYGONS:
        rounded = shapely.orient_polygons(rounded)
    return shapely.geometry.mapping(rounded)


def write_json(path: Path, content: dict) -> None:
    """Zapisuje JSON deterministycznie: UTF-8, wcięcie 2, znak nowej linii na końcu."""
    path.write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def main() -> None:
    """Pobiera dane z Overpass (albo czyta zapisaną odpowiedź z `--input`) i zapisuje jeden plik GeoJSON."""
    parser = argparse.ArgumentParser(description="Eksport konturów budynków PWr i ich wejść z OpenStreetMap do data/buildings/pwr-buildings.geojson.")
    parser.add_argument("--input", type=Path, help="zapisana odpowiedź Overpass (JSON) zamiast zapytania")
    args = parser.parse_args()
    response = json.loads(args.input.read_text(encoding="utf-8")) if args.input else fetch()
    found = features(elements(response))
    write_json(OUTPUT, {"type": "FeatureCollection"} | SOURCE | {"features": found})
    print(f"Zapisano {len(found)} obiektów w {OUTPUT.name}")


if __name__ == "__main__":
    main()
