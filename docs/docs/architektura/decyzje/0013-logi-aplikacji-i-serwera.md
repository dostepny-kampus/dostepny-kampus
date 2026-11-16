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

**Aplikacja** używa Timber (licencja Apache-2.0). Wersja debug zapisuje wszystko od poziomu DEBUG, wersja release tylko WARN i ERROR do logcat. Aplikacja nie wysyła logów nigdzie poza telefon.

Timber 5.0.1 jest wyjątkiem od kryterium ADR-0004 „wydanie z ostatnich 12 miesięcy”: ostatnie wydanie pochodzi z sierpnia 2021. Biblioteka jest mała i stabilna, nie ma znanych podatności ani nowszej wersji, a własna nakładka na `android.util.Log` byłaby własnym rozwiązaniem problemu ogólnego, które ADR-0004 wyklucza. Ocena jest w [ocenach zależności](../oceny-zaleznosci.md#timber-501).

**Serwer** używa SLF4J z Logback ze Spring Boot. Filtr żądań działa przed łańcuchem zabezpieczeń, więc odpowiedzi 401, 403 i 429 też mają identyfikator. Filtr bierze identyfikator z nagłówka `X-Request-Id`, jeśli jest poprawny (do 64 znaków: litery, cyfry, `-`, `_`), a w przeciwnym razie tworzy nowy. Identyfikator trafia do MDC pod kluczem `requestId`, do wzorca linii logu i do nagłówka `X-Request-Id` odpowiedzi. Poziom pakietu ustawia `logging.level` w `application.yaml`, a zmienna `DK_LOG_LEVEL` zmienia go bez przebudowy.

Co logujemy: tylko granice i zdarzenia, nigdy pętle, klatki ani obsługę ruchu kamery.

| Gdzie | Zdarzenie | Poziom |
| --- | --- | --- |
| aplikacja | start z wersją | INFO |
| aplikacja | wynik żądania sieciowego: metoda, ścieżka ze znacznikiem `{id}`, kod albo klasa błędu, czas | DEBUG, błąd sieci i 5xx WARN, 4xx INFO |
| aplikacja | źródło i rozmiar katalogu budynków, wynik przebiegu wysyłki zgłoszeń | INFO |
| aplikacja | wejście na ekran: nazwa ekranu bez argumentów | DEBUG |
| aplikacja | złapany wyjątek w ViewModelu albo repozytorium: klasa wyjątku bez komunikatu | WARN |
| serwer | jedna linia na koniec żądania: metoda, wzorzec ścieżki, status, czas | 5xx i wolne (≥ 1 s) WARN, 4xx INFO, reszta DEBUG |
| serwer | błąd 5xx ze stosem, raz, w obsłudze błędów | ERROR |
| serwer | import danych: liczba rekordów | INFO |
| serwer | operacje na magazynie zdjęć: wynik bez nazwy pliku osoby | WARN przy błędzie |

Start serwera z wersją i profilem zapisuje sam Spring Boot, więc osobnej linii nie dodajemy.

Czego nie logujemy nigdy: identyfikatorów kont, nazw, e-maili, tokenów i nagłówka `Authorization`, dokładnych współrzędnych osoby, treści zgłoszeń, nazw i treści zdjęć, parametrów zapytania i komunikatów wyjątków, które mogą zawierać wartości z żądania. Ścieżka w logu to wzorzec (`/submissions/{id}`), nie adres z identyfikatorem.

Raportów awarii w osobnej usłudze nie dodajemy. Narzędzie zewnętrzne łamie NFR-28, a rozwiązanie bez usługi chmurowej wymaga osobnej decyzji właściciela.

## Konsekwencje

- Dobrze: błąd w telefonie i na serwerze zostawia jedną linię z powodem; żądanie z linii logu serwera da się znaleźć po identyfikatorze, który klient widzi w nagłówku odpowiedzi.
- Dobrze: wersja release nie zapisuje w logcat nic poniżej WARN, więc nie ujawnia przebiegu użycia.
- Dobrze: logi żądań są na poziomie DEBUG, więc domyślnie serwer nie zapisuje linii na każde udane żądanie.
- Gorzej: nowa zależność Timber w modułach, które logują, bez wydania od 2021 roku.
- Gorzej: wersja release nie pokazuje przebiegu przed błędem; żeby go zobaczyć, trzeba uruchomić wersję debug.

## Źródła

1. [ADR-0004: Gotowe reguły i biblioteki](0004-gotowe-reguly-i-biblioteki.md)
2. [Timber](https://github.com/JakeWharton/timber), licencja Apache-2.0
3. [Wymagania niefunkcjonalne: RODO](../../wymagania/niefunkcjonalne.md#rodo)
4. [Oceny zależności: Timber 5.0.1](../oceny-zaleznosci.md#timber-501)
