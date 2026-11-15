# :core:designsystem

## Zadanie

Wspólne elementy interfejsu Compose: motyw, nagłówki, przyciski, ikony i znaki oceny dostępności.

## Pakiety

- `core.designsystem` — motyw, skala czcionki, kontener grupy, wybór piętra i ikony.
- `core.designsystem.bar` — nagłówek z powrotem, dolny pasek i przypięte przyciski ekranu.
- `core.designsystem.marking` — oznaczenie wejścia i bariery: etykieta, obraz, znak uciążliwości i nazwy barier.

## API

Funkcje `@Composable` i stałe wyglądu w pakiecie `core.designsystem`.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie pobiera danych i nie zna nawigacji ani ViewModel.

## Kiedy dodawać tutaj

Element użyty na ekranach co najmniej dwóch funkcji. Element jednego ekranu zostaje w module funkcji.
