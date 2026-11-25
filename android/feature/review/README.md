# :feature:review

## Zadanie

Sprawdzanie zgłoszeń przez Weryfikatora: kolejka weryfikacji (R1) z „Wziętymi przeze mnie” i sprawdzenie zgłoszenia (R2).

## Pakiety

- `feature.review.check` — ekran, stan i ViewModel sprawdzenia zgłoszenia.
- `feature.review.navigation` — trasa ekranu kolejki weryfikacji.
- `feature.review.queue` — ekran, stan i ViewModel kolejki weryfikacji.

## API

Trasa ekranu w pakiecie `navigation`; ekran, jego czynności i ViewModel w pakiecie `queue`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; kolejka i wzięcie zgłoszenia są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan sprawdzania zgłoszeń przez Weryfikatora. Pobieranie i zapis danych należy do `:core:data`.
