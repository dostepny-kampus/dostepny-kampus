# ADR-0015: Jeden sterownik kamery mapy

Status: przyjęta. Data: 2026-11-16. Źródło: NFR-56 i historie mapy V1 z kartą V2.

## Kontekst i problem

Kamerę mapy V1 zmieniają dziś cztery mechanizmy, każdy z osobnego efektu kompozycji:

- ruch do celu (przesunięcie albo przelot),
- dopełnienie pod kartą V2, przesuwane osobną animacją,
- granice terenu (granica środka i najmniejsze powiększenie),
- zmiana terenu w trakcie przelotu, która wczytuje styl mapy od nowa.

O poprawności decyduje kolejność ich wywołań. W MapLibre ustawienie granic kamery robi skok kamery, a każdy nowy ruch kończy poprzedni [1]. Ruch przerwany skokiem w kodzie natywnym zgłasza się wywołaniem `onFinish`, a nie `onCancel` [1]. Aplikacja bierze wtedy przerwany przelot za skończony i nie rusza dalej.

Cel ruchu liczy się dwa razy na jeden wybór budynku. Pierwszy raz liczy się przed kartą, do punktu budynku. Drugi raz liczy się po zasłonięciu mapy kartą, do konturu budynku. Karta i pasek górny idą za policzonym celem i jego czasem, nie za kamerą.

Na telefonie i emulatorze wychodzą z tego dwa ruchy przy A-1, przelot do B-2 zatrzymany nad Odrą i rozjazd karty z mapą.

## Rozważone opcje

- **Poprawki w miejscach błędów.** Kolejność wywołań zostaje kruchą umową między efektami.
- **Własny sterownik kamery według wzorca `CameraPositionState` z Google Maps Compose [2].** Ruch to funkcja `suspend`. Nowy ruch przerywa poprzedni przez `MutatorMutex` i rusza po zakończeniu przerwania.
- **Biblioteka `maplibre-compose` [3].** Kamera przez `suspend animateTo`. Przejście zmienia jednak cały widok mapy razem ze stylem i warstwami.

## Decyzja

- W `feature/map/camera` jest jeden sterownik kamery i tylko on woła kamerę MapLibre. Ruch, dopełnienie i granice przechodzą przez niego.
- Zasada „ostatni wygrywa”: nowy cel przerywa trwający ruch, a nieaktualne ruchy nie czekają w kolejce.
- Dopełnienie jedzie w tym samym ruchu co cel. Granice idą do mapy tylko w spoczynku kamery. Żaden ruch nie zaczyna się z wywołania końca albo przerwania poprzedniego.
- Sterownik odróżnia koniec od przerwania po położeniu kamery względem celu, nie tylko po wywołaniu MapLibre.
- Jeden wybór budynku daje jeden cel, liczony przy znanej wysokości karty.
- Karta i pasek górny biorą położenie z postępu ruchu kamery w każdej klatce.
- Zmiana terenu zmienia dane źródeł i widoczność warstw we wczytanym stylu, bez ponownego wczytania stylu.
- Kolejność zdarzeń sprawdzają testy na udawanej mapie. Skok przy ustawieniu granic sprawdza test na prawdziwym MapLibre.
- `maplibre-compose` oceniamy osobno, po zakończeniu prac nad stylem mapy.

## Konsekwencje

- Dobrze: jedno miejsce odpowiada za kamerę, a przerwanie ruchu ma jedną, sprawdzoną ścieżkę.
- Dobrze: karta i mapa nie rozjeżdżają się przy przerwanym ruchu.
- Gorzej: własny kod nad MapLibre do utrzymania, dopóki ocena biblioteki nie zdecyduje inaczej.
- Gorzej: test na prawdziwym MapLibre potrzebuje emulatora, więc nie biegnie w każdym PR.

## Źródła

1. [MapLibre Native: `Transform` (Android i rdzeń)](https://github.com/maplibre/maplibre-native)
2. [Google Maps Compose: `CameraPositionState`](https://github.com/googlemaps/android-maps-compose)
3. [maplibre-compose](https://github.com/maplibre/maplibre-compose)
