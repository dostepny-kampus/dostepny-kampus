# Mapy ścieżki użytkownika

!!! warning "Hipoteza, do potwierdzenia w wywiadach"
    Etapy i odczucia wynikają z person [PE-1 i PE-2](persony.md), a nie z obserwacji. Po wywiadach poprawimy oceny i ryzyka.

Mapa ścieżki pokazuje, co persona robi, myśli i czuje na każdym etapie zadania. Kolumna „Dziś” podaje problem z [procesu AS-IS](../biznes/procesy/as-is.md), kolumna „Z aplikacją” — ekran i historyjkę, które go rozwiązują.

Skala odczuć: 1 — bardzo źle, 2 — źle, 3 — obojętnie, 4 — dobrze, 5 — bardzo dobrze. Każdy etap ma dwie oceny: dziś i z aplikacją. Ryzyko dla osoby starszej: **wysokie**, średnie, niskie.

## J1 — planuję wizytę (PE-1)

Pani Halina idzie na wykład w C-13, w którym nigdy nie była. Cel: dotrzeć bez schodów i bez proszenia o pomoc.

| Etap | Czynność | Myśli i odczucia (dziś → z aplikacją) | Dziś | Z aplikacją | Ryzyko |
| --- | --- | --- | --- | --- | --- |
| J1-1 | dowiaduje się o wykładzie otwartym | „Chętnie pójdę, ale nie znam budynku” (4 → 4) | — | — | niskie |
| J1-2 | instaluje i pierwszy raz otwiera aplikację | „Czy dam radę? Nie chcę nic zepsuć” (— → 3) | — | V5: instrukcja, którą można pominąć i otworzyć ponownie (US5.1) | średnie |
| J1-3 | szuka C-13 | „Gdzie to w ogóle jest?” (2 → 4) | P-2 | V1: wyszukiwanie po symbolu w każdej pisowni, bez logowania (US1.1, US1.2); lista zamiast gestów na mapie (NFR-19) | średnie |
| J1-4 | sprawdza, którym wejściem wejdzie bez schodów | „Którędy mam wejść?” (2 → 4) | P-1, P-9 | V3 „Zaplanuj wizytę” w ≤ 3 dotknięciach (US3.2); V2: oznaczenie każdego wejścia tekstem i ikoną (US2.1) | **wysokie** |
| J1-5 | ocenia, czy informacja jest aktualna | „Czy to jeszcze prawda?” (2 → 4) | P-3, P-4 | V2: data i źródło oznaczenia wejścia (US17.3), zdjęcie i data weryfikacji bariery (US2.2) | średnie |
| J1-6 | sprawdza windę i toaletę na piętrze wykładu | „Czy winda działa?” (1 → 3) | P-5 | V2: przełącznik pięter i plan piętra z windami i toaletami (US16.1) | **wysokie** |
| J1-7 | dojeżdża i szuka wskazanego wejścia | „Czy to na pewno tu?” (2 → 3) | P-9 | V2: wejście na mapie i zdjęcie; nawigacja po trasie jest poza zakresem | średnie |
| J1-8 | trafia na przeszkodę, np. nieczynną windę | „I co teraz? Komu to powiedzieć?” (1 → 2) | P-5, P-6 | V4: zdjęcie, grupa barier, wysłanie (US4.1, US4.3); bez sieci zgłoszenie czeka w telefonie (US4.2) | **wysokie** |
| J1-9 | widzi potwierdzenie po wysłaniu | „Dobrze, ktoś to sprawdzi” (2 → 4) | P-6 | V4: potwierdzenie, że zgłoszenie trafi do weryfikacji (US4.4) | niskie |

### Gdzie osoba starsza najłatwiej zrezygnuje

| Etap | Ryzyko | Co wymaga od ekranu |
| --- | --- | --- |
| J1-4 | wejście ma oznaczenie „brak danych” albo budynek nie ma wejścia dostępnego; persona nie wie, co zrobić | zdanie wprost zamiast pustego wyniku (US3.1); słowa użytkownika zamiast żargonu |
| J1-6 | mały plan piętra i przełącznik pięter wymagają precyzji; aplikacja nie zna awarii, której nikt nie zgłosił | cele dotyku ≥ 48 dp (NFR-17), powiększona czcionka bez ucinania (NFR-20); zgłoszona bariera przy windzie ma zdjęcie i datę weryfikacji (US2.2) |
| J1-8 | stres przed budynkiem, laska w jednej ręce, słońce na ekranie; przed zdjęciem prośba o logowanie (US14.2) | duże przyciski grup barier (US4.3), brak limitu czasu (NFR-23); logowanie to najsłabszy punkt ścieżki |

Etap J1-8 ma najniższą ocenę także z aplikacją. Logowanie chroni dane przed spamem (BR-2), ale dla PE-1 jest dodatkowym krokiem w najgorszym momencie. Pytania 4.6 i 4.8 scenariusza sprawdzą, czy persona w ogóle zgłosi barierę.

## J2 — obchód budynku (PE-2)

Kuba obchodzi budynek szczegółowy A-1 piętro po piętrze. Cel: zapisać każdą barierę dokładnie i nie stracić niczego bez sieci.

| Etap | Czynność | Myśli i odczucia (dziś → z aplikacją) | Dziś | Z aplikacją | Skutek dla osoby starszej |
| --- | --- | --- | --- | --- | --- |
| J2-1 | loguje się kontem z rolą z zaproszenia i otwiera kartę A-1 | „Mam dziś dwa piętra do zrobienia” (3 → 4) | — | logowanie (US14, US12.1); V2 karta budynku | — |
| J2-2 | sprawdza oznaczenie każdego wejścia i robi nowe zdjęcie | „Czy to wejście nadal jest bez progu?” (2 → 4) | P-4, P-9 | V2 i ekran zmiany oznaczenia wejścia (US17.2) | **wysokie**: błędne oznaczenie prowadzi PE-1 do złego wejścia (J1-4, J1-7) |
| J2-3 | uśrednia pozycję przy barierze, poprawia punkt przy ścianie | „Pozycja skacze o kilka metrów” (2 → 3) | P-9 | C1: 30 s uśredniania, podpowiedź budynku, korekta ręczna na mapie (US6.1, US6.2, US6.4) | średnie: zły punkt utrudnia znalezienie bariery na planie |
| J2-4 | wybiera typ bariery, uciążliwość i piętro, dodaje zdjęcie | „Szybciej, ręka mi drętwieje” (2 → 3) | P-1 | C2: obowiązkowy typ, uciążliwość i zdjęcie (US7.1, US7.2) | niskie: zdjęcie buduje zaufanie PE-1 (N2) |
| J2-5 | zapisuje zgłoszenia w piwnicy bez zasięgu | „Czy to się nie zgubi?” (1 → 4) | — | C4: wskaźnik braku sieci, status „lokalne” (US9.1) | — |
| J2-6 | oznacza piętro jako obejrzane | „Jedno piętro mniej” (— → 4) | — | C1, oznaczenie piętra; ekran ustali faza B (US15.1, US15.3) | niskie: postęp pokazuje budynki z pełnymi danymi |
| J2-7 | na karcie widzi barierę już usuniętą | „Ta pochylnia jest już naprawiona” (2 → 3) | P-3, P-7 | V2 → zmiana „bariera usunięta” (US8.4); punkt startu ustali faza B | średnie: do decyzji Weryfikatora PE-1 widzi nieaktualną barierę |
| J2-8 | po powrocie sieci zgłoszenia wysyłają się same; przy konflikcie wybiera wersję | „Która wersja jest dobra?” (1 → 3) | — | C4: synchronizacja bez działania (US9.2), wybór wersji (US9.3) | — |
| J2-9 | sprawdza statusy, poprawia odrzucone zgłoszenie | „Co mam poprawić?” (2 → 4) | P-6 | C3: status i powód odrzucenia (US8.1, US8.3) | — |

Największe ryzyko dla osoby starszej w J2 to etap J2-2. Oznaczenie wejścia z obchodu jest dla PE-1 podstawą decyzji, którędy wejść. Dlatego zmianę oznaczenia sprawdza inny Weryfikator (US17.2, BR-4).

## Źródła

1. S. Gibbons, „Journey Mapping 101”, NN/g: <https://www.nngroup.com/articles/journey-mapping-101/>

Dostęp do źródeł: 27.10.2026.
