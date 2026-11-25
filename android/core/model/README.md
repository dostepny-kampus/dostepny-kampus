# :core:model

## Zadanie

Typy domeny wspólne dla całej aplikacji: budynek, wejście, bariera, zgłoszenie, konto, punkt, obszar i postęp inwentaryzacji.

## Pakiety

- `core.model` — konto, punkt i obszar geograficzny oraz strefa czasu kampusu.
- `core.model.building` — budynek, katalog budynków, piętra, wejście i jego ocena.
- `core.model.export` — format pliku eksportu.
- `core.model.inspection` — postęp inwentaryzacji budynku.
- `core.model.path` — odcinek ścieżki dostosowanej z mapy kampusu.
- `core.model.room` — pomieszczenie z planu piętra i grupa jego funkcji.
- `core.model.submission` — zgłoszenie z treścią i typem bariery.

## API

Klasy danych i wyliczenia w pakiecie `core.model`; moduł jest w trybie jawnego API, więc każdy typ publiczny ma `public`.

## Zależności

Brak zależności od innych modułów projektu.

## Czego moduł nie robi

Nie zna Androida, sieci, bazy ani interfejsu. Nie zawiera logiki pobierania ani zapisu.

## Kiedy dodawać tutaj

Typ, którego używa więcej niż jeden moduł i który opisuje pojęcie domeny. Typ jednego ekranu zostaje w module funkcji.
