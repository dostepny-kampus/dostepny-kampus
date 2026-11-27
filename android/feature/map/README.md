# :feature:map

## Zadanie

Mapa kampusu: budynki, wejścia i bariery na mapie, wyszukiwanie budynku, karta budynku i lista wejść.

## Pakiety

- `feature.map` — ekran mapy kampusu, jego model widoku, nakładka mapy, wyszukiwanie i widok MapLibre.
- `feature.map.area` — wybór terenu kampusu i komunikat o pokazywanym terenie.
- `feature.map.barrier` — bariery na mapie, ich wybór, arkusz bariery ze zdjęciem i wiersz bariery wejścia.
- `feature.map.building` — karta budynku, lista i wyszukiwanie budynków, wejścia i grupa barier.
- `feature.map.camera` — położenie i ruch kamery mapy; kamerę zmienia jeden sterownik (ADR-0015). Widok mapy
  zgłasza dotknięte kontury i znaki.
- `feature.map.campus` — dane mapy kampusu (kontury, tereny, wejścia, punkty barier, pokazany plan piętra)
  i zaznaczenie.
- `feature.map.entrance` — arkusz wejścia po dotknięciu jego znaku na mapie i obrazy znaków wejść na mapie.
- `feature.map.navigation` — trasa ekranu mapy.
- `feature.map.overlay` — kontrolki mapy: przyciski, legenda, informacja i ramka arkuszy.
- `feature.map.path` — ścieżki dostosowane w danych mapy.
- `feature.map.plan` — plan piętra budynku po przybliżeniu mapy i arkusz znaku planu.
- `feature.map.room` — znaleziona sala: wynik pod polem szukaj, karta sali i droga do niej (wejście zalecane, winda i sala).
- `feature.map.search` — wyniki pod polem szukaj: budynki i sale, prośba o budynek i karta „brak wyniku”.
- `feature.map.style` — styl mapy: warstwy i obrazy, w tym znak bariery obok pinezki wybranego wejścia.
- `feature.map.tiles` — klient HTTP kafelków podkładu z nagłówkiem User-Agent aplikacji. Pobiera kafelki przecinające
  prostokąt terenu co najmniej jednego widocznego widoku mapy.
- `feature.map.trace` — sekcje śladu systemowego (Perfetto) wokół pracy mapy.

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
