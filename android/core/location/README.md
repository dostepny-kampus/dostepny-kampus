# :core:location

## Zadanie

Pozycja telefonu z GNSS: źródła położenia, satelity i uśrednianie pomiaru.

## API

Interfejsy źródeł położenia i ich wiązania Hilt (`LocationModule`).

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie pokazuje mapy ani ekranów pomiaru i nie zapisuje pozycji.

## Kiedy dodawać tutaj

Kod, który czyta położenie albo stan odbiornika GNSS. Ekran pomiaru należy do `:feature:capture`.
