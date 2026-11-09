# Raport „Sprint 4”: Dostępny Kampus

| Pole | Wartość |
| --- | --- |
| Projekt | Dostępny Kampus — aplikacja mobilna i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr) |
| Kurs | Mobilne systemy informatyczne – Projekt (W04IST-SM0302G) [1] |
| Prowadzący | dr inż. Zbigniew Telec |
| Autor | Myroslav Hryniuka |
| Członek zespołu | Vladyslav Yevkov |
| Członek zespołu | Yevhenii Lukianenko |
| Data | 09.11.2026 |
| Zakres raportu | Sprint 4 według harmonogramu kursu: logika dziedziny; praca od 05.11 (po raporcie Sprintu 3) do 09.11.2026 |

Raport podaje skrót i odsyła do stron dokumentacji projektu. Liczby pochodzą z historii PR i ze stanu gałęzi `sprint/3` i `sprint/4` w dniu raportu. Numery w nawiasach, takie jak (#320), to numery PR w repozytorium `github.com/dostepny-kampus/dostepny-kampus`.

## 1. Cel sprintu i wynik

Cel pochodzi z planu sprintów w raporcie [Tydzień 2](tydzien-2.md), sekcja „Plan sprintów” (wiersz S4), i z planu na końcu [Sprintu 3](sprint-3.md), sekcja 9.

| Obszar | Cel Sprintu 4 | Wynik | Stan |
| --- | --- | --- | --- |
| Pozycja | uśrednianie pozycji przez 30 s z widoczną dokładnością | średnia ważona odczytów GNSS z promieniem ufności (#321, #322) | zrobione |
| Ekran pomiaru | liczba satelitów dla każdego systemu GNSS i mapa nieba | ekran C1 z odliczaniem, satelitami według systemów i mapą nieba (#323–#326, #330, #335) | zrobione |
| Podpowiedź budynku | budynek z konturu albo najbliższy w progu 50 m | podpowiedź i wybór innego budynku po pomiarze (#352–#355) | zrobione |
| Współrzędne | przeliczenie punktu na PUWG 1992 (EPSG:2180) | przeliczenie WGS84 → PUWG 1992 w serwerze (#320) | zrobione |
| Formularz zgłoszenia | treść zgłoszenia i zdjęcie z telefonu | C2 z typem bariery TB-1…TB-29, uciążliwością, piętrem i zdjęciem z aparatu (#343–#351, #360–#363, #367–#369, #372); V4 w trzech krokach (#370, #371, #373–#377) | zrobione |
| Podobne zgłoszenia | lista zgłoszeń w promieniu 10 m dla Weryfikatora | `GET /submissions/{submissionId}/similar` od najbliższego, z odległością (#366) | zrobione w serwerze |
| Automatyczna wysyłka | wywołanie kolejki z formularza zgłoszenia | zapis z C2 i V4 trafia do kolejki, a zapis planuje wysyłkę po powrocie sieci; ponowienie tylko przy braku sieci (#342, #347, #359, #374) | zrobione |

Najważniejszy wynik: w kodzie aplikacji są ekrany pomiaru C1, formularza C2 i zgłoszenia szybkiego V4 oraz zapis do kolejki w telefonie. W aplikacji nie da się ich jeszcze użyć, bo nie ma logowania ani ról (stan w sekcji 1.1). Serwer przelicza punkt na PUWG 1992 i podaje podobne zgłoszenia, ale eksportu i ekranu Weryfikatora jeszcze nie ma.

### 1.1. Stan w aplikacji (dopisek z 09.11.2026)

Kolumna „Stan” w tabeli wyżej mówi, czy kod jest scalony. Ta tabela mówi, czy osoba może tego użyć w aplikacji. „Dostępne w aplikacji” piszemy tylko wtedy, gdy tak jest. Inaczej piszemy „w kodzie, niedostępne” z powodem.

| Obszar | Stan faktyczny | Odwołanie |
| --- | --- | --- |
| Pozycja | w kodzie, niedostępne: pomiar C1 wymaga roli Zbierającego, a aplikacja daje tylko rolę Gościa | US14.1 |
| Ekran pomiaru | w kodzie, niedostępne: ten sam powód co przy pozycji | US14.1 |
| Podpowiedź budynku | w kodzie, niedostępne: działa tylko po pomiarze C1 | US6.2 |
| Współrzędne | częściowo: przeliczenie na PUWG 1992 jest, eksportu nie ma, więc nic go nie używa | US13.2 |
| Formularz zgłoszenia | w kodzie, niedostępne: C2 wymaga roli Zbierającego. V4 wymaga logowania, a Gość z „Zgłoś barierę” trafia na zaślepkę logowania L1 | US14.1, US14.2 |
| Podobne zgłoszenia | w serwerze: aplikacja nie ma jeszcze ekranu Weryfikatora | US10.1 |
| Automatyczna wysyłka | częściowo: aplikacja nie prosi o ponowne logowanie po odpowiedzi 401. Odmowa serwera 4xx zostaje w kolejce bez statusu „Nie wysłano” | BR-26, US8.1 |
| Piętra obejrzane | nie zrobione: oznaczania pięter obejrzanych (ekran C5) nie ma w kodzie | US15 |

## 2. Repozytorium i CI

Przepływy CI zostały bez zmian względem Sprintu 3: `pr-guard`, `straznik-jakosci`, `api`, `backend`, `android` z testami na emulatorze API 26 i API 36 oraz `docs`. Próg pokrycia to nadal 80% linii i 70% gałęzi.

Prace ze Sprintu 4 trafiały do `sprint/3` do 8 listopada. Potem nowe PR trafiają do gałęzi `sprint/4`, odgałęzionej od `sprint/3`. W dniu raportu `sprint/3` czeka na scalenie do `main` przez PR z przeglądem (#365).

## 3. Serwer

| Element | Stan na koniec sprintu | Odwołanie |
| --- | --- | --- |
| PUWG 1992 | przeliczenie punktu WGS84 na EPSG:2180 do eksportu dla QGIS | #320, US13.2, NFR-43 |
| Podobne zgłoszenia | zgłoszenia wysłane, w weryfikacji i przyjęte w promieniu 10 m, z każdego piętra, od najbliższego | #366, US10.1, BR-13 |

Przeliczenia na PUWG 1992 nie używa jeszcze żaden eksport, a podobnych zgłoszeń żaden ekran aplikacji (sekcja 1.1).

## 4. Aplikacja

| Ekran albo część | Co działa | Odwołanie |
| --- | --- | --- |
| C1 Pomiar | zgoda na dokładną lokalizację, 30 s uśredniania, satelity według systemów, mapa nieba, podpowiedź i wybór budynku | #321–#328, #330, #335, #352–#355 |
| C2 Opisz barierę | miejsce z pomiaru i piętro, w budynku szczegółowym z planu PWr. Typ bariery, uciążliwość, zdjęcie z aparatu i opis. Braki po „Zapisz”, pytanie przy wyjściu bez zapisu i poprawka z C3 bez duplikatu | #343–#351, #360–#363, #367, #368, #372 |
| Zdjęcia | zdjęcie z aparatu zmniejszone do 2048 px i obrócone według EXIF przed zapisem w kolejce | #369 |
| V4 Zgłoszenie szybkie | trzy kroki: zdjęcie, grupa barier z piętrem według BR-14 i wysłanie z bieżącą pozycją. „Wstecz” nie gubi zdjęcia ani grupy, a rezygnacja ze zdjęciem wymaga potwierdzenia | #370, #371, #373–#377 |
| Mapa | „Pokaż wejście na mapie” i kamera na budynku; geometria mapy liczona poza wątkiem interfejsu | #327, #329, #331–#334, #336, #339–#341 |
| Kolejka zgłoszeń | wysyłka rozróżnia awarię serwera 5xx i wygasłe logowanie 401; zadanie ponawia tylko przy braku sieci | #342, #359 |

Ekrany C1, C2 i V4 są w kodzie, ale bez logowania i ról osoba do nich nie dojdzie (sekcja 1.1).

## 5. UX w Sprincie 4

| Zmiana | Odwołanie |
| --- | --- |
| akceptacja makiet hi-fi C2 i C3 | #345 |
| romb ze znakiem zapytania dla braku danych w słowniku | #337 |
| budynek podstawowy B-8 i piętro po zmianie miejsca w scenariuszach | #357 |
| status „Nie wysłano” i odpowiedzi serwera przy wysyłce z kolejki w wymaganiach | #358 |

## 6. Praca z Git

| Miara | Wartość |
| --- | --- |
| PR scalone w sprincie | 56 (#319–#378), wszystkie metodą squash: 43 do `sprint/3`, 13 do `sprint/4` |
| Okres | 05.11–09.11.2026: 21, 7, 15, 2 i 11 PR w kolejnych dniach |
| PR według typu commitu | `feat` 39, `docs` 6, `test` 6, `fix` 5 |
| Autorzy PR | 3 konta: Myroslav Hryniuka 28, Vladyslav Yevkov 14, Yevhenii Lukianenko 14 |
| Scalenie do `main` | `sprint/3` czeka na przegląd (#365) |

Każdy PR miał jedno zadanie i najwyżej 300 zmienionych linii według `rozmiar-pr.sh`. Formularz C2 i V4 dzieliliśmy na model, model widoku i ekran, np. V4 na treść zgłoszenia, krok 2, krok 3, model widoku i ekran (#370, #371, #373–#376).

## 7. Zmiany względem planu i ryzyka

| Zmiana albo problem | Skutek | Odwołanie |
| --- | --- | --- |
| Podobne zgłoszenia tylko w serwerze | lista dla Weryfikatora w aplikacji przechodzi do Sprintu 5 razem z weryfikacją | US10.1 |
| Brak ekranu C3 | po zapisie z C2 aplikacja wraca do ekranu sprzed pomiaru, a nie do listy zgłoszeń | US8 |
| Brak telefonu do testów GNSS | uśrednianie i mapę nieba sprawdzają testy jednostkowe i emulator, nie teren | R-3, R-10 |
| Scalenie `sprint/3` do `main` czeka na przegląd | praca idzie dalej w `sprint/4`, odgałęzionej od `sprint/3` | #365 |
| Zależność od jednej osoby maleje | największe konto wnosi 28 z 56 PR, czyli 50% | R-2 |

Pełny rejestr z ocenami i sygnałami jest na stronie [Ryzyka](../biznes/ryzyka.md).

## 8. Plan Sprintu 5

Według harmonogramu kursu Sprint 5 obejmuje logowanie, role i pracę bez sieci. Do tego dochodzą zadania przeniesione ze Sprintu 4.

| Zadanie | Wynik | Odwołanie |
| --- | --- | --- |
| Logowanie i role | usługa logowania, role i zaproszenia; logowanie w aplikacji | US14, US14.1 |
| Kolejka offline | stan wysyłki zgłoszeń widoczny dla Zbierającego | NFR-5, NFR-6 |
| Konflikt wersji | ekran wyboru wersji przy konflikcie 409 | NFR-9 |
| Weryfikacja | ekran Weryfikatora z listą podobnych zgłoszeń z serwera | US10.1 |
| Ekran C3 | lista własnych zgłoszeń ze statusem i poprawką przed decyzją Weryfikatora | US8 |
| Oznaczenie wejścia | zmiana oznaczenia wejścia | US17 |

## Źródła

1. Programy studiów WIT PWr, karta przedmiotu W04IST-SM0302G: <https://wit.pwr.edu.pl/studenci/programy-studiow/2024-2025-studia-ii-stopnia>

Dostęp do źródeł: 09.11.2026.
