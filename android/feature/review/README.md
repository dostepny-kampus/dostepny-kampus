# :feature:review

## Zadanie

Sprawdzanie zgłoszeń przez Weryfikatora: kolejka weryfikacji (R1) z „Wziętymi przeze mnie” i sprawdzenie zgłoszenia (R2).

## Pakiety

- `feature.review.check` — ekran, stan, ViewModel i decyzja sprawdzenia zgłoszenia.
- `feature.review.navigation` — trasa ekranu kolejki weryfikacji.
- `feature.review.queue` — ekran, stan i ViewModel kolejki weryfikacji.

## API

Trasa ekranu w pakiecie `navigation`; ekran R1, jego czynności i ViewModel w pakiecie `queue`; ekran R2, jego czynności i ViewModel w pakiecie `check`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; kolejka i wzięcie zgłoszenia są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan sprawdzania zgłoszeń przez Weryfikatora. Pobieranie i zapis danych należy do `:core:data`.
