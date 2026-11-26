# :core:map

## Zadanie

Mapa kampusu MapLibre wspólna dla ekranów z mapą.

## Pakiety

- `core.map` — wczytanie biblioteki natywnej MapLibre.
- `core.map.style` — budowa stylu mapy w JSON: warstwy, wyrażenia i ścieżki dostosowane.

## API

Wczytanie biblioteki natywnej i funkcje budowy stylu MapLibre w JSON.

## Zależności

- `:core:designsystem`

## Czego moduł nie robi

Nie zna ekranów ani ról: znaki punktów, ich obrazy i nazwy dla TalkBack podaje ekran, który używa mapy.

## Kiedy dodawać tutaj

Kod mapy kampusu potrzebny więcej niż jednemu ekranowi (V1, C1, R1). Kod jednego ekranu zostaje w jego module funkcji.
