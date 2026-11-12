# QFD: potrzeby a funkcje

Strona to uproszczony dom jakości (QFD): potrzeby N1–N9 z wagami w wierszach, historyjki US1–US18 w kolumnach. Ważność techniczna historyjki to suma iloczynów wagi potrzeby i siły związku. Z ważności wynika priorytet MoSCoW historyjki na stronie [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md). Regułę podaje sekcja „Od ważności do priorytetu”.

Wersja uproszczona nie ma „dachu” (zależności między funkcjami) ani oceny konkurencji.

## Wagi potrzeb

Skala 1–5, gdzie 5 to potrzeba najważniejsza. Wagi ustala zespół (hipoteza, do potwierdzenia w wywiadach), a grupą docelową kursu UX są osoby starsze.

Reguła wag: potrzeba grupy docelowej (RO-GOS, RO-ODW) ma wagę co najmniej 4, a potrzeba ról zespołu (RO-ZBI, RO-WER, RO-KOO) — wagę 3.

| Waga | Potrzeba | Uzasadnienie wagi |
| --- | --- | --- |
| 5 | N1 | To cel projektu z wizji: osoba starsza planuje wizytę bez przeszkód (M-1, M-4). |
| 4 | N2 | Data i zdjęcie usuwają problemy AS-IS P-1, P-3 i P-4, ale same nie wskazują wejścia. |
| 4 | N3 | Potrzeba grupy docelowej (RO-ODW). Nie 5, bo zgłoszenie Odwiedzającego uzupełnia obchód Zbierającego, a nie zastępuje go. |
| 5 | N4 | Grupa docelowa kursu UX to osoby starsze, stąd NFR-15…NFR-24, NFR-51 i cel M-3. |
| 3 | N5 | Potrzeba roli zespołu (RO-ZBI). Cel poniżej 1 m dotyczy tylko terenu otwartego (NFR-1). |
| 3 | N6 | Potrzeba roli zespołu (RO-ZBI). Utrata zgłoszenia to strata pracy w terenie, a cel M-6 wymaga 100% zgłoszeń na serwerze. |
| 3 | N7 | Potrzeba ról zespołu (RO-ZBI, RO-WER). Konflikt wymaga równoczesnej edycji (hipoteza: rzadko), ale grozi utratą zmiany (NFR-9). |
| 3 | N8 | Potrzeba roli zespołu (RO-WER). Grupie docelowej służy pośrednio: od odsiewu błędów zależy zaufanie z N2 (M-7). |
| 3 | N9 | Potrzeba roli zespołu (RO-KOO). Eksport służy uczelni, nie grupie docelowej kursu UX. |

## Siła związku

Związki N ↔ US pochodzą z tabeli historyjek i z [macierzy śledzenia](../wymagania/sledzenie.md). Siłę ustalamy jedną regułą:

- **9** — kryterium akceptacji historyjki sprawdza potrzebę wprost;
- **3** — historyjka wspiera potrzebę, ale jej kryteria nie sprawdzają jej wprost, albo historyjka jest umożliwiająca (US14);
- **1** — związek spoza tabeli historyjek: wymaganie dostępności NFR-15…NFR-24, NFR-51 wymienia N4 i ścieżkę historyjki Gościa niezalogowanego lub Odwiedzającego.

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
| 3 | US10 | N7 | US10.4: zgłoszenie wzięte znika z kolejki pozostałych Weryfikatorów. To zapobiega równoczesnej edycji, ale żadne kryterium nie sprawdza nadpisania. |
| 9 | US10 | N8 | US10.1 i US10.2: podobne zgłoszenia w promieniu 10 m i odrzucenie z powodem. |
| 9 | US11 | N9 | US11.1: postęp każdego budynku. |
| 3 | US12 | N9 | Zaproszenie buduje zespół, ale nie pokazuje postępu ani nie eksportuje danych. |
| 9 | US13 | N9 | US13.1 i US13.2: pliki otwierają się w QGIS. |
| 3 | US14 | N2 | Historyjka umożliwiająca: o publikacji decyduje osoba z nadaną rolą (BR-2). |
| 3 | US14 | N8 | Historyjka umożliwiająca: konto chroni dane przed spamem. |
| 9 | US15 | N6 | US15.3: znacznik bez sieci czeka w telefonie i wysyła się sam jak zgłoszenie w US9.1 i US9.2. |
| 9 | US15 | N9 | US15.1: znacznik piętra obejrzanego jest podstawą postępu z US11.1. |
| 3 | US16 | N1 | Plan piętra pokazuje windy i toalety w środku, a nie wybór wejścia z zewnątrz. |
| 9 | US17 | N1 | US17.1: oznaczenie każdego wejścia. |
| 9 | US17 | N2 | US17.3: data i źródło oznaczenia. |
| 3 | US18 | N1 | Wyszukanie sali podaje piętro i drogę do sali w środku, a nie wybór wejścia z zewnątrz. |

Związki z siłą 1 dotyczą tylko N4. Inne NFR łączące potrzebę z historyjką (NFR-5, NFR-41, NFR-50 i ścieżki US6–US9 w NFR-23) nie dotyczą dostępności albo historyjek Gościa i Odwiedzającego. Pozostałych 26 związków to dokładnie powiązania z macierzy śledzenia, bez dopisków.

## Macierz

Puste pole oznacza brak związku. W wierszu „Priorytet” M to „Musi być”, P to „Powinno być”.

| Potrzeba | Waga | US1 | US2 | US3 | US4 | US5 | US6 | US7 | US8 | US9 | US10 | US11 | US12 | US13 | US14 | US15 | US16 | US17 | US18 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Wejście bez barier (N1) | 5 | 3 | 9 | 9 | | | | | | | | | | | | | 3 | 9 | 3 |
| Data i zdjęcie (N2) | 4 | | 9 | | | | | | | | 3 | | | | 3 | | | 9 | |
| Łatwe zgłoszenie (N3) | 4 | | | | 9 | | | | | | | | | | | | | | |
| Wzrok i dłonie (N4) | 5 | 1 | 1 | 1 | 9 | 3 | | | | | | | | | | | | | |
| Miejsce poniżej 1 m (N5) | 3 | | | | | | 9 | 3 | | | | | | | | | | | |
| Praca bez sieci (N6) | 3 | | | | | | | | 3 | 9 | | | | | | 9 | | | |
| Bez nadpisania (N7) | 3 | | | | | | | | | 9 | 3 | | | | | | | | |
| Odsiew błędów (N8) | 3 | | | | | | | | | | 9 | | | | 3 | | | | |
| Postęp i QGIS (N9) | 3 | | | | | | | | | | | 9 | 3 | 9 | | 9 | | | |
| **Ważność techniczna** | | **20** | **86** | **50** | **81** | **15** | **27** | **9** | **9** | **54** | **48** | **27** | **9** | **27** | **21** | **54** | **15** | **81** | **15** |
| Miejsce | | 12 | 1 | 6 | 2 | 13 | 8 | 16 | 16 | 4 | 7 | 8 | 16 | 8 | 11 | 4 | 13 | 2 | 13 |
| Priorytet | | M | M | M | M | P | M | M | M | M | M | M | P | M | M | M | P | M | P |

Przykład dla US2: 5 × 9 + 4 × 9 + 5 × 1 = 86. Suma ważności wszystkich historyjek wynosi 648.

## Od ważności do priorytetu

Priorytet historyjki wynika z jej ważności. Zadajemy po kolei dwa pytania:

1. **Czy historyjka wprost zaspokaja potrzebę?** Tak, gdy ma ważność co najmniej 27. To najniższy wynik historyjki, która ma choć jeden związek o sile 9. Taka historyjka dostaje „Musi być”.
2. **Gdy ważność jest niższa niż 27: czy bez niej nie działa historyjka „Musi być”?** Jeśli tak, dostaje „Musi być” jako warunek, a tabela wskazuje historyjkę, której służy. Jeśli nie, dostaje „Powinno być”.

| Miejsce | Historyjka | Ważność | Udział | Priorytet | Powód |
| --- | --- | --- | --- | --- | --- |
| 1 | US2 | 86 | 13,3% | Musi być | wprost zaspokaja N1 i N2 |
| 2 | US4 | 81 | 12,5% | Musi być | wprost zaspokaja N3 i N4 |
| 2 | US17 | 81 | 12,5% | Musi być | wprost zaspokaja N1 i N2 |
| 4 | US9 | 54 | 8,3% | Musi być | wprost zaspokaja N6 i N7 |
| 4 | US15 | 54 | 8,3% | Musi być | wprost zaspokaja N6 i N9 |
| 6 | US3 | 50 | 7,7% | Musi być | wprost zaspokaja N1 |
| 7 | US10 | 48 | 7,4% | Musi być | wprost zaspokaja N8 |
| 8 | US6 | 27 | 4,2% | Musi być | wprost zaspokaja N5 |
| 8 | US11 | 27 | 4,2% | Musi być | wprost zaspokaja N9 |
| 8 | US13 | 27 | 4,2% | Musi być | wprost zaspokaja N9 |
| 11 | US14 | 21 | 3,2% | Musi być | warunek: bez konta nie ma zgłoszenia szybkiego (US4) ani czynności ról pracowniczych (US6–US13) |
| 12 | US1 | 20 | 3,1% | Musi być | warunek: bez wyszukania nie ma ścieżki do karty budynku (US2) |
| 13 | US5 | 15 | 2,3% | Powinno być | wspiera N4, żadna historyjka „Musi być” od niej nie zależy |
| 13 | US16 | 15 | 2,3% | Powinno być | wspiera N1, żadna historyjka „Musi być” od niej nie zależy |
| 13 | US18 | 15 | 2,3% | Powinno być | wspiera N1, żadna historyjka „Musi być” od niej nie zależy |
| 16 | US7 | 9 | 1,4% | Musi być | warunek: bez formularza pomiaru z US6 nie da się zapisać jako zgłoszenia |
| 16 | US8 | 9 | 1,4% | Musi być | warunek: status mówi Zbierającemu, co dotarło (US9) i co poprawić po odrzuceniu (US10) |
| 16 | US12 | 9 | 1,4% | Powinno być | wspiera N9; do czasu K2 role nadaje administrator usługi logowania (BR-2) |

Udział to ważność podzielona przez 648, zaokrąglona do 0,1 punktu procentowego. Równe wyniki dzielą miejsce.

## Wnioski

1. **Najważniejsze są karta budynku (US2), oznaczenie wejścia (US17) i zgłoszenie szybkie (US4).** Razem dają 38% sumy ważności. To rdzeń wartości dla grupy docelowej.
2. **US3 „Którędy wejść” ma ważność 50 i priorytet „Musi być”.** Wcześniej miała „Powinno być”, choć stała wyżej niż 8 historyjek „Musi być”. Cel M-4 mierzy US3.2, a N1 ma najwyższą wagę.
3. **„Musi być” ma 14 z 18 historyjek.** Pierwsza wersja musi zamknąć cały obieg: zebrać dane, zweryfikować je i pokazać. Ważność ustala kolejność prac w tej grupie i mówi, co odpada najpierw, gdy zabraknie czasu: historyjki z końca rankingu.
4. **US14, US1, US7 i US8 mają niski wynik, bo QFD liczy tylko wkład bezpośredni.** Każda jest warunkiem historyjki „Musi być”, którą wskazuje tabela, więc sama też musi być.
5. **US8 może mieć niedoszacowany związek z N2.** Zbierający zaczyna zmianę „bariera usunięta” w US8.4, a po jej przyjęciu bariera znika z mapy (US10.6), co tekst pod kryteriami wiąże z N2. Macierz liczy N2 tylko po stronie Weryfikatora (US10), bo tabela historyjek nie wiąże US8 z N2.
6. **N4 ma wagę 5, a w macierzy tylko jeden silny związek (US4).** Potrzebę N4 realizują głównie wymagania przekrojowe NFR-15…NFR-24, NFR-51, obecne na każdym ekranie. Dlatego wymagania ekranów i makiety sprawdzają dostępność na każdym ekranie, a nie tylko w historyjkach z N4.
7. **Wynik zależy od wag pod kurs UX.** US6, US11 i US13 są w środku rankingu, bo potrzeby zespołu N5–N9 mają wagę 3. Dla kursu Mobilne, gdzie pomiar GNSS i eksport są rdzeniem, te wagi byłyby wyższe.

## Źródła

- „Quality Function Deployment (QFD): The House of Quality”, Fabrico — skala 9/3/1 i ważność techniczna jako suma iloczynów wagi i siły związku: <https://www.fabrico.io/blog/qfd/>
- Priorytety MoSCoW (Agile Business Consortium): <https://www.agilebusiness.org/dsdm-project-framework/moscow-prioritisation.html>

Dostęp do źródeł: 27.10.2026.
