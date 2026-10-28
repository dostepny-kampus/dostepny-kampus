"""Dostępny Kampus — eksport konturów budynków PWr i ich wejść z OpenStreetMap do data/buildings. Author: Vladyslav Yevkov. Copyright (c) 2026 Vladyslav Yevkov."""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass, field
from pathlib import Path

import osm2geojson
import requests
import shapely

OUTPUT = Path(__file__).resolve().parent.parent / "buildings"
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
SOURCE = {
    "source": "OpenStreetMap",
    "license": "ODbL-1.0",
    "attribution": "© OpenStreetMap contributors",
}


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


@dataclass
class Building:
    """Budynek PWr o jednym symbolu: kontury z OSM (zwykle jeden) i wejścia leżące na ich obrysie."""

    symbol: str
    outlines: list[Element] = field(default_factory=list)
    entrances: list[Element] = field(default_factory=list)


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


def buildings(found: list[Element]) -> list[Building]:
    """Grupuje kontury według symbolu PWr i przypisuje każdemu wejścia leżące na jego obrysie."""
    by_symbol: dict[str, Building] = {}
    outlines = [e for e in found if "building" in e.tags and e.shape.geom_type in POLYGONS]
    entrances = [e for e in found if "entrance" in e.tags and e.shape.geom_type == "Point"]
    for outline in sorted(outlines, key=Element.sort_key):
        for symbol in symbols(outline.tags):
            building = by_symbol.setdefault(symbol, Building(symbol))
            building.outlines.append(outline)
    for building in by_symbol.values():
        rings = shapely.union_all([o.shape.boundary for o in building.outlines])
        building.entrances = sorted(
            (e for e in entrances if e.shape.distance(rings) <= ON_OUTLINE),
            key=Element.sort_key,
        )
    return [by_symbol[s] for s in sorted(by_symbol, key=symbol_key)]


def geometry(shape: shapely.Geometry) -> dict:
    """Zwraca geometrię GeoJSON: współrzędne zaokrąglone do 7 miejsc, pierścień zewnętrzny przeciwnie do ruchu wskazówek zegara (RFC 7946)."""
    rounded = shapely.transform(shape, lambda coords: coords.round(PRECISION))
    if rounded.geom_type in POLYGONS:
        rounded = shapely.orient_polygons(rounded)
    return shapely.geometry.mapping(rounded)


def feature_collection(building: Building) -> dict:
    """Plik budynku: kontury (właściwości `symbol`, `name`, `osm_id`), potem wejścia jako punkty."""
    features = [
        {
            "type": "Feature",
            "properties": {"symbol": building.symbol, "name": o.tags.get("name"), "osm_id": o.osm_id},
            "geometry": geometry(o.shape),
        }
        for o in building.outlines
    ]
    features += [
        {
            "type": "Feature",
            "properties": {
                "osm_id": e.osm_id,
                "entrance": e.tags["entrance"],
                "ref": e.tags.get("ref"),
                "wheelchair": e.tags.get("wheelchair"),
            },
            "geometry": geometry(e.shape),
        }
        for e in building.entrances
    ]
    return {"type": "FeatureCollection", "features": features}


def index(found: list[Building]) -> dict:
    """Spis budynków z atrybucją źródła: symbol, nazwa z OSM, plik, obiekty OSM i liczba wejść."""
    return SOURCE | {
        "buildings": [
            {
                "symbol": b.symbol,
                "name": next((o.tags["name"] for o in b.outlines if "name" in o.tags), None),
                "file": f"{b.symbol}.geojson",
                "osm_ids": [o.osm_id for o in b.outlines],
                "entrances": len(b.entrances),
            }
            for b in found
        ],
    }


def write_json(path: Path, content: dict) -> None:
    """Zapisuje JSON deterministycznie: UTF-8, wcięcie 2, znak nowej linii na końcu."""
    path.write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def export(found: list[Building], output: Path) -> None:
    """Zapisuje pliki budynków i `index.json`, usuwa pliki budynków, których nie ma już w danych."""
    output.mkdir(parents=True, exist_ok=True)
    expected = {f"{b.symbol}.geojson" for b in found}
    for stale in output.glob("*.geojson"):
        if stale.name not in expected:
            stale.unlink()
    for building in found:
        write_json(output / f"{building.symbol}.geojson", feature_collection(building))
    write_json(output / "index.json", index(found))


def main() -> None:
    """Pobiera dane z Overpass (albo czyta zapisaną odpowiedź z `--input`) i odświeża data/buildings."""
    parser = argparse.ArgumentParser(description="Eksport konturów budynków PWr i ich wejść z OpenStreetMap do data/buildings.")
    parser.add_argument("--input", type=Path, help="zapisana odpowiedź Overpass (JSON) zamiast zapytania")
    args = parser.parse_args()
    response = json.loads(args.input.read_text(encoding="utf-8")) if args.input else fetch()
    found = buildings(elements(response))
    export(found, OUTPUT)
    print(f"Zapisano {len(found)} budynków w {OUTPUT.name}/")


if __name__ == "__main__":
    main()
