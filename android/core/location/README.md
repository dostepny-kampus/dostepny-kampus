# :core:location

## Zadanie

Pozycja telefonu z GNSS: źródła położenia, satelity i uśrednianie pomiaru.

## Pakiety

- `core.location` — źródła położenia GNSS, stan satelitów i uśrednianie pomiarów.

## API

Interfejsy źródeł położenia i uśrednianie pozycji. Implementacje GNSS i wiązania Hilt (`LocationModule`) są `internal`.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie pokazuje mapy ani ekranów pomiaru i nie zapisuje pozycji.

## Kiedy dodawać tutaj

Kod, który czyta położenie albo stan odbiornika GNSS. Ekran pomiaru należy do `:feature:capture`.
