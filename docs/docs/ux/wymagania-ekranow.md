# Wymagania do ekranów

Strona opisuje każdy ekran, zanim powstanie makieta. Element ekranu istnieje tylko wtedy, gdy wynika z kryterium akceptacji, reguły albo wymagania niefunkcjonalnego (decyzja 16). Historyjki i kryteria są na stronie [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), uprawnienia w [macierzy ról](../biznes/role.md#macierz-uprawnien).

Napisy przycisków i treść komunikatów w cudzysłowie pochodzą ze [słownika użytkownika](slownik-uzytkownika.md) (propozycje PW-1…PW-15) i są hipotezą do sprawdzenia w wywiadach. Opis elementu bez cudzysłowu używa pojęć ze [słownika pojęć](../biznes/slownik.md). Kolumna „Dokąd” podaje ekran docelowy i jest wejściem do mapy nawigacji.

## Lista ekranów

| Ekran | Nazwa robocza | Nagłówek na ekranie | Role z dostępem | Historyjki | Nowy w fazie B |
| --- | --- | --- | --- | --- | --- |
| V1 | Mapa kampusu | „Mapa kampusu” | wszystkie | US1, US10.6 | — |
| V2 | Karta budynku | „Budynek C-13” i nazwa z danych | wszystkie; czynności Zbierającego tylko RO-ZBI | US2, US16, US17, US3.2; start US8.4, US15, US17.2 | — |
| V3 | Którędy wejść | „Którędy wejść do C-13” | wszystkie | US3 | — |
| V4 | Zgłoszenie szybkie | „Zgłoś barierę” | RO-ODW i role pracownicze; RO-GOS przechodzi do L1 | US4, US14.2 | — |
| V5 | Pomoc | „Jak korzystać z aplikacji” | wszystkie | US5 | — |
| L1 | Prośba o logowanie | „Konto” | RO-GOS | US14.2, US14.3 | tak |
| L2 | Konto | „Konto”; role pracownicze: „Konto i praca” | RO-ODW i role pracownicze | US14.1, US14.3 | tak |
| C1 | Pomiar pozycji | „Pomiar pozycji” | RO-ZBI | US6 | — |
| C2 | Formularz zgłoszenia | jak nazwa robocza | RO-ZBI | US7, US8.3 | — |
| C3 | Moje zgłoszenia | „Moje zgłoszenia” | RO-ZBI, tylko własne | US8, US9.1 | — |
| C4 | Synchronizacja i wybór wersji | „Synchronizacja” | RO-ZBI | US9 | — |
| C5 | Piętra obejrzane | „Obejrzane piętra” | RO-ZBI, RO-KOO | US15, US11.3 | tak |
| C6 | Zmiana oznaczenia wejścia | „Zmień oznaczenie wejścia” | RO-ZBI | US17.2 | tak |
| C7 | Usunięcie bariery | „Zgłoś, że bariery nie ma” | RO-ZBI | US8.4 | tak |
| R1 | Kolejka weryfikacji | „Zgłoszenia do sprawdzenia” | RO-WER | US10.4, US10.5 | — |
| R2 | Sprawdzenie zgłoszenia | „Sprawdzenie zgłoszenia” | RO-WER | US10, US17.2 | — |
| K1 | Postęp inwentaryzacji | „Postęp inwentaryzacji” | RO-KOO | US11 | — |
| K2 | Zaproszenie | „Zaproś osobę” | RO-KOO | US12 | — |
| K3 | Eksport | „Pobierz eksport” | RO-KOO | US13 | — |

Nazwa robocza służy dokumentom i diagramom, a nagłówek to napis, który osoba czyta w aplikacji (PW-2, PW-14). Symbol „C-13” w nagłówkach V2 i V3 to przykład.

Odwiedzający ma uprawnienie „Wysyłka po powrocie sieci”, ale nie ma ekranu C4. Jego zgłoszenie szybkie wysyła się w tle, a on widzi tylko potwierdzenie (US4.2, US4.4).

## Wymagania wspólne

Dotyczą każdego ekranu. Przy ekranach podajemy tylko to, co do nich dochodzi.

| Wymaganie | Odwołanie |
| --- | --- |
| Nagłówek z nazwą ekranu i przyciskiem „Wstecz” bez ikony, zawsze widocznym w tym samym miejscu. Wyjątek: V1 jako ekran startowy. | zasada dla osób starszych, NFR-15 (WCAG 3.2.3) [1] |
| Wskaźnik braku sieci: napis „Brak internetu” i ikona. Gdy dane pochodzą z telefonu, obok jest napis „Dane z 12.11.2026”. Zbierający widzi zamiast niego „Ostatnia synchronizacja: 12.11.2026, 10:42”. | US9.1, NFR-10, NFR-21; PW-4 |
| Pasek dolny na V1, V2, V3, V5 i L2: „Mapa” (V1), „Zgłoś barierę” (V4), „Pomoc” (V5), „Konto” (L1 albo L2). Osoba z co najmniej jedną rolą pracowniczą widzi na czwartej pozycji, w tym samym miejscu, napis „Konto i praca” (PW-1). Pozycja prowadzi do L2. Na formularzach i krokach zgłoszenia paska nie ma. | US1, US4, US5.1, US14, US14.1; [mapa nawigacji](mapa-nawigacji.md), P1 |
| Jedna czynność główna na ekranie, wyróżniona położeniem i rozmiarem, nie kolorem. | NFR-21, zasada dla osób starszych |
| Cel dotyku co najmniej 48 × 48 dp. | NFR-17 |
| Kontrast tekstu, ikon statusu, ramek pól i fokusu według progów NFR-16. | NFR-16 |
| Tekst czytelny przy czcionce 200%, bez ucinania i nakładania. | NFR-20 |
| Status, oznaczenie wejścia i uciążliwość zawsze tekstem i ikoną. Uciążliwość 1–3 ma dla wszystkich ról napisy „Utrudnienie”, „Tylko z pomocą”, „Blokada”. Low-fi jest szare, a kolor nie niesie znaczenia. | NFR-21; PW-13 |
| Każdy element interaktywny ma polską nazwę, rolę i stan dla TalkBack. | NFR-18 |
| Każda czynność wykonywana gestem ma odpowiednik w przyciskach: gesty dwoma palcami i przesunięcia palcem po ścieżce, a także przeciąganie mapy i punktu. | NFR-15 (WCAG 2.5.1) [1]; przeciąganie: zasada dla osób starszych, NFR-3 |
| Ekran działa w orientacji pionowej i poziomej. | NFR-22 |
| Brak limitów czasu na odpowiedź użytkownika. | NFR-23 |
| Komunikat o błędzie mówi, co się stało i co zrobić, bez żargonu technicznego, w najwyżej 2 zdaniach. Aplikacja mówi o sobie „my”, np. „Wczytujemy”, „Nie znaleźliśmy”. | NFR-15 (WCAG 3.3.1, 3.3.3) [1], NFR-50 [2]; PW-9, PW-10 |
| Gość i Odwiedzający nie widzą słów „zgłoszenie”, „karta budynku”, „synchronizacja”, „sieć”. Zamiast nich są napisy ze słownika użytkownika, np. „zdjęcie bariery”, „opis budynku”, „Brak internetu”. | zasada dla osób starszych; PW-4, PW-5 |
| Bez wymaganej roli ekranu nie ma w aplikacji, a serwer odrzuca żądanie. | US14.1, NFR-33 |
| Po wysłaniu, zapisie albo decyzji formularz i kroki przed nim znikają z historii „Wstecz”. Dotyczy to V4 po „Wyślij”, C1 i C2 po „Zapisz”, C6 i C7 po „Wyślij do sprawdzenia”, C4 po wyborze wersji i R2 po decyzji. „Wstecz” z ekranu, który się wtedy otwiera, wraca tam, skąd zaczęła się praca, np. z C3 do L2 albo V2, z R1 do L2. | NFR-15 (WCAG 3.2.3) [1], zasada dla osób starszych; [mapa nawigacji](mapa-nawigacji.md), P2 |

Stany wspólne dla ekranów ról pracowniczych, które potrzebują serwera (R1, R2, K1, K2, K3). L1 ma własne stany, bez słowa „serwer”:

| Stan | Komunikat |
| --- | --- |
| Brak sieci | „Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie.” Przycisk „Spróbuj ponownie”. |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Spróbuj ponownie za kilka minut.” Przycisk „Spróbuj ponownie” (NFR-50, PW-10) |
| Brak uprawnień | Gdy rolę odebrano w trakcie pracy: „Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie.” Przycisk „Wróć do mapy” (V1). (US14.1, PW-10) |

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
| Bariery ze zgłoszeń przyjętych; uciążliwość napisem z wymagań wspólnych i kształtem | dane | US1.3, US10.6, NFR-21 | V2 (budynek bariery) |
| Pole symbolu budynku z podpowiedzią „Wpisz symbol budynku, np. C-13”; przyjmuje też zapis „c13” | czynność główna | US1.1, PW-8 | V2 |
| Przełącznik „Mapa” i „Lista budynków”; domyślnie wybrana „Mapa” | czynność | NFR-19 | lista na tym ekranie |
| Dotknięcie konturu albo budynku na liście | czynność | US2, US3.2 | V2 |
| Przyciski „Powiększ” i „Pomniejsz” oraz „W górę”, „W dół”, „W lewo”, „W prawo” | czynność | NFR-15 (WCAG 2.5.1, powiększanie dwoma palcami); przesuwanie: zasada dla osób starszych; PW-8 | — |
| Napis „© OpenStreetMap contributors” z odnośnikiem | dane | NFR-37 | strona praw autorskich OSM |
| Pasek dolny | czynność | wymagania wspólne | V4, V5, L1 / L2 |

Wybór wyniku wyszukiwania zaznacza budynek na mapie i otwiera V2. Po „Wstecz” z V2 widać zaznaczony budynek.

| Stan | Co widać |
| --- | --- |
| Zwykły | mapa z podkładem OpenStreetMap, konturami i barierami |
| Pierwsze uruchomienie | przed mapą pokazuje się instrukcja V5 (US5.1) |
| Ładowanie | „Wczytujemy mapę kampusu…”; kontury pojawiają się przed podkładem (NFR-11) |
| Pusty — brak wyniku | „Nie znaleźliśmy budynku »X«. Sprawdź symbol, np. C-13, albo wybierz budynek z listy.” Przycisk „Lista budynków” (UC1 3a) |
| Brak sieci | kontury i bariery z telefonu, bez podkładu, z napisami „Brak internetu” i „Dane z 12.11.2026” (NFR-10, NFR-39) |
| Brak sieci przy pierwszym uruchomieniu | „Pierwsze otwarcie aplikacji wymaga internetu. Połącz się z internetem, a mapa wczyta się sama.” (NFR-11) |
| Błąd — brak podkładu | kontury i bariery bez podkładu, napis „Mapa ulic chwilowo się nie wczytuje. Budynki i bariery nadal możesz oglądać.” (NFR-50) |

Dostępność i osoby starsze: lista budynków daje tę samą informację co mapa i jest drogą dla TalkBack (NFR-19). Wyszukiwanie przyjmuje symbol w każdym zapisie, bez wymogu myślnika (US1.1).

### V2 Karta budynku

| Pole | Treść |
| --- | --- |
| Cel | Osoba sprawdza, którym wejściem wejdzie i jakie bariery są w budynku. |
| Role | wszystkie; przyciski Zbierającego widzi tylko RO-ZBI (US14.1) |
| Realizuje | US2.1, US2.2, US16.1, US16.2, US17.1, US17.3, US10.6, US3.2; start US8.4, US15.1, US17.2 |
| Czynność główna | „Sprawdź, którędy wejść” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Budynek C-13” i nazwa z danych | dane | US2 | — |
| Każde wejście: nazwa, oznaczenie „Dostępne”, „Niedostępne” albo „Brak danych” tekstem i ikoną | dane | US2.1, US17.1, NFR-21 | — |
| Przy wejściu: data i źródło oznaczenia, „Sprawdzone na miejscu 12.11.2026” (obchód) albo „Z map i opisów, stan z 1.10.2026” (import) | dane | US17.3, BR-24, PW-3 | — |
| Zgłoszenia przyjęte pod nagłówkiem „Bariery w budynku”: typ bariery, uciążliwość, zdjęcie, data weryfikacji jako „Sprawdzone 12.11.2026” | dane | US2.2, BR-17, NFR-21, PW-3 | — |
| Typ bariery nazwą z klasyfikacji, np. „niesprawna winda”. Gość i Odwiedzający widzą prostsze napisy trzech typów. TB-20: „toaleta zamknięta, zastawiona lub trudna do dojścia”. TB-22: „brak planu dotykowego lub głosowego”. TB-28: „brak obniżonej lady lub miejsca dla wózka”. Te same napisy są na V1 i V3. | dane | BR-7, słownik użytkownika | — |
| Przełącznik pięter: „Piętro:” i etykiety z danych PWr (budynek szczegółowy) | czynność | US16.1 | plan na tym ekranie |
| „Plan piętra”: windy, schody, toalety, wejścia, zgłoszenia przyjęte | dane | US16.1 | — |
| Podpis „Źródło: Politechnika Wrocławska” pod planem | dane | NFR-40 | — |
| Napis „Nie mamy planu pięter tego budynku.” (budynek podstawowy); drugie zdanie „Bariery są na liście niżej.” tylko przy niepustej liście barier | dane | US16.2, BR-G1, PW-6, UC2 2a | — |
| Przycisk „Sprawdź, którędy wejść” | czynność główna | US3, US3.2, PW-2 | V3 |
| Zbierający: „Zmień oznaczenie” przy każdym wejściu | czynność | US17.2, BR-24 | C6 |
| Zbierający: „Zgłoś, że bariery nie ma” przy każdym zgłoszeniu przyjętym | czynność | US8.4, BR-19, PW-12 | C7 |
| Zbierający: „Popraw zgłoszenie” przy własnym zgłoszeniu przyjętym i przy każdym przyjętym zgłoszeniu szybkim | czynność | BR-3, BR-18 | C2 |
| Zbierający: „Oznacz obejrzane piętra” dla całego budynku | czynność | US15.1, PW-12 | C5 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5, L1 / L2 |

Bariery z planu piętra są też na liście pod planem, więc TalkBack nie potrzebuje planu (NFR-19). Gość i Odwiedzający nie widzą przycisków Zbierającego, więc ich karta jest krótka.

| Stan | Co widać |
| --- | --- |
| Zwykły — budynek szczegółowy | wejścia, przełącznik pięter, plan, lista barier |
| Zwykły — budynek podstawowy | wejścia, napis o braku planu, lista barier (US16.2, NFR-48) |
| Ładowanie | tekst od razu, w miejscu zdjęcia napis „Wczytujemy zdjęcie…” (NFR-12) |
| Pusty — brak zgłoszeń przyjętych | „Nie mamy jeszcze sprawdzonych barier w tym budynku. To nie znaczy, że barier tu nie ma.” W budynku podstawowym napis o braku planu jest wtedy bez zdania „Bariery są na liście niżej.” (UC2 2a, PW-6) |
| Brak sieci | karta z telefonu z napisami „Brak internetu” i „Dane z 12.11.2026”; zdjęcia, których nie ma w telefonie, zastępuje napis „Zdjęcie zobaczysz po połączeniu z internetem.” (NFR-10) |
| Brak sieci, budynku nie ma w telefonie | „Tego budynku nie ma jeszcze w telefonie. Połącz się z internetem, aby zobaczyć opis budynku.” Przycisk „Spróbuj ponownie” (NFR-10) |
| Błąd zdjęcia | „Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę.” Przycisk „Spróbuj ponownie” (NFR-50) |

Dostępność i osoby starsze: oznaczenie wejścia jest pierwszą informacją przy wejściu, a data i źródło stoją tuż pod nim (US17.3). „Brak danych” ma własną ikonę, inną niż „Niedostępne” (US17.1).

### V3 Którędy wejść {#v3-zaplanuj-wizyte}

| Pole | Treść |
| --- | --- |
| Cel | Osoba wie, którym wejściem wejdzie do budynku bez barier, albo wie, że takiego wejścia nie ma. |
| Role | wszystkie, bez logowania |
| Realizuje | US3.1, US3.2, US2.1, US2.2, US17.3 |
| Czynność główna | brak; ekran odpowiada na jedno pytanie, a dalej prowadzą tylko „Wstecz” (V2) i pasek dolny |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Którędy wejść do C-13” | dane | US3, PW-2 | — |
| Zdanie z odpowiedzią, np. „Budynek A-1 ma wejście dostępne: »nazwa wejścia«.”; nazwa wejścia pochodzi z danych | dane | US3, US3.1 | — |
| Każde wejście dostępne: nazwa, oznaczenie tekstem i ikoną | dane | US2.1, NFR-21 | — |
| Przy wejściu: data i źródło oznaczenia | dane | US17.3 | — |
| Zgłoszenia przyjęte przy tym wejściu ze zdjęciem i datą weryfikacji jako „Sprawdzone 12.11.2026” | dane | US2.2, UC4 4c, PW-3 | — |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5, L1 / L2 |

Droga z mapy: dotknięcie budynku (V2), „Sprawdź, którędy wejść” (V3). To 2 dotknięcia, a próg US3.2 to 3.

| Stan | Co widać |
| --- | --- |
| Zwykły | zdanie z odpowiedzią i wejścia dostępne |
| Brak wejścia dostępnego | „Budynek B-4 nie ma wejścia dostępnego. Wybierz »Wstecz«, aby w opisie budynku zobaczyć bariery przy każdym wejściu.” (US3.1, UC4 4a, PW-10) |
| Brak danych o wejściach | „Nie wiemy jeszcze, czy budynek B-4 ma wejście dostępne: brak danych o 2 z 3 wejść. Wybierz »Wstecz«, aby w opisie budynku zobaczyć każde wejście z datą.” (PW-10) Liczby zależą od budynku, także wtedy, gdy danych brak o wszystkich wejściach (UC4 4b, US17.1) |
| Brak sieci | dane z telefonu z napisami „Brak internetu” i „Dane z 12.11.2026” (NFR-10) |

Dostępność i osoby starsze: odpowiedź jest pełnym zdaniem na górze ekranu, a nie samym brakiem wyniku (US3.1). TalkBack czyta je jako pierwsze (NFR-18).

### V4 Zgłoszenie szybkie

| Pole | Treść |
| --- | --- |
| Cel | Odwiedzający przekazuje informację o barierze w 3 krokach, bez formularza. |
| Role | RO-ODW i role pracownicze; Gość niezalogowany przechodzi najpierw do L1 (US14.2) |
| Realizuje | US4.1, US4.2, US4.3, US4.4, US14.2 |
| Czynność główna | krok 1: „Zrób zdjęcie”, a po powrocie z kroku 2 „Dalej”; krok 2: wybór grupy; krok 3: „Wyślij” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Napis „Krok 1 z 3”, „Krok 2 z 3”, „Krok 3 z 3” | dane | US4.1 | — |
| Nagłówek „Zgłoś barierę”, ten sam napis co na pasku dolnym | dane | US4, PW-14 | — |
| Krok 1: przypomnienie „Nie fotografuj twarzy ani tablic rejestracyjnych.” | dane | NFR-25 | — |
| Krok 1: „Zrób zdjęcie” i „Zrób nowe zdjęcie” | czynność | US4.1, BR-8, PW-8 | aparat, potem krok 2 |
| Krok 1 po powrocie z kroku 2: miniatura zdjęcia i „Dalej” nad „Zrób nowe zdjęcie” | czynność główna | US4.1, heurystyka Nielsena 3 [5] | krok 2; zdjęcie i grupa zostają |
| Krok 2: pytanie „Czego dotyczy bariera?” i 6 dużych przycisków grup barier, sam tekst: „Wejście i drzwi”, „Schody i windy”, „Przejścia i chodniki”, „Toalety”, „Napisy i oznaczenia”, „Coś innego” | czynność | US4.3, NFR-17, PW-7 | krok 3 |
| Krok 3: miniatura zdjęcia i wybrana grupa | dane | US4.1 | — |
| Krok 3: napis „Dołączymy miejsce, w którym teraz stoisz.” | dane | US4.1, NFR-27 | — |
| Krok 3: „Wyślij” | czynność główna | US4.1 | potwierdzenie |
| Potwierdzenie: „Dziękujemy. Sprawdzimy zdjęcie, a jeśli bariera się potwierdzi, pokażemy ją na mapie.” | dane | US4.4, PW-5 | — |
| Potwierdzenie: „Wróć do mapy” | czynność | US4.4 | V1 |

Aplikacja nie pyta o typ bariery, uciążliwość, budynek ani piętro (US4.3, BR-14). Pozycję zaczyna ustalać po wejściu na ekran, w trakcie kroków 1 i 2 (NFR-27).

| Stan | Co widać |
| --- | --- |
| Zwykły | kroki 1–3 i potwierdzenie |
| Gość niezalogowany | zamiast aparatu ekran L1 (US14.2) |
| Brak zgody na lokalizację | przed aparatem: „Aby wysłać zdjęcie bariery, potrzebujemy jej miejsca. Sprawdzimy je tylko teraz i nie będziemy Cię śledzić.” Przyciski „Pozwól na lokalizację” i „Wróć do mapy” (UC7 2a, NFR-27) [4] |
| Odmowa zgody | „Bez lokalizacji nie wyślesz zdjęcia bariery. Możesz ją włączyć w ustawieniach telefonu.” Przyciski „Otwórz ustawienia” i „Wróć do mapy”. Aparat się nie otwiera, a aplikacja nie ponawia prośby sama (UC7 2a, BR-8) [4] |
| Zgoda tylko na przybliżoną lokalizację | przed aparatem: „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. Pozwól na dokładną lokalizację.” Przyciski „Pozwól na dokładną lokalizację” i „Wróć do mapy” (BR-8, BR-14) [3] |
| Lokalizacja wyłączona w telefonie | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby wysłać zdjęcie bariery.” Przyciski „Otwórz ustawienia” i „Wróć do mapy” (UC7 4c) |
| Ładowanie — pozycja jeszcze nieznana w kroku 3 | „Ustalamy, gdzie jesteś…”; „Wyślij” czeka, aż telefon poda pozycję albo zgłosi, że jej nie ustali. Aplikacja nie przerywa zgłoszenia upływem czasu (NFR-23) |
| Brak pozycji | telefon zgłosił, że nie ustali pozycji: „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” Przyciski „Spróbuj ponownie” i „Zrezygnuj” (PW-5). Zdjęcie i grupa zostają do ponownej próby (UC7 4c). „Zrezygnuj” otwiera potwierdzenie rezygnacji |
| Potwierdzenie rezygnacji | pojawia się przed usunięciem zrobionego zdjęcia. Wywołuje je „Zrezygnuj” w stanie „brak pozycji” albo „Wstecz” w kroku 1 po zrobieniu zdjęcia. Komunikat: „Jeśli zrezygnujesz, usuniemy zrobione zdjęcie i niczego nie wyślemy. Aby je zachować, wybierz »Wróć do zdjęcia«.” Przyciski „Tak, zrezygnuj” i „Wróć do zdjęcia”, tej samej wielkości. „Tak, zrezygnuj” usuwa zdjęcie, a zgłoszenie nie powstaje, jak przy rezygnacji z logowania (UC7 1b). Po „Zrezygnuj” aplikacja otwiera mapę (V1), a po „Wstecz” ekran sprzed V4. „Wróć do zdjęcia” wraca tam, skąd osoba przyszła, a zdjęcie i grupa zostają (heurystyki Nielsena 3 i 5 [5]; recenzja B4, uwaga 17) |
| Brak sieci | potwierdzenie: „Zapisaliśmy zdjęcie w telefonie. Wyślemy je sami, gdy wróci internet.” Przycisk „Wróć do mapy” (US4.2, UC7 7a) |
| Błąd zapisu | „W telefonie brakuje miejsca, więc nie zapisaliśmy zdjęcia bariery. Usuń niepotrzebne zdjęcia albo filmy i spróbuj ponownie.” Przycisk „Spróbuj ponownie”. Zdjęcie i grupa zostają (NFR-5, NFR-7) |

Dostępność i osoby starsze: każdy krok ma jedną czynność, a przyciski grup wypełniają szerokość ekranu (US4.3, NFR-17). Odwiedzający nie traci zdjęcia przy braku pozycji ani przy błędzie, a zdjęcie znika dopiero po potwierdzeniu rezygnacji.

„Wstecz” w krokach 2 i 3 wraca o krok, a zdjęcie i grupa zostają. W kroku 1 ze zdjęciem „Dalej” prowadzi znowu do kroku 2 bez nowego zdjęcia. „Wstecz” w kroku 1 po zrobieniu zdjęcia najpierw pyta o potwierdzenie rezygnacji.

„Wstecz” z potwierdzenia wraca do ekranu, z którego otwarto V4, a nie do kroku 3 (wymagania wspólne).

#### Jak ograniczamy przerwanie przez logowanie

US14.2 wymaga logowania przed zdjęciem, czyli w środku zadania. W granicach obecnych kryteriów ograniczamy to przerwanie tak:

| Środek | Odwołanie |
| --- | --- |
| Prośba o logowanie pojawia się od razu po „Zgłoś barierę”, zanim osoba coś zrobi. Nie traci zdjęcia ani wybranej grupy. | US14.2 |
| L1 mówi, co będzie dalej: „Po zalogowaniu od razu zrobisz zdjęcie. Całość to 3 krótkie kroki.” | US14.2, US4.1 |
| Po zalogowaniu albo rejestracji aplikacja sama wraca do kroku 1 V4, bez ponownego wybierania „Zgłoś barierę”. | US14.2, UC7 1a |
| Aplikacja pamięta zalogowanie, więc przerwanie zdarza się tylko przy pierwszym zgłoszeniu. | NFR-35 |
| Konto można założyć wcześniej, w spokojnej chwili: „Konto” na pasku dolnym prowadzi do L1. Instrukcja V5 mówi o tym jednym zdaniem. | US5.1, US14 |
| „Nie teraz” wraca do mapy bez komunikatu o błędzie i bez utraty danych. | UC7 1b |

Zostaje jedno przerwanie przy pierwszym zgłoszeniu. Warianty kolejności opisuje sekcja [Moment logowania](#moment-logowania).

### V5 Pomoc

| Pole | Treść |
| --- | --- |
| Cel | Nowa osoba wie, jak korzystać z aplikacji. |
| Role | wszystkie |
| Realizuje | US5.1 |
| Czynność główna | „Dalej” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Krótka instrukcja w kilku krokach: tekst, a obraz tylko wtedy, gdy pokazuje prawdziwy przycisk aplikacji, z opisem dla TalkBack | dane | US5.1, NFR-18 | — |
| Numer kroku, np. „Krok 2 z 4”; liczbę kroków ustali treść instrukcji | dane | US5.1 | — |
| Nagłówek „Jak korzystać z aplikacji” | dane | US5.1 | — |
| Zdanie w instrukcji: „Aby zgłosić barierę, potrzebne jest konto. Możesz je założyć teraz w »Konto«.” | dane | US5.1, US14.2, PW-5 | — |
| Zdanie w instrukcji: „Napis »Dostępne« przy wejściu znaczy, że wejdziesz bez pokonywania stopni: z poziomu chodnika, pochylnią albo podnośnikiem.” | dane | US5.1, US2.1, BR-24, PW-15 | — |
| „Dalej”, a w ostatnim kroku „Zacznij” | czynność główna | US5.1 | następny krok albo V1 |
| „Pomiń instrukcję” | czynność | US5.1, PW-8 | V1 |
| „Wstecz”: poprzedni krok instrukcji; w pierwszym kroku przy pierwszym uruchomieniu działa jak „Pomiń instrukcję” | czynność | wymagania wspólne, NFR-15, US5.1 | poprzedni krok albo V1; przy otwarciu z paska pierwszy krok wraca do ekranu, z którego otwarto V5 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, L1 / L2 |

| Stan | Co widać |
| --- | --- |
| Pierwsze uruchomienie | instrukcja otwiera się sama przed V1 (US5.1) |
| Otwarcie z pomocy | ta sama instrukcja z paska dolnego; nie otwiera się już sama (US5.1) |

Zdanie o napisie „Dostępne” objaśnia go raz, bo osoba starsza może odczytać to słowo jako „otwarte” (recenzja B6, spór 19).

Treść instrukcji jest w telefonie, więc ekran nie ma stanu braku sieci. Dostępność i osoby starsze: przejście dalej przyciskiem zamiast przesunięcia palcem (NFR-15, WCAG 2.5.1); brak automatycznego przewijania (NFR-23).

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
| Nagłówek „Konto”, ten sam napis co na pasku dolnym | dane | US14 | — |
| Zdanie „Aby zgłosić barierę, potrzebne jest konto. Konto chroni mapę przed fałszywymi informacjami.” | dane | US14.2, BR-8, PW-5 | — |
| Przy wejściu z V4: „Po zalogowaniu od razu zrobisz zdjęcie. Całość to 3 krótkie kroki.” | dane | US14.2, US4.1, PW-5 | — |
| „Zaloguj się” | czynność główna | US14, NFR-31 | strona Usługi logowania w przeglądarce systemowej; po zalogowaniu z V4 krok 1 V4, z „Konto” L2 |
| „Załóż konto” | czynność | US14.3, NFR-30 | rejestracja w przeglądarce z klauzulą informacyjną; potem jak po zalogowaniu |
| „Nie teraz” | czynność | UC7 1b | V1 |

Pola hasła w aplikacji nie ma (NFR-31). Po zalogowaniu z V4 aplikacja otwiera aparat w kroku 1 (US14.2).

Po zalogowaniu albo rejestracji z „Konto” otwiera się L2 z rolami i czynnościami (UC6 krok 3, US12.1).

| Stan | Co widać |
| --- | --- |
| Zwykły | zdanie i trzy przyciski |
| Brak sieci | „Logowanie wymaga internetu. Mapa i opisy budynków działają bez niego.” Przycisk „Wróć do mapy” (UC6 1a) |
| Błąd | „Logowanie się nie udało albo zostało przerwane. Sprawdź internet i spróbuj ponownie.” Przyciski „Spróbuj ponownie” i „Nie teraz” |
| Blokada konta | stan strony logowania w przeglądarce, nie ekranu L1: „Konto jest chwilowo zablokowane po kilku błędnych hasłach. Spróbuj ponownie później.” Treść ustawia się w Usłudze logowania (NFR-31, NFR-32) |

Dostępność i osoby starsze: strona logowania w przeglądarce też spełnia WCAG 2.1 AA (NFR-15). „Nie teraz” jest tak samo duże, jak „Zaloguj się”, więc rezygnacja nie wymaga szukania.

### L2 Konto

| Pole | Treść |
| --- | --- |
| Cel | Zalogowana osoba widzi swoje role i przechodzi do czynności swojej roli. |
| Role | RO-ODW i role pracownicze |
| Realizuje | US14.1, US14.3 |
| Czynność główna | rola pracownicza: wybór czynności z listy na górze ekranu; RO-ODW: brak czynności roli, ekran ma tylko dane konta |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Konto”, a u ról pracowniczych „Konto i praca”, ten sam napis co na pasku dolnym | dane | US14.1, PW-1 | — |
| Sekcja „Twoja praca”: tylko czynności posiadanych ról | czynność główna | US14.1, BR-1, PW-1 | — |
| Zbierający: „Zmierz i zgłoś barierę” | czynność | US6, PW-12 | C1 |
| Zbierający: „Moje zgłoszenia” | czynność | US8 | C3 |
| Weryfikator: „Zgłoszenia do sprawdzenia” | czynność | US10 | R1 |
| Koordynator: „Postęp inwentaryzacji” | czynność | US11 | K1 |
| Koordynator: „Zaproś osobę” | czynność | US12 | K2 |
| Koordynator: „Pobierz eksport” | czynność | US13 | K3 |
| Nazwa wyświetlana i lista ról, np. „Twoje role: Odwiedzający, Zbierający” | dane | BR-1, US14.3 | — |
| „Informacja o danych osobowych” | czynność | NFR-30 | stan „Klauzula informacyjna” na tym ekranie |
| „Wycofaj zgodę” | czynność | NFR-30, NFR-29 | potwierdzenie; po potwierdzeniu V1 |
| „Wyloguj się” | czynność | macierz uprawnień, PW-14 | V1 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5 |

Kolejność od góry: „Twoja praca”, dane konta, klauzula, „Wycofaj zgodę”, „Wyloguj się”. Osoba z rolą pracowniczą otwiera L2 głównie po to, by pracować, więc czynności są pierwsze. Lista czynności to zwykła lista przycisków z tekstem, a nie kafelki.

| Stan | Co widać |
| --- | --- |
| Zwykły — Odwiedzający | nagłówek „Konto”, dane konta, klauzula, „Wycofaj zgodę”, „Wyloguj się”; bez sekcji „Twoja praca” (US14.3) |
| Zwykły — rola pracownicza | nagłówek „Konto i praca”, na górze sekcja „Twoja praca”, pod nią dane konta, klauzula, „Wycofaj zgodę” i „Wyloguj się” |
| Klauzula informacyjna | treść klauzuli informacyjnej z podstawą przetwarzania, ta sama co przy rejestracji; „Wstecz” wraca do zwykłego stanu L2 (NFR-30) |
| Potwierdzenie wycofania zgody | „Wylogujemy Cię i w ciągu miesiąca usuniemy Twoje dane osobowe. Wysłane zdjęcia barier zostaną, ale bez Twoich danych.” Przyciski „Wycofaj zgodę” i „Zostaw zgodę” (PW-5). Po potwierdzeniu osoba jest wylogowana, a aplikacja otwiera V1, jak po „Wyloguj się” (NFR-29) |
| Brak sieci | role z ostatniego logowania; ekrany czynności pokazują własne stany braku sieci |

Dostępność i osoby starsze: klauzula jest najwyżej 2 dotknięcia od mapy: „Konto” (u ról pracowniczych „Konto i praca”), „Informacja o danych osobowych” (NFR-30). Klauzula jest stanem L2, a nie osobnym ekranem, bo NFR-30 wymaga jej w profilu.

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
| Postęp uśredniania 30 s, np. „Pomiar: zostało 12 s” | dane | US6.1 | — |
| Dokładność pozycji w metrach z dopiskiem „szacunek”, np. „Dokładność: 0,8 m (szacunek)” | dane | US6.1, NFR-2 | — |
| Liczba satelitów osobno dla każdego systemu GNSS, np. „Satelity: GPS 7, Galileo 5” | dane | US6.1, NFR-2 | — |
| Mapa nieba; system satelity rozróżniony kształtem i podpisem | dane | US6.5, NFR-21 | — |
| „Powtórz pomiar” | czynność | UC8 2a, NFR-23 | początek pomiaru |
| Podpowiedź budynku: „Czy to budynek C-13?” i „Potwierdź”, „Wybierz inny”, „Poza budynkiem” | czynność | US6.2, BR-G2 | „Wybierz inny”: lista budynków na C1 |
| Przesunięcie punktu na mapie, także przyciskami strzałek | czynność | US6.4, NFR-3, NFR-15 | — |
| Przesunięcie punktu na planie wybranego piętra (budynek szczegółowy) | czynność | US6.3, NFR-3 | — |
| Napis „Korekta ręczna” po przesunięciu punktu | dane | US6.3, US6.4 | — |
| Napis „© OpenStreetMap contributors”; pod planem „Źródło: Politechnika Wrocławska” | dane | NFR-37, NFR-40 | — |
| „Dalej” | czynność główna | US6 | C2 |

| Stan | Co widać |
| --- | --- |
| Zwykły | uśrednianie, dokładność, satelity, podpowiedź budynku |
| Ładowanie — szukanie satelitów | „Szukamy satelitów. Na otwartej przestrzeni pomiar będzie dokładniejszy.” Przycisk „Powtórz pomiar” (NFR-1, PW-9) |
| Wybór innego budynku | po „Wybierz inny” w miejscu pytania sekcja „Wybierz budynek”: budynki najbliżej punktu, od najbliższego, i „Poza budynkiem”. Wybór zamyka listę i pokazuje „Budynek [symbol]” z „Wybierz inny”. Lista działa bez sieci, z konturów w telefonie (US6.2, BR-G2, NFR-4). |
| Daleko od budynków PWr | „Poza budynkiem”, bez podpowiedzi i bez komunikatu o błędzie (BR-G4, NFR-49) |
| Budynek podstawowy | bez wyboru piętra i korekty na planie. W ich miejscu napis „Nie mamy planu pięter tego budynku.”, jak na V2. Korekta na mapie działa (US6.4, BR-G1, PW-6). |
| Brak zgody, zgoda tylko przybliżona albo lokalizacja wyłączona | jak w V4: prośba o zgodę albo o dokładną lokalizację, a po odmowie „Otwórz ustawienia” (NFR-27). Bez przycisku „Wróć do mapy”: rezygnacja przez „Wstecz” do ekranu, z którego otwarto C1, czyli L2, C2 albo C3 (NFR-15) |
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
| Piętro: liczba całkowita i pole „Półpiętro” (budynek podstawowy); nad polem „Nie mamy planu pięter tego budynku. Wpisz numer piętra, 0 to parter.” | czynność | US7.2, BR-G1, PW-6 | — |
| „Typ bariery”, pogrupowany w 6 grup barier | czynność | US7.1, BR-7 | — |
| „Uciążliwość”: „Utrudnienie”, „Tylko z pomocą”, „Blokada”, każda wartość z opisem i ikoną; pod nagłówkiem „Oceń dla osoby, dla której bariera jest najtrudniejsza.” | czynność | US7.1, NFR-21, PW-13 | — |
| Zdjęcie, co najmniej jedno: „Zrób zdjęcie” i „Zrób nowe zdjęcie”, z przypomnieniem o twarzach i tablicach | czynność | US7.1, NFR-25, PW-8 | aparat |
| Opis; obowiązkowy przy TB-29 „inna bariera” | czynność | US7.3 | — |
| Przy zmianie zgłoszenia przyjętego: „Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.” | dane | BR-18 | — |
| „Zapisz” | czynność główna | BR-7 | C3 |

Poza budynkiem pola piętra nie ma (BR-G4). C2 otwiera się z C1, z C3 przy edycji (US8.3) i z V2 przy zmianie zgłoszenia przyjętego (BR-18).

| Stan | Co widać |
| --- | --- |
| Zwykły — nowe zgłoszenie | puste pola, miejsce z C1 |
| Edycja | pola z zapisanymi wartościami (US8.3) |
| Błąd walidacji | nad formularzem „Brakuje typu bariery. Uzupełnij to pole, aby zapisać.”, a przy polu ten sam tekst (PW-10); zapis się nie wykonuje (US7.1, US7.3) |
| Brak sieci | zapis działa, zgłoszenie dostaje status „lokalne”, na ekranie „W telefonie” (US9.1, PW-12) |
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
| Rodzaj: zgłoszenie, „bariera usunięta” (na ekranie „Bariery już nie ma”) albo zmiana oznaczenia wejścia | dane | US8.4, US17.2 | — |
| Jeden status tekstem i ikoną: „W telefonie” (lokalne), „Wysłane”, „W weryfikacji”, „Przyjęte”, „Odrzucone”, „Archiwalne” | dane | US8.1, US8.2, NFR-21, PW-12 | — |
| Przy odrzuconym: powód odrzucenia | dane | US8.1, BR-16 | — |
| Znak „Zmiana czeka na wysłanie” przy niewysłanej edycji | dane | BR-9, [reguły, cykl życia zgłoszenia](../biznes/reguly.md) | — |
| Znak „Dwie wersje: wybierz jedną” przy konflikcie | czynność | US9.3 | C4 |
| Dotknięcie zgłoszenia przed decyzją albo odrzuconego | czynność główna | US8.3, BR-3 | C2; zmiana oznaczenia wejścia: C6; „bariera usunięta”: C7 |
| Przy powodzie „nieaktualne”: „Ktoś już zmienił to zgłoszenie. Twoja zmiana trafi do jego aktualnej wersji.” (przy zmianie oznaczenia wejścia: „Oznaczenie tego wejścia już się zmieniło. Sprawdź je i wyślij swoją zmianę ponownie.”) i przycisk „Wyślij ponownie” | czynność | BR-23, BR-24, PW-14 | C2; zmiana oznaczenia wejścia: C6; „bariera usunięta”: C7 |
| Przy „nieaktualne” po usunięciu bariery: „Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa.” Przycisk „Zmierz i zgłoś barierę” | dane, czynność | BR-23, PW-12 | C1 |
| Stan synchronizacji, np. „Czeka na wysłanie: 3” | czynność | US9.1 | C4 |
| „Zmierz i zgłoś barierę” | czynność | US6, PW-12 | C1 |

| Stan | Co widać |
| --- | --- |
| Zwykły | lista własnych zgłoszeń, najnowsze na górze |
| Pusty | „Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę.” Przycisk „Zmierz i zgłoś barierę” (C1, PW-12) |
| Ładowanie | lista z telefonu zostaje widoczna; obok napis „Sprawdzamy statusy…” |
| Brak sieci | lista z telefonu ze statusami z dnia ostatniej synchronizacji; nowe zgłoszenia „W telefonie” (US9.1) |
| Błąd | „Nie udało się sprawdzić statusów, ale zgłoszenia są bezpieczne w telefonie. Spróbuj ponownie za chwilę.” Przycisk „Spróbuj ponownie” (NFR-5, NFR-50, PW-10) |

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
| „Ostatnia synchronizacja: 12.11.2026, 10:42” | dane | NFR-10 | — |
| „Synchronizuj teraz”; synchronizacja i tak rusza sama | czynność | UC10, US9.2 | — |
| Ostrzeżenie „W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia.” | dane | NFR-7, PW-10 | — |
| Konflikt: napis „Dwie wersje: wybierz jedną. Druga wersja zostanie usunięta.”; przy zgłoszeniu przyjętym drugie zdanie „Przyjęte zgłoszenie zostanie, a Twoja wersja, jeśli ją wybierzesz, trafi do sprawdzenia jako nowe.” (UC11 3b) | dane | US9.3, BR-10, PW-12 | — |
| Konflikt: wersja własna i wersja z serwera naraz. Nad nimi napis „Różni się:” z nazwą pola, np. „Różni się: uciążliwość”, a w obu wersjach to pole jest pogrubione | dane | US9.3, NFR-21 | — |
| „Zostaw moją wersję” i „Weź wersję z serwera” | czynność główna | US9.3, BR-10 | C3 |
| „Zdecyduję później”; zmiana czeka w telefonie | czynność | BR-10 | C3 |

| Stan | Co widać |
| --- | --- |
| Zwykły — kolejka | liczba czekających i data ostatniej synchronizacji |
| Pusty | „Wszystko wysłane. Ostatnia synchronizacja: 12.11.2026, 10:42.” |
| Ładowanie | „Wysyłamy 2 z 5…” |
| Brak sieci | „Brak internetu. Wyślemy wszystko sami, gdy internet wróci.” (US9.1, US9.2) |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później.” (NFR-50) |
| Konflikt | napis o skutku, obie wersje i trzy przyciski wyboru (US9.3) |

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
| Przy wierszu: „Obejrzane” tekstem i ikoną albo „Nieobejrzane” samym tekstem | dane | US15.1, PW-11 | — |
| Przy znaczniku: „Twój znacznik” albo „Znacznik innej osoby” | dane | US15.2 | — |
| Przy znaczniku w telefonie: „Czeka na wysłanie” | dane | US15.3 | — |
| Zbierający: „Oznacz jako obejrzane” przy wierszu bez znacznika | czynność główna | US15.1 | — |
| Zbierający: „Cofnij znacznik” tylko przy własnym znaczniku | czynność | US15.2, PW-12 | — |
| Koordynator: „Cofnij znacznik” przy każdym znaczniku, z potwierdzeniem | czynność główna | US11.3, BR-20 | C5 z nowym stanem; do K1 przez „Wstecz” |

C5 otwiera Zbierający z karty budynku (V2), a Koordynator z postępu (K1). Koordynator nie stawia znaczników (BR-20), więc nie widzi „Oznacz jako obejrzane”.

| Stan | Co widać |
| --- | --- |
| Zwykły | lista pięter albo wiersz „Cały budynek” |
| Potwierdzenie cofnięcia (RO-KOO) | „Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający.” Przyciski „Cofnij znacznik” i „Zostaw znacznik” (BR-20, PW-12) |
| Brak sieci | „Znacznik zapisany w telefonie. Wyślemy go, gdy wróci internet.” Cofnięcie działa tak samo (US15.3, BR-20) |
| Błąd zapisu | „Nie udało się zapisać znacznika w telefonie. Spróbuj ponownie, a jeśli to nie pomoże, zwolnij miejsce.” Przycisk „Spróbuj ponownie” (NFR-7) |

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
| Obecne oznaczenie tekstem i ikoną, z datą i źródłem jak na V2 | dane | US17.3, NFR-21, PW-3 | — |
| Dwa duże przyciski: „Dostępne” i „Niedostępne” | czynność | US17.2, BR-24, NFR-17 | — |
| Przy wyborze obecnej wartości: „Potwierdzasz obecne oznaczenie. Zmieni się data i źródło.” | dane | BR-24, UC18 2a | — |
| Zdjęcie wejścia, obowiązkowe: „Zrób zdjęcie” i „Zrób nowe zdjęcie”, z przypomnieniem o twarzach i tablicach | czynność | US17.2, BR-24, NFR-25, PW-8 | aparat |
| Napis „Zmianę sprawdzi inna osoba. Do tego czasu karta budynku pokazuje obecne oznaczenie.” | dane | BR-4, BR-24 | — |
| „Wyślij do sprawdzenia” | czynność główna | US17.2 | C3 |

„Brak danych” nie jest do wyboru, bo nadaje je tylko import (BR-24).

| Stan | Co widać |
| --- | --- |
| Zwykły | obecne oznaczenie, wybór i zdjęcie |
| Błąd walidacji | „Dodaj zdjęcie wejścia, aby wysłać zmianę.” albo „Wybierz: dostępne czy niedostępne.” (BR-24) |
| Brak sieci | „Zmiana zapisana w telefonie. Wyślemy ją, gdy wróci internet.” Status „W telefonie” (US9.1) |
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
| Nowe zdjęcie miejsca, obowiązkowe: „Zrób zdjęcie” i „Zrób nowe zdjęcie” | czynność | US8.4, BR-19, NFR-25, PW-8 | aparat |
| Napis „Bariera zostanie na mapie, dopóki Weryfikator nie sprawdzi zgłoszenia.” | dane | US8.4 | — |
| „Wyślij do sprawdzenia” | czynność główna | US8.4 | C3 |

Uciążliwości ani typu się nie wybiera: przechodzą z poprzedniego zgłoszenia albo nie są wymagane (BR-19).

| Stan | Co widać |
| --- | --- |
| Zwykły | dane poprzedniego zgłoszenia i miejsce na nowe zdjęcie |
| Błąd walidacji | „Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.” (BR-19) |
| Brak sieci | „Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet.” Status „W telefonie” (US8.4, US9.1) |
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
| Przełącznik „Mapa” i „Lista”, w tej samej kolejności co na V1; domyślnie wybrana „Lista”. Lista daje tę samą informację. | czynność | UC14, NFR-19 | — |
| Napis „© OpenStreetMap contributors” na mapie | dane | NFR-37 | — |
| Sekcja „Wzięte przeze mnie” ze zgłoszeniami „w weryfikacji” tego Weryfikatora; w każdej pozycji „Otwórz” z ikoną | czynność | BR-11, UC14 2b, NFR-21 | R2 |
| „Weź do sprawdzenia” | czynność główna | US10.4, BR-11 | R2 |

Własnych zgłoszeń, także zmian „bariera usunięta” i zmian oznaczenia wejścia, lista nie pokazuje (US10.5, BR-4).

| Stan | Co widać |
| --- | --- |
| Zwykły | lista albo mapa zgłoszeń |
| Pusty | „Nie ma zgłoszeń do sprawdzenia. Zajrzyj tu później.” (PW-10) |
| Ładowanie | „Wczytujemy zgłoszenia…” |
| Zgłoszenie zajęte | „Ktoś inny wziął już to zgłoszenie. Wybierz inne.” Zgłoszenie znika z listy (UC14 2a, BR-11) |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne (UC14 1a, NFR-50) |

### R2 Sprawdzenie zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Weryfikator przyjmuje zgłoszenie albo odrzuca je z powodem, bez przepuszczenia błędu i duplikatu. |
| Role | RO-WER, tylko cudze zgłoszenia wzięte przez siebie (BR-4, BR-11) |
| Realizuje | US10.1, US10.2, US10.3, US10.6, US17.2 |
| Czynność główna | „Przyjmij” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Dane zgłoszenia: typ, uciążliwość napisem z wymagań wspólnych, budynek, piętro, miejsce; bez edycji przy pomiarze precyzyjnym | dane | BR-15, PW-13 | — |
| Zdjęcie | dane | US10 | — |
| Rola i pseudonim autora, np. „Zbierający, Autor 17” | dane | BR-5 | — |
| „Podobne zgłoszenia do 10 m”: typ, zdjęcie, odległość, piętro, status | dane | US10.1, BR-13 | — |
| Zgłoszenie szybkie: wybór „Typ bariery” i „Uciążliwość” z wartościami „Utrudnienie”, „Tylko z pomocą”, „Blokada” | czynność | US10.3, BR-14, PW-13 | — |
| „Zasłoń twarz lub tablicę” na zdjęciu | czynność | BR-15, NFR-25 | — |
| Zmiana oznaczenia wejścia: obecne oznaczenie z datą i źródłem, wybrane oznaczenie, nowe zdjęcie | dane | US17.2, BR-24 | — |
| Zmiana oznaczenia wejścia: tekstem i ikoną, czy oznaczenie jest „Bez zmian od wysłania”, czy „Zmienione po wysłaniu” przez inną przyjętą zmianę | dane | BR-23, BR-24, NFR-21 | — |
| „Bariera usunięta”: zdjęcie poprzedniego zgłoszenia obok nowego | dane | US10.6, BR-19 | — |
| „Bariera usunięta”: „Poprzednie zgłoszenie:” i jego status tekstem i ikoną, np. „Archiwalne” | dane | BR-19, BR-23, NFR-21 | — |
| „Przyjmij” | czynność główna | US10, US10.6 | R1 |
| „Odrzuć” z obowiązkowym polem „Powód odrzucenia”; gotowe powody „Duplikat” ze wskazaniem wcześniejszego zgłoszenia i „Nieaktualne” oraz „Inny powód” z polem „Opisz powód dla autora” | czynność | US10.2, BR-13, BR-16, BR-23, NFR-18 | R1 |

| Stan | Co widać |
| --- | --- |
| Zwykły | dane, zdjęcie, podobne zgłoszenia, decyzja |
| Pusty — brak podobnych | „W promieniu 10 m nie ma podobnych zgłoszeń.” (US10.1) |
| Ładowanie | tekst od razu, zdjęcia z napisem „Wczytujemy zdjęcie…” |
| Błąd walidacji — przyjęcie | „Ustal typ bariery i uciążliwość, aby przyjąć zgłoszenie.” (US10.3) |
| Błąd walidacji — odrzucenie | „Podaj powód odrzucenia, aby autor wiedział, co poprawić.” (US10.2) |
| Nowa wersja | „Autor zmienił zgłoszenie. Sprawdź nową wersję przed decyzją.” Pod komunikatem „Zmienione:” i nazwy zmienionych pól, np. „Zmienione: uciążliwość” (BR-3, BR-15, UC14 5e, PW-10) |
| Brak sieci | „Decyzja wymaga internetu, a zgłoszenie czeka w »Wziętych przeze mnie«. Połącz się z internetem i podejmij decyzję ponownie.” (BR-11, PW-10) |
| Błąd serwera, brak uprawnień | stany wspólne |

Dostępność: podobne zgłoszenia mają odległość tekstem, np. „5 m”, a nie tylko punkt na mapie (NFR-19).

## Koordynator

### K1 Postęp inwentaryzacji

| Pole | Treść |
| --- | --- |
| Cel | Koordynator wie, gdzie zespół musi jeszcze pójść. |
| Role | RO-KOO |
| Realizuje | US11.1, US11.2, US11.3, US11.4 |
| Czynność główna | dotknięcie budynku — otwarcie jego pięter |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Lista budynków z symbolem | dane | US11.1 | — |
| Budynek szczegółowy: „4 z 5 pięter, 80%” | dane | US11.1, US11.2, BR-21 | — |
| Budynek podstawowy: „Obejrzany” tekstem i ikoną albo „Nieobejrzany” samym tekstem | dane | US11.1, PW-11 | — |
| Liczby: „Czeka na sprawdzenie: 3” i „Przyjęte: 4”, jak w scenariuszu `@US11.1` | dane | US11.1, BR-21 | — |
| Budynek szczegółowy: „Wejścia sprawdzone na miejscu: 4 z 6” | dane | US11.4, BR-24, PW-3 | — |
| Dotknięcie budynku; w ramce „Otwórz” z ikoną | czynność główna | US11.3, NFR-21 | C5 |
| „Odśwież” i godzina odczytu „Stan z 10:42” | czynność | US11.2 | — |

Sortowania ani filtrów nie ma, bo nie wynikają z żadnego kryterium (decyzja 16). Wiersz o wejściach pozwala odczytać miarę M-1 z [wizji](../biznes/wizja.md) bez liczenia na kartach budynków (decyzja 38). Wejście liczy się dopiero po przyjęciu zmiany oznaczenia, bo wtedy dostaje źródło „obchód” (BR-24).

| Stan | Co widać |
| --- | --- |
| Zwykły | lista budynków z postępem i liczbami |
| Ładowanie | „Wczytujemy postęp…” |
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
| Wybór „Rola”: Zbierający, Weryfikator, Koordynator | czynność | US12.1, BR-2 | — |
| „Wyślij zaproszenie” | czynność główna | US12.1 | potwierdzenie na tym ekranie |

| Stan | Co widać |
| --- | --- |
| Zwykły | pole adresu i wybór roli |
| Sukces | „Zaproszenie wysłane na podany adres.” (US12.1) |
| Sukces — konto istnieje | „To konto już istnieje. Dodaliśmy mu rolę Weryfikatora, dotychczasowe role zostają.” (US12.2) |
| Błąd walidacji | „Adres e-mail jest niepełny, np. brakuje znaku @. Popraw go i wyślij ponownie.” albo „Wybierz rolę.” (PW-10) |
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
| Ładowanie | „Przygotowujemy plik…” |
| Pusty | „Nie ma jeszcze przyjętych barier. Pobrany plik będzie pusty.” Pobranie działa (UC17 2a) |
| Sukces | „Plik zapisany w pobranych plikach.” |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne (UC17 1a) |

## Decyzje fazy B

| Temat | Rozwiązanie | Dlaczego |
| --- | --- | --- |
| Ekran oznaczenia piętra (US15, UC13) | Nowy ekran C5 „Piętra obejrzane” z nagłówkiem „Obejrzane piętra”. Zbierający otwiera go z karty budynku (V2), Koordynator z postępu (K1). | C1 służy jednemu pomiarowi bariery, a piętro oznacza się po obejściu całego piętra, często bez żadnej bariery. W budynku podstawowym C1 nie ma pięter. Jeden ekran obsługuje też cofnięcie przez Koordynatora (US11.3). |
| Start zmiany „bariera usunięta” (US8.4, UC12) | Przycisk „Zgłoś, że bariery nie ma” przy zgłoszeniu przyjętym na V2, widoczny tylko dla RO-ZBI. Prowadzi do nowego ekranu C7. | Zbierający stoi przy barierze i widzi ją na karcie budynku. C2 wymaga typu i uciążliwości (US7.1), a C7 tylko nowego zdjęcia (BR-19). Osobny ekran nie miesza tych reguł. |
| Start zmiany zgłoszenia przyjętego (UC9 2c) | Przycisk „Popraw zgłoszenie” na V2, tylko przy własnym zgłoszeniu i przy zgłoszeniu szybkim. Otwiera C2 z wartościami. | To ta sama karta co dla „bariera usunięta”, a formularz ten sam co przy pierwszym zapisie (BR-18). |
| Zmiana oznaczenia wejścia (US17.2, UC18) | Przycisk „Zmień oznaczenie” przy wejściu na V2, tylko dla RO-ZBI. Prowadzi do nowego ekranu C6. | Lista wejść z obecnym oznaczeniem jest na V2, a Zbierający stoi przy wejściu. C6 ma dwa przyciski i zdjęcie, bez typu i uciążliwości (BR-24). |
| Zgłoszenie szybkie bez zgody na lokalizację (UC7 2a) | Prośba o zgodę przed aparatem. Po odmowie komunikat i „Otwórz ustawienia”; aparat się nie otwiera. | Bez pozycji zgłoszenia nie da się wysłać (BR-8). Senior nie powinien robić zdjęcia, które i tak się nie wyśle. Wskazanie miejsca na mapie byłoby czwartym krokiem (US4.1). |
| Zgłoszenie szybkie bez pozycji (UC7 4c) | Pozycja ustala się w trakcie kroków 1 i 2. Gdy przy „Wyślij” jej brak, komunikat z radą i „Spróbuj ponownie”; zdjęcie i grupa zostają. | Zapis bez pozycji łamie BR-8, a pozycja dopisana później byłaby z innego miejsca. Odwiedzający nie traci pracy, a limit czasu nie istnieje (NFR-23). |
| Komunikat „brak wejścia dostępnego” (US3.1, UC4 4a, 4b) | Dwa różne zdania na V3: „nie ma wejścia dostępnego” i „nie wiemy jeszcze”. Treść w tabeli stanów V3. | „Brak danych” to co innego niż „niedostępne” (US17.1). Senior nie może zrezygnować z wizyty przez brak danych. |
| Brak wyniku wyszukiwania (UC1 3a) | Zdanie z przykładem symbolu i przycisk „Lista budynków”. | Komunikat mówi, co zrobić, a lista działa też z TalkBack (NFR-19). |
| Karta bez zgłoszeń przyjętych (UC2 2a) | Zdanie, że brak zgłoszeń nie znaczy braku barier. | Wynika z P-4 procesu TO-BE: pusta lista nie może brzmieć jak „wszystko dostępne” (N2). |
| Eksport bez zgłoszeń przyjętych (UC17 2a) | Komunikat, a pobranie pustego pliku działa. | Pusty plik GeoJSON jest poprawny (NFR-43) i nie wymaga osobnej reguły na serwerze. |
| Znak „zmiana czeka na wysłanie” (reguły, cykl życia) | Dodatkowy znak przy zgłoszeniu na C3, obok statusu. | Status się nie zmienia, a Zbierający musi wiedzieć, że edycja jest tylko w telefonie (BR-9). |
| Odczyt miary M-1 (decyzja 38) | W K1 przy budynku szczegółowym wiersz „Wejścia sprawdzone na miejscu: x z y” i kryterium US11.4. | Bez tego M-1 nie da się zmierzyć z jednego ekranu. Dane już są: każde wejście ma źródło oznaczenia (BR-24). |
| Moment logowania (decyzja 38) | US14.2 bez zmian: logowanie przed zdjęciem, a przerwanie łagodzą środki z V4 i L1. | Zmianę kolejności sprawdzimy dopiero w teście z osobami starszymi. |
| Wejście do pracy ról pracowniczych (mapa nawigacji, P1) | Czwarta pozycja paska dolnego dalej prowadzi do L2. Osoba z rolą pracowniczą widzi na niej „Konto i praca” (słownik użytkownika, PW-1), a L2 zaczyna się sekcją „Twoja praca”. Gość i Odwiedzający widzą „Konto” bez zmian. | Osoba szukająca „Moich zgłoszeń” nie skojarzy ich z kontem. Bez nowego ekranu i bez piątej pozycji liczba dotknięć, cele dotyku (NFR-17) i droga do klauzuli (NFR-30) się nie zmieniają. Jedna osoba widzi zawsze tę samą nazwę w tym samym miejscu (WCAG 3.2.3, 3.2.4). |
| Historia „Wstecz” po wysłaniu (mapa nawigacji, P2) | Wysłany, zapisany albo rozstrzygnięty formularz znika z historii „Wstecz” (wymagania wspólne). | Bez tego powrót do mapy z C3 wymaga do 5 dotknięć, a osoba widzi formularz, który już wysłała. Z potwierdzenia V4 nie da się też wrócić do kroku 3 i wysłać zgłoszenia drugi raz. |
| Ekran po zalogowaniu (mapa nawigacji, P3) | Z V4: krok 1 V4. Z „Konto”: L2. | Po zalogowaniu aplikacja pokazuje czynności ról osoby (UC6 krok 3). Osoba zaproszona przez Koordynatora od razu widzi swoje role (US12.1). |
| Przyciski bez ekranu docelowego (V4, L2) | Potwierdzone „Zrezygnuj” w stanie „brak pozycji” i potwierdzone „Wycofaj zgodę” prowadzą do V1. „Informacja o danych osobowych” otwiera stan L2 „Klauzula informacyjna”. | Rezygnacja ze zgłoszenia działa jak w UC7 1b, a wycofanie zgody wylogowuje jak „Wyloguj się” (NFR-29). NFR-30 wymaga klauzuli w profilu, więc nie potrzeba nowego ID. |
| Rezygnacja ze zdjęcia w V4 (recenzja B4, uwaga 17) | „Zrezygnuj” w stanie „brak pozycji” i „Wstecz” w kroku 1 po zrobieniu zdjęcia najpierw pytają o potwierdzenie. Stan „Potwierdzenie rezygnacji” ma przyciski „Tak, zrezygnuj” i „Wróć do zdjęcia”. | Przypadkowe dotknięcie nie usuwa zdjęcia bez cofnięcia (heurystyka Nielsena 5), a osoba może wycofać się z decyzji (heurystyka 3) [5]. Pytanie nie ma limitu czasu (NFR-23). |
| Powrót do kroku 1 V4 ze zdjęciem (recenzja B5) | Krok 1 ze zrobionym zdjęciem pokazuje miniaturę, „Dalej” jako czynność główną i „Zrób nowe zdjęcie”. „Dalej” prowadzi do kroku 2, a wybrana grupa zostaje. | Bez „Dalej” osoba, która cofnęła się ze zdjęciem, musi zrobić nowe zdjęcie albo zrezygnować. Swobodny powrót bez utraty pracy to heurystyka Nielsena 3 [5]. |
| Napis budynku podstawowego bez barier (recenzja B5) | Drugie zdanie napisu o braku planu, „Bariery są na liście niżej.”, pojawia się tylko przy niepustej liście barier. | Przy pustej liście zdanie odsyłałoby do barier, których nie ma, obok zdania „Nie mamy jeszcze sprawdzonych barier…” (UC2 2a). |
| „Wstecz” w V5 przy pierwszym uruchomieniu (recenzja B5) | „Wstecz” wraca do poprzedniego kroku instrukcji. W pierwszym kroku przy pierwszym uruchomieniu działa jak „Pomiń instrukcję” i otwiera V1. | Przy pierwszym uruchomieniu nie ma ekranu przed V5, a „Wstecz” musi mieć cel (wymagania wspólne, NFR-15). |
| Rada w stanach „brak” V3 (recenzja B5) | Druga część komunikatu wskazuje przycisk: „Wybierz »Wstecz«, aby w opisie budynku zobaczyć…”. | Rada „Wróć do opisu budynku” odsyłała do przycisku, którego nazwy nie ma na ekranie (heurystyka Nielsena 6 [5]). |

## Moment logowania

Mapa ścieżki osoby starszej pokazuje, że logowanie przed zdjęciem przerywa zadanie w najgorszym momencie. Środki z V4 zmniejszają to przerwanie, ale go nie usuwają. Decyzja 38 zostawia US14.2 bez zmian do testu z osobami starszymi.

| Wariant | Co się zmienia | Za | Przeciw |
| --- | --- | --- | --- |
| A. Bez zmian (wybrany, decyzja 38) | US14.2 zostaje: logowanie przed zdjęciem, ze środkami z V4. | Osoba nie robi zdjęcia, które może się nie wysłać. Brak nowych reguł. | Przy pierwszym zgłoszeniu bariera może zniknąć z oczu, zanim osoba się zaloguje. |
| B. Logowanie przed wysłaniem | Gość robi zdjęcie i wybiera grupę, a logowanie pojawia się przy „Wyślij”. Zdjęcie czeka w telefonie. Wymaga zmiany US14.2, BR-8, UC7 1a i trzech scenariuszy `@US14.2`. | Zdjęcie powstaje, gdy bariera jest przed osobą. | Niezalogowany telefon trzyma zdjęcie bez konta; rezygnacja z logowania wyrzuca wykonaną pracę. |

Jeśli test pokaże rezygnacje przy L1, wracamy do wariantu B.

## Pokrycie kryteriów

Każde kryterium akceptacji ma ekran albo dopisek „bez ekranu”, gdy dotyczy tylko serwera lub pliku.

| Historyjka | Kryterium | Ekran | Uwagi |
| --- | --- | --- | --- |
| Wyszukiwanie budynku | US1.1 | V1 | pole symbolu budynku |
| | US1.2 | V1, V2 | bez logowania |
| | US1.3 | V1, V2 | tylko zgłoszenia przyjęte |
| Karta budynku | US2.1 | V2, V3 | oznaczenie tekstem i ikoną |
| | US2.2 | V2, V3 | zdjęcie i data weryfikacji |
| Którędy wejść | US3.1 | V3 | stan „brak wejścia dostępnego” |
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
| | US11.4 | K1 | wejścia ze źródłem „obchód”; liczbę liczy serwer |
| Zaproszenie | US12.1 | K2, L1, L2 | zaproszona osoba loguje się przez L1 i widzi swoje role w L2 |
| | US12.2 | K2 | komunikat „konto istnieje” |
| Eksport | US13.1 | K3 | wybór formatu; treść pliku bez ekranu |
| | US13.2 | K3 | wybór formatu; treść pliku bez ekranu |
| | US13.3 | K3 | opis zawartości; filtr bez ekranu |
| Logowanie | US14.1 | L2, wszystkie ekrany ról | lista czynności tylko posiadanych ról |
| | US14.2 | V4, L1 | — |
| | US14.3 | L2 | lista ról; nadanie roli bez ekranu |
| Piętra obejrzane | US15.1 | C5 | — |
| | US15.2 | C5 | „Cofnij znacznik” tylko przy własnym |
| | US15.3 | C5 | znacznik w telefonie |
| Plan piętra | US16.1 | V2, C1 | przełącznik i plan |
| | US16.2 | V2, C2 | napis o braku planu, pole liczby |
| Oznaczenie wejścia | US17.1 | V2, V3 | trzy oznaczenia; nadanie przy imporcie bez ekranu |
| | US17.2 | V2, C6, R2, C3 | start, zmiana, sprawdzenie, status |
| | US17.3 | V2, V3, C6, R2 | data i źródło |

Wszystkie 53 kryteria mają ekran. Kryteria w całości serwerowe nie występują: każde ma co najmniej widoczny skutek.

## Źródła

1. WCAG 2.1 (W3C), m.in. 2.5.1 „Pointer Gestures”, 3.3.1 „Error Identification”, 3.3.3 „Error Suggestion”: <https://www.w3.org/TR/WCAG21/>
2. Treść komunikatów o błędach: co się stało i jak to naprawić (Nielsen Norman Group): <https://www.nngroup.com/articles/error-message-guidelines/>
3. Android: lokalizacja przybliżona i dokładna, prośba o dokładną lokalizację: <https://developer.android.com/develop/sensors-and-location/location/permissions>
4. Android: prośba o uprawnienie w kontekście, wyjaśnienie z możliwością rezygnacji, odmowa bez ponawiania: <https://developer.android.com/training/permissions/requesting>
5. Nielsen J., 10 Usability Heuristics for User Interface Design (Nielsen Norman Group): <https://www.nngroup.com/articles/ten-usability-heuristics/>

Wewnętrzne źródła: [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), [Przypadki użycia](../wymagania/przypadki-uzycia.md), [Role i uprawnienia](../biznes/role.md), [Reguły biznesowe](../biznes/reguly.md), [Wymagania niefunkcjonalne](../wymagania/niefunkcjonalne.md), [Wizja](../biznes/wizja.md).

Dostęp do źródeł: 27.10.2026.
