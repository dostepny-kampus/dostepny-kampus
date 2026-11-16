# :feature:map

## Zadanie

Mapa kampusu: budynki, wejścia i bariery na mapie, wyszukiwanie budynku, karta budynku i lista wejść.

## Pakiety

- `feature.map` — ekran mapy kampusu, jego model widoku, wyszukiwanie i widok MapLibre.
- `feature.map.area` — wybór terenu kampusu i komunikat o pokazywanym terenie.
- `feature.map.barrier` — bariery na mapie, ich wybór i arkusz bariery ze zdjęciem.
- `feature.map.building` — karta budynku, lista i wyszukiwanie budynków, wejścia i grupa barier.
- `feature.map.camera` — położenie i ruch kamery mapy; kamerę zmienia jeden sterownik (ADR-0015).
- `feature.map.campus` — dane mapy kampusu i zaznaczenie.
- `feature.map.navigation` — trasa ekranu mapy.
- `feature.map.overlay` — nakładki mapy: sterowanie, legenda, informacja i stan sieci.
- `feature.map.plan` — plan piętra budynku po przybliżeniu mapy.
- `feature.map.style` — styl mapy: warstwy, obrazy i kafle.

## API

Trasa mapy w pakiecie `navigation` i ekran mapy dla `:app`.

## Zależności

- `:core:data`
- `:core:designsystem`
- `:core:geo`
- `:core:model`

## Czego moduł nie robi

Nie przyjmuje zgłoszeń i nie mierzy położenia; zgłoszenie zaczyna `:feature:capture`.

## Kiedy dodawać tutaj

Kod tego, co osoba widzi i wybiera na mapie i w karcie budynku. Wspólny element interfejsu kilku funkcji należy do `:core:designsystem`.
