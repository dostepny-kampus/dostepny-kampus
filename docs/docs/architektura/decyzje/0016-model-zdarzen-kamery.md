# ADR-0016: Model zdarzeń kamery mapy

Status: przyjęta, uzupełnia ADR-0015. Data: 2026-11-26. Źródło: NFR-56, US2.8.

## Kontekst i problem

ADR-0015 dał mapie jednego wykonawcę ruchu kamery. Kamerę MapLibre zmienia tylko sterownik kamery. Cel ruchu, rodzaj ruchu, dopełnienie i granice wylicza jednak ekran w kompozycji, z kilkunastu obiektów stanu w kilku plikach. Część z nich to zwykłe obiekty zapisywane w efektach kompozycji, więc ich stan zależy od liczby i kolejności kompozycji.

Ruch kamery to dziś wartość. Widok mapy rusza kamerę, gdy ta wartość się zmieni. Każda zmiana danych wejściowych może więc dać nowy ruch, choć osoba nic nie zrobiła: rozmiar mapy, wysokość karty, pasek, napis o terenie, teren. Dane przychodzą w różnych klatkach, a kod trzyma wartości z poprzedniej klatki albo czeka na ruch przez stały czas.

Z tych przyczyn wracają te same błędy pod różnymi nazwami. Przykłady to dwa ruchy po jednym wyborze, ruch spóźniony o sekundę, cel przycięty starym terenem i przeskok przy zamknięciu karty. Trzy punkty ADR-0015 nie są spełnione:

- dopełnienie jedzie w tym samym ruchu co cel,
- jeden wybór daje jeden cel przy znanej wysokości karty,
- karta i pasek biorą ruch z kamery.

## Rozważone opcje

- **Dalsze poprawki w miejscach błędów.** Każda poprawka dokłada warunek, który zmienia warunki następnych.
- **Model zdarzeń i stanu kamery w zwykłej klasie.** Ekran przekazuje zdarzenia, model wydaje polecenia z numerem czynności, sterownik je wykonuje. Ten wzorzec stosują kamery map: `CameraPositionState` w Google Maps Compose [1] oraz stany i przejścia widoku w Mapbox Maps i Mapbox Navigation [2], [3]. Jest to też jednokierunkowy przepływ danych z przewodnika architektury Androida [4].
- **Biblioteka `maplibre-compose`.** Daje kamerę z przerwaniem ruchu, ale nie zna reguł aplikacji: kiedy i dokąd jechać. Oceniamy ją osobno (#2855).

## Decyzja

- Ruch kamery powstaje tylko ze zdarzenia: czynności osoby albo zdarzenia systemu, np. pomiaru mapy. Polecenie ruchu ma numer czynności. Widok mapy wykonuje polecenie raz, po nowym numerze, a nie po zmianie wartości.
- Stan kamery mapy jest w zwykłej klasie w `feature/map/camera`, bez Compose i bez MapLibre. Wejście to zdarzenia, wyjście to stan i polecenia. Ekran tylko przekazuje zdarzenia i czyta stan.
- Rozmiar mapy, zasłonięta góra i dół, miejsce napisu i pas przycisków tworzą jedną geometrię mapy z jednego pomiaru. Z niej liczą się wolna część mapy, granice kamery i obszar podkładu.
- Czynność, której cel zależy od wysokości karty, czeka w jawnym stanie na pomiar karty. Mapa nie rusza przed pomiarem. Potem mapa i karta jadą razem jednym ruchem, po tej samej krzywej.
- Sterownik kamery z ADR-0015 zostaje jedynym, który zmienia kamerę MapLibre, także przy pierwszym widoku. Publikuje stan prawdziwego ruchu: start, koniec, przerwanie. Karta, pasek i napis o terenie biorą czas z tego stanu.
- Styl mapy ma jednego właściciela: wczytanie stylu, teren, podkład i jego odsłonięcie. Przelot zmienia teren przez zdarzenia sterownika kamery.
- Reguły ruchu (rodzaj i czas ruchu, kadr budynku, sali i terenu) zostają czystymi funkcjami. Zmienia się tylko to, kto i kiedy je woła.
- Przejście idzie małymi krokami bez zmiany zachowania. Pierwsze są testy, które liczą ruchy prawdziwego sterownika po czynnościach osoby.

## Konsekwencje

- Dobrze: jedna czynność osoby daje najwyżej jeden ruch. Pilnuje tego test modelu w każdym PR.
- Dobrze: przyczyny ruchu są czytelne w jednym miejscu, bez zależności od kolejności kompozycji.
- Dobrze: widok MapLibre stoi za wąskim interfejsem, więc wymiana na `maplibre-compose` dotyczyłaby tylko jego.
- Gorzej: kilka PR przenosin, każdy do sprawdzenia nagraniem klatek na emulatorze.
- Gorzej: na czas przejścia część logiki jest w modelu, a część jeszcze w ekranie.

## Źródła

1. [Google Maps Compose: `CameraPositionState`](https://github.com/googlemaps/android-maps-compose)
2. [Mapbox Maps SDK for Android: `ViewportPlugin`](https://docs.mapbox.com/android/maps/api/11.0.0/mapbox-maps-android/com.mapbox.maps.plugin.viewport/-viewport-plugin/)
3. [Mapbox Navigation SDK for Android: `NavigationCamera`](https://docs.mapbox.com/android/navigation/guides/ui-components/camera/)
4. [Android Developers: warstwa interfejsu i przepływ danych](https://developer.android.com/topic/architecture/ui-layer)
