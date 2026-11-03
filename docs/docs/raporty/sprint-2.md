# Raport „Sprint 2”: Dostępny Kampus

| Pole | Wartość |
| --- | --- |
| Projekt | Dostępny Kampus — aplikacja mobilna i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr) |
| Kurs | Mobilne systemy informatyczne – Projekt (W04IST-SM0302G) [1] |
| Prowadzący | dr inż. Zbigniew Telec |
| Autor | Myroslav Hryniuka |
| Członek zespołu | Vladyslav Yevkov |
| Członek zespołu | Yevhenii Lukianenko |
| Data | 03.11.2026 |
| Zakres raportu | Sprint 2 według harmonogramu kursu: interfejs użytkownika, nawigacja i model danych; praca od 28.10 do 31.10.2026 |

Raport podaje skrót i odsyła do stron dokumentacji projektu. Liczby pochodzą z historii PR i ze stanu gałęzi `sprint/2` w dniu scalenia do `main`. Numery w nawiasach, takie jak (#58), to numery PR w repozytorium `github.com/dostepny-kampus/dostepny-kampus`.

## 1. Cel sprintu i wynik

Cel pochodzi z planu sprintów w raporcie [Tydzień 2](tydzien-2.md), sekcja 4. Doszły zadania przeniesione ze Sprintu 1 ([Sprint 1](sprint-1.md), sekcja 7).

| Obszar | Cel Sprintu 2 | Wynik | Stan |
| --- | --- | --- | --- |
| Repozytorium kodu | jedno repozytorium z `api/`, `backend/`, `android/`, `data/`, `specs/` i `docs/` | szkielet monorepo (#1), scenariusze w katalogu głównym, [ADR-0002](../architektura/decyzje/0002-monorepo.md) | zrobione |
| CI i kontrola PR | rozmiar zmiany, format commitów, sekrety, lintery | sześć przepływów GitHub Actions, sekcja 2 | zrobione |
| Kontrakt API | pierwsza wersja kontraktu OpenAPI | kontrakt v0: 21 ścieżek w pięciu częściach (#12–#16) | zrobione |
| Szkielety serwera i aplikacji | projekty z linterami, uruchamiane w CI | serwer (#10, #17–#19) i aplikacja (#20–#22) | zrobione |
| Model i geometria | model dziedziny z geometrią | `core:model` i `core:geo` w aplikacji (#23, #59, #60); tabela budynków z PostGIS na serwerze (#18) | zrobione |
| System projektowy | motyw z tokenów wizualnych | motyw generowany z pliku tokenów i komponenty ze zrzutami (#30–#32, #34, #54) | zrobione |
| Nawigacja z rolami | ekrany i przejścia według mapy nawigacji, pasek dolny | trasy, nawigator historii „Wstecz”, pasek dolny i z boku, dostawca ról (#55, #58, #63, #68, #70, #72) | zrobione |
| Mapa z konturami | V1 z konturami budynków, V2 karta budynku | V1 z mapą MapLibre, listą i wyszukiwaniem; V2 jako arkusz nad mapą (#71–#82, #85, #93, #94) | zrobione; treść V2 w Sprincie 3 |
| Baza lokalna | baza w aplikacji do pracy bez sieci | moduł `core:database` bez kodu | przeniesione do Sprintu 3 |
| Dane budynków | eksport konturów z OSM; import budynków z wejściami do bazy | plik GeoJSON: 95 budynków i 72 wejścia (#8, #11), plik w aplikacji (#67); tabela na serwerze pusta | częściowo |
| Wywiady | rozmowy według scenariusza | brak zapisu wywiadów w dokumentacji | nie zrobione |

Najważniejszy wynik: aplikacja uruchamia się z mapą kampusu bez internetu. Odwiedzający znajduje budynek po symbolu albo na liście i otwiera jego kartę. Serwer i aplikacja przechodzą wszystkie kontrole w CI.

## 2. Repozytorium i CI

Kontrole uruchamiają się według zmienionych katalogów, więc zmiana w aplikacji nie buduje serwera.

| Przepływ | Co sprawdza |
| --- | --- |
| `pr-guard` | rozmiar PR do 300 linii, commitlint dla commitów i tytułu PR, zakazane słowa, sekrety (gitleaks), nagłówek autorski w plikach z kodem |
| `straznik-jakosci` | brak wyciszeń analizy i plików baseline; zmiana reguł jakości wymaga uzasadnienia w opisie PR |
| `api` | kontrakt regułami Redocly; zmiana łamiąca zgodność (oasdiff) blokuje scalenie |
| `backend` | `./mvnw verify`: testy, qulice, Spring Modulith, ArchUnit, próg pokrycia JaCoCo |
| `android` | `./gradlew check assembleDebug`: testy, detekt, ktlint, Android Lint, Konsist, zrzuty Roborazzi, próg pokrycia Kover |
| `docs` | `verify.py`, odwołania wierszy w wymaganiach ekranów i znaczniki scenariuszy w testach Androida |

Próg pokrycia na serwerze i w aplikacji to 80% linii i 70% gałęzi (#19, #42). Tabela pokrycia scenariuszy testami (#57) pokazuje, które scenariusze z `specs/` mają test.

Gałąź `sprint/2` scaliliśmy do `main` 31.10.2026 przez PR #111 „chore: scalenie sprintu 2 do main”. Przeglądy dodali wszyscy trzej członkowie zespołu.

## 3. Kontrakt API i serwer

| Element | Stan na koniec sprintu | Odwołanie |
| --- | --- | --- |
| Kontrakt v0 | `api/openapi.yaml`: budynki, piętra, pomieszczenia, zgłoszenia z wersjami i zdjęciami, weryfikacja, synchronizacja, postęp, eksport, profil, zaproszenia | #12–#16 |
| Serwer | Spring Boot 4.1, Java 25, Maven; moduły `building`, `barrier`, `review`, `sync`, `export`, `identity` | #10, #17, [ADR-0001](../architektura/decyzje/0001-stos-technologiczny.md) |
| Baza | PostgreSQL z PostGIS w `infra/docker-compose.yml`; migracja Flyway `V1__budynki.sql` z tabelą `buildings` | #18 |
| Testy | Testcontainers z PostGIS; Spring Modulith i ArchUnit pilnują granic modułów | #17, #18 |
| Decyzje | siedem decyzji ADR-0001–ADR-0007 i diagramy C4 kontekstu i kontenerów | #7, #9, #122 |

Kontrakt powstał przed kodem serwera. W Sprincie 2 serwer nie ma jeszcze żadnego punktu końcowego. Pierwsze punkty końcowe budynków powstały na początku Sprintu 3.

## 4. Aplikacja

Aplikacja ma 14 modułów: `app`, siedem modułów `core` i sześć modułów `feature`. Na koniec sprintu w kodzie głównym jest 54 plików Kotlin, a w testach 33.

| Ekran albo część | Co działa | Odwołanie |
| --- | --- | --- |
| Motyw | kolory i wymiary generowane z `tokeny.yml`, krój Atkinson Hyperlegible Next, motyw jasny i ciemny | #30, #31, NFR-16, NFR-21 |
| Komponenty | oznaczenie wejścia, kontener grupy, nagłówek z „Wstecz”, przypięty pasek czynności, pasek dolny | #32, #34, #54 |
| Nawigacja | trasy ekranów, zapis historii „Wstecz”, pasek z boku w szerokim oknie | #58, #63, #68, #77, NFR-54 |
| V1 Mapa kampusu | mapa MapLibre z konturami z pliku w aplikacji, przyciski zamiast gestów, pole symbolu, lista budynków, linia „bez internetu” | #71–#76, #85, #88, #93, #94, US1, NFR-19 |
| V2 Karta budynku | arkusz nad żywą mapą, wybór budynku przy konturze wspólnym | #75, #80–#82, #126, US2 |
| V5 Pomoc | instrukcja w trzech krokach z obrazami | #84, #86 |
| Pierwsze uruchomienie | flaga pierwszego uruchomienia w `FirstLaunchViewModel` | #55, #121 |
| Architektura | Hilt i MVVM; reguła Konsist sprawdza ViewModel | #113, #119–#122, #135, [ADR-0007](../architektura/decyzje/0007-hilt-i-mvvm.md) |

Komponenty i oboczka z mapą V1 i arkuszem V2 mają zrzuty Roborazzi w motywie jasnym, ciemnym i przy czcionce 200% (#32, #79). Zrzuty powstają w testach na JVM. Testów na emulatorze z planu „Tydzień 2” jeszcze nie ma.

## 5. Dane budynków

| Element | Wynik | Odwołanie |
| --- | --- | --- |
| Eksport z OpenStreetMap | skrypt `data/scripts/export_buildings.py`, licencja ODbL w `data/buildings/LICENSE` | #8, R-11 |
| Plik danych | `data/buildings/pwr-buildings.geojson`: 95 budynków (93 wielokąty i 2 wielokąty złożone) i 72 wejścia | #11 |
| Aplikacja | ten sam plik w APK, katalog budynków i wyszukiwanie po symbolu w różnym zapisie, np. „C13” i „C-13” | #59, #60, #67 |
| Serwer | tabela `buildings` bez danych; import przeniesiony do Sprintu 3 | #18 |

Plik w aplikacji pozwala pokazać kontury przy pierwszym uruchomieniu bez internetu.

## 6. UX w Sprincie 2

Równolegle z kodem powstały makiety hi-fi wszystkich 19 ekranów ([Ekrany](../ux/ekrany/index.md), #50, #51, #105–#108). Strona ma 342 obrazy: 114 stanów ekranów w trzech wariantach. Kod aplikacji powstaje według tych makiet.

| Zmiana | Odwołanie |
| --- | --- |
| styl „Karta miejsca” w tokenach: kolory, krój, kształty, odstępy, ciemny motyw jako mapa nocna | #24–#29, #35 |
| reguły dużej czcionki i strona „Dostępność w aplikacji” z NFR-51 dla dużych ekranów | #36, #38 |
| nowa historyjka US18, kryteria śledzenia ekranów i kryteria ekranów C, R i K | #46, #99 |
| raport UX (PDU) i przegląd kognitywny makiet hi-fi | #62 |

## 7. Praca z Git

| Miara | Wartość |
| --- | --- |
| PR scalone do `sprint/2` | 123, wszystkie scalone metodą squash |
| Okres | 28.10–31.10.2026: 41, 9, 59 i 14 PR w kolejnych dniach |
| PR według typu commitu | `docs` 44, `feat` 42, `ci` 13, `build` 8, `fix` 6, `test` 4, `refactor` 4, `style` 1, `chore` 1 |
| Autorzy PR | 1 konto |
| Przegląd scalenia do `main` | PR #111 z przeglądami trzech członków zespołu |
| Otwarte po sprincie | PR Dependabota z aktualizacjami zależności |

Każdy PR w sprincie miał jedno zadanie i najwyżej 300 zmienionych linii. Większe zadania podzieliliśmy na części, np. kontrakt API na pięć PR (#12–#16).

## 8. Zmiany względem planu i ryzyka

| Zmiana albo problem | Skutek | Odwołanie |
| --- | --- | --- |
| Brak bazy lokalnej w aplikacji | praca bez sieci tylko z pliku konturów; Room przechodzi do Sprintu 3 | NFR-10 |
| Pusta tabela budynków na serwerze | import z pliku GeoJSON przechodzi do Sprintu 3 | US1, R-11 |
| Brak wywiadów | persony, ścieżki i napisy zostają hipotezami | R-6, R-7 |
| Brak testów na emulatorze | ekrany sprawdzają testy Compose i zrzuty Roborazzi na JVM | NFR-17 |
| Wszystkie PR z jednego konta | sygnał R-2 nadal występuje; reakcja w Sprincie 3: kolejne osoby otwierają własne PR | R-2 |
| Raport „Sprint 2” po terminie | sygnał R-1; raport oddajemy 03.11.2026 | R-1 |

Pełny rejestr z ocenami i sygnałami jest na stronie [Ryzyka](../biznes/ryzyka.md).

## 9. Plan Sprintu 3

Według harmonogramu kursu Sprint 3 obejmuje serwer i komunikację przez API. Do tego dochodzą zadania przeniesione ze Sprintu 2.

| Zadanie | Wynik | Odwołanie |
| --- | --- | --- |
| Budynki na serwerze | `GET /buildings`, `GET /buildings/locate` i `GET /buildings/{symbol}` | US1, US2, BR-G4 |
| Import budynków do bazy | kontury i wejścia z `pwr-buildings.geojson` w bazie serwera; decyzja ADR o sposobie importu | R-11, US17 |
| Zgłoszenia z wersją | zapis zgłoszeń; konflikt wersji nigdy nie nadpisuje danych po cichu | NFR-9, `specs/zgloszenie-szybkie.feature` |
| Zdjęcia | przechowywanie zdjęć zgłoszeń w MinIO | US4 |
| Klient API | klient HTTP w aplikacji; kontury z serwera, a bez sieci z pliku | NFR-11 |
| Baza lokalna | pamięć podręczna Room z datą synchronizacji | NFR-10 |
| Karta budynku | treść V2 i ekran V3 „Którędy wejść” | US2, US3 |

Na 03.11.2026 punkty końcowe budynków, klient API i ekran V3 są już w gałęzi `sprint/3` (#137–#147). Ich wynik opisze raport „Sprint 3”.

## Źródła

1. Programy studiów WIT PWr, karta przedmiotu W04IST-SM0302G: <https://wit.pwr.edu.pl/studenci/programy-studiow/2024-2025-studia-ii-stopnia>
2. Conventional Commits 1.0.0: <https://www.conventionalcommits.org/pl/v1.0.0/>

Dostęp do źródeł: 03.11.2026.
