# :macrobenchmark

## Zadanie

Pomiary wydajności aplikacji: czas zimnego startu i czas klatek przy przesuwaniu mapy oraz zmianie terenu.

## Pakiety

- `macrobenchmark` — scenariusze pomiarowe Macrobenchmark uruchamiane na wersji release aplikacji.

## API

Brak API; moduł zawiera tylko testy pomiarowe.

## Zależności

Brak zależności od innych modułów projektu; moduł mierzy zainstalowaną aplikację przez UI Automator.

## Czego moduł nie robi

Nie wchodzi do APK aplikacji, nie biegnie w kontroli PR ani w kolejce scalania i nie sprawdza poprawności funkcji.

## Kiedy dodawać tutaj

Nowy scenariusz pomiaru wydajności ścieżki użytkownika. Test funkcji zostaje w module funkcji albo w `:systemtest`.
