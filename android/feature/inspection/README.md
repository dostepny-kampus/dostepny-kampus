# :feature:inspection

## Zadanie

Postęp inwentaryzacji: lista budynków z postępem dla Koordynatora (K1), znaczniki budynku do cofnięcia (C5 Koordynatora) i piętra do oznaczenia jako obejrzane (C5 Zbierającego).

## Pakiety

- `feature.inspection.export` — stan i ViewModel eksportu K3.
- `feature.inspection.invitation` — stan i ViewModel zaproszenia K2.
- `feature.inspection.navigation` — trasa ekranu postępu.
- `feature.inspection.progress` — ekrany, stan i ViewModele postępu budynków i znaczników pięter.

## API

Trasa ekranu w pakiecie `navigation`; ekran i jego ViewModel w pakiecie `progress`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; odczyt postępu, piętra budynku, kolejka znaczników i cofnięcie znacznika są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan postępu inwentaryzacji. Pobieranie i zapis danych należy do `:core:data`.
