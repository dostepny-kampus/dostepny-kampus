# ADR-0008: Import budynków do bazy serwera

Status: przyjęta. Data: 2026-11-03.

## Kontekst i problem

Kontury i wejścia budynków PWr są w pliku `data/buildings/pwr-buildings.geojson` z OpenStreetMap. Aplikacja ma kopię tego pliku, a serwer ma tabelę `buildings` z migracji `V1__budynki.sql`. Tabela była pusta, więc `GET /buildings` zwracał pustą listę, a `GET /buildings/locate` nigdy nie wskazywał budynku.

Nie mamy jednej wspólnej bazy. Każda osoba z zespołu uruchamia bazę u siebie przez `infra/docker-compose.yml`, a każdy test serwera w CI dostaje nową bazę w Testcontainers. Ręczne wgranie danych do jednej bazy nie wystarczy.

## Rozważone opcje

- Skrypt `ogr2ogr` z pakietu GDAL [1], uruchamiany ręcznie po starcie bazy.
- Migracja SQL z poleceniami `INSERT` wygenerowanymi z pliku.
- Migracja Java we Flyway [2], która czyta plik przy starcie serwera.

## Decyzja

Wybieramy migrację Java `OsmBuildings` w module `building`.

- Maven kopiuje plik z `data/buildings` do zasobów serwera. Plik zostaje jedynym źródłem konturów dla aplikacji i serwera.
- Migracja jest powtarzalna. Jej suma kontrolna to CRC32 pliku, więc Flyway wykonuje ją przy pierwszym starcie i po każdej zmianie pliku [2].
- Zapis to `INSERT … ON CONFLICT (symbol) DO UPDATE`. Ponowny import zmienia kontur, nazwę i atrybucję, ale nie usuwa budynków i nie zmienia poziomu szczegółowości.
- Reguły są takie jak w aplikacji: kontury z tym samym symbolem tworzą jeden wielokąt złożony, a kontur z kilkoma symbolami daje budynek dla każdego symbolu.

Opcję z `ogr2ogr` odrzucamy, bo wymaga GDAL u każdej osoby i ręcznego kroku, a testy w CI dalej widziałyby pustą bazę. Migrację SQL odrzucamy, bo powiela dane z pliku, a po każdym eksporcie z OSM trzeba ją generować od nowa.

## Konsekwencje

- Dobrze: baza ma budynki od pierwszego startu, także w testach. Testy sprawdzają punkty końcowe na prawdziwych konturach.
- Dobrze: nowy eksport z OSM trafia do bazy przy następnym starcie serwera, bez nowej migracji.
- Gorzej: budynek usunięty z pliku zostaje w bazie. Usunięcie wymaga osobnej migracji, bo do budynku mogą prowadzić zgłoszenia.
- Gorzej: serwer musi być budowany z katalogu głównego repozytorium, bo czyta plik z `data/`.

## Źródła

1. [GDAL: ogr2ogr](https://gdal.org/en/stable/programs/ogr2ogr.html)
2. [Flyway: migracje Java i powtarzalne](https://documentation.red-gate.com/fd/migrations-271585107.html)
