# Wymagania do ekranów

Strona opisuje każdy ekran, zanim powstanie makieta. Element ekranu istnieje tylko wtedy, gdy wynika z kryterium akceptacji, reguły albo wymagania niefunkcjonalnego (decyzja 16). Historyjki i kryteria są na stronie [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), uprawnienia w [macierzy ról](../biznes/role.md#macierz-uprawnien).

Etykiety przycisków i treść komunikatów w cudzysłowach są robocze. Ostateczne słowa ustali słownik użytkownika. Kolumna „Dokąd” podaje ekran docelowy i jest wejściem do mapy nawigacji.

## Lista ekranów

| Ekran | Nazwa robocza | Role z dostępem | Historyjki | Nowy w fazie B |
| --- | --- | --- | --- | --- |
| V1 | Mapa kampusu | wszystkie | US1, US10.6 | — |
| V2 | Karta budynku | wszystkie; czynności Zbierającego tylko RO-ZBI | US2, US16, US17, US3.2; start US8.4, US15, US17.2 | — |
| V3 | Zaplanuj wizytę | wszystkie | US3 | — |
| V4 | Zgłoszenie szybkie | RO-ODW i role pracownicze; RO-GOS przechodzi do L1 | US4, US14.2 | — |
| V5 | Pomoc | wszystkie | US5 | — |
| L1 | Prośba o logowanie | RO-GOS | US14.2, US14.3 | tak |
| L2 | Konto | RO-ODW i role pracownicze | US14.1, US14.3 | tak |
| C1 | Pomiar pozycji | RO-ZBI | US6 | — |
| C2 | Formularz zgłoszenia | RO-ZBI | US7, US8.3 | — |
| C3 | Moje zgłoszenia | RO-ZBI, tylko własne | US8, US9.1 | — |
| C4 | Synchronizacja i wybór wersji | RO-ZBI | US9 | — |
| C5 | Piętra obejrzane | RO-ZBI, RO-KOO | US15, US11.3 | tak |
| C6 | Zmiana oznaczenia wejścia | RO-ZBI | US17.2 | tak |
| C7 | Usunięcie bariery | RO-ZBI | US8.4 | tak |
| R1 | Kolejka weryfikacji | RO-WER | US10.4, US10.5 | — |
| R2 | Sprawdzenie zgłoszenia | RO-WER | US10, US17.2 | — |
| K1 | Postęp inwentaryzacji | RO-KOO | US11 | — |
| K2 | Zaproszenie | RO-KOO | US12 | — |
| K3 | Eksport | RO-KOO | US13 | — |

Odwiedzający ma uprawnienie „Wysyłka po powrocie sieci”, ale nie ma ekranu C4. Jego zgłoszenie szybkie wysyła się w tle, a on widzi tylko potwierdzenie (US4.2, US4.4).

## Wymagania wspólne

Dotyczą każdego ekranu. Przy ekranach podajemy tylko to, co do nich dochodzi.

| Wymaganie | Odwołanie |
| --- | --- |
| Nagłówek z nazwą ekranu i przyciskiem „Wstecz”, zawsze widocznym. Wyjątek: V1 jako ekran startowy. | zasada dla osób starszych, NFR-18 |
| Wskaźnik braku sieci tekstem i ikoną. Gdy dane pochodzą z telefonu, obok jest data ostatniej synchronizacji. | US9.1, NFR-10, NFR-21 |
| Pasek dolny na V1, V2, V3, V5 i L2: „Mapa” (V1), „Zgłoś barierę” (V4), „Pomoc” (V5), „Konto” (L1 albo L2). Na formularzach i krokach zgłoszenia paska nie ma. | US1, US4, US5.1, US14 |
| Jedna czynność główna na ekranie, wyróżniona położeniem i rozmiarem, nie kolorem. | NFR-21, zasada dla osób starszych |
| Cel dotyku co najmniej 48 × 48 dp. | NFR-17 |
| Kontrast tekstu, ikon statusu, ramek pól i fokusu według progów NFR-16. | NFR-16 |
| Tekst czytelny przy czcionce 200%, bez ucinania i nakładania. | NFR-20 |
| Status, oznaczenie wejścia i uciążliwość zawsze tekstem i ikoną. Low-fi jest szare, a kolor nie niesie znaczenia. | NFR-21 |
| Każdy element interaktywny ma polską nazwę, rolę i stan dla TalkBack. | NFR-18 |
| Każda czynność wykonywana gestem, np. przesunięcie mapy albo punktu, ma odpowiednik w przyciskach. | NFR-15 (WCAG 2.5.1) [1] |
| Ekran działa w orientacji pionowej i poziomej. | NFR-22 |
| Brak limitów czasu na odpowiedź użytkownika. | NFR-23 |
| Komunikat błędu mówi, co się stało i co zrobić, bez żargonu technicznego. | NFR-15 (WCAG 3.3.1, 3.3.3) [1], NFR-50 [2] |
| Bez wymaganej roli ekranu nie ma w aplikacji, a serwer odrzuca żądanie. | US14.1, NFR-33 |

Stany wspólne dla ekranów, które potrzebują serwera (R1, R2, K1, K2, K3, L1):

| Stan | Komunikat |
| --- | --- |
| Brak sieci | „Ta czynność wymaga internetu. Połącz się z siecią i spróbuj ponownie.” Przycisk „Spróbuj ponownie”. |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Twoje dane w telefonie są bezpieczne. Spróbuj ponownie za kilka minut.” (NFR-50) |
| Brak uprawnień | Gdy rolę odebrano w trakcie pracy: „Nie masz już uprawnień do tej czynności. Wróć do mapy.” Przycisk „Wróć do mapy” (V1). (US14.1) |

## Gość niezalogowany i Odwiedzający

### V1 Mapa kampusu

| Pole | Treść |
| --- | --- |
| Cel | Osoba znajduje budynek na kampusie. |
| Role | wszystkie, bez logowania (US1.2) |
| Realizuje | US1.1, US1.2, US1.3, US10.6, początek US3.2 |
| Czynność główna | wyszukanie budynku po symbolu |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Mapa z konturami budynków | dane | US1, NFR-11 | — |
| Bariery ze zgłoszeń przyjętych; uciążliwość tekstem i kształtem | dane | US1.3, US10.6, NFR-21 | V2 (budynek bariery) |
| Pole „Symbol budynku”, np. „c13” | czynność główna | US1.1 | V2 |
| Przełącznik „Mapa / Lista budynków” | czynność | NFR-19 | lista na tym ekranie |
| Dotknięcie konturu albo budynku na liście | czynność | US2, US3.2 | V2 |
| Przyciski „+” i „−” oraz przesuwania mapy | czynność | NFR-15 (WCAG 2.5.1) | — |
| Napis „© OpenStreetMap contributors” z odnośnikiem | dane | NFR-37 | strona praw autorskich OSM |
| Pasek dolny | czynność | wymagania wspólne | V4, V5, L1 / L2 |

Wybór wyniku wyszukiwania zaznacza budynek na mapie i otwiera V2. Po „Wstecz” z V2 widać zaznaczony budynek.

| Stan | Co widać |
| --- | --- |
| Zwykły | mapa z podkładem OpenStreetMap, konturami i barierami |
| Pierwsze uruchomienie | przed mapą pokazuje się instrukcja V5 (US5.1) |
| Ładowanie | „Wczytuję mapę kampusu…”; kontury pojawiają się przed podkładem (NFR-11) |
| Pusty — brak wyniku | „Nie znaleziono budynku »X«. Sprawdź symbol, np. C-13, albo wybierz budynek z listy.” Przycisk „Lista budynków” (UC1 3a) |
| Brak sieci | kontury i bariery z telefonu, bez podkładu, z napisem „Dane z 12.11.2026” (NFR-10, NFR-39) |
| Brak sieci przy pierwszym uruchomieniu | „Do pierwszego uruchomienia potrzebny jest internet. Połącz się z siecią, a mapa wczyta się sama.” (NFR-11) |
| Błąd — brak podkładu | kontury i bariery bez podkładu, napis „Podkład mapy chwilowo niedostępny. Budynki i bariery widać dalej.” (NFR-50) |

Dostępność i osoby starsze: lista budynków daje tę samą informację co mapa i jest drogą dla TalkBack (NFR-19). Wyszukiwanie przyjmuje symbol w każdym zapisie, bez wymogu myślnika (US1.1).

### V2 Karta budynku

| Pole | Treść |
| --- | --- |
| Cel | Osoba sprawdza, którym wejściem wejdzie i jakie bariery są w budynku. |
| Role | wszystkie; przyciski Zbierającego widzi tylko RO-ZBI (US14.1) |
| Realizuje | US2.1, US2.2, US16.1, US16.2, US17.1, US17.3, US10.6, US3.2; start US8.4, US15.1, US17.2 |
| Czynność główna | „Zaplanuj wizytę” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Symbol i nazwa budynku | dane | US2 | — |
| Każde wejście: nazwa, oznaczenie „dostępne”, „niedostępne” albo „brak danych” tekstem i ikoną | dane | US2.1, US17.1, NFR-21 | — |
| Przy wejściu: data i źródło oznaczenia, „import” albo „obchód” | dane | US17.3, BR-24 | — |
| Zgłoszenia przyjęte: typ, uciążliwość, zdjęcie, data weryfikacji | dane | US2.2, BR-17, NFR-21 | — |
| Przełącznik pięter z etykietami z danych PWr (budynek szczegółowy) | czynność | US16.1 | plan na tym ekranie |
| Plan piętra: windy, schody, toalety, wejścia, zgłoszenia przyjęte | dane | US16.1 | — |
| Podpis „Źródło: Politechnika Wrocławska” pod planem | dane | NFR-40 | — |
| Napis „Plan piętra niedostępny dla tego budynku” (budynek podstawowy) | dane | US16.2, BR-G1 | — |
| Przycisk „Zaplanuj wizytę” | czynność główna | US3, US3.2 | V3 |
| RO-ZBI: „Zmień oznaczenie” przy każdym wejściu | czynność | US17.2, BR-24 | C6 |
| RO-ZBI: „Bariery już nie ma” przy każdym zgłoszeniu przyjętym | czynność | US8.4, BR-19 | C7 |
| RO-ZBI: „Popraw zgłoszenie” przy własnym zgłoszeniu przyjętym i przy każdym przyjętym zgłoszeniu szybkim | czynność | BR-3, BR-18 | C2 |
| RO-ZBI: „Piętra obejrzane” dla całego budynku | czynność | US15.1 | C5 |

Bariery z planu piętra są też na liście pod planem, więc TalkBack nie potrzebuje planu (NFR-19). Gość i Odwiedzający nie widzą przycisków Zbierającego, więc ich karta jest krótka.

| Stan | Co widać |
| --- | --- |
| Zwykły — budynek szczegółowy | wejścia, przełącznik pięter, plan, lista barier |
| Zwykły — budynek podstawowy | wejścia, napis o braku planu, lista barier (US16.2, NFR-48) |
| Ładowanie | tekst od razu, w miejscu zdjęcia napis „Zdjęcie się wczytuje” (NFR-12) |
| Pusty — brak zgłoszeń przyjętych | „Nikt jeszcze nie zgłosił tu bariery, którą sprawdziliśmy. To nie znaczy, że barier nie ma.” (UC2 2a) |
| Brak sieci | karta z telefonu z napisem „Dane z 12.11.2026”; zdjęcia, których nie ma w telefonie, zastępuje napis „Zdjęcie niedostępne bez internetu” (NFR-10) |
| Brak sieci, budynku nie ma w telefonie | „Tego budynku nie ma jeszcze w telefonie. Połącz się z internetem, aby zobaczyć kartę.” Przycisk „Spróbuj ponownie” (NFR-10) |
| Błąd zdjęcia | „Nie udało się wczytać zdjęcia.” Przycisk „Spróbuj ponownie” (NFR-50) |

Dostępność i osoby starsze: oznaczenie wejścia jest pierwszą informacją przy wejściu, a data i źródło stoją tuż pod nim (US17.3). „Brak danych” ma własną ikonę, inną niż „niedostępne” (US17.1).

### V3 Zaplanuj wizytę

| Pole | Treść |
| --- | --- |
| Cel | Osoba wie, którym wejściem wejdzie do budynku bez barier, albo wie, że takiego wejścia nie ma. |
| Role | wszystkie, bez logowania |
| Realizuje | US3.1, US3.2, US2.1, US2.2, US17.3 |
| Czynność główna | brak; ekran odpowiada na jedno pytanie, jedyna czynność to „Wstecz” (V2) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Zdanie z odpowiedzią, np. „Do A-1 wejdziesz bez schodów wejściem z tyłu budynku.” | dane | US3, US3.1 | — |
| Każde wejście dostępne: nazwa, oznaczenie tekstem i ikoną | dane | US2.1, NFR-21 | — |
| Przy wejściu: data i źródło oznaczenia | dane | US17.3 | — |
| Zgłoszenia przyjęte przy tym wejściu ze zdjęciem i datą weryfikacji | dane | US2.2, UC4 4c | — |

Droga z mapy: dotknięcie budynku (V2), „Zaplanuj wizytę” (V3). To 2 dotknięcia, a próg US3.2 to 3.

| Stan | Co widać |
| --- | --- |
| Zwykły | zdanie z odpowiedzią i wejścia dostępne |
| Brak wejścia dostępnego | „Budynek B-4 nie ma wejścia dostępnego. Wszystkie wejścia są oznaczone jako niedostępne. Sprawdź bariery przy wejściach na karcie budynku.” (US3.1, UC4 4a) |
| Brak danych o wejściach | „Nie wiemy jeszcze, czy budynek B-4 ma wejście dostępne. O 2 wejściach nie mamy danych, pozostałe są niedostępne. Datę i źródło każdego wejścia zobaczysz na karcie budynku.” (UC4 4b, US17.1) |
| Brak sieci | dane z telefonu z napisem „Dane z 12.11.2026” (NFR-10) |

Dostępność i osoby starsze: odpowiedź jest pełnym zdaniem na górze ekranu, a nie samym brakiem wyniku (US3.1). TalkBack czyta je jako pierwsze (NFR-18).

### V4 Zgłoszenie szybkie

| Pole | Treść |
| --- | --- |
| Cel | Odwiedzający przekazuje informację o barierze w 3 krokach, bez formularza. |
| Role | RO-ODW i role pracownicze; Gość niezalogowany przechodzi najpierw do L1 (US14.2) |
| Realizuje | US4.1, US4.2, US4.3, US4.4, US14.2 |
| Czynność główna | krok 1: „Zrób zdjęcie”; krok 2: wybór grupy; krok 3: „Wyślij” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Napis „Krok 1 z 3”, „Krok 2 z 3”, „Krok 3 z 3” | dane | US4.1 | — |
| Krok 1: przypomnienie „Nie fotografuj twarzy ani tablic rejestracyjnych” | dane | NFR-25 | — |
| Krok 1: „Zrób zdjęcie” i „Zrób ponownie” | czynność | US4.1, BR-8 | aparat, potem krok 2 |
| Krok 2: 6 dużych przycisków grup barier, każdy z tekstem i ikoną | czynność | US4.3, NFR-17, NFR-21 | krok 3 |
| Krok 3: miniatura zdjęcia i wybrana grupa | dane | US4.1 | — |
| Krok 3: napis „Dołączymy miejsce, w którym teraz stoisz” | dane | US4.1, NFR-27 | — |
| Krok 3: „Wyślij” | czynność główna | US4.1 | potwierdzenie |
| Potwierdzenie: „Dziękujemy. Zgłoszenie trafi do sprawdzenia.” | dane | US4.4 | — |
| Potwierdzenie: „Wróć do mapy” | czynność | US4.4 | V1 |

Aplikacja nie pyta o typ bariery, uciążliwość, budynek ani piętro (US4.3, BR-14). Pozycję zaczyna ustalać po wejściu na ekran, w trakcie kroków 1 i 2 (NFR-27).

| Stan | Co widać |
| --- | --- |
| Zwykły | kroki 1–3 i potwierdzenie |
| Gość niezalogowany | zamiast aparatu ekran L1 (US14.2) |
| Brak zgody na lokalizację | przed aparatem: „Do zgłoszenia potrzebne jest miejsce bariery. Aplikacja sprawdzi je tylko teraz i nie śledzi Cię.” Przycisk „Pozwól na lokalizację” (UC7 2a) |
| Odmowa zgody | „Bez lokalizacji nie wyślesz zgłoszenia. Możesz ją włączyć w ustawieniach telefonu.” Przyciski „Otwórz ustawienia” i „Wróć do mapy”. Aparat się nie otwiera (UC7 2a, BR-8) |
| Lokalizacja wyłączona w telefonie | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby wysłać zgłoszenie.” Przyciski „Otwórz ustawienia” i „Wróć do mapy” (UC7 4c) |
| Ładowanie — pozycja jeszcze nieznana w kroku 3 | „Ustalam miejsce bariery…”; „Wyślij” czeka bez limitu czasu (NFR-23) |
| Brak pozycji | „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” Przyciski „Spróbuj ponownie” i „Anuluj zgłoszenie”. Zdjęcie i grupa zostają (UC7 4c) |
| Brak sieci | potwierdzenie: „Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet, i trafi do sprawdzenia.” (US4.2, UC7 7a) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zgłoszenie się nie zapisało. Zwolnij miejsce i spróbuj ponownie.” Zdjęcie i grupa zostają (NFR-5, NFR-7) |

Dostępność i osoby starsze: każdy krok ma jedną czynność, a przyciski grup wypełniają szerokość ekranu (US4.3, NFR-17). Odwiedzający nie traci zdjęcia przy braku pozycji ani przy błędzie.

#### Jak ograniczamy przerwanie przez logowanie

US14.2 wymaga logowania przed zdjęciem, czyli w środku zadania. W granicach obecnych kryteriów ograniczamy to przerwanie tak:

| Środek | Odwołanie |
| --- | --- |
| Prośba o logowanie pojawia się od razu po „Zgłoś barierę”, zanim osoba coś zrobi. Nie traci zdjęcia ani wybranej grupy. | US14.2 |
| L1 mówi, co będzie dalej: „Po zalogowaniu od razu zrobisz zdjęcie. Całe zgłoszenie to 3 kroki.” | US14.2, US4.1 |
| Po zalogowaniu albo rejestracji aplikacja sama wraca do kroku 1 V4, bez ponownego wybierania „Zgłoś barierę”. | US14.2, UC7 1a |
| Aplikacja pamięta zalogowanie, więc przerwanie zdarza się tylko przy pierwszym zgłoszeniu. | NFR-35 |
| Konto można założyć wcześniej, w spokojnej chwili: „Konto” na pasku dolnym prowadzi do L1. Instrukcja V5 mówi o tym jednym zdaniem. | US5.1, US14 |
| „Nie teraz” wraca do mapy bez komunikatu o błędzie i bez utraty danych. | UC7 1b |

Zostaje jedno przerwanie przy pierwszym zgłoszeniu. Zmianę kolejności opisujemy w sekcji [do decyzji orkiestratora](#do-decyzji-orkiestratora-logowanie-po-zdjęciu).

### V5 Pomoc

| Pole | Treść |
| --- | --- |
| Cel | Nowa osoba wie, jak korzystać z aplikacji. |
| Role | wszystkie |
| Realizuje | US5.1 |
| Czynność główna | „Dalej” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Krótka instrukcja: kilka kroków tekstu z ilustracją i opisem dla TalkBack | dane | US5.1, NFR-18 | — |
| Napis „Krok 2 z 4” | dane | US5.1 | — |
| Zdanie w instrukcji: „Do zgłoszenia bariery potrzebne jest konto. Możesz je założyć teraz w »Konto«.” | dane | US5.1, US14.2 | — |
| „Dalej”, a w ostatnim kroku „Zacznij” | czynność główna | US5.1 | następny krok albo V1 |
| „Pomiń” | czynność | US5.1 | V1 |

| Stan | Co widać |
| --- | --- |
| Pierwsze uruchomienie | instrukcja otwiera się sama przed V1 (US5.1) |
| Otwarcie z pomocy | ta sama instrukcja z paska dolnego; nie otwiera się już sama (US5.1) |

Treść instrukcji jest w telefonie, więc ekran nie ma stanu braku sieci. Dostępność i osoby starsze: przejście dalej przyciskiem, nie przesunięciem palca (NFR-15, WCAG 2.5.1); brak automatycznego przewijania (NFR-23).

## Logowanie i konto

### L1 Prośba o logowanie

| Pole | Treść |
| --- | --- |
| Cel | Gość niezalogowany loguje się albo zakłada konto, aby zgłosić barierę. |
| Role | RO-GOS |
| Realizuje | US14.2, US14.3 |
| Czynność główna | „Zaloguj się” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Zdanie „Zgłoszenie bariery wymaga konta. Konto chroni mapę przed fałszywymi zgłoszeniami.” | dane | US14.2, BR-8 | — |
| Przy wejściu z V4: „Po zalogowaniu od razu zrobisz zdjęcie. Całe zgłoszenie to 3 kroki.” | dane | US14.2, US4.1 | — |
| „Zaloguj się” | czynność główna | US14, NFR-31 | strona Usługi logowania w przeglądarce systemowej, potem ekran, z którego przyszła osoba |
| „Załóż konto” | czynność | US14.3, NFR-30 | rejestracja w przeglądarce z klauzulą informacyjną |
| „Nie teraz” | czynność | UC7 1b | V1 |

Pola hasła w aplikacji nie ma (NFR-31). Po zalogowaniu z V4 aplikacja otwiera aparat w kroku 1 (US14.2).

| Stan | Co widać |
| --- | --- |
| Zwykły | zdanie i trzy przyciski |
| Brak sieci | „Logowanie wymaga internetu. Mapa i karty budynków działają bez niego.” Przycisk „Wróć do mapy” (UC6 1a) |
| Błąd | „Nie udało się zalogować. Spróbuj ponownie.” Przycisk „Spróbuj ponownie” |
| Blokada konta | „Konto jest chwilowo zablokowane po kilku błędnych hasłach. Spróbuj ponownie później.” (NFR-32) |

Dostępność i osoby starsze: strona logowania w przeglądarce też spełnia WCAG 2.1 AA (NFR-15). „Nie teraz” jest równie duże jak „Zaloguj się”, więc rezygnacja nie wymaga szukania.

### L2 Konto

| Pole | Treść |
| --- | --- |
| Cel | Zalogowana osoba widzi swoje role i przechodzi do czynności swojej roli. |
| Role | RO-ODW i role pracownicze |
| Realizuje | US14.1, US14.3 |
| Czynność główna | rola pracownicza: wybór czynności; RO-ODW: brak czynności roli, ekran ma tylko dane konta |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nazwa wyświetlana i lista ról, np. „Odwiedzający, Zbierający” | dane | BR-1, US14.3 | — |
| „Twoje czynności”: tylko czynności posiadanych ról | czynność główna | US14.1, BR-1 | — |
| RO-ZBI: „Nowe zgłoszenie z pomiarem” | czynność | US6 | C1 |
| RO-ZBI: „Moje zgłoszenia” | czynność | US8 | C3 |
| RO-WER: „Zgłoszenia do sprawdzenia” | czynność | US10 | R1 |
| RO-KOO: „Postęp inwentaryzacji” | czynność | US11 | K1 |
| RO-KOO: „Zaproś osobę” | czynność | US12 | K2 |
| RO-KOO: „Pobierz eksport” | czynność | US13 | K3 |
| „Informacja o danych osobowych” | czynność | NFR-30 | treść klauzuli |
| „Wycofaj zgodę” | czynność | NFR-30, NFR-29 | potwierdzenie |
| „Wyloguj” | czynność | macierz uprawnień | V1 |

Lista czynności to zwykła lista przycisków z tekstem, a nie kafelki.

| Stan | Co widać |
| --- | --- |
| Zwykły — Odwiedzający | dane konta, klauzula, „Wycofaj zgodę”, „Wyloguj”; bez listy czynności (US14.3) |
| Zwykły — rola pracownicza | dane konta i lista czynności ról |
| Potwierdzenie wycofania zgody | „Po wycofaniu zgody wylogujemy Cię, a Twoje dane osobowe usuniemy w ciągu miesiąca. Zgłoszenia zostaną bez autora.” Przyciski „Wycofaj zgodę” i „Anuluj” (NFR-29) |
| Brak sieci | role z ostatniego logowania; ekrany czynności pokazują własne stany braku sieci |

Dostępność i osoby starsze: klauzula jest najwyżej 2 dotknięcia od mapy: „Konto”, „Informacja o danych osobowych” (NFR-30).

## Zbierający

### C1 Pomiar pozycji

| Pole | Treść |
| --- | --- |
| Cel | Zbierający zapisuje dokładne miejsce bariery i wskazuje budynek. |
| Role | RO-ZBI |
| Realizuje | US6.1, US6.2, US6.3, US6.4, US6.5 |
| Czynność główna | „Dalej” — zatwierdzenie miejsca |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Postęp uśredniania 30 s, np. „Pozostało 12 s” | dane | US6.1 | — |
| Dokładność pozycji w metrach z dopiskiem „szacunek” | dane | US6.1, NFR-2 | — |
| Liczba satelitów osobno dla każdego systemu GNSS | dane | US6.1, NFR-2 | — |
| Mapa nieba; system satelity rozróżniony kształtem i podpisem | dane | US6.5, NFR-21 | — |
| „Powtórz pomiar” | czynność | UC8 2a, NFR-23 | początek pomiaru |
| Podpowiedź budynku: „Potwierdź”, „Wybierz inny”, „Poza budynkiem” | czynność | US6.2, BR-G2 | — |
| Przesunięcie punktu na mapie, także przyciskami strzałek | czynność | US6.4, NFR-3, NFR-15 | — |
| Przesunięcie punktu na planie wybranego piętra (budynek szczegółowy) | czynność | US6.3, NFR-3 | — |
| Napis „korekta ręczna” po przesunięciu punktu | dane | US6.3, US6.4 | — |
| Napis „© OpenStreetMap contributors”; pod planem „Źródło: Politechnika Wrocławska” | dane | NFR-37, NFR-40 | — |
| „Dalej” | czynność główna | US6 | C2 |

| Stan | Co widać |
| --- | --- |
| Zwykły | uśrednianie, dokładność, satelity, podpowiedź budynku |
| Ładowanie — szukanie satelitów | „Szukam satelitów. Na otwartej przestrzeni pomiar jest dokładniejszy.” (NFR-1) |
| Daleko od budynków PWr | „Poza budynkiem”, bez podpowiedzi i bez komunikatu o błędzie (BR-G4, NFR-49) |
| Budynek podstawowy | bez korekty na planie; korekta na mapie działa (US6.4, BR-G1) |
| Brak zgody na lokalizację | jak w V4: prośba o zgodę, a po odmowie „Otwórz ustawienia” (NFR-27) |
| Brak sieci | pomiar i podpowiedź działają; mapa bez podkładu (NFR-4, NFR-39) |

Dostępność: liczby dokładności i satelitów są tekstem, a nie tylko wykresem (NFR-18). Uśrednianie można przerwać i powtórzyć bez limitu (NFR-23).

### C2 Formularz zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Zbierający opisuje barierę tak, by Weryfikator mógł ją ocenić. |
| Role | RO-ZBI |
| Realizuje | US7.1, US7.2, US7.3, US8.3 |
| Czynność główna | „Zapisz” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Budynek i miejsce z C1, przycisk „Zmień miejsce” | dane, czynność | US6.2 | C1 |
| Piętro: wybór z etykiet z danych PWr (budynek szczegółowy) | czynność | US7.2 | — |
| Piętro: liczba całkowita i pole „półpiętro” (budynek podstawowy) | czynność | US7.2, BR-G1 | — |
| Typ bariery, pogrupowany w 6 grup barier | czynność | US7.1, BR-7 | — |
| Uciążliwość 1–3, każda wartość z opisem i ikoną | czynność | US7.1, NFR-21 | — |
| Zdjęcie, co najmniej jedno, z przypomnieniem o twarzach i tablicach | czynność | US7.1, NFR-25 | aparat |
| Opis; obowiązkowy przy TB-29 „inna bariera” | czynność | US7.3 | — |
| Przy zmianie zgłoszenia przyjętego: „Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.” | dane | BR-18 | — |
| „Zapisz” | czynność główna | BR-7 | C3 |

Poza budynkiem pola piętra nie ma (BR-G4). C2 otwiera się z C1, z C3 przy edycji (US8.3) i z V2 przy zmianie zgłoszenia przyjętego (BR-18).

| Stan | Co widać |
| --- | --- |
| Zwykły — nowe zgłoszenie | puste pola, miejsce z C1 |
| Edycja | pola z zapisanymi wartościami (US8.3) |
| Błąd walidacji | nad formularzem „Brakuje: typu bariery. Uzupełnij, aby zapisać.”, a przy polu ten sam tekst; zapis się nie wykonuje (US7.1, US7.3) |
| Brak sieci | zapis działa, zgłoszenie dostaje status „lokalne” (US9.1) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” (NFR-7) |

Dostępność: błąd walidacji jest tekstem przy polu i w podsumowaniu, a fokus TalkBack przechodzi do pierwszego brakującego pola (NFR-15, WCAG 3.3.1).

### C3 Moje zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Zbierający wie, które zgłoszenia dotarły, a które trzeba poprawić. |
| Role | RO-ZBI, tylko własne zgłoszenia |
| Realizuje | US8.1, US8.2, US8.3, US9.1, US8.4 (status zmiany), US17.2 (status zmiany) |
| Czynność główna | otwarcie zgłoszenia do poprawy |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Zgłoszenie: typ, budynek, piętro, data | dane | US8.1 | — |
| Rodzaj: zgłoszenie, „bariera usunięta” albo zmiana oznaczenia wejścia | dane | US8.4, US17.2 | — |
| Jeden status tekstem i ikoną: lokalne, wysłane, w weryfikacji, przyjęte, odrzucone, archiwalne | dane | US8.1, US8.2, NFR-21 | — |
| Przy odrzuconym: powód odrzucenia | dane | US8.1, BR-16 | — |
| Znak „zmiana czeka na wysłanie” przy niewysłanej edycji | dane | BR-9, [cykl życia](../biznes/reguly.md#cykl-zycia-zgloszenia) | — |
| Znak „wybierz wersję” przy konflikcie | czynność | US9.3 | C4 |
| Dotknięcie zgłoszenia przed decyzją albo odrzuconego | czynność główna | US8.3, BR-3 | C2 |
| Przy powodzie „nieaktualne”: „Wyślij ponownie do bieżącego zgłoszenia” | czynność | BR-23 | C2 |
| Przy „nieaktualne” po usunięciu bariery: „Bariery nie ma już na mapie. Jeśli nadal istnieje, złóż nowe zgłoszenie.” | dane, czynność | BR-23 | C1 |
| Stan synchronizacji, np. „Czeka na wysłanie: 3” | czynność | US9.1 | C4 |
| „Nowe zgłoszenie” | czynność | US6 | C1 |

| Stan | Co widać |
| --- | --- |
| Zwykły | lista własnych zgłoszeń, najnowsze na górze |
| Pusty | „Nie masz jeszcze zgłoszeń.” Przycisk „Nowe zgłoszenie” (C1) |
| Ładowanie | lista z telefonu zostaje widoczna; obok napis „Sprawdzam statusy…” |
| Brak sieci | lista z telefonu ze statusami z dnia ostatniej synchronizacji; nowe zgłoszenia „lokalne” (US9.1) |
| Błąd | „Nie udało się sprawdzić statusów. Zgłoszenia są bezpieczne w telefonie.” Przycisk „Spróbuj ponownie” (NFR-5, NFR-50) |

Dostępność: status jest słowem, a ikona go tylko wzmacnia (NFR-21). TalkBack czyta status przed typem bariery (NFR-18).

### C4 Synchronizacja i wybór wersji

| Pole | Treść |
| --- | --- |
| Cel | Zbierający wie, co czeka na wysłanie, i nie traci żadnej wersji zgłoszenia. |
| Role | RO-ZBI |
| Realizuje | US9.1, US9.2, US9.3 |
| Czynność główna | przy konflikcie: wybór wersji; bez konfliktu: „Synchronizuj teraz” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Liczba zgłoszeń i znaczników czekających w telefonie | dane | US9.1, BR-9 | — |
| Data ostatniej synchronizacji | dane | NFR-10 | — |
| „Synchronizuj teraz”; synchronizacja i tak rusza sama | czynność | UC10, US9.2 | — |
| Ostrzeżenie „W telefonie zostało mniej niż 100 MB” | dane | NFR-7 | — |
| Konflikt: wersja własna i wersja z serwera naraz, pole różniące się oznaczone słowem „różni się” | dane | US9.3, NFR-21 | — |
| „Zostaw moją wersję” i „Weź wersję z serwera” | czynność główna | US9.3, BR-10 | C3 |
| „Zdecyduję później”; zmiana czeka w telefonie | czynność | BR-10 | C3 |

| Stan | Co widać |
| --- | --- |
| Zwykły — kolejka | liczba czekających i data ostatniej synchronizacji |
| Pusty | „Wszystko wysłane.” z datą synchronizacji |
| Ładowanie | „Wysyłam 2 z 5…” |
| Brak sieci | „Brak internetu. Wyślemy wszystko sami, gdy sieć wróci.” (US9.1, US9.2) |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później.” (NFR-50) |
| Konflikt | obie wersje i trzy przyciski wyboru (US9.3) |

Dostępność: w pionie wersje stoją jedna pod drugą, w poziomie obok siebie, by przy czcionce 200% nic się nie ucinało (NFR-20, NFR-22).

### C5 Piętra obejrzane

| Pole | Treść |
| --- | --- |
| Cel | Zbierający oznacza piętro albo budynek jako obejrzany, a Koordynator cofa błędny znacznik. |
| Role | RO-ZBI: oznacza i cofa własne; RO-KOO: cofa każdy znacznik |
| Realizuje | US15.1, US15.2, US15.3, US11.3 |
| Czynność główna | RO-ZBI: „Oznacz jako obejrzane”; RO-KOO: „Cofnij znacznik” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Symbol budynku | dane | US15.1 | — |
| Budynek szczegółowy: lista pięter z etykietami z danych PWr | dane | US15.1, US16.1 | — |
| Budynek podstawowy: jeden wiersz „Cały budynek” | dane | US15.1, BR-20 | — |
| Przy wierszu: „obejrzane” albo „nieobejrzane” tekstem i ikoną | dane | US15.1, NFR-21 | — |
| Przy znaczniku: „Twój znacznik” albo „znacznik innej osoby” | dane | US15.2 | — |
| Przy znaczniku w telefonie: „czeka na wysłanie” | dane | US15.3 | — |
| RO-ZBI: „Oznacz jako obejrzane” przy wierszu bez znacznika | czynność główna | US15.1 | — |
| RO-ZBI: „Cofnij” tylko przy własnym znaczniku | czynność | US15.2 | — |
| RO-KOO: „Cofnij znacznik” przy każdym znaczniku, z potwierdzeniem | czynność główna | US11.3, BR-20 | K1 po potwierdzeniu |

C5 otwiera Zbierający z karty budynku (V2), a Koordynator z postępu (K1). Koordynator nie stawia znaczników (BR-20), więc nie widzi „Oznacz jako obejrzane”.

| Stan | Co widać |
| --- | --- |
| Zwykły | lista pięter albo wiersz „Cały budynek” |
| Potwierdzenie cofnięcia (RO-KOO) | „Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający.” Przyciski „Cofnij” i „Anuluj” (BR-20) |
| Brak sieci | „Znacznik zapisany w telefonie. Wyślemy go, gdy wróci internet.” Cofnięcie działa tak samo (US15.3, BR-20) |
| Błąd zapisu | „Nie udało się zapisać znacznika. Spróbuj ponownie.” |

Ekran nie ma stanu pustego: budynek szczegółowy ma piętra z danych PWr, a budynek bez nich działa jak podstawowy (BR-G3).

### C6 Zmiana oznaczenia wejścia

| Pole | Treść |
| --- | --- |
| Cel | Zbierający przy wejściu zgłasza, czy wejście jest dostępne, i dołącza zdjęcie. |
| Role | RO-ZBI, każde wejście |
| Realizuje | US17.2, US17.3 |
| Czynność główna | „Wyślij do sprawdzenia” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Budynek i nazwa wejścia | dane | US17.2 | — |
| Obecne oznaczenie tekstem i ikoną, z datą i źródłem | dane | US17.3, NFR-21 | — |
| Dwa duże przyciski: „dostępne” i „niedostępne” | czynność | US17.2, BR-24, NFR-17 | — |
| Przy wyborze obecnej wartości: „Potwierdzasz obecne oznaczenie. Zmieni się data i źródło.” | dane | BR-24, UC18 2a | — |
| Zdjęcie wejścia, obowiązkowe, z przypomnieniem o twarzach i tablicach | czynność | US17.2, BR-24, NFR-25 | aparat |
| Napis „Zmianę sprawdzi inna osoba. Do tego czasu karta pokazuje obecne oznaczenie.” | dane | BR-4, BR-24 | — |
| „Wyślij do sprawdzenia” | czynność główna | US17.2 | C3 |

„Brak danych” nie jest do wyboru, bo nadaje je tylko import (BR-24).

| Stan | Co widać |
| --- | --- |
| Zwykły | obecne oznaczenie, wybór i zdjęcie |
| Błąd walidacji | „Dodaj zdjęcie wejścia, aby wysłać zmianę.” albo „Wybierz: dostępne czy niedostępne.” (BR-24) |
| Brak sieci | „Zmiana zapisana w telefonie. Wyślemy ją, gdy wróci internet.” Status „lokalne” (US9.1) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” (NFR-7) |

### C7 Usunięcie bariery

| Pole | Treść |
| --- | --- |
| Cel | Zbierający zgłasza, że przyjętej bariery już nie ma, np. winda działa. |
| Role | RO-ZBI, także dla cudzych zgłoszeń (BR-19) |
| Realizuje | US8.4 |
| Czynność główna | „Wyślij do sprawdzenia” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Zgłoszenie przyjęte: typ, piętro, miejsce, zdjęcie, data weryfikacji; bez edycji | dane | BR-19 | — |
| Nowe zdjęcie miejsca, obowiązkowe | czynność | US8.4, BR-19, NFR-25 | aparat |
| Napis „Bariera zostanie na mapie, dopóki Weryfikator nie sprawdzi zgłoszenia.” | dane | US8.4 | — |
| „Wyślij do sprawdzenia” | czynność główna | US8.4 | C3 |

Uciążliwości ani typu się nie wybiera: przechodzą z poprzedniego zgłoszenia albo nie są wymagane (BR-19).

| Stan | Co widać |
| --- | --- |
| Zwykły | dane poprzedniego zgłoszenia i miejsce na nowe zdjęcie |
| Błąd walidacji | „Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.” (BR-19) |
| Brak sieci | „Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet.” Status „lokalne” (US8.4, US9.1) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” (NFR-7) |

## Weryfikator

### R1 Kolejka weryfikacji

| Pole | Treść |
| --- | --- |
| Cel | Weryfikator wybiera zgłoszenie do sprawdzenia. |
| Role | RO-WER |
| Realizuje | US10.4, US10.5 |
| Czynność główna | „Weź do sprawdzenia” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Lista zgłoszeń „wysłane”: typ albo grupa barier, budynek, rodzaj, data wysłania | dane | US10.4, BR-14 | — |
| Przełącznik „Lista / Mapa”; lista daje tę samą informację | czynność | UC14, NFR-19 | — |
| Napis „© OpenStreetMap contributors” na mapie | dane | NFR-37 | — |
| Sekcja „Wzięte przeze mnie” ze zgłoszeniami „w weryfikacji” tego Weryfikatora | czynność | BR-11, UC14 2b | R2 |
| „Weź do sprawdzenia” | czynność główna | US10.4, BR-11 | R2 |

Własnych zgłoszeń, także zmian „bariera usunięta” i zmian oznaczenia wejścia, lista nie pokazuje (US10.5, BR-4).

| Stan | Co widać |
| --- | --- |
| Zwykły | lista albo mapa zgłoszeń |
| Pusty | „Nie ma zgłoszeń do sprawdzenia.” |
| Ładowanie | „Wczytuję kolejkę…” |
| Zgłoszenie zajęte | „Ktoś inny wziął już to zgłoszenie. Wybierz inne.” Zgłoszenie znika z listy (UC14 2a, BR-11) |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne (UC14 1a, NFR-50) |

### R2 Sprawdzenie zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Weryfikator przyjmuje zgłoszenie albo odrzuca je z powodem, bez przepuszczenia błędu i duplikatu. |
| Role | RO-WER, tylko zgłoszenia wzięte przez siebie i nie własne (BR-4, BR-11) |
| Realizuje | US10.1, US10.2, US10.3, US10.6, US17.2 |
| Czynność główna | „Przyjmij” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Dane zgłoszenia: typ, uciążliwość, budynek, piętro, miejsce; bez edycji przy pomiarze precyzyjnym | dane | BR-15 | — |
| Zdjęcie | dane | US10 | — |
| Rola i pseudonim autora, np. „Zbierający, Autor 17” | dane | BR-5 | — |
| Podobne zgłoszenia w promieniu 10 m: typ, zdjęcie, odległość, piętro, status | dane | US10.1, BR-13 | — |
| Zgłoszenie szybkie: wybór typu bariery i uciążliwości | czynność | US10.3, BR-14 | — |
| Zasłonięcie twarzy albo tablicy na zdjęciu | czynność | BR-15, NFR-25 | — |
| Zmiana oznaczenia wejścia: obecne oznaczenie z datą i źródłem, wybrane oznaczenie, nowe zdjęcie | dane | US17.2, BR-24 | — |
| „Bariera usunięta”: zdjęcie poprzedniego zgłoszenia obok nowego | dane | US10.6, BR-19 | — |
| „Przyjmij” | czynność główna | US10, US10.6 | R1 |
| „Odrzuć” z obowiązkowym powodem; gotowe powody „duplikat” ze wskazaniem wcześniejszego zgłoszenia i „nieaktualne” | czynność | US10.2, BR-13, BR-16, BR-23 | R1 |

| Stan | Co widać |
| --- | --- |
| Zwykły | dane, zdjęcie, podobne zgłoszenia, decyzja |
| Pusty — brak podobnych | „W promieniu 10 m nie ma podobnych zgłoszeń.” (US10.1) |
| Ładowanie | tekst od razu, zdjęcia z napisem „Zdjęcie się wczytuje” |
| Błąd walidacji — przyjęcie | „Ustal typ bariery i uciążliwość, aby przyjąć zgłoszenie.” (US10.3) |
| Błąd walidacji — odrzucenie | „Podaj powód odrzucenia, aby autor wiedział, co poprawić.” (US10.2) |
| Nowa wersja | „Autor zmienił zgłoszenie. Widzisz nową wersję.” (BR-3, UC14 5e) |
| Brak sieci | „Decyzja wymaga internetu. Zgłoszenie zostaje u Ciebie w »Wziętych przeze mnie«.” (BR-11) |
| Błąd serwera, brak uprawnień | stany wspólne |

Dostępność: podobne zgłoszenia mają odległość tekstem, np. „5 m”, a nie tylko punkt na mapie (NFR-19).

## Koordynator

### K1 Postęp inwentaryzacji

| Pole | Treść |
| --- | --- |
| Cel | Koordynator wie, gdzie zespół musi jeszcze pójść. |
| Role | RO-KOO |
| Realizuje | US11.1, US11.2, US11.3 |
| Czynność główna | dotknięcie budynku — otwarcie jego pięter |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Lista budynków z symbolem | dane | US11.1 | — |
| Budynek szczegółowy: „4 z 5 pięter, 80%” | dane | US11.1, US11.2, BR-21 | — |
| Budynek podstawowy: „obejrzany” albo „nieobejrzany” tekstem i ikoną | dane | US11.1, NFR-21 | — |
| Liczby: „czeka na sprawdzenie” i „przyjęte” | dane | US11.1, BR-21 | — |
| Dotknięcie budynku | czynność główna | US11.3 | C5 |
| „Odśwież” | czynność | US11.2 | — |

Sortowania ani filtrów nie ma, bo nie wynikają z żadnego kryterium (decyzja 16). Wiersz o wejściach dla miary M-1 czeka na decyzję, patrz [niżej](#do-decyzji-orkiestratora-odczyt-miary-m-1).

| Stan | Co widać |
| --- | --- |
| Zwykły | lista budynków z postępem i liczbami |
| Ładowanie | „Wczytuję postęp…” |
| Brak sieci | stan wspólny; postęp liczy serwer, więc bez sieci go nie widać (UC15 1a) |
| Błąd serwera, brak uprawnień | stany wspólne (NFR-50) |

Stanu pustego nie ma: lista budynków pochodzi z importu, a 0% to zwykła wartość postępu.

### K2 Zaproszenie

| Pole | Treść |
| --- | --- |
| Cel | Koordynator zaprasza osobę z wybraną rolą. |
| Role | RO-KOO |
| Realizuje | US12.1, US12.2 |
| Czynność główna | „Wyślij zaproszenie” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Pole „Adres e-mail” | czynność | US12.1 | — |
| Wybór roli: Zbierający, Weryfikator, Koordynator | czynność | US12.1, BR-2 | — |
| „Wyślij zaproszenie” | czynność główna | US12.1 | potwierdzenie na tym ekranie |

| Stan | Co widać |
| --- | --- |
| Zwykły | pole adresu i wybór roli |
| Sukces | „Zaproszenie wysłane na adres podany w polu.” (US12.1) |
| Sukces — konto istnieje | „To konto już istnieje. Dodaliśmy mu rolę Weryfikatora, dotychczasowe role zostają.” (US12.2) |
| Błąd walidacji | „Sprawdź adres e-mail, np. brakuje znaku @.” albo „Wybierz rolę.” |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne; błąd serwera obejmuje Usługę logowania |

### K3 Eksport

| Pole | Treść |
| --- | --- |
| Cel | Koordynator pobiera plik z barierami do QGIS. |
| Role | RO-KOO |
| Realizuje | US13.1, US13.2, US13.3 |
| Czynność główna | „Pobierz plik” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Wybór formatu: „GeoJSON (WGS84)” albo „CSV (PUWG 1992, EPSG:2180)” | czynność | US13.1, US13.2 | — |
| Napis „Plik zawiera tylko obecne, sprawdzone bariery, bez danych autorów.” | dane | US13.3, BR-22 | — |
| „Pobierz plik” | czynność główna | US13 | plik w pobranych plikach telefonu |

| Stan | Co widać |
| --- | --- |
| Zwykły | wybór formatu i opis zawartości |
| Ładowanie | „Przygotowuję plik…” |
| Pusty | „Nie ma jeszcze przyjętych barier. Pobrany plik będzie pusty.” Pobranie działa (UC17 2a) |
| Sukces | „Plik zapisany w pobranych plikach.” |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne (UC17 1a) |

## Decyzje fazy B

| Temat | Rozwiązanie | Dlaczego |
| --- | --- | --- |
| Ekran oznaczenia piętra (US15, UC13) | Nowy ekran C5 „Piętra obejrzane”. Zbierający otwiera go z karty budynku (V2), Koordynator z postępu (K1). | C1 służy jednemu pomiarowi bariery, a piętro oznacza się po obejściu całego piętra, często bez żadnej bariery. W budynku podstawowym C1 nie ma pięter. Jeden ekran obsługuje też cofnięcie przez Koordynatora (US11.3). |
| Start zmiany „bariera usunięta” (US8.4, UC12) | Przycisk „Bariery już nie ma” przy zgłoszeniu przyjętym na V2, widoczny tylko dla RO-ZBI. Prowadzi do nowego ekranu C7. | Zbierający stoi przy barierze i widzi ją na karcie budynku. C2 wymaga typu i uciążliwości (US7.1), a C7 tylko nowego zdjęcia (BR-19). Osobny ekran nie miesza tych reguł. |
| Start zmiany zgłoszenia przyjętego (UC9 2c) | Przycisk „Popraw zgłoszenie” na V2, tylko przy własnym zgłoszeniu i przy zgłoszeniu szybkim. Otwiera C2 z wartościami. | To ta sama karta co dla „bariera usunięta”, a formularz ten sam co przy pierwszym zapisie (BR-18). |
| Zmiana oznaczenia wejścia (US17.2, UC18) | Przycisk „Zmień oznaczenie” przy wejściu na V2, tylko dla RO-ZBI. Prowadzi do nowego ekranu C6. | Lista wejść z obecnym oznaczeniem jest na V2, a Zbierający stoi przy wejściu. C6 ma dwa przyciski i zdjęcie, bez typu i uciążliwości (BR-24). |
| Zgłoszenie szybkie bez zgody na lokalizację (UC7 2a) | Prośba o zgodę przed aparatem. Po odmowie komunikat i „Otwórz ustawienia”; aparat się nie otwiera. | Bez pozycji zgłoszenia nie da się wysłać (BR-8). Senior nie powinien robić zdjęcia, które i tak się nie wyśle. Wskazanie miejsca na mapie byłoby czwartym krokiem (US4.1). |
| Zgłoszenie szybkie bez pozycji (UC7 4c) | Pozycja ustala się w trakcie kroków 1 i 2. Gdy przy „Wyślij” jej brak, komunikat z radą i „Spróbuj ponownie”; zdjęcie i grupa zostają. | Zapis bez pozycji łamie BR-8, a pozycja dopisana później byłaby z innego miejsca. Odwiedzający nie traci pracy, a limit czasu nie istnieje (NFR-23). |
| Komunikat „brak wejścia dostępnego” (US3.1, UC4 4a, 4b) | Dwa różne zdania na V3: „nie ma wejścia dostępnego” i „nie wiemy jeszcze”. Treść w tabeli stanów V3. | „Brak danych” to co innego niż „niedostępne” (US17.1). Senior nie może zrezygnować z wizyty przez brak danych. |
| Brak wyniku wyszukiwania (UC1 3a) | Zdanie z przykładem symbolu i przycisk „Lista budynków”. | Komunikat mówi, co zrobić, a lista działa też z TalkBack (NFR-19). |
| Karta bez zgłoszeń przyjętych (UC2 2a) | Zdanie, że brak zgłoszeń nie znaczy braku barier. | Wynika z P-4 procesu TO-BE: pusta lista nie może brzmieć jak „wszystko dostępne” (N2). |
| Eksport bez zgłoszeń przyjętych (UC17 2a) | Komunikat, a pobranie pustego pliku działa. | Pusty plik GeoJSON jest poprawny (NFR-43) i nie wymaga osobnej reguły na serwerze. |
| Znak „zmiana czeka na wysłanie” (reguły, cykl życia) | Dodatkowy znak przy zgłoszeniu na C3, obok statusu. | Status się nie zmienia, a Zbierający musi wiedzieć, że edycja jest tylko w telefonie (BR-9). |

## Do decyzji orkiestratora: odczyt miary M-1

Miara M-1 z [wizji](../biznes/wizja.md) wymaga 100% wejść budynków szczegółowych ze źródłem „obchód”. K1 pokazuje tylko piętra i liczby zgłoszeń, więc Koordynator nie odczyta tej miary z jednego ekranu. Wizji nie zmieniamy.

| Wariant | Co się zmienia | Za | Przeciw |
| --- | --- | --- | --- |
| A. Wiersz w K1 (zalecany) | W K1 przy budynku szczegółowym wiersz „Wejścia potwierdzone na obchodzie: 4 z 6”. Potrzebne nowe kryterium US11.4, scenariusz i dopisek w BR-21. | Dane już są: każde wejście ma źródło (BR-24). Służy też US11, bo wejście bez obchodu to miejsce, gdzie trzeba jeszcze pójść. | Nowy element K1; decyzja 34 odrzuciła nowe pola K1, choć przy innym problemie. |
| B. Odczyt z kart V2 | Bez zmian w ekranach. Koordynator liczy wejścia ze źródłem „obchód” na 5 kartach budynków (US17.3). | Zero nowego zakresu. | Ręczne liczenie kilkudziesięciu wejść; łatwo o pomyłkę przy pomiarze sukcesu. |
| C. Zmiana miary | M-1 liczy tylko piętra obejrzane. | K1 wystarcza bez zmian. | Wymaga zmiany wizji i decyzji 36; nie zalecamy. |

Proponowane brzmienie kryterium dla wariantu A: „US11.4 — Dla każdego budynku szczegółowego widać liczbę wejść ze źródłem »obchód« wśród wszystkich jego wejść.” Do decyzji makieta K1 nie ma tego wiersza.

## Do decyzji orkiestratora: logowanie po zdjęciu

Mapa ścieżki osoby starszej pokazuje, że logowanie przed zdjęciem przerywa zadanie w najgorszym momencie. Środki z V4 zmniejszają to przerwanie, ale go nie usuwają. Obecne kryteria zostawiamy bez zmian.

| Wariant | Co się zmienia | Za | Przeciw |
| --- | --- | --- | --- |
| A. Bez zmian (zalecany na teraz) | US14.2 zostaje: logowanie przed zdjęciem, ze środkami z V4. | Osoba nie robi zdjęcia, które może się nie wysłać. Brak nowych reguł. | Przy pierwszym zgłoszeniu bariera może zniknąć z oczu, zanim osoba się zaloguje. |
| B. Logowanie przed wysłaniem | Gość robi zdjęcie i wybiera grupę, a logowanie pojawia się przy „Wyślij”. Zdjęcie czeka w telefonie. Wymaga zmiany US14.2, BR-8, UC7 1a i 3 scenariuszy `@US14.2`. | Zdjęcie powstaje, gdy bariera jest przed osobą. | Niezalogowany telefon trzyma zdjęcie bez konta; rezygnacja z logowania wyrzuca wykonaną pracę. |

Zalecamy wariant A do czasu wywiadów i testu z osobami starszymi. Jeśli test pokaże rezygnacje przy L1, wracamy do wariantu B.

## Pokrycie kryteriów

Każde kryterium akceptacji ma ekran albo dopisek „bez ekranu”, gdy dotyczy tylko serwera lub pliku.

| Historyjka | Kryterium | Ekran | Uwagi |
| --- | --- | --- | --- |
| Wyszukiwanie budynku | US1.1 | V1 | pole symbolu budynku |
| | US1.2 | V1, V2 | bez logowania |
| | US1.3 | V1, V2 | tylko zgłoszenia przyjęte |
| Karta budynku | US2.1 | V2, V3 | oznaczenie tekstem i ikoną |
| | US2.2 | V2, V3 | zdjęcie i data weryfikacji |
| Zaplanuj wizytę | US3.1 | V3 | stan „brak wejścia dostępnego” |
| | US3.2 | V1, V2, V3 | 2 dotknięcia |
| Zgłoszenie szybkie | US4.1 | V4 | 3 kroki, pozycja sama |
| | US4.2 | V4 | potwierdzenie bez sieci; wysyłka w tle bez ekranu |
| | US4.3 | V4 | 6 przycisków grup |
| | US4.4 | V4 | potwierdzenie |
| Pomoc | US5.1 | V5 | — |
| Pomiar pozycji | US6.1 | C1 | — |
| | US6.2 | C1 | podpowiedź budynku |
| | US6.3 | C1 | korekta na planie |
| | US6.4 | C1 | korekta na mapie |
| | US6.5 | C1 | mapa nieba |
| Formularz | US7.1 | C2 | walidacja |
| | US7.2 | C2 | pole piętra |
| | US7.3 | C2 | opis przy TB-29 |
| Moje zgłoszenia | US8.1 | C3 | status i powód odrzucenia |
| | US8.2 | C3 | statusy „wysłane” i „w weryfikacji”; moment zmiany ustala serwer |
| | US8.3 | C3, C2 | edycja |
| | US8.4 | V2, C7, C3 | start, formularz, status |
| Synchronizacja | US9.1 | C3, C4 | wskaźnik braku sieci na każdym ekranie |
| | US9.2 | C4 | samo uruchomienie bez ekranu |
| | US9.3 | C4 | wybór wersji |
| Weryfikacja | US10.1 | R2 | podobne zgłoszenia |
| | US10.2 | R2 | powód odrzucenia |
| | US10.3 | R2 | typ i uciążliwość |
| | US10.4 | R1, C3 | zniknięcie z kolejki ustala serwer |
| | US10.5 | R1 | filtr robi serwer |
| | US10.6 | R2, V1, V2 | skutek przyjęcia |
| Postęp | US11.1 | K1 | — |
| | US11.2 | K1 | przeliczenie robi serwer |
| | US11.3 | K1, C5 | cofnięcie z potwierdzeniem |
| Zaproszenie | US12.1 | K2, L1 | zaproszona osoba loguje się przez L1 albo L2 |
| | US12.2 | K2 | komunikat „konto istnieje” |
| Eksport | US13.1 | K3 | wybór formatu; treść pliku bez ekranu |
| | US13.2 | K3 | wybór formatu; treść pliku bez ekranu |
| | US13.3 | K3 | opis zawartości; filtr bez ekranu |
| Logowanie | US14.1 | L2, wszystkie ekrany ról | lista czynności tylko posiadanych ról |
| | US14.2 | V4, L1 | — |
| | US14.3 | L2 | lista ról; nadanie roli bez ekranu |
| Piętra obejrzane | US15.1 | C5 | — |
| | US15.2 | C5 | „Cofnij” tylko przy własnym |
| | US15.3 | C5 | znacznik w telefonie |
| Plan piętra | US16.1 | V2, C1 | przełącznik i plan |
| | US16.2 | V2, C2 | napis o braku planu, pole liczby |
| Oznaczenie wejścia | US17.1 | V2, V3 | trzy oznaczenia; nadanie przy imporcie bez ekranu |
| | US17.2 | V2, C6, R2, C3 | start, zmiana, sprawdzenie, status |
| | US17.3 | V2, V3, C6, R2 | data i źródło |

Wszystkie 52 kryteria mają ekran. Kryteria w całości serwerowe nie występują: każde ma co najmniej widoczny skutek.

## Źródła

1. WCAG 2.1 (W3C), m.in. 2.5.1 „Pointer Gestures”, 3.3.1 „Error Identification”, 3.3.3 „Error Suggestion”: <https://www.w3.org/TR/WCAG21/>
2. Treść komunikatów o błędach: co się stało i jak to naprawić (Nielsen Norman Group): <https://www.nngroup.com/articles/error-message-guidelines/>
3. Android: prośba o uprawnienie do lokalizacji i obsługa odmowy: <https://developer.android.com/develop/sensors-and-location/location/permissions>

Wewnętrzne źródła: [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), [Przypadki użycia](../wymagania/przypadki-uzycia.md), [Role i uprawnienia](../biznes/role.md), [Reguły biznesowe](../biznes/reguly.md), [Wymagania niefunkcjonalne](../wymagania/niefunkcjonalne.md), [Wizja](../biznes/wizja.md).

Dostęp do źródeł: 27.10.2026.
