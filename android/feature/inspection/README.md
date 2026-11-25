# :feature:inspection

## Zadanie

Postęp inwentaryzacji: lista budynków z postępem dla Koordynatora (K1), znaczniki budynku do cofnięcia (C5 Koordynatora) i piętra do oznaczenia jako obejrzane (C5 Zbierającego) oraz eksport barier dla Koordynatora (K3).

## Pakiety

- `feature.inspection.export` — ekran eksportu K3, jego stan i ViewModel.
- `feature.inspection.navigation` — trasy ekranów postępu i eksportu.
- `feature.inspection.progress` — ekrany, stan i ViewModele postępu budynków i znaczników pięter.

## API

Trasy ekranów w pakiecie `navigation`; ekrany i ViewModele w pakietach `progress` i `export`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie pobiera danych z serwera sam; odczyt postępu, piętra budynku, kolejka znaczników i cofnięcie znacznika są w `:core:data`.

## Kiedy dodawać tutaj

Ekrany i stan postępu inwentaryzacji i eksportu. Pobieranie i zapis danych należy do `:core:data`.
