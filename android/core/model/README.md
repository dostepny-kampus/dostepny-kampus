# :core:model

## Zadanie

Typy domeny wspólne dla całej aplikacji: budynek, wejście, bariera, zgłoszenie, konto, punkt i obszar.

## API

Klasy danych i wyliczenia w pakiecie `core.model`; moduł jest w trybie jawnego API, więc każdy typ publiczny ma `public`.

## Zależności

Brak zależności od innych modułów projektu.

## Czego moduł nie robi

Nie zna Androida, sieci, bazy ani interfejsu. Nie zawiera logiki pobierania ani zapisu.

## Kiedy dodawać tutaj

Typ, którego używa więcej niż jeden moduł i który opisuje pojęcie domeny. Typ jednego ekranu zostaje w module funkcji.
