# :core:testing

## Zadanie

Kroki testów na urządzeniu na ekranach aplikacji, wspólne dla testów aplikacji (`androidTest`) i testów systemowych.

## Pakiety

- `core.testing` — kroki na regule testu Compose: czekanie na węzeł, zamknięcie pomocy, otwarcie karty budynku.

## API

`DeviceSteps` na zwykłym `ComposeTestRule`, `inCatalog` (wiersz budynku w wynikach) i `CHECK_ENTRANCES`; moduł jest
w trybie jawnego API.

## Zależności

Brak zależności od innych modułów projektu.

## Czego moduł nie robi

Nie uruchamia aktywności, nie podmienia sieci ani grafu Hilt i nie zawiera samych testów; tylko kod testowy, nigdy
w zależności `implementation` aplikacji.

## Kiedy dodawać tutaj

Krok na ekranach aplikacji, którego potrzebują testy na urządzeniu w więcej niż jednym module.
