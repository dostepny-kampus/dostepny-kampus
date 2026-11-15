# :feature:inspection

## Zadanie

Postęp inwentaryzacji dla Koordynatora: lista budynków z postępem (K1) i znaczniki budynku do cofnięcia (C5).

## Pakiety

- `feature.inspection.progress` — stan i ViewModel listy postępu budynków.

## API

Stan listy postępu i jego ViewModel w pakiecie `progress`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; odczyt postępu i cofnięcie znacznika są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan postępu inwentaryzacji. Pobieranie i zapis danych należy do `:core:data`.
