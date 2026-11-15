# :systemtest

## Zadanie

Testy systemowe: APK testów na urządzeniu sprawdza klienta sieci i ekrany aplikacji na uruchomionym serwerze z bazą
i logowaniem.

## Pakiety

- `systemtest` — testy klienta sieci na uruchomionym serwerze: budynki, karta budynku i wysyłka zgłoszenia.

## API

Brak API; moduł zawiera tylko testy.

## Zależności

- `:core:network`
- `:core:testing`

## Czego moduł nie robi

Nie podmienia sieci ani grafu Hilt aplikacji i nie zastępuje testów jednostkowych modułów.

## Kiedy dodawać tutaj

Test, który sprawdza zgodność aplikacji i serwera na prawdziwym serwerze, także przez ekrany aplikacji. Test jednego modułu zostaje w tym module.
