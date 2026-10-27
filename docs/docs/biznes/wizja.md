# Wizja produktu

„Dostępny Kampus” to aplikacja mobilna i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr). Bariery zaznacza się na miejscu, weryfikuje i publikuje na mapie.

## Problem

| Fakt | Źródło |
| --- | --- |
| Podmiot publiczny musi zapewnić m.in. przestrzenie komunikacyjne bez barier, dostęp do wszystkich pomieszczeń i informację o ich rozkładzie (art. 6 pkt 1 ustawy) | [1], [2] |
| Co 4 lata, do 31 marca, podmiot publiczny przekazuje raport o stanie zapewniania dostępności i publikuje go w BIP (art. 11 ust. 1) | [1], [2] |
| PWr złożyła taki raport. Stan na 1.01.2021: 94 budynki, bez barier komunikacyjnych 42, dostęp do wszystkich pomieszczeń 38 | [3] |
| Serwis dostepnosc.pwr.edu.pl publikuje deklaracje dostępności 67 budynków, każdą na osobnej podstronie | [4] |
| Deklaracja to tekst z nagłówkami, bez mapy i zdjęć. Przykład: deklarację C-13 zaktualizowano w marcu 2024 | [5] |

Dane o dostępności istnieją, ale są rozproszone po kilkudziesięciu stronach tekstu. Nie ma mapy, która pokaże dostępne wejście do konkretnego budynku. Osoba starsza albo osoba na wózku nie sprawdzi tego wygodnie na telefonie przed wizytą (hipoteza N1).

Według naszego rozpoznania inwentaryzację prowadzi się obchodem z papierowym formularzem, a wynik trafia do tekstu deklaracji (do potwierdzenia w wywiadach). Taki tryb utrudnia częste aktualizacje.

## Cel projektu

> Chcemy rozwiązać problem rozproszonej informacji o barierach na kampusie PWr dotyczący grupy osób starszych i z ograniczoną mobilnością po to, aby planowały wizytę bez przeszkód.

Druga strona to uczelnia. Dostaje sprawdzone dane o barierach w formacie GIS, przydatne do deklaracji i raportu o stanie dostępności.

## Interesariusze

Role w systemie opisuje strona [Role i uprawnienia](role.md). Potrzeby N1…N9 opisuje strona [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md).

| Interesariusz | Rola w systemie | Czego potrzebuje | Co zyskuje |
| --- | --- | --- | --- |
| Osoby starsze odwiedzające kampus, np. słuchacze Politechniki Trzeciego Wieku (od 2025 r., ok. 200 osób w wieku 60+, biuro w C-13) [6], [7] | Odwiedzający | N1, N2, N3, N4 | przed wyjściem z domu wie, którym wejściem wejdzie do budynku |
| Osoby z ograniczoną mobilnością, goście uczelni | Odwiedzający | N1, N2, N3 | informację o wejściach, windach i barierach w jednym miejscu |
| Student lub pracownik uczelni prowadzący obchód | Zbierający | N5, N6, N7 | dokładne zaznaczenie bariery na miejscu, także bez zasięgu sieci |
| Pracownik sprawdzający zgłoszenia | Weryfikator | N7, N8 | kolejkę zgłoszeń z podpowiedzią możliwych duplikatów |
| Kierownik inwentaryzacji | Koordynator | N9 | postęp w każdym budynku i eksport danych do QGIS |
| Dział Dostępności PWr [8] | brak (tylko interesariusz) | aktualne dane o barierach (hipoteza) | materiał do deklaracji i raportu; na tym etapie bez kontaktu z działem |

## Mierzalne cele

| ID | Cel | Miara i próg | Potrzeba |
| --- | --- | --- | --- |
| M-1 | Budynki szczegółowe zinwentaryzowane | 5 z 5 budynków (A-1, B-1, C-13, D-1, D-2) obejrzanych na miejscu; każde wejście z danych PWr ma status „dostępne / niedostępne” | N1, N9 |
| M-2 | Dokładna pozycja bariery | po 30 s uśredniania błąd < 1 m względem punktów kontrolnych, na ≥ 10 punktach na zewnątrz budynków | N5 |
| M-3 | Aplikacja wygodna dla grupy docelowej | wynik SUS ≥ 70 w badaniu z ≥ 5 użytkownikami z grupy docelowej | N4 |
| M-4 | Szybkie znalezienie dostępnego wejścia | ≥ 4 z 5 uczestników znajduje dostępne wejście w ≤ 3 dotknięciach od mapy (US3) | N1 |
| M-5 | Dane gotowe dla GIS | plik GeoJSON otwiera się w QGIS bez błędów; CSV ma współrzędne w układzie PUWG 1992 (US13) | N9 |
| M-6 | Praca bez sieci bez utraty danych | 0 utraconych wpisów: wpisy zapisane bez sieci trafiają na serwer po jej powrocie (US9) | N6, N7 |
| M-7 | Publikowane są tylko sprawdzone bariery | 100 % barier widocznych dla Odwiedzającego ma status „zaakceptowana” (US1) | N2, N8 |

## Zakres

Nie budujemy gotowego systemu dla całego kampusu. Pięć budynków opracowujemy szczegółowo, a reszta działa w trybie podstawowym i nigdy nie przestaje działać.

| Poziom | Gdzie | Co działa |
| --- | --- | --- |
| 1. Szczegółowy | A-1, B-1, C-13, D-1, D-2 | plan piętra z wyborem kondygnacji; windy, schody, toalety i wejścia z danych PWr; bariera jako punkt na planie; obchód i sprawdzenie na miejscu |
| 2. Podstawowy | pozostałe budynki PWr | obrys budynku, wejścia z OpenStreetMap, karta budynku z barierami; numer piętra wpisywany ręcznie |
| 3. Poza budynkami | teren kampusu i okolica | bariera przypięta do punktu na ulicy, np. krawężnik, pochylnia, chodnik |

Zachowanie na poziomach 2 i 3 opisują reguły BR-G1…BR-G4 na stronie [Reguły biznesowe](reguly.md). Lista budynków szczegółowych to ustawienie, więc kolejny budynek można dodać bez zmian w aplikacji.

### Poza zakresem

- nawigacja piesza po trasie, także wewnątrz budynków;
- wersja na iOS i publikacja w Google Play;
- funkcje oparte na sztucznej inteligencji i uczeniu maszynowym;
- czat i powiadomienia push.

## Źródła

1. Ustawa z dnia 19 lipca 2019 r. o zapewnianiu dostępności osobom ze szczególnymi potrzebami, Dz.U. 2019 poz. 1696, tekst jednolity Dz.U. 2024 poz. 1411 — metryka aktu: <https://api.sejm.gov.pl/eli/acts/DU/2019/1696>
2. Tekst ogłoszony ustawy (art. 3, 6, 11): <https://eli.gov.pl/eli/DU/2019/1696/ogl/pol/pdf>
3. Raport PWr o stanie zapewniania dostępności, stan na 1.01.2021: <https://bip.pwr.edu.pl/fcp/uGBUKOQtTKlQhbx08SlkTVxZeUTgtCgg9ACFDC0RFT3lBG1gnBVcoFW8SBDRKHg/4/public/bip/raport_29032021_wersja_dostepna.pdf>
4. Deklaracje dostępności budynków PWr: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow>
5. Deklaracja dostępności budynku C-13: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow/budynki-c/c13>
6. „Seniorzy na PWr. Ruszyła Politechnika Trzeciego Wieku”: <https://pwr.edu.pl/uczelnia/aktualnosci/seniorzy-na-pwr-ruszyla-politechnika-trzeciego-wieku-13912.html>
7. Politechnika Trzeciego Wieku, kontakt: <https://ptw.pwr.edu.pl/kontakt>
8. Dostępność na PWr, Dział Dostępności: <https://dostepnosc.pwr.edu.pl/>

Dostęp do źródeł: 27.10.2026.
