# :core:designsystem

## Zadanie

Wspólne elementy interfejsu Compose: motyw, nagłówki, przyciski, ikony, kolumna tekstu i znaki oceny dostępności.

Pasek nawigacji z boku uwzględnia wcięcia systemowe przy krawędziach okna.

## Pakiety

- `core.designsystem` — motyw, skala czcionki, kontener grupy, wybór piętra, ikony, plakietka i linia braku sieci, wspólne błędy ekranów pracownika, znak „!” w kole i komunikat błędu z nim oraz zapis dnia.
- `core.designsystem.account` — nazwy ról pracowniczych.
- `core.designsystem.bar` — nagłówek z powrotem, dolny pasek i przypięte przyciski wyrównane w obrębie wiersza.
- `core.designsystem.card` — karta pozycji listy, jej czynność, panel informacyjny, wynik czynności pracownika w panelu i przycisk obrysowany w treści ekranu.
- `core.designsystem.submission` — zgłoszenie: słowa, ikony i wiersz statusów oraz nazwy pól treści.
- `core.designsystem.marking` — oznaczenie wejścia i bariery: etykieta, obraz, dzień i źródło oznaczenia, znak uciążliwości ze słowem, wybór uciążliwości, nazwy, wybór typu, zdjęcie i miejsce bariery słowami.

## API

Funkcje `@Composable` i stałe wyglądu w pakiecie `core.designsystem` i jego podpakietach.

## Zależności

- `:core:model`

## Czego moduł nie robi

Nie pobiera danych i nie zna nawigacji ani ViewModel.

## Kiedy dodawać tutaj

Element użyty na ekranach co najmniej dwóch funkcji. Element jednego ekranu zostaje w module funkcji.
