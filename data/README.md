# Dane: kontury budynków PWr z OpenStreetMap

Kontury budynków Politechniki Wrocławskiej i wejścia na ich obrysie, pobrane z OpenStreetMap (OSM). Aplikacja pokazuje je na mapie bez sieci (US1, US2), a wejścia z tagiem `wheelchair` dają pierwszy status wejścia (US17.1).

| Ścieżka | Co zawiera |
| --- | --- |
| `buildings/<symbol>.geojson` | jeden budynek, np. `C-13.geojson` |
| `buildings/index.json` | spis budynków i atrybucja źródła |
| `buildings/LICENSE` | nota licencyjna ODbL dla danych |
| `scripts/export_buildings.py` | skrypt eksportu z Overpass API |
| `requirements.txt` | zależności skryptu z dokładnymi wersjami |

Stan eksportu z 28.10.2026: 95 symboli budynków, 95 konturów z OSM i 72 wejścia, w tym 1 z tagiem `wheelchair`.

## Format plików

Każdy plik budynku to `FeatureCollection` w GeoJSON (RFC 7946, EPSG:4326). Współrzędne mają 7 miejsc po przecinku (około 1 cm).

| Obiekt | Geometria | Właściwości |
| --- | --- | --- |
| kontur | `Polygon` albo `MultiPolygon` | `symbol` (np. `C-13`), `name` (tag `name` z OSM albo `null`), `osm_id` (np. `way/101124650`) |
| wejście | `Point` | `osm_id`, `entrance` (np. `main`, `yes`), `ref`, `wheelchair` (`yes`, `no`, `limited` albo `null`) |

Kontury są pierwsze, wejścia po nich, każda grupa posortowana po `osm_id`. Plik `index.json` podaje dla każdego symbolu nazwę, plik, listę `osm_ids` i liczbę wejść.

Przypadki szczególne:

- jeden obiekt OSM z dwoma symbolami (tag `ref` = `C3;C4`) trafia do obu plików, `C-3` i `C-4`;
- dwa obiekty OSM z tym samym symbolem (np. `H-14`) dają dwa kontury w jednym pliku;
- wejście na wspólnej ścianie sąsiednich budynków należy do obu plików.

## Skąd są dane

Skrypt wysyła do Overpass API jedno zapytanie w granicach prostokąta obejmującego Wrocław:

1. obszary z `amenity=university`, których `name` albo `operator` zawiera „Politechnika Wrocławska”;
2. budynki (`building=*`, linie i relacje) w tych obszarach oraz budynki z `operator` „Politechnika Wrocławska” poza nimi;
3. węzły z tagiem `entrance` leżące na obrysach tych budynków.

Symbol budynku pochodzi z tagu `ref` (`A1`, `C19`, `D8a` zamieniamy na `A-1`, `C-19`, `D-8a`). Gdy `ref` nie ma symbolu, bierzemy symbol z `name` (np. `Dom Studencki T-6 "Alcatraz"` daje `T-6`). Budynek bez symbolu pomijamy.

## Licencja i atrybucja

Dane w `buildings/` są na licencji [Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/) (ODbL). Każde użycie, także na mapie w aplikacji, podaje atrybucję:

> © OpenStreetMap contributors

Skrypt jest częścią kodu repozytorium i ma licencję MIT.

## Jak odświeżyć dane

Potrzebny jest Python 3.11 lub nowszy. Polecenia uruchamiamy w katalogu głównym repozytorium:

```sh
python -m venv .venv
.venv/bin/pip install -r data/requirements.txt
.venv/bin/python data/scripts/export_buildings.py
```

W systemie Windows zamiast `.venv/bin/` wpisujemy `.venv\Scripts\`. Skrypt nadpisuje pliki w `buildings/` i usuwa pliki budynków, których nie ma już w OSM. Wynik jest powtarzalny: te same dane OSM dają identyczne pliki, więc `git diff` pokazuje tylko prawdziwe zmiany w mapie.

Opcja `--input <plik.json>` przetwarza zapisaną odpowiedź Overpass bez nowego zapytania. Publiczny serwer Overpass ma limity, dlatego skrypt nie ponawia zapytań, a odpowiedź z błędem wykonania (pole `remark`) przerywa go bez zmiany plików. Uruchamiamy go ręcznie, gdy chcemy odświeżyć dane, a wynik zapisujemy w repozytorium.

## Źródła

- Licencja i atrybucja OSM: <https://www.openstreetmap.org/copyright>
- Tekst ODbL 1.0: <https://opendatacommons.org/licenses/odbl/1-0/>
- Overpass API i zasady korzystania z publicznych serwerów: <https://wiki.openstreetmap.org/wiki/Overpass_API>
- Tag `entrance`: <https://wiki.openstreetmap.org/wiki/Key:entrance>
- GeoJSON, RFC 7946: <https://www.rfc-editor.org/rfc/rfc7946>
