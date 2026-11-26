# :core:designsystem

## Zadanie

Wspólne elementy interfejsu Compose: motyw, nagłówki, przyciski, ikony i znaki oceny dostępności.

## Pakiety

- `core.designsystem` — motyw, skala czcionki, kontener grupy, wybór piętra, ikony, plakietka i linia braku sieci, wspólne błędy ekranów pracownika, znak „!” w kole oraz zapis dnia.
- `core.designsystem.account` — nazwy ról pracowniczych.
- `core.designsystem.bar` — nagłówek z powrotem, dolny pasek i przypięte przyciski ekranu.
- `core.designsystem.card` — karta pozycji listy, jej czynność, panel informacyjny i wynik czynności pracownika w panelu.
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
