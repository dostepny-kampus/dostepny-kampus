# Raport „Sprint 3”: Dostępny Kampus

| Pole | Wartość |
| --- | --- |
| Projekt | Dostępny Kampus — aplikacja mobilna i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr) |
| Kurs | Mobilne systemy informatyczne – Projekt (W04IST-SM0302G) [1] |
| Prowadzący | dr inż. Zbigniew Telec |
| Autor | Myroslav Hryniuka |
| Członek zespołu | Vladyslav Yevkov |
| Członek zespołu | Yevhenii Lukianenko |
| Data | 05.11.2026 |
| Zakres raportu | Sprint 3 według harmonogramu kursu: serwer i komunikacja przez API; praca od 01.11 do 05.11.2026 |

Raport podaje skrót i odsyła do stron dokumentacji projektu. Liczby pochodzą z historii PR i ze stanu gałęzi `sprint/3` w dniu raportu. Numery w nawiasach, takie jak (#137), to numery PR w repozytorium `github.com/dostepny-kampus/dostepny-kampus`.

## 1. Cel sprintu i wynik

Cel pochodzi z planu sprintów w raporcie [Tydzień 2](tydzien-2.md), sekcja „Plan sprintów” (wiersz S3), i z planu na końcu [Sprintu 2](sprint-2.md), sekcja 9.

| Obszar | Cel Sprintu 3 | Wynik | Stan |
| --- | --- | --- | --- |
| Budynki na serwerze | `GET /buildings`, `GET /buildings/locate` i `GET /buildings/{symbol}` | trzy punkty końcowe czytają budynki i wejścia z bazy, nie z pliku (#137–#139, #168, #172–#173) | zrobione |
| Import budynków | kontury i wejścia z `pwr-buildings.geojson` w bazie; decyzja ADR | migracje `V1`–`V2`, import przy starcie i [ADR-0008](../architektura/decyzje/0008-import-budynkow.md) (#168, #172) | zrobione |
| Zgłoszenia z wersją | zapis zgłoszeń; konflikt wersji nigdy nie nadpisuje danych po cichu | `POST`, `GET` i `PUT /submissions`, wersja i konflikt 409 jako problem+json (#177–#290) | zrobione |
| Zdjęcia | przechowywanie zdjęć zgłoszeń w MinIO | magazyn RustFS zgodny z S3 zamiast MinIO, [ADR-0009](../architektura/decyzje/0009-magazyn-zdjec.md) (#185, #189–#190) | zrobione ze zmianą |
| Klient API | klient HTTP w aplikacji; kontury z serwera, a bez sieci z pliku | modele odpowiedzi i klient budynków (#143–#145, #147), klient zgłoszeń (#192–#199), zapas z pliku (#202, #293) | zrobione |
| Baza lokalna | pamięć podręczna Room z datą synchronizacji | tabela `building_cache` z migracją 1→2 (#260), kopia listy z datą i offline (#313); kolejka zgłoszeń w Room (#214, #225, #229) | zrobione |
| Karta budynku | treść V2 i ekran V3 „Którędy wejść” | V2 z wejściami i oznaczeniem (#141, #233, #254), V3 z odpowiedzią serwera (#142), zgłoszenie przy wejściu (#247) | zrobione |
| Automatyczna wysyłka | synchronizacja po powrocie sieci (NFR-6) | worker WorkManagera i planowanie po zapisie w `core:data` (#314), planowanie przy starcie aplikacji w `app` (#316) | zrobione |
| Testy na emulatorze | przepływ na emulatorze | nadal tylko testy JVM i zrzuty Roborazzi; emulatora nie ma | nie zrobione |

Najważniejszy wynik: aplikacja bierze kontury i wejścia z serwera, a bez sieci wraca do pliku i do kopii w bazie z datą pobrania. Zgłoszenie z telefonu przechodzi przez kolejkę lokalną do serwera, który pilnuje wersji i przy konflikcie zwraca 409, a zdjęcia leżą w magazynie RustFS. Po powrocie sieci kolejkę wysyła zadanie WorkManagera.

## 2. Repozytorium i CI

Kontrole nadal uruchamiane są według zmienionych katalogów. W Sprincie 3 doszły strażnicy zgodności i architektury.

| Przepływ | Co sprawdza | Zmiany w Sprincie 3 |
| --- | --- | --- |
| `pr-guard` | rozmiar PR do 300 linii, commitlint, zakazane słowa, sekrety, nagłówek autorski | autor z zespołu we wzorze nagłówka (#174, #176, #186, #206) |
| `straznik-jakosci` | brak wyciszeń analizy i plików baseline | pilnuje też zmian NullAway i reguł architektury (#222) |
| `api` | kontrakt regułami Redocly; oasdiff blokuje zmianę łamiącą zgodność | warianty synchronizacji jako `oneOf` (#216) |
| `backend` | `./mvnw verify`: testy, qulice, Spring Modulith, ArchUnit, próg pokrycia | qulice w osobnym zadaniu bez testów (#226), NullMarked i NullAway (#215, #255, #302) |
| `android` | `./gradlew check assembleDebug`: testy, detekt, ktlint, Lint, Konsist, zrzuty, Kover | Konsist pilnuje `null` tylko na granicy modułu (#227), pamięć podręczna zależności (#151) |
| `docs` | `verify.py`, odwołania wierszy wymagań i znaczniki scenariuszy | bez zmian |

Próg pokrycia w serwerze i w aplikacji to nadal 80% linii i 70% gałęzi. Doszły strażnicy: parytet tras i odpowiedzi HTTP z `openapi.yaml` (#235, #242, #305, #312) oraz jeden kontekst Springa dla testów integracyjnych (#265, #276, #304). Modułu zgłoszeń pilnuje ArchUnit (#217, #301).

W dniu raportu gałąź `sprint/3` czeka na scalenie do `main`. Scalaliśmy ją przez PR z przeglądem, tak jak `sprint/2` (#111).

## 3. Kontrakt API i serwer

| Element | Stan na koniec sprintu | Odwołanie |
| --- | --- | --- |
| Kontrakt | `api/openapi.yaml`: 21 ścieżek; dopisane warianty synchronizacji i opcjonalny rodzaj wejścia | #170, #216 |
| Budynki | lista, podpowiedź najbliższego budynku i karta budynku z wejściami; dane z bazy | #137–#139, #173 |
| Zgłoszenia | wysłanie bez dubli, lista i odczyt, edycja z wersją i konfliktem 409; reguły treści, statusu i szczegółów bariery | #177–#290 |
| Zdjęcia | zapis i odczyt przez S3 w RustFS, bez metadanych JPEG, limit 5 MB, kubełek tworzony przy starcie | #185, #188–#190, #264, #295 |
| Baza | PostgreSQL z PostGIS; migracje Flyway `V1` budynki, `V2` wejścia, `V3` zgłoszenia, `V4` opcjonalna nazwa budynku | #168, #172, #177 |
| Moduły | `barrier`, `building`, `export`, `identity`, `review`, `sync`; granice pilnuje Spring Modulith i ArchUnit | [ADR-0001](../architektura/decyzje/0001-stos-technologiczny.md), #217 |

Serwer w tym sprincie przestał być szkieletem: przyjmuje zgłoszenia z telefonu i oddaje gotowy katalog budynków. Weryfikacja, eksport, profil i zaproszenia mają kontrakt, ale jeszcze nie mają punktów końcowych.

## 4. Aplikacja

Aplikacja ma 14 modułów: `app`, siedem modułów `core` i sześć modułów `feature`. W kodzie głównym jest 82 pliki Kotlin, a w testach 80.

| Ekran albo część | Co działa | Odwołanie |
| --- | --- | --- |
| Budynki z serwera | klient HTTP budynków, modele odpowiedzi i katalog z serwera z zapasem z pliku i z kopii w bazie | #143–#145, #147, #202, #293, #313 |
| V2 Karta budynku | wejścia z serwera, dzień i źródło oznaczenia, linia „Bez internetu”, zgłoszenie bariery przy wejściu | #141, #159, #233, #247, #254, #274 |
| V3 Którędy wejść | odpowiedź serwera z wejściami, przejście „Pokaż wejście na mapie” | #142, #306 |
| Karta w nowym modelu | mała karta nad mapą i pełny ekran bez przypiętego pasa, gest wstecz, duża czcionka, TalkBack | #157, #162, #175, #183, #223, #236 |
| Kolejka zgłoszeń | DAO z postępem wysyłki, mapowanie na kontrakt, klient zgłoszeń i zdjęć, wysyłka od najstarszego | #193, #199, #225, #229, #248 |
| Baza lokalna | Room z migracją 1→2, kopia listy budynków z datą pobrania i praca bez sieci | #214, #260, #313 |
| Automatyczna wysyłka | worker WorkManagera z warunkiem sieci i ponowieniem; planowanie po zapisie zgłoszenia i przy starcie aplikacji | #314, #316 |
| Architektura | Hilt i MVVM; Konsist pilnuje `null` tylko na granicy modułu | #227, [ADR-0007](../architektura/decyzje/0007-hilt-i-mvvm.md) |

Zrzuty Roborazzi nadal powstają w testach na JVM, w motywie jasnym, ciemnym i przy czcionce 200%. Testów na emulatorze z planu „Tydzień 2” nie ma.

## 5. Dane budynków

| Element | Wynik | Odwołanie |
| --- | --- | --- |
| Import do bazy | ten sam plik `pwr-buildings.geojson` trafia do tabel budynków i wejść; decyzja o sposobie importu w ADR | #168, #172, [ADR-0008](../architektura/decyzje/0008-import-budynkow.md) |
| Odczyt przez API | karta budynku i podpowiedź biorą wejścia z bazy, z dniem i źródłem oznaczenia | #139, #173 |
| Podpowiedź budynku | próg 50 m od konturu jako reguła biznesowa BR-G4 | #148 |

Plik w aplikacji zostaje jako zapas przy pierwszym uruchomieniu bez internetu i przy braku odpowiedzi serwera.

## 6. UX w Sprincie 3

Makiety zmieniły się razem z kodem karty budynku i listy.

| Zmiana | Odwołanie |
| --- | --- |
| mała karta budynku nad mapą i pełny ekran bez mapy | #157, #236 |
| lista budynków bez przycisku przy pozycji i V3 z „Pokaż wejście na mapie” | #234 |
| mapa V1 w granicach kampusu z zapasem 500 m | #146 |

## 7. Praca z Git

| Miara | Wartość |
| --- | --- |
| PR scalone do `sprint/3` | 166, wszystkie scalone metodą squash |
| Okres | 01.11–05.11.2026: 19, 38, 87 i 22 PR w kolejnych dniach |
| PR według typu commitu | `feat` 66, `test` 54, `fix` 17, `refactor` 9, `docs` 8, `ci` 4, `build` 4, `chore` 3, `revert` 1 |
| Autorzy PR | 3 konta: Vladyslav Yevkov 122, Myroslav Hryniuka 43, Yevhenii Lukianenko 1 |
| Scal do `main` | w dniu raportu czeka na PR |
| Otwarte po sprincie | PR #309 z refaktorem systemu projektowego |

Każdy PR w sprincie miał jedno zadanie i najwyżej 300 zmienionych linii według `rozmiar-pr.sh`. Większe zadania dzieliliśmy na części, np. kolejkę zgłoszeń na DAO, mapowanie i wysyłkę (#225, #229, #248).

## 8. Zmiany względem planu i ryzyka

| Zmiana albo problem | Skutek | Odwołanie |
| --- | --- | --- |
| MinIO zastąpione magazynem RustFS | magazyn zdjęć działa lokalnie i w CI; różnica względem planu jest w ADR | R-12, [ADR-0009](../architektura/decyzje/0009-magazyn-zdjec.md) |
| Brak testów na emulatorze | przepływ sprawdzają testy JVM i zrzuty Roborazzi; nadal brak urządzenia | R-3, R-10 |
| Brak wywiadów | persony, ścieżki i napisy zostają hipotezami | R-6, R-7 |
| Zależność od jednej osoby maleje | największe konto wnosi 122 z 166 PR, czyli 73% — poniżej progu 80% z sygnału R-2 | R-2 |
| Raport „Sprint 3” na czas | raport oddajemy 05.11.2026, po terminie Sprintu 2 | R-1 |

Pełny rejestr z ocenami i sygnałami jest na stronie [Ryzyka](../biznes/ryzyka.md).

## 9. Plan Sprintu 4

Według harmonogramu kursu Sprint 4 obejmuje logikę dziedziny.

| Zadanie | Wynik | Odwołanie |
| --- | --- | --- |
| Pozycja | uśrednianie pozycji przez 30 s z widoczną dokładnością | NFR-1, US6.1 |
| Ekran pomiaru | liczba satelitów dla każdego systemu GNSS i mapa nieba | US6.1, US6.5, NFR-21 |
| Współrzędne | przeliczenie punktu na PUWG 1992 (EPSG:2180) | US13.2, NFR-43 |
| Formularz zgłoszenia | treść zgłoszenia i zdjęcie z telefonu w trzech krokach | US4.1 |
| Podobne zgłoszenia | lista zgłoszeń w promieniu 10 m dla Weryfikatora | US10.1 |
| Automatyczna wysyłka | wywołanie kolejki z formularza zgłoszenia | NFR-6 |

## Źródła

1. Programy studiów WIT PWr, karta przedmiotu W04IST-SM0302G: <https://wit.pwr.edu.pl/studenci/programy-studiow/2024-2025-studia-ii-stopnia>
2. Conventional Commits 1.0.0: <https://www.conventionalcommits.org/pl/v1.0.0/>

Dostęp do źródeł: 05.11.2026.
