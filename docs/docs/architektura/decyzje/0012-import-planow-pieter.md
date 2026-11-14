# ADR-0012: Ręczny import planów pięter PWr

Status: przyjęta. Data: 2026-11-14. Źródło: decyzja projektu nr 17, NFR-40, ADR-0006.

## Kontekst i problem

Budynki trafiają do bazy automatycznie z pliku w repozytorium (ADR-0008). Plany pięter PWr nie mogą leżeć w repozytorium (NFR-40, ADR-0006), więc źródłem jest usługa ArcGIS PWr, a nie plik. Trzeba rozstrzygnąć, kiedy serwer pobiera te dane.

Usługa należy do PWr: bywa niedostępna, zmienia schemat warstw, a część budynków ma po dwie wersje warstwy. Każdy start serwera, także w testach i na komputerach zespołu, nie powinien zależeć od cudzej usługi.

## Rozważone opcje

- Import przy każdym starcie serwera (tak działa import budynków).
- Import cykliczny, uruchamiany przez harmonogram serwera.
- Import ręczny, uruchamiany poleceniem serwera.

## Decyzja

Plany pięter importuje się ręcznie, poleceniem serwera uruchamianym przez osobę z dostępem do bazy. Budynki zostają importowane automatycznie z pliku przy starcie (ADR-0008).

- Plik budynków jest częścią repozytorium, niezmienny między uruchomieniami i dostępny zawsze, więc automatyczny import jest powtarzalny. Usługa PWr leży poza naszą kontrolą, więc pobranie jej danych jest świadomym krokiem operatora.
- Polecenie jest idempotentne: ponowne uruchomienie aktualizuje piętra i pomieszczenia bez duplikatów.
- Dane PWr nie trafiają do repozytorium ani do zasobów serwera; w repozytorium jest tylko kod importu (NFR-40).
- Aplikacja i serwer działają dalej na zapisanej kopii, gdy usługa PWr jest niedostępna (NFR-50).

Import przy każdym starcie odrzucamy. Start serwera zależałby od cudzej usługi i od sieci. Testy integracyjne w CI pobierałyby dane PWr albo wymagałyby ich podmiany. Każde uruchomienie pytałoby usługę PWr o kilka tysięcy pomieszczeń.

Import cykliczny odrzucamy. Plany pięter zmieniają się rzadko, a harmonogram dodaje ukryte zapytania do usługi PWr i utrudnia diagnozę, gdy zmieni się schemat warstw.

## Konsekwencje

- Dobrze: start serwera i testy nie zależą od usługi PWr ani od sieci.
- Dobrze: zmiana schematu usługi PWr psuje tylko polecenie importu, a nie działający serwer.
- Gorzej: po świeżym uruchomieniu bazy tabele pięter i pomieszczeń są puste, dopóki ktoś nie uruchomi polecenia.
- Gorzej: dane mogą się zestarzeć; odświeża je osoba uruchamiająca import.

## Źródła

1. [ADR-0006: Dane PWr o pomieszczeniach](0006-dane-pwr-o-pomieszczeniach.md)
2. [ADR-0008: Import budynków do bazy serwera](0008-import-budynkow.md)
