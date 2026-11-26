# :feature:review

## Zadanie

Sprawdzanie zgłoszeń przez Weryfikatora: kolejka weryfikacji (R1) z „Wziętymi przeze mnie” i sprawdzenie zgłoszenia (R2).

## Pakiety

- `feature.review.check` — ekran, stan, ViewModel i decyzja sprawdzenia zgłoszenia.
- `feature.review.photo` — zasłony twarzy i tablic na zdjęciu zgłoszenia, ich wypalenie w pikselach, stan i ekran zasłaniania.
- `feature.review.navigation` — trasy kolejki weryfikacji, sprawdzenia zgłoszenia i zasłaniania zdjęcia.
- `feature.review.queue` — ekran, stan i ViewModel kolejki weryfikacji.

## API

Trasy ekranów w pakiecie `navigation`; ekran R1, jego czynności i ViewModel w pakiecie `queue`; ekran R2,
jego czynności i ViewModel w pakiecie `check`; ekran zasłaniania zdjęcia, jego czynności i ViewModel w pakiecie
`photo`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; kolejka i wzięcie zgłoszenia są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan sprawdzania zgłoszeń przez Weryfikatora. Pobieranie i zapis danych należy do `:core:data`.
