# :core:designsystem

## Zadanie

Wspólne elementy interfejsu Compose: motyw, nagłówki, przyciski, ikony i znaki oceny dostępności.

## Pakiety

- `core.designsystem` — motyw, skala czcionki, kontener grupy, wybór piętra, ikony, plakietka i linia braku sieci, ikony statusów zgłoszenia oraz zapis dnia.
- `core.designsystem.bar` — nagłówek z powrotem, dolny pasek i przypięte przyciski ekranu.
- `core.designsystem.marking` — oznaczenie wejścia i bariery: etykieta, obraz, dzień i źródło oznaczenia, znak uciążliwości, nazwy i zdjęcie bariery.

## API

Funkcje `@Composable` i stałe wyglądu w pakiecie `core.designsystem`.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie pobiera danych i nie zna nawigacji ani ViewModel.

## Kiedy dodawać tutaj

Element użyty na ekranach co najmniej dwóch funkcji. Element jednego ekranu zostaje w module funkcji.
