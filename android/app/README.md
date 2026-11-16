# :app

## Zadanie

Aplikacja: start, nawigacja między ekranami funkcji, powłoka z paskiem nawigacji i wysyłka zgłoszeń po starcie.

## Pakiety

- `app` — aktywność, powłoka aplikacji z arkuszem, start wysyłki zgłoszeń, ekran wygasłego logowania i drzewo logów wersji release.
- `app.di` — wiązania Hilt aplikacji: nagłówek klienta mapy i ładowanie obrazów.
- `app.navigation` — nawigacja między ekranami mapy, zgłoszeń i logowania.

## API

Brak API dla innych modułów; to moduł końcowy, który składa graf Hilt i ekrany z `feature/*`.

## Zależności

- `:core:data`
- `:core:database`
- `:core:designsystem`
- `:core:model`
- `:feature:auth`
- `:feature:capture`
- `:feature:help`
- `:feature:inspection`
- `:feature:map`

## Czego moduł nie robi

Nie ma logiki ekranów, pobierania ani zapisu danych; te są w modułach funkcji i `core`.

## Kiedy dodawać tutaj

Kod, który łączy moduły funkcji: trasa między ekranami różnych funkcji, powłoka aplikacji, wiązanie Hilt całej aplikacji. Ekran jednej funkcji należy do jej modułu.
