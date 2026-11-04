# Mapy ścieżki użytkownika

!!! info "Skąd pochodzą mapy"
    J1 poprawiliśmy po wywiadach z etapu 2 kursu PDU (6 osób w wieku 61–75 lat). J3 opiera się na analizie źródeł o osobach na wózku. J2 to nadal hipoteza: osób na obchodzie nie badaliśmy. Etapy i odczucia wynikają z [person](persony.md), a nie z obserwacji tych samych zadań.

Mapa ścieżki pokazuje, co persona robi, myśli i czuje na każdym etapie zadania. Kolumna „Dziś” podaje problem z [procesu AS-IS](../biznes/procesy/as-is.md), kolumna „Z aplikacją” — ekran i historyjkę, które go rozwiązują.

Skala odczuć: 1 — bardzo źle, 2 — źle, 3 — obojętnie, 4 — dobrze, 5 — bardzo dobrze. Każdy etap ma dwie oceny: dziś i z aplikacją. Ryzyko, że persona zrezygnuje: **wysokie**, średnie, niskie.

## J1 — planuję wizytę (PE-1)

Pani Halina idzie na wykład w A-1, w którym nigdy nie była. Wizytę sprawdza razem z córką. Cel: dotrzeć bez schodów i rzadziej prosić innych o pomoc.

Mapa pokazuje A-1, bo tam wejście główne ma schody, a dostępne jest inne wejście ([P-9](../biznes/procesy/as-is.md#problemy-as-is)).

| Etap | Czynność | Myśli i odczucia (dziś → z aplikacją) | Dziś | Z aplikacją | Ryzyko |
| --- | --- | --- | --- | --- | --- |
| J1-1 | dowiaduje się o wykładzie otwartym | „Chętnie pójdę, ale nie znam budynku” (4 → 4) | — | — | niskie |
| J1-2 | córka instaluje aplikację i razem pierwszy raz ją otwierają | „Czy to będzie proste? Czy trafię w przyciski?” (— → 3) | — | V5: instrukcja, którą można pominąć i otworzyć ponownie (US5.1) | średnie |
| J1-3 | razem szukają A-1 | „Gdzie to w ogóle jest?” (2 → 4) | P-2 | V1: wyszukiwanie po symbolu w każdej pisowni, bez logowania (US1.1, US1.2); lista zamiast gestów na mapie (NFR-19) | średnie |
| J1-4 | sprawdzają, którym wejściem wejdzie bez schodów | „Którędy mam wejść?” (2 → 4) | P-1, P-9 | V3 „Którędy wejść” w ≤ 3 dotknięciach (US3.2); V2: oznaczenie każdego wejścia tekstem i ikoną (US2.1) | **wysokie** |
| J1-5 | ocenia, czy informacja jest aktualna | „Czy to jeszcze prawda? Ostatnio piętro się nie zgadzało” (2 → 4) | P-3, P-4 | V2: data i źródło oznaczenia wejścia (US17.3), zdjęcie i data weryfikacji bariery (US2.2) | średnie |
| J1-6 | sprawdza windę, salę 329 i toaletę na piętrze wykładu | „Czy winda działa? Gdzie jest sala?” (1 → 3) | P-5 | V2: przełącznik pięter i plan piętra z windami i toaletami (US16.1); V1: sala po numerze z krokami od wejścia (US18) | **wysokie** |
| J1-7 | dojeżdża tramwajem i szuka wskazanego wejścia | „Czy to na pewno tu?” (2 → 3) | P-9 | V2: wejście na planie piętra z oznaczeniem tekstem i ikoną (US16.1, US2.1); nawigacja po trasie jest poza zakresem | średnie |
| J1-8 | w budynku szuka sali i toalety | „Już drugi raz idę tym korytarzem” (1 → 3) | — | V1: piętro sali i kroki od wejścia (US18); V2: toalety na planie piętra (US16.1) | **wysokie** |
| J1-9 | trafia na przeszkodę, np. nieczynną windę | „I co teraz? Zapytam portiera” (1 → 2) | P-5, P-6 | najczęściej pyta portiera także z aplikacją. Jeśli zgłasza: V4 zdjęcie, grupa barier, wysłanie (US4.1, US4.3); w budynku piętro albo „Nie wiem” (US4.5); bez sieci zgłoszenie czeka w telefonie (US4.2) | **wysokie** |
| J1-10 | widzi potwierdzenie po wysłaniu | „Dobrze, ktoś to sprawdzi” (2 → 4) | P-6 | V4: potwierdzenie, że zgłoszenie trafi do weryfikacji (US4.4) | niskie |

### Gdzie osoba starsza najłatwiej zrezygnuje

| Etap | Ryzyko | Co wymaga od ekranu |
| --- | --- | --- |
| J1-4 | wejście ma oznaczenie „brak danych” albo budynek nie ma wejścia dostępnego; persona nie wie, co zrobić | zdanie wprost zamiast pustego wyniku (US3.1); słowa użytkownika zamiast żargonu |
| J1-6 | mały plan piętra i przełącznik pięter wymagają precyzji; aplikacja nie zna awarii, której nikt nie zgłosił | cele dotyku ≥ 48 dp (NFR-17), powiększona czcionka bez ucinania (NFR-20); zgłoszona bariera przy windzie ma zdjęcie i datę weryfikacji (US2.2) |
| J1-8 | błądzenie po korytarzach; w wywiadach 3 z 6 osób miało kłopot z trafieniem w budynku | piętro sali i kroki od wejścia dużym tekstem (US18); toalety na planie piętra (US16.1) |
| J1-9 | stres przed budynkiem, laska w jednej ręce, słońce na ekranie; przed zdjęciem prośba o logowanie (US14.2) | duże przyciski grup barier (US4.3), brak limitu czasu (NFR-23); logowanie to najsłabszy punkt ścieżki |

Etap J1-9 ma najniższą ocenę także z aplikacją. Logowanie chroni dane przed spamem (BR-8), ale dla PE-1 jest dodatkowym krokiem w najgorszym momencie.

W wywiadach 6 z 6 osób w kłopocie pytało ludzi na miejscu, a nikt nie zgłaszał barier. Dlatego dla PE-1 ważniejsze jest, żeby awarię zgłoszoną przez innych zobaczyła jeszcze w domu (J1-5, J1-6).

## J2 — obchód budynku (PE-2)

Kuba obchodzi budynek szczegółowy A-1 piętro po piętrze. Cel: zapisać każdą barierę dokładnie i nie stracić niczego bez sieci.

| Etap | Czynność | Myśli i odczucia (dziś → z aplikacją) | Dziś | Z aplikacją | Skutek dla osoby starszej |
| --- | --- | --- | --- | --- | --- |
| J2-1 | loguje się kontem z rolą z zaproszenia i otwiera kartę A-1 | „Mam dziś dwa piętra do zrobienia” (3 → 4) | — | logowanie (US14, US12.1); V2 karta budynku | — |
| J2-2 | sprawdza oznaczenie każdego wejścia i robi nowe zdjęcie | „Czy to wejście nadal jest bez progu?” (2 → 4) | P-4, P-9 | V2 → C6: „dostępne” albo „niedostępne” i nowe zdjęcie wejścia (US17.2) | **wysokie**: błędne oznaczenie prowadzi PE-1 do złego wejścia (J1-4, J1-7) |
| J2-3 | uśrednia pozycję przy barierze, poprawia punkt przy ścianie | „Pozycja skacze o kilka metrów” (2 → 3) | P-9 | C1: 30 s uśredniania, podpowiedź budynku, korekta ręczna na mapie (US6.1, US6.2, US6.4) | średnie: zły punkt utrudnia znalezienie bariery na planie |
| J2-4 | wybiera typ bariery, uciążliwość i piętro, dodaje zdjęcie | „Szybciej, ręka mi drętwieje” (2 → 3) | P-1 | C2: obowiązkowy typ, uciążliwość i zdjęcie (US7.1, US7.2) | niskie: zdjęcie buduje zaufanie PE-1 (N2) |
| J2-5 | zapisuje zgłoszenia w piwnicy bez zasięgu | „Czy to się nie zgubi?” (1 → 4) | — | C4: wskaźnik braku sieci, status „lokalne” (US9.1) | — |
| J2-6 | oznacza piętro jako obejrzane | „Jedno piętro mniej” (— → 4) | — | V2 → C5: „Oznacz jako obejrzane”; bez sieci znacznik czeka w telefonie (US15.1, US15.3) | niskie: postęp pokazuje budynki z pełnymi danymi |
| J2-7 | na karcie widzi barierę już usuniętą | „Ta pochylnia jest już naprawiona” (2 → 3) | P-3, P-7 | V2 → C7: „Bariery już nie ma” przy zgłoszeniu przyjętym i nowe zdjęcie (US8.4) | średnie: do decyzji Weryfikatora PE-1 widzi nieaktualną barierę |
| J2-8 | po powrocie sieci zgłoszenia wysyłają się same; przy konflikcie wybiera wersję | „Która wersja jest dobra?” (1 → 3) | — | C4: synchronizacja bez działania (US9.2), wybór wersji (US9.3) | — |
| J2-9 | sprawdza statusy, poprawia odrzucone zgłoszenie | „Co mam poprawić?” (2 → 4) | P-6 | C3: status i powód odrzucenia (US8.1, US8.3) | — |

Największe ryzyko dla osoby starszej w J2 to etap J2-2. Oznaczenie wejścia z obchodu jest dla PE-1 podstawą decyzji, którędy wejść. Dlatego zmianę oznaczenia sprawdza inny Weryfikator (US17.2, BR-4).

## J3 — sprawdzam dostępność przed zajęciami (PE-4)

Tomasz ma pierwsze zajęcia w A-1 i porusza się na wózku. Cel: przed wyjazdem wiedzieć, którym wejściem wjedzie i czy dojedzie do sali. Odczucia „dziś” opierają się na źródłach [persony PE-4](persony.md#pe-4-tomasz-osoba-na-wozku-planujaca-zajecia).

| Etap | Czynność | Myśli i odczucia (dziś → z aplikacją) | Dziś | Z aplikacją | Ryzyko |
| --- | --- | --- | --- | --- | --- |
| J3-1 | dostaje plan zajęć w nieznanym budynku | „Znowu nowy budynek. Czy wjadę?” (2 → 3) | — | — | niskie |
| J3-2 | szuka A-1 i otwiera kartę budynku | „Oby to nie był znowu sam tekst” (2 → 4) | P-1, P-2 | V1, V2: skrót faktów na górze karty: wejście dostępne, winda, toaleta (US2.3) | niskie |
| J3-3 | sprawdza wejście dostępne i szerokość drzwi | „Czy to wejście na pewno jest bez progu?” (2 → 4) | P-4, P-9 | V3: wejście zalecane z dojściem, szerokością drzwi i najbliższą windą (US3.3); data i źródło oznaczenia (US17.3) | **wysokie** |
| J3-4 | sprawdza drogę od wejścia do sali | „Czy po drodze nie ma stopnia?” (1 → 3) | P-9 | V1: sala po numerze i kroki bez schodów albo zdanie, że takiej drogi nie ma (US18); plan piętra (US16.1) | **wysokie** |
| J3-5 | sprawdza, czy winda działa | „Ostatnio winda stała, a nikt nie uprzedził” (1 → 3) | P-5 | V2: sprawdzona awaria przy windzie ze zdjęciem i datą (US2.2) | **wysokie** |
| J3-6 | parkuje i szuka wskazanego wejścia | „Które to drzwi?” (2 → 4) | P-9 | V3: wejście dostępne na mapie (US3.3) | średnie |
| J3-7 | przy wejściu widzi za stromy podjazd | „Zgłoszę, żeby następny wiedział” (2 → 4) | P-6 | V4: zdjęcie, grupa barier, wysłanie (US4.1, US4.3); bez sieci zgłoszenie czeka w telefonie (US4.2) | niskie |
| J3-8 | widzi potwierdzenie po wysłaniu | „Dobrze, ktoś to sprawdzi” (2 → 4) | P-6 | V4: potwierdzenie, że zgłoszenie trafi do weryfikacji (US4.4) | niskie |

### Gdzie osoba na wózku najłatwiej zrezygnuje

| Etap | Ryzyko | Co wymaga od ekranu |
| --- | --- | --- |
| J3-3 | wejście ma oznaczenie „dostępne”, a na miejscu jest próg; jedno takie doświadczenie podważa zaufanie do całej aplikacji | data i źródło oznaczenia (US17.3), zdjęcie wejścia z obchodu (US17.2) |
| J3-4 | aplikacja nie zna szerokości korytarzy ani wymiarów windy, a dla wózka to rozstrzyga | zdanie wprost, czego nie wiemy, zamiast zgadywania; luka w danych do sprawdzenia w badaniach z użytkownikami |
| J3-5 | świeża awaria windy nie jest jeszcze sprawdzona, więc jej nie widać (BR-6) | sprawdzone awarie przy windzie ze zdjęciem i datą (US2.2); szybka weryfikacja zgłoszeń przy windach |

Etap J3-5 to świadomy kompromis. Pokazujemy tylko awarie sprawdzone przez Weryfikatora, bo niesprawdzone zgłoszenie mogłoby być błędne i podważyłoby zaufanie (N2). Kosztem jest opóźnienie między zgłoszeniem a publikacją.

## Źródła

1. S. Gibbons, „Journey Mapping 101”, NN/g: <https://www.nngroup.com/articles/journey-mapping-101/>

Dostęp do źródeł: 27.10.2026.
