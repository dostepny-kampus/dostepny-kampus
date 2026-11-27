# :core:map

## Zadanie

Mapa kampusu MapLibre wspólna dla ekranów z mapą.

## Pakiety

- `core.map` — wczytanie biblioteki natywnej MapLibre.
- `core.map.style` — budowa stylu mapy w JSON: warstwy, wyrażenia i ścieżki dostosowane.
- `core.map.tiles` — klient HTTP kafelków podkładu z nagłówkiem User-Agent aplikacji; kafelki poza prostokątami terenów widocznych map tylko z pamięci podręcznej.

## API

Wczytanie biblioteki natywnej, funkcje budowy stylu MapLibre w JSON i klient kafelków z grafu Hilt (`MapTileHttp`, `MapTileHttpEntryPoint`, `MapUserAgent`).

## Zależności

- `:core:designsystem`
- `:core:model`

## Czego moduł nie robi

Nie zna ekranów ani ról: znaki punktów, ich obrazy i nazwy dla TalkBack podaje ekran, który używa mapy.

## Kiedy dodawać tutaj

Kod mapy kampusu potrzebny więcej niż jednemu ekranowi (V1, C1, R1). Kod jednego ekranu zostaje w jego module funkcji.
