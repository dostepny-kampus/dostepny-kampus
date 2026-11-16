# ADR-0013: Logi aplikacji i serwera

Status: przyjęta. Data: 2026-11-16. Źródło: NFR-28, NFR-57, ADR-0004.

## Kontekst i problem

Aplikacja Android nie ma żadnego logowania, więc po błędzie sieci albo uszkodzonym katalogu budynków nie zostaje ślad w logcat. Serwer loguje w kilku klasach, ale nie ma identyfikatora żądania, a samo żądanie nie zostawia linii, więc linii logu nie da się powiązać z żądaniem.

Logi mają pomagać w rozwoju i diagnozie, a nie zalewać: mało linii, każda mówi, co się stało. Dane osób zbierających bariery są chronione (NFR-25…NFR-30), a aplikacja nie używa zewnętrznych narzędzi analitycznych ani reklamowych (NFR-28).

## Rozważone opcje

- Aplikacja: `android.util.Log` bezpośrednio albo Timber.
- Aplikacja: raporty awarii w osobnej usłudze.
- Serwer: bez identyfikatora żądania, własny identyfikator w kodzie każdego kontrolera albo filtr żądań z MDC.
- Serwer: linia na każde żądanie na jednym poziomie albo poziom zależny od wyniku.

## Decyzja

**Aplikacja** używa Timber (licencja Apache-2.0).

- Wersja debug zapisuje wszystko od poziomu DEBUG.
- Wersja release zapisuje tylko WARN i ERROR do logcat.
- Aplikacja nie wysyła logów nigdzie poza telefon.

Timber jest wyjątkiem od kryterium ADR-0004 „wydanie z ostatnich 12 miesięcy”:

- ostatnie wydanie jest z sierpnia 2021,
- biblioteka jest mała, stabilna i bez znanych podatności,
- własna nakładka na `android.util.Log` byłaby własnym rozwiązaniem problemu ogólnego, które ADR-0004 wyklucza,
- ocena jest w [ocenach zależności](../oceny-zaleznosci.md#timber-501).

**Serwer** używa SLF4J z Logback ze Spring Boot.

- Filtr żądań działa przed łańcuchem zabezpieczeń, więc odpowiedzi 401, 403 i 429 też mają identyfikator.
- Identyfikator pochodzi z nagłówka `X-Request-Id`, jeśli jest poprawny: do 64 znaków, litery, cyfry, `-` i `_`. W przeciwnym razie filtr tworzy nowy.
- Identyfikator trafia do MDC pod kluczem `requestId`, do wzorca linii logu i do nagłówka `X-Request-Id` odpowiedzi.
- Poziom pakietu ustawia `logging.level` w `application.yaml`. Zmienna `DK_LOG_LEVEL` zmienia go bez przebudowy.

Logujemy tylko granice i zdarzenia. Pętli, klatek i obsługi ruchu kamery nie logujemy.

| Gdzie | Zdarzenie | Poziom |
| --- | --- | --- |
| aplikacja | start z wersją | INFO |
| aplikacja | wynik żądania sieciowego: metoda, ścieżka ze znacznikiem `{id}`, kod albo klasa błędu, czas | DEBUG; błąd sieci i kod 5xx WARN; kod 4xx INFO |
| aplikacja | źródło i rozmiar katalogu budynków, wynik przebiegu wysyłki zgłoszeń | INFO |
| aplikacja | wejście na ekran: nazwa ekranu bez argumentów | DEBUG |
| aplikacja | złapany wyjątek w ViewModelu albo repozytorium: klasa wyjątku bez komunikatu | WARN |
| serwer | jedna linia na koniec żądania: metoda, wzorzec ścieżki, status, czas | DEBUG; kod 4xx INFO; kod 5xx i żądanie od 1 s WARN |
| serwer | błąd 5xx ze stosem, raz, w obsłudze błędów | ERROR |
| serwer | import danych: liczba rekordów | INFO |
| serwer | operacje na magazynie zdjęć: wynik bez nazwy pliku osoby | WARN przy błędzie |

Start serwera z wersją i profilem zapisuje sam Spring Boot. Osobnej linii nie dodajemy.

Nigdy nie logujemy:

- identyfikatorów kont, nazw, e-maili, tokenów i nagłówka `Authorization`,
- dokładnych współrzędnych osoby,
- treści zgłoszeń oraz nazw i treści zdjęć,
- parametrów zapytania,
- komunikatów wyjątków, bo mogą zawierać wartości z żądania.

Ścieżka w logu to wzorzec, np. `/submissions/{id}`, a nie adres z identyfikatorem.

Raportów awarii w osobnej usłudze nie dodajemy. Narzędzie zewnętrzne łamie NFR-28. Rozwiązanie bez usługi chmurowej wymaga osobnej decyzji właściciela.

## Konsekwencje

- Dobrze: błąd w telefonie i na serwerze zostawia jedną linię z powodem.
- Dobrze: żądanie znajdziemy w logu serwera po identyfikatorze z nagłówka odpowiedzi.
- Dobrze: wersja release nie zapisuje nic poniżej WARN, więc nie ujawnia przebiegu użycia.
- Dobrze: domyślnie serwer nie zapisuje linii na każde udane żądanie.
- Gorzej: nowa zależność Timber w modułach, które logują, bez wydania od 2021 roku.
- Gorzej: wersja release nie pokazuje przebiegu przed błędem. Trzeba uruchomić wersję debug.

## Źródła

1. [ADR-0004: Gotowe reguły i biblioteki](0004-gotowe-reguly-i-biblioteki.md)
2. [Timber](https://github.com/JakeWharton/timber), licencja Apache-2.0
3. [Wymagania niefunkcjonalne: RODO](../../wymagania/niefunkcjonalne.md#rodo)
4. [Oceny zależności: Timber 5.0.1](../oceny-zaleznosci.md#timber-501)
