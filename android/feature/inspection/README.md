# :feature:inspection

## Zadanie

Postęp inwentaryzacji dla Koordynatora: lista budynków z postępem (K1) i znaczniki budynku do cofnięcia (C5).

## Pakiety

- `feature.inspection.navigation` — trasa ekranu postępu.
- `feature.inspection.progress` — ekran, stan i ViewModel listy postępu budynków.

## API

Trasa ekranu w pakiecie `navigation`; ekran i jego ViewModel w pakiecie `progress`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; odczyt postępu i cofnięcie znacznika są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan postępu inwentaryzacji. Pobieranie i zapis danych należy do `:core:data`.
