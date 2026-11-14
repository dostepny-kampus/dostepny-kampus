# :feature:capture

## Zadanie

Zgłoszenie bariery: pomiar położenia, formularz z zdjęciami, szybkie zgłoszenie wejścia i lista własnych zgłoszeń.

## API

Trasy pomiaru, formularza, poprawki i listy zgłoszeń w pakiecie `navigation`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:geo`
- `:core:location`
- `:core:model`

## Czego moduł nie robi

Nie wysyła zgłoszeń sam; zapis i kolejka wysyłki są w `:core:data`. Nie pokazuje mapy kampusu.

## Kiedy dodawać tutaj

Kod kroków zgłoszenia i listy własnych zgłoszeń. Pobieranie i zapis danych należy do `:core:data`.
