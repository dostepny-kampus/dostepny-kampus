# ADR-0006: Dane PWr o pomieszczeniach

Status: przyjęta. Data: 2026-10-28. Źródło: decyzje projektu nr 15, 17 i 44.

## Kontekst i problem

Publiczne usługi ArcGIS PWr [1] mają plany pięter 25 budynków: około 4680 pomieszczeń z numerem, funkcją i piętrem. Warstwa kampusu ma 135 wejść z informacją o przystosowaniu i szerokości drzwi. Usługi nie podają licencji, prawa do danych ma PWr.

Dane służą US16, US17 i wyszukiwaniu sali.

## Rozważone opcje

- Import z publicznej usługi przez serwer, bez danych w repozytorium.
- Dane w publicznym repozytorium.
- Ręczna digitalizacja albo rysowanie wnętrz w OpenStreetMap.
- Brak planów pięter.

## Decyzja

- Serwer importuje piętra, pomieszczenia i wejścia z usługi ArcGIS i trzyma kopię w PostGIS. Aplikacja dostaje je przez API.
- W repozytorium jest tylko skrypt importu, bez danych (NFR-40).
- Każdy widok planu piętra i odpowiedź API podaje źródło „Politechnika Wrocławska”.
- Budynki szczegółowe to wszystkie budynki z planem PWr. Ich lista jest konfiguracją (NFR-47).
- Wejścia dostają pierwsze oznaczenia z warstwy wejść PWr.
- Podstawą jest cel dydaktyczny i niekomercyjny ze wskazaniem źródła (art. 8 ust. 1 pkt 2 ustawy o ochronie baz danych [2]). Aplikacji nie publikujemy.

## Konsekwencje

- Dobrze: plany pięter, wyszukiwanie sali i oznaczenia wejść bez ręcznego wprowadzania.
- Dobrze: publiczne repozytorium nie zawiera cudzych danych.
- Gorzej: zmiana schematu usługi PWr wymaga zmiany importu.
- Gorzej: dane mogą się zestarzeć, dlatego oznaczenie wejścia pokazuje datę i źródło (US17).
- Gorzej: trasy do sali nie wyznaczymy, bo w danych nie ma drzwi.

## Źródła

1. [Usługi ArcGIS PWr (katalog REST)](https://services3.arcgis.com/rrLByHvJ7zuJQDVw/arcgis/rest/services)
2. [Ustawa z dnia 27 lipca 2001 r. o ochronie baz danych](https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20011281402)
