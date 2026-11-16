# ADR-0014: Podkład mapy wektorowy

Status: przyjęta, zastępuje część ADR-0005 o podkładzie. Data: 2026-11-16. Źródło: NFR-16, NFR-37, NFR-39.

## Kontekst i problem

ADR-0005 ustala podkład z rastrowych kafelków OSM, pobierany tylko online. Tokeny mapy mają jednak paletę podkładu z parami kontrastu, a makiety hi-fi rysują podkład w tych kolorach. Rastra nie da się pokolorować tokenami. Aplikacja wygląda więc inaczej niż makiety, a pary kontrastu podkładu nie działają (NFR-16).

Na rastrze są też znaki i numery domów, które zasłaniają budynki PWr, a powyżej powiększenia 19 obraz jest rozmyty.

Właściciel porównał w prototypie ten sam widok terenu w kilku podkładach: raster OSM, raster w spokojnym stylu i wektor w kilku paletach.

## Rozważone opcje

- Raster OSM, tylko przygaszony: bez kontroli kolorów, podpisów i kontrastu.
- Raster w spokojnym stylu od dostawcy z kluczem: dalej bez tokenów, do tego klucz w aplikacji.
- Kafelki wektorowe OpenFreeMap w schemacie OpenMapTiles, styl z tokenów [1], [2].
- Własny wycinek Wrocławia w pliku PMTiles w pakiecie aplikacji [3]: podkład także bez sieci, ale większa aplikacja i skrypt odświeżania.

## Decyzja

- Podkład to kafelki wektorowe OpenFreeMap. Styl mapy budujemy w aplikacji z tokenów sekcji `mapa` [4].
- Paleta jasna to wariant „żywy” z prototypu: wyraźna zieleń i woda, budynki spoza PWr od powiększenia 13, nazwy dzielnic wielkimi literami.
- Budynki PWr, plany pięter i bariery dalej pochodzą z danych aplikacji (ADR-0005). Podkład ich nie zastępuje.
- Podkład pobieramy tylko online, z pamięcią podręczną według nagłówków serwera (NFR-39). Glify podpisów też pochodzą z OpenFreeMap.
- Napis „© OpenStreetMap contributors” zostaje w ramce mapy (NFR-37). OpenFreeMap i OpenMapTiles są w źródłach mapy pod „ⓘ”, jak przycisk atrybucji MapLibre [2], [5].
- Własny wycinek w PMTiles zostaje wariantem na później, gdy będzie potrzebny podkład bez sieci.

## Konsekwencje

- Dobrze: aplikacja wygląda jak makiety, a kontrast podkładu sprawdzają pary tokenów (NFR-16).
- Dobrze: bez klucza i bez konta u dostawcy. Ciemny motyw dostaje własną paletę zamiast odwróconego rastra.
- Gorzej: OpenFreeMap nie gwarantuje dostępności. Brak podkładu nie blokuje konturów i list (NFR-50).
- Gorzej: styl mapy jest kodem aplikacji i zmienia się razem ze schematem OpenMapTiles.

## Źródła

1. [OpenFreeMap](https://openfreemap.org/)
2. [OpenMapTiles: schemat i licencja](https://openmaptiles.org/schema/)
3. [PMTiles](https://docs.protomaps.com/pmtiles/)
4. [MapLibre Style Spec](https://maplibre.org/maplibre-style-spec/)
5. [OSMF: wytyczne atrybucji](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines)
