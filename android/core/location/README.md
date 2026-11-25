# :core:location

## Zadanie

Pozycja telefonu z GNSS: źródła położenia, satelity i uśrednianie pomiaru.

## Pakiety

- `core.location` — źródła położenia GNSS, stan satelitów i uśrednianie pomiarów.

## API

Interfejsy źródeł położenia i uśrednianie pozycji. Implementacje GNSS i wiązania Hilt (`LocationModule`) są `internal`.

Źródła wybiera wariant budowy (`VariantSources.kt` w `src/release` i `src/debug`). Release ma tylko odbiornik GNSS.
Debug ma też tryb pokazu (`DemoMode`, `DemoRoute`, adnotacja `TrybPokazu`): przy włączonym trybie pomiar C1 odtwarza
trasę `src/debug/assets/trasa-pokazu.json` zamiast odbiornika.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie pokazuje mapy ani ekranów pomiaru i nie zapisuje pozycji.

## Kiedy dodawać tutaj

Kod, który czyta położenie albo stan odbiornika GNSS. Ekran pomiaru należy do `:feature:capture`.
