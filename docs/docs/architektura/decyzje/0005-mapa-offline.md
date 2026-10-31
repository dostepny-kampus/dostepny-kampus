# ADR-0005: Mapa bez sieci

Status: przyjęta. Data: 2026-10-27. Źródło: decyzje projektu nr 14 i 27.

## Kontekst i problem

Zbierający pracuje w miejscach bez sieci (N6), a Odwiedzający sprawdza budynek przed wizytą (N1). Polityka serwerów kafelków OSM zabrania pobierania kafelków do użytku offline i pobierania masowego [1]. Mapa kampusu potrzebuje konturów budynków z symbolem budynku.

## Rozważone opcje

- Kafelki OSM zapisane w telefonie: łamie politykę serwerów kafelków.
- Mapa tylko online: bez sieci nie widać budynków ani barier.
- Własne dane rysowane przez MapLibre bez sieci, podkład OSM tylko online.
- Źródło konturów: OpenStreetMap, BDOT10k albo mapa kampusu PWr (bez API, tylko lista budynków do sprawdzenia).

## Decyzja

- Mapę rysuje MapLibre Native [2].
- Kontury budynków eksportujemy raz z OpenStreetMap przez Overpass API do `data/buildings/` (GeoJSON, EPSG:4326). Kontur z błędem uzupełnia BDOT10k.
- Kluczem budynku jest symbol budynku, np. `A-1`, znormalizowany z tagu `ref`.
- Kontury trafiają do pakietu aplikacji i do bazy serwera. Bez sieci aplikacja rysuje kontury, plany pięter i bariery z bazy lokalnej.
- Podkład OSM pobieramy tylko online, z pamięcią podręczną według nagłówków serwera (NFR-39).
- Bariery są osobną warstwą, powiązaną z budynkiem tylko symbolem (NFR-38).

## Konsekwencje

- Dobrze: mapa i podpowiedź budynku działają bez sieci (NFR-4, NFR-10).
- Dobrze: zgodność z polityką kafelków OSM i licencją ODbL [3].
- Gorzej: bez sieci nie ma ulic ani tła, tylko kontury i plany pięter.
- Gorzej: kontury trzeba odświeżać skryptem, a atrybucja OSM jest stale widoczna na mapie (NFR-37).

## Źródła

1. [OSMF: polityka korzystania z kafelków](https://operations.osmfoundation.org/policies/tiles/)
2. [MapLibre Native](https://maplibre.org/projects/native/)
3. [Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/)
