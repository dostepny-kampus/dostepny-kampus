# Dane: kontury budynków PWr z OpenStreetMap

Kontury budynków Politechniki Wrocławskiej i wejścia na ich obrysie, pobrane z OpenStreetMap (OSM). Aplikacja pokazuje je na mapie bez sieci (US1, US2), a wejścia z tagiem `wheelchair` dają pierwszy status wejścia (US17.1).

| Ścieżka | Co zawiera |
| --- | --- |
| `buildings/pwr-buildings.geojson` | wszystkie kontury i wejścia w jednym pliku |
| `buildings/LICENSE` | nota licencyjna ODbL dla danych |
| `scripts/export_buildings.py` | skrypt eksportu z Overpass API |
| `areas/` | granice pięciu terenów PWr (`pwr-areas.geojson`), zapisane obszary uczelni z OSM i `LICENSE` ODbL |
| `scripts/export_areas.py` | skrypt liczenia granic terenów |
| `requirements.txt` | zależności skryptów z dokładnymi wersjami |

Stan eksportu z 28.10.2026: 95 symboli budynków, 95 konturów z OSM i 72 wejścia, w tym 1 z tagiem `wheelchair`.

## Format pliku

Plik to jeden `FeatureCollection` w GeoJSON (RFC 7946, EPSG:4326), gotowy jako jedno źródło mapy. Współrzędne mają 7 miejsc po przecinku (około 1 cm). Pola `source`, `license` i `attribution` na poziomie kolekcji podają źródło danych.

| `kind` | Geometria | Pozostałe właściwości |
| --- | --- | --- |
| `building` | `Polygon` albo `MultiPolygon` | `symbols` (np. `["C-13"]`), `name` (tag `name` z OSM albo `null`), `display_name` (nazwa do pokazania obok symbolu; pola nie ma, gdy takiej nazwy brak), `osm_id` (np. `way/101124650`) |
| `entrance` | `Point` | `symbols`, `osm_id`, `entrance` (np. `main`, `yes`), `ref`, `wheelchair` (wartość tagu z OSM albo `null`) |

Każdy obiekt OSM występuje raz. Kontury są pierwsze, wejścia po nich, każda grupa posortowana po `osm_id`. Lista `symbols` jest posortowana naturalnie (`A-2` przed `A-10`).

Wartości tagów `entrance` i `wheelchair` są przepisane z OSM bez zmian. Aplikacja zamienia `wheelchair` na oznaczenie wejścia (BR-24):

| `wheelchair` | Oznaczenie wejścia |
| --- | --- |
| `yes`, `designated` | dostępne |
| `no` | niedostępne |
| `limited`, inna wartość, `null` | brak danych |

Wejścia z `entrance` = `emergency`, `exit` albo `garage` nie są wejściami dla odwiedzających. Aplikacja ich nie pokazuje: nie stoją ani na liście wejść, ani na mapie (BR-25).

Przypadki szczególne:

- obiekt OSM z dwoma symbolami (tag `ref` = `C3;C4`) ma `symbols` = `["C-3", "C-4"]`;
- dwa obiekty OSM z tym samym symbolem (np. `H-14`) to dwa kontury z tym samym `symbols`;
- wejście na wspólnej ścianie sąsiednich budynków ma symbole obu budynków (np. `["C-1", "C-3", "C-4"]`).

## Skąd są dane

Skrypt wysyła do Overpass API jedno zapytanie w granicach prostokąta obejmującego Wrocław:

1. obszary z `amenity=university`, których `name` albo `operator` zawiera „Politechnika Wrocławska”;
2. budynki (`building=*`, linie i relacje) w tych obszarach oraz budynki z `operator` „Politechnika Wrocławska” poza nimi;
3. węzły z tagiem `entrance` leżące na obrysach tych budynków.

Symbol budynku pochodzi z tagu `ref` (`A1`, `C19`, `D8a` zamieniamy na `A-1`, `C-19`, `D-8a`). Gdy `ref` nie ma symbolu, bierzemy symbol z `name` (np. `Dom Studencki T-6 "Alcatraz"` daje `T-6`). Budynek bez symbolu pomijamy.

Pole `display_name` to nazwa budynku do pokazania obok jego symbolu: aplikacja i serwer czytają ją z pliku i same nazwy nie przerabiają. Powstaje z `name` w `scripts/display_name.py`: symbol swojego budynku znika razem z łącznikiem `/` albo `-`, także ze środka nazwy (`A-1 / Budynek główny` daje `Budynek główny`, `Dom Studencki T-6 "Alcatraz"` daje `Dom Studencki "Alcatraz"`). Kontur bez nazwy albo z nazwą z samych symboli (`C-3/C-4`) nie ma tego pola. Gdy po symbolu zostają mniej niż 4 litery (`M-6 bis`), `display_name` jest równy `name`. Cudzysłowy zostają jak w OSM.

## Granice terenów

`areas/pwr-areas.geojson` to `FeatureCollection` (EPSG:4326, 7 miejsc po przecinku) z pięcioma obiektami, po jednym na teren, w kolejności „Kampus główny”, „Wittigowo”, „Bartoszowice”, „Długa”, „T-9 Atol”. Każdy ma geometrię `Polygon` albo `MultiPolygon` i właściwości `name` oraz `symbols` (symbole budynków terenu, posortowane naturalnie). Aplikacja i serwer używają tej geometrii bez przeliczania (BR-G5).

Budynki dzielą się na tereny tak jak w aplikacji: do jednego terenu należą kontury, z których każdy leży najwyżej 600 m od innego konturu tego terenu. Nazwę terenu wyznacza symbol jego budynku: `A-1`, `T-15`, `P-14`, `M-3`, `T-9`.

Granicę każdego terenu skrypt liczy w metrach, w lokalnym rzucie równoodległościowym wokół środka terenu (promień Ziemi 6 371 008,8 m); dla terenu do ok. 2 km błąd odległości to rząd centymetrów:

1. Obszary `amenity=university` Politechniki Wrocławskiej z OSM, zapisane w `areas/osm-university-areas.geojson` (z polami `retrieved` i `osm_base`), przypisuje do terenu, gdy po buforze +50 m styka się z konturem budynku tego terenu albo go przecina. Obszar bez budynku PWr w pobliżu nie należy do żadnego terenu i jest pomijany.
2. Łączy kontury budynków terenu z przypisanymi obszarami OSM i robi bufor +50 m, potem -20 m: przerwy węższe niż ok. 100 m się zamykają, a granica ma zapas 30 m.
3. Upraszcza wynik algorytmem Douglasa-Peuckera z tolerancją 10 m, z zachowaniem topologii.
4. Sprawdza, że każdy kontur budynku leży całkowicie wewnątrz granicy swojego terenu, także po zaokrągleniu współrzędnych. Gdy nie, powtarza liczenie z buforem +55 m, a gdy dalej nie leży, kończy się błędem.

Stan eksportu z 15.11.2026 (obszary OSM z bazy z 14.11.2026):

| Teren | Symboli budynków | Obszarów OSM w granicy |
| --- | --- | --- |
| Kampus główny | 78 | 13 |
| Wittigowo | 9 | 1 |
| Bartoszowice | 3 | 1 |
| Długa | 4 | 1 |
| T-9 Atol | 1 | 0 |

Wszystkie 16 obszarów OSM trafiło do terenu. Teren „T-9 Atol” nie ma obszaru w OSM, więc jego granica wynika z samych konturów budynku. Żaden kontur nie wymagał dodatkowego bufora +5 m.

Granice liczymy poleceniem `.venv/bin/python data/scripts/export_areas.py` (offline, z zapisanych obszarów OSM i `buildings/pwr-buildings.geojson`). Opcja `--fetch` najpierw pobiera obszary OSM z Overpass API i nadpisuje `areas/osm-university-areas.geojson`. Po odświeżeniu konturów budynków granice liczymy od nowa.

## Licencja i atrybucja

Dane w `buildings/` i `areas/` są na licencji [Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/) (ODbL). Każde użycie, także na mapie w aplikacji, podaje atrybucję:

> © OpenStreetMap contributors

Skrypt jest częścią kodu repozytorium i ma licencję MIT.

## Jak odświeżyć dane

Potrzebny jest Python 3.11 lub nowszy. Polecenia uruchamiamy w katalogu głównym repozytorium:

```sh
python -m venv .venv
.venv/bin/pip install -r data/requirements.txt
.venv/bin/python data/scripts/export_buildings.py
```

W systemie Windows zamiast `.venv/bin/` wpisujemy `.venv\Scripts\`. Skrypt nadpisuje `buildings/pwr-buildings.geojson`. Wynik jest powtarzalny: te same dane OSM dają identyczny plik, więc `git diff` pokazuje tylko prawdziwe zmiany w mapie.

Opcja `--input <plik.json>` przetwarza zapisaną odpowiedź Overpass bez nowego zapytania. Publiczny serwer Overpass ma limity, dlatego skrypt nie ponawia zapytań, a odpowiedź z błędem wykonania (pole `remark`) przerywa go bez zmiany pliku. Uruchamiamy go ręcznie, gdy chcemy odświeżyć dane, a wynik zapisujemy w repozytorium.

## Źródła

- Licencja i atrybucja OSM: <https://www.openstreetmap.org/copyright>
- Tekst ODbL 1.0: <https://opendatacommons.org/licenses/odbl/1-0/>
- Overpass API i zasady korzystania z publicznych serwerów: <https://wiki.openstreetmap.org/wiki/Overpass_API>
- Tag `entrance`: <https://wiki.openstreetmap.org/wiki/Key:entrance>
- GeoJSON, RFC 7946: <https://www.rfc-editor.org/rfc/rfc7946>
