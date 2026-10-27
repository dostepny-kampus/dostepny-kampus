# QFD: potrzeby a funkcje

Strona to uproszczony dom jakości (QFD): potrzeby N1–N9 z wagami w wierszach, historyjki US1–US17 w kolumnach. Ważność techniczna historyjki to suma iloczynów wagi potrzeby i siły związku. Ranking porównujemy z priorytetami MoSCoW ze strony [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md). Wersja uproszczona nie ma „dachu” (zależności między funkcjami) ani oceny konkurencji.

## Wagi potrzeb

Skala 1–5, gdzie 5 to potrzeba najważniejsza. Wagi ustala zespół (hipoteza, do potwierdzenia w wywiadach), a grupą docelową kursu UX są osoby starsze.

| Waga | Potrzeba | Uzasadnienie wagi |
| --- | --- | --- |
| 5 | N1 | To cel projektu z wizji: osoba starsza planuje wizytę bez przeszkód (M-1, M-4). |
| 4 | N2 | Data i zdjęcie usuwają problemy AS-IS P-1, P-3 i P-4, ale same nie wskazują wejścia. |
| 3 | N3 | Zgłoszenie Odwiedzającego uzupełnia obchód Zbierającego, a nie zastępuje go. |
| 5 | N4 | Grupa docelowa kursu UX to osoby starsze, stąd NFR-15…NFR-24 i cel M-3. |
| 3 | N5 | Potrzeba jednej roli (RO-ZBI), a cel poniżej 1 m dotyczy tylko terenu otwartego (NFR-1). |
| 4 | N6 | Utrata zgłoszenia to strata pracy w terenie, a cel M-6 wymaga 100% zgłoszeń na serwerze. |
| 3 | N7 | Konflikt wymaga równoczesnej edycji tego samego zgłoszenia (hipoteza: rzadko), ale grozi utratą zmiany (NFR-9). |
| 4 | N8 | Od odsiewu błędów zależy zaufanie z N2: publikujemy tylko bariery przyjęte (M-7). |
| 3 | N9 | Potrzeba jednej osoby (RO-KOO), a eksport służy uczelni, nie grupie docelowej kursu UX. |

## Siła związku

Związki N ↔ US pochodzą z tabeli historyjek i z [macierzy śledzenia](../wymagania/sledzenie.md). Siłę ustalamy jedną regułą:

- **9** — kryterium akceptacji historyjki sprawdza potrzebę wprost;
- **3** — historyjka wspiera potrzebę, ale jej kryteria nie sprawdzają jej wprost, albo historyjka jest umożliwiająca (US14);
- **1** — związek spoza tabeli historyjek, wynikający z wymagania niefunkcjonalnego.

| Siła | Historyjka | Potrzeba | Uzasadnienie |
| --- | --- | --- | --- |
| 3 | US1 | N1 | Wyszukanie wskazuje budynek, a wejście pokazują dopiero US2 i US3. US1 to pierwszy krok ścieżki z US3.2. |
| 1 | US1 | N4 | Dodane: NFR-18 wymaga przejścia ścieżek US1–US5 z TalkBack, a NFR-19 listy zamiast samej mapy. |
| 9 | US2 | N1 | US2.1: każde wejście ma oznaczenie tekstem i ikoną. |
| 9 | US2 | N2 | US2.2: zgłoszenie przyjęte ma zdjęcie i datę weryfikacji. |
| 1 | US2 | N4 | Dodane: ścieżka z TalkBack według NFR-18. |
| 9 | US3 | N1 | US3.1 i US3.2: wejście dostępne albo zdanie, że go nie ma, w 3 dotknięciach. |
| 1 | US3 | N4 | Dodane: ścieżka z TalkBack według NFR-18. |
| 9 | US4 | N3 | US4.1: zgłoszenie w najwyżej 3 krokach. |
| 9 | US4 | N4 | US4.3: duże przyciski grup barier (NFR-17). |
| 3 | US5 | N4 | Instrukcja pomaga się nie zgubić, ale US5.1 nie dotyczy wzroku ani dłoni. |
| 9 | US6 | N5 | US6.1: uśrednianie pozycji przez 30 s z widoczną dokładnością. |
| 3 | US7 | N5 | Formularz opisuje barierę. Z miejscem wiąże go tylko piętro (US7.2). |
| 3 | US8 | N6 | Status „lokalne” i „wysłane” pokazuje, czy zgłoszenie dotarło, ale przed utratą chroni US9. |
| 9 | US9 | N6 | US9.1 i US9.2: kolejka bez sieci i samoczynna synchronizacja. |
| 9 | US9 | N7 | US9.3: konflikt pokazuje obie wersje, nic nie znika po cichu. |
| 3 | US10 | N2 | Weryfikacja jest podstawą zaufania (M-7), ale Odwiedzający widzi ją przez US2.2. |
| 3 | US10 | N7 | US10.4: zgłoszenie wzięte znika z kolejki pozostałych Weryfikatorów; to jedno z sześciu kryteriów. |
| 9 | US10 | N8 | US10.1 i US10.2: podobne zgłoszenia w promieniu 10 m i odrzucenie z powodem. |
| 9 | US11 | N9 | US11.1: postęp każdego budynku. |
| 3 | US12 | N9 | Zaproszenie buduje zespół, ale nie pokazuje postępu ani nie eksportuje danych. |
| 9 | US13 | N9 | US13.1 i US13.2: pliki otwierają się w QGIS. |
| 3 | US14 | N2 | Historyjka umożliwiająca: o publikacji decyduje osoba z nadaną rolą (BR-2). |
| 3 | US14 | N8 | Historyjka umożliwiająca: konto chroni dane przed spamem. |
| 9 | US15 | N6 | US15.3: znacznik bez sieci czeka w telefonie i wysyła się sam, jak zgłoszenie w US9.1 i US9.2. |
| 9 | US15 | N9 | US15.1: znacznik piętra obejrzanego jest podstawą postępu z US11.1. |
| 3 | US16 | N1 | Plan piętra pokazuje windy i toalety w środku, a nie wybór wejścia z zewnątrz. |
| 9 | US17 | N1 | US17.1: oznaczenie każdego wejścia. |
| 9 | US17 | N2 | US17.3: data i źródło oznaczenia. |

Związki z siłą 1 dotyczą tylko N4. Pozostałych 25 związków to dokładnie powiązania z macierzy śledzenia, bez dopisków.

## Macierz

Puste pole oznacza brak związku. W wierszu „Priorytet” M to „Musi być”, P to „Powinno być”.

| Potrzeba | Waga | US1 | US2 | US3 | US4 | US5 | US6 | US7 | US8 | US9 | US10 | US11 | US12 | US13 | US14 | US15 | US16 | US17 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Wejście bez barier (N1) | 5 | 3 | 9 | 9 | | | | | | | | | | | | | 3 | 9 |
| Data i zdjęcie (N2) | 4 | | 9 | | | | | | | | 3 | | | | 3 | | | 9 |
| Łatwe zgłoszenie (N3) | 3 | | | | 9 | | | | | | | | | | | | | |
| Wzrok i dłonie (N4) | 5 | 1 | 1 | 1 | 9 | 3 | | | | | | | | | | | | |
| Miejsce poniżej 1 m (N5) | 3 | | | | | | 9 | 3 | | | | | | | | | | |
| Praca bez sieci (N6) | 4 | | | | | | | | 3 | 9 | | | | | | 9 | | |
| Bez nadpisania (N7) | 3 | | | | | | | | | 9 | 3 | | | | | | | |
| Odsiew błędów (N8) | 4 | | | | | | | | | | 9 | | | | 3 | | | |
| Postęp i QGIS (N9) | 3 | | | | | | | | | | | 9 | 3 | 9 | | 9 | | |
| **Ważność techniczna** | | **20** | **86** | **50** | **72** | **15** | **27** | **9** | **12** | **63** | **57** | **27** | **9** | **27** | **24** | **63** | **15** | **81** |
| Miejsce | | 12 | 1 | 7 | 3 | 13 | 8 | 16 | 15 | 4 | 6 | 8 | 16 | 8 | 11 | 4 | 13 | 2 |
| Priorytet | | M | M | P | M | P | M | M | M | M | M | M | P | M | M | M | P | M |

Przykład dla US2: 5 × 9 + 4 × 9 + 5 × 1 = 86. Suma ważności wszystkich historyjek wynosi 657.

## Ranking a priorytety

| Miejsce | Historyjka | Ważność | Udział | Priorytet | Zgodność rankingu z priorytetem |
| --- | --- | --- | --- | --- | --- |
| 1 | US2 | 86 | 13,1% | Musi być | zgodny |
| 2 | US17 | 81 | 12,3% | Musi być | zgodny |
| 3 | US4 | 72 | 11,0% | Musi być | zgodny |
| 4 | US9 | 63 | 9,6% | Musi być | zgodny |
| 4 | US15 | 63 | 9,6% | Musi być | zgodny |
| 6 | US10 | 57 | 8,7% | Musi być | zgodny |
| 7 | US3 | 50 | 7,6% | Powinno być | **rozbieżność**: wyżej niż 7 historyjek „Musi być” |
| 8 | US6 | 27 | 4,1% | Musi być | zgodny (waga N5 zależy od kursu) |
| 8 | US11 | 27 | 4,1% | Musi być | zgodny (waga N9 zależy od kursu) |
| 8 | US13 | 27 | 4,1% | Musi być | zgodny (waga N9 zależy od kursu) |
| 11 | US14 | 24 | 3,7% | Musi być | historyjka umożliwiająca |
| 12 | US1 | 20 | 3,0% | Musi być | **rozbieżność**: poniżej US3 |
| 13 | US5 | 15 | 2,3% | Powinno być | zgodny |
| 13 | US16 | 15 | 2,3% | Powinno być | zgodny |
| 15 | US8 | 12 | 1,8% | Musi być | **rozbieżność**: przedostatnie miejsce |
| 16 | US7 | 9 | 1,4% | Musi być | **rozbieżność**: ostatnie miejsce |
| 16 | US12 | 9 | 1,4% | Powinno być | zgodny |

Udział to ważność podzielona przez 657, zaokrąglona do 0,1 punktu procentowego. Równe wyniki dzielą miejsce.

## Wnioski

1. **US3 „Zaplanuj wizytę” jest na 7. miejscu, choć ma priorytet „Powinno być”.** Cel M-4 mierzy US3.2, a N1 ma najwyższą wagę. Propozycja do decyzji właściciela projektu: utrzymać priorytet, jeśli karta budynku (US2, US17) pokaże wejście dostępne w 3 dotknięciach od mapy. W przeciwnym razie podnieść US3 do „Musi być”.
2. **US7, US8 i US1 mają niski wynik, bo QFD liczy tylko wkład bezpośredni.** Bez formularza (US7) nie ma danych o barierach, a bez wyszukiwania (US1) nie ma ścieżki do karty budynku. Priorytet MoSCoW uwzględnia zależności i scenariusz pokazu, więc go nie zmieniamy. QFD nie zastępuje priorytetów, tylko pokazuje, skąd bierze się wartość dla użytkownika.
3. **US8 może mieć niedoszacowany związek z N2.** Zmiana „bariera usunięta” (US8.4) usuwa z mapy naprawioną barierę, a tekst pod kryteriami wiąże to z N2. Tabela historyjek nie wiąże jednak US8 z N2, więc macierz tego nie liczy. Propozycja: rozważyć dopisanie N2 do US8 na stronie historyjek (poza zakresem tej strony).
4. **N4 ma wagę 5, a w macierzy tylko jeden silny związek (US4).** Potrzebę N4 realizują głównie wymagania przekrojowe NFR-15…NFR-24, obecne na każdym ekranie. Dlatego wymagania ekranów i makiety fazy B sprawdzają dostępność na każdym ekranie, a nie tylko w historyjkach z N4.
5. **Wynik zależy od wag pod kurs UX.** US6, US11 i US13 są w środku rankingu, bo N5 i N9 mają wagę 3. Dla kursu Mobilne, gdzie pomiar GNSS i eksport są rdzeniem, te wagi byłyby wyższe. Ranking warto przeliczyć po wywiadach, gdy wagi N1–N4 przestaną być hipotezą (R-7).

## Źródła

- „Quality Function Deployment (QFD): The House of Quality”, Fabrico — skala 9/3/1 i ważność techniczna jako suma iloczynów wagi i siły związku: <https://www.fabrico.io/blog/qfd/>
- Priorytety MoSCoW (Agile Business Consortium): <https://www.agilebusiness.org/dsdm-project-framework/moscow-prioritisation.html>

Dostęp do źródeł: 27.10.2026.
