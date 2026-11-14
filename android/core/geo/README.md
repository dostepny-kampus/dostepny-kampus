# :core:geo

## Zadanie

Geometria kampusu: obszary, odległości, wyszukiwanie i dopasowanie budynku do pozycji.

## API

Funkcje i typy w pakiecie `core.geo`, m.in. `LocateResult`, `SearchBuildings`, `CampusAreas`, parser katalogu budynków.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie zna Androida, sieci ani bazy; liczy tylko na danych, które dostanie.

## Kiedy dodawać tutaj

Obliczenie geometryczne albo odczyt GeoJSON potrzebny w więcej niż jednym module. Rysowanie mapy należy do `:feature:map`.
