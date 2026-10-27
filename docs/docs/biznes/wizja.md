# Wizja produktu

„Dostępny Kampus” to aplikacja mobilna i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr). Bariery zaznacza się na miejscu, weryfikuje i publikuje na mapie.

## Problem

| Fakt | Źródło |
| --- | --- |
| Podmiot publiczny musi zapewnić m.in. przestrzenie komunikacyjne bez barier, dostęp do wszystkich pomieszczeń i informację o ich rozkładzie (art. 4 ust. 1 i art. 6 pkt 1 ustawy) | [1], [2] |
| Co 4 lata, do 31 marca, podmiot publiczny przekazuje raport o stanie zapewniania dostępności i publikuje go w BIP (art. 11 ust. 1) | [1], [2] |
| PWr złożyła taki raport. Stan na 1.01.2021: 94 budynki; w 42 przestrzenie komunikacyjne bez barier, w 38 dostęp do wszystkich pomieszczeń | [3] |
| Stan na 1.01.2025, kampus główny: 44 budynki, w tym 27 z pełnym dostępem i 5 bez dostosowanego wejścia. W 13, m.in. A-1, dostosowane jest tylko wejście inne niż główne | [9] |
| Serwis dostepnosc.pwr.edu.pl publikuje deklaracje dostępności 67 budynków, każdą na osobnej podstronie | [4] |
| Deklaracja to tekst z nagłówkami, bez mapy i zdjęć. Przykład: deklarację C-13 zaktualizowano w marcu 2024 | [5] |

Dane o dostępności istnieją, ale są rozproszone i nieaktualne. PWr ma Cyfrowy Przewodnik z Interaktywną Mapą Kampusu: 25 budynków, w tym A-1, B-1, C-13, D-1 i D-2 [10], [11]. Plany pięter mapa pokazuje tylko dla wybranych budynków [11].

Deklaracje budynków nie odsyłają ani do Przewodnika, ani do mapy [4]. Dane mapy pochodzą z semestru zimowego 2022/2023, a deklaracje z marca 2024 [4], [11]. Żaden kanał nie pokazuje bieżącego stanu barier, takiego jak niesprawna winda: aktualności Przewodnika są puste [12]. Osoba na wózku nie sprawdzi więc przed wizytą, czy na trasie przez dostępne wejście do A-1 [9] wszystko działa (hipoteza N1, N2).

Plan PWr na lata 2025–2028 opisuje zbieranie danych z ankiet administratorów obiektów i z audytów eksperckich z dokumentacją fotograficzną [13]. Zdjęć nie ma w żadnej deklaracji, a 66 z 67 deklaracji ma datę „marzec 2024” [4]. Dane aktualizuje się więc rzadko. Szczegóły opisuje [Proces AS-IS](procesy/as-is.md).

## Cel projektu

> Chcemy rozwiązać problem rozproszonej informacji o barierach na kampusie PWr dotyczący grupy osób starszych i z ograniczoną mobilnością po to, aby planowały wizytę bez przeszkód.

Druga strona to uczelnia. Dostaje sprawdzone dane o barierach w formacie GIS, przydatne do deklaracji i raportu o stanie dostępności (hipoteza).

## Interesariusze

Role w systemie opisuje strona [Role i uprawnienia](role.md). Potrzeby N1…N9 opisuje strona [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md).

| Interesariusz | Rola w systemie | Czego potrzebuje | Co zyskuje |
| --- | --- | --- | --- |
| Osoby starsze odwiedzające kampus, np. słuchacze Politechniki Trzeciego Wieku (od 2025 r., ok. 200 osób w wieku 60+, biuro w C-13) [6], [7] | Odwiedzający | N1, N2, N3, N4 | przed wyjściem z domu wie, którym wejściem wejdzie do budynku |
| Osoby z ograniczoną mobilnością, goście uczelni | Odwiedzający | N1, N2, N3 | informację o wejściach, windach i barierach w jednym miejscu |
| Student lub pracownik uczelni prowadzący obchód | Zbierający | N5, N6, N7 | dokładne zaznaczenie bariery na miejscu, także bez zasięgu sieci |
| Pracownik uczelni weryfikujący zgłoszenia | Weryfikator | N7, N8 | kolejkę zgłoszeń z podpowiedzią możliwych duplikatów |
| Kierownik inwentaryzacji | Koordynator | N9 | postęp w każdym budynku i eksport danych do QGIS |
| Dział Dostępności PWr [8] | brak (tylko interesariusz) | aktualne dane o barierach (hipoteza) | materiał do deklaracji i raportu; na tym etapie bez kontaktu z działem |

## Mierzalne cele

| ID | Cel | Miara i próg | Potrzeba |
| --- | --- | --- | --- |
| M-1 | Budynki szczegółowe zinwentaryzowane | 5 z 5 budynków (A-1, B-1, C-13, D-1, D-2) obejrzanych na miejscu; każde wejście z danych PWr ma status „dostępne / niedostępne” | N1, N9 |
| M-2 | Dokładna pozycja bariery | cel w terenie otwartym: po 30 s uśredniania mediana błędu poziomego < 1 m; sprawdzamy ≥ 10 pomiarami nad punktami kontrolnymi (NFR-1). To cel, nie gwarancja | N5 |
| M-3 | Aplikacja wygodna dla grupy docelowej | średni wynik SUS ≥ 68 w badaniu z ≥ 5 użytkownikami z grupy docelowej (NFR-26) | N4 |
| M-4 | Szybkie znalezienie dostępnego wejścia | ≥ 4 z 5 uczestników samodzielnie znajduje dostępne wejście do wskazanego budynku w ≤ 3 dotknięciach od mapy (US3.2) | N1 |
| M-5 | Dane gotowe dla GIS | plik GeoJSON w WGS84 otwiera się w QGIS bez błędów; CSV ma współrzędne w układzie PUWG 1992 (US13.1, US13.2) | N9 |
| M-6 | Praca bez sieci bez utraty danych | w teście ≥ 20 zgłoszeń zapisanych bez sieci: po jej powrocie 100 % trafia na serwer bez działania użytkownika (US9.2) | N6, N7 |
| M-7 | Publikowane są tylko sprawdzone bariery | 100 % barier widocznych dla Odwiedzającego to zgłoszenia przyjęte przez Weryfikatora (US1.3) | N2, N8 |

## Zakres

Nie budujemy gotowego systemu dla całego kampusu. Pięć budynków opracowujemy szczegółowo. Pozostałe budynki i teren kampusu obsługujemy prościej, ale zgłoszenie bariery działa zawsze.

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
2. Tekst ogłoszony ustawy (art. 3, 4, 6, 11): <https://eli.gov.pl/eli/DU/2019/1696/ogl/pol/pdf>
3. Raport PWr o stanie zapewniania dostępności, stan na 1.01.2021: <https://bip.pwr.edu.pl/fcp/uGBUKOQtTKlQhbx08SlkTVxZeUTgtCgg9ACFDC0RFT3lBG1gnBVcoFW8SBDRKHg/4/public/bip/raport_29032021_wersja_dostepna.pdf>
4. Deklaracje dostępności budynków PWr: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow>
5. Deklaracja dostępności budynku C-13: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow/budynki-c/c13>
6. „Seniorzy na PWr. Ruszyła Politechnika Trzeciego Wieku”: <https://pwr.edu.pl/uczelnia/aktualnosci/seniorzy-na-pwr-ruszyla-politechnika-trzeciego-wieku-13912.html>
7. Politechnika Trzeciego Wieku, kontakt: <https://ptw.pwr.edu.pl/kontakt>
8. Dostępność na PWr, Dział Dostępności: <https://dostepnosc.pwr.edu.pl/>
9. Stan dostosowania obiektów PWr na dzień 01.01.2025, kampus główny: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/stan-dostosowania-obiektow-pwr-na-dzien-01-01-2025/kampus-glowny-pwr-na-dzien-01-01-2025>
10. Cyfrowy Przewodnik PWr, lista 25 budynków: <https://przewodnik.pwr.edu.pl/pl/budynki>
11. Interaktywna Mapa Kampusu PWr (25 budynków, dane z semestru zimowego 2022/2023, plany wybranych budynków): <https://mapakampusu.pwr.edu.pl/>
12. Cyfrowy Przewodnik PWr, Aktualności: <https://przewodnik.pwr.edu.pl/pl/aktualnosci>
13. Plan działania na rzecz poprawy dostępności osobom ze szczególnymi potrzebami na lata 2025–2028, PWr (tabela 1 poz. 2–3): <https://bip.pwr.edu.pl/d/gGBUKOTtQKxVvBFh6GDxYBxZIQX14QU8OS39DQQpVVm8QRQxpXhsJFXkSGQcBBxNZBUREenlUWx8/plan_poprawy_dostepnosci_2025-28.pdf>

Dostęp do źródeł: 27.10.2026.
