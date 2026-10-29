# Wymagania do ekranów

Strona opisuje każdy ekran, zanim powstanie makieta. Element ekranu istnieje tylko wtedy, gdy wynika z kryterium akceptacji, reguły albo wymagania niefunkcjonalnego (decyzja 16). Historyjki i kryteria są na stronie [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), uprawnienia w [macierzy ról](../biznes/role.md#macierz-uprawnien).

Napisy przycisków i treść komunikatów w cudzysłowie pochodzą ze [słownika użytkownika](slownik-uzytkownika.md) (propozycje PW-1…PW-15) i są hipotezą do sprawdzenia w wywiadach. Opis elementu bez cudzysłowu używa pojęć ze [słownika pojęć](../biznes/slownik.md). Kolumna „Dokąd” podaje ekran docelowy i jest wejściem do mapy nawigacji.

## Lista ekranów

| Ekran | Nazwa robocza | Nagłówek na ekranie | Role z dostępem | Historyjki | Nowy w fazie B |
| --- | --- | --- | --- | --- | --- |
| V1 | Mapa kampusu | „Mapa kampusu” | wszystkie | US1, US10.6, US16.1 | — |
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
| Nagłówek z nazwą ekranu i przyciskiem „Wstecz”: strzałka w lewo i słowo, zawsze w tym samym miejscu. Przy przewijaniu w dół może się schować razem z górnym paskiem i wraca przy przewijaniu w górę (reguła 10). Wyjątek: V1 jako ekran startowy. | zasada dla osób starszych, NFR-15 (WCAG 3.2.3) [1]; PW-11 |
| Wskaźnik braku sieci to jedna linia: ikona i napis „Bez internetu · dane z 12.11.2026”. Linia zostaje, bo dane w telefonie mogą być nieaktualne. Część „dane z …” przechodzi do drugiego wiersza w całości. Zbierający widzi w tej linii „Ostatnia synchronizacja: 12.11.2026, 10:42” zamiast „dane z …”. | US9.1, NFR-10, NFR-21; PW-4; decyzja 54 |
| Pasek dolny na V1, V2, V3, V5 i L2: „Mapa” (V1), „Zgłoś barierę” (V4), „Pomoc” (V5), „Konto” (L1 albo L2). Osoba z co najmniej jedną rolą pracowniczą widzi na czwartej pozycji, w tym samym miejscu, napis „Konto i praca” (PW-1). Pozycja prowadzi do L2. Na formularzach i krokach zgłoszenia paska nie ma. | US1, US4, US5.1, US14, US14.1; [mapa nawigacji](mapa-nawigacji.md), P1 |
| Pasek dolny nigdy się nie chowa, także przy przewijaniu. Przy przewijaniu w dół może się schować tylko górny pasek, w którym jest „Wstecz”. | NFR-15 (WCAG 3.2.3) [1], NFR-20; decyzja 53 |
| Przy dużej czcionce elementy przypięte razem zajmują najwyżej 25% wysokości okna w bieżącej orientacji (próg zespołu). Czynności, które by się nie zmieściły, stoją w treści według reguły 10 sekcji [Duża czcionka](#duza-czcionka). | NFR-20, NFR-21, WCAG 2.4.11 [7]; decyzja 53 |
| Arkusz bariery i arkusz znalezionej sali na V1 oraz panele „Legenda” i „ⓘ” zamyka obrysowany przycisk ze słowem „Zamknij” w ich nagłówku, bez znaku „×”. Arkusz budynku (V2) ma zamiast niego „Wstecz”. | NFR-17, NFR-18; PW-11; decyzja 53 |
| Komunikat ostrzegawczy przed czynnością: obrysowane koło ze znakiem „!”, pogrubione słowo „Ważne”, wymaganie i jedno zdanie z powodem. Ramka, koło i słowo mają kolor roli `error` z [tokenów](tokeny.md#zasady), ale znaczenie niosą kształt i słowo. Trójkąt jest zarezerwowany dla uciążliwości „Utrudnienie”, więc komunikat go nie używa. | NFR-16, NFR-21; PW-13; decyzja 54 |
| Jedna czynność główna na ekranie, wyróżniona położeniem, rozmiarem i wypełnieniem, nigdy samym kolorem. | NFR-21, zasada dla osób starszych |
| Cel dotyku co najmniej 48 × 48 dp. | NFR-17 |
| Kontrast tekstu, ikon statusu, ramek pól i fokusu według progów NFR-16. | NFR-16 |
| Tekst czytelny przy czcionce 200%, bez ucinania i nakładania. Przy skali czcionki co najmniej 1,5 obowiązują reguły z sekcji [Duża czcionka](#duza-czcionka). | NFR-20 |
| Status, oznaczenie wejścia i uciążliwość zawsze tekstem i ikoną. Uciążliwość 1–3 ma dla wszystkich ról napisy „Utrudnienie”, „Tylko z pomocą”, „Blokada”. Low-fi jest szare, a kolor nie niesie znaczenia. | NFR-21; PW-13 |
| Każdy element interaktywny ma polską nazwę, rolę i stan dla TalkBack. | NFR-18 |
| Każda czynność wykonywana gestem ma odpowiednik w przyciskach: gesty dwoma palcami i przesunięcia palcem po ścieżce, a także przeciąganie mapy i punktu. | NFR-15 (WCAG 2.5.1) [1]; przeciąganie: zasada dla osób starszych, NFR-3 |
| Ekran działa w orientacji pionowej i poziomej. W szerokim oknie układ spełnia NFR-51. | NFR-22, NFR-51 |
| Brak limitów czasu na odpowiedź użytkownika. | NFR-23 |
| Komunikat o błędzie mówi, co się stało i co zrobić, bez żargonu technicznego, w najwyżej 2 zdaniach. Aplikacja mówi o sobie „my”, np. „Wczytujemy”, „Nie znaleźliśmy”. | NFR-15 (WCAG 3.3.1, 3.3.3) [1], NFR-50 [2]; PW-9, PW-10 |
| Gość i Odwiedzający nie widzą słów „zgłoszenie”, „karta budynku”, „synchronizacja”, „sieć”. Zamiast nich są napisy ze słownika użytkownika, np. „zdjęcie bariery”, „opis budynku”, „Bez internetu”. | zasada dla osób starszych; PW-4, PW-5 |
| Bez wymaganej roli ekranu nie ma w aplikacji, a serwer odrzuca żądanie. | US14.1, NFR-33 |
| Po wysłaniu, zapisie albo decyzji formularz i kroki przed nim znikają z historii „Wstecz”. Dotyczy to V4 po „Wyślij”, C1 i C2 po „Zapisz”, C6 i C7 po „Wyślij do sprawdzenia”, C4 po wyborze wersji i R2 po decyzji. „Wstecz” z ekranu, który się wtedy otwiera, wraca tam, skąd zaczęła się praca, np. z C3 do L2 albo V2, z R1 do L2. | NFR-15 (WCAG 3.2.3) [1], zasada dla osób starszych; [mapa nawigacji](mapa-nawigacji.md), P2 |

Układ ekranów w szerokim oknie z NFR-51 nie ma jeszcze makiet i zostanie zaprojektowany osobno. Do tego czasu obowiązuje sama miara NFR-51.

### Duża czcionka {#duza-czcionka}

Reguły włączają się, gdy skala czcionki w telefonie wynosi co najmniej 1,5. Reguły 1 i 9 obowiązują przy każdej czcionce. Android 14 powiększa duży tekst mniej niż mały, więc zrzuty do kontroli NFR-20 robimy z tym samym skalowaniem [8].

| Nr | Reguła | Odwołanie |
| --- | --- | --- |
| 1 | Data i źródło oceny wejść to jeden krótki wiersz bez ikony nad wejściami, np. „Z map i opisów, 18.12.2023”. Gdy wejścia mają różne źródła, taki wiersz stoi przy każdym wejściu. | US17.3, BR-24, decyzja 48 |
| 2 | „Źródło: Politechnika Wrocławska” stoi raz na dole V2, a na mapie tylko przy planie piętra. Grupy pięter go nie powtarzają. | NFR-40 |
| 3 | Podpis OpenStreetMap jest tylko na ekranie z mapą: jeden wiersz „© OpenStreetMap contributors” w rogu mapy, jako odnośnik. | NFR-37 |
| 4 | Napisy na mapie i na mapie nieba (C1) rosną z czcionką, najwyżej 1,4 raza. Mapa chowa napisy, które się nie mieszczą, i nie nakłada ich. | NFR-20 |
| 5 | V2 otwiera się od razu na cały ekran, bez fragmentu mapy u góry. Na górze jest tylko „Wstecz”, a „Sprawdź, którędy wejść” i „Pokaż na mapie” stoją pod nazwą budynku i chipami (reguła 10). | NFR-20, US3.2; decyzja 53 |
| 6 | V3 nie ma fragmentu mapy. Drogą do mapy jest „Pokaż wejście na mapie” przy wejściu. | NFR-20; decyzja 53 |
| 7 | V1 otwiera się na liście budynków. Widok mapy V1 się nie przewija: mapa wypełnia miejsce między polem wyszukiwania a paskiem dolnym. | NFR-19, NFR-20 |
| 8 | Pasek dolny ma zawsze jeden rząd z czterema pozycjami, a napis może mieć dwa wiersze. Pasek nie chowa się przy przewijaniu. Napisy rosną, dopóki najdłuższe słowo mieści się w pozycji, ale co najmniej 1,3 raza. To [odstępstwo od pełnych 200%](dostepnosc.md#odstepstwo-pasek-dolny). | NFR-20, wymagania wspólne (pasek dolny); decyzja 53 |
| 9 | Widoczny napis przycisku jest krótki, a pełną nazwę czyta TalkBack, np. „Zgłoś barierę przy wejściu” i „Zgłoś barierę przy wejściu: Wejście Cesarskie”. Nazwa dla TalkBack zaczyna się od widocznych słów. | NFR-18, WCAG 2.5.3 [1] |
| 10 | Elementy przypięte to górny pasek, pas czynności i pasek dolny. Gdy razem zajęłyby ponad 25% wysokości okna w bieżącej orientacji, pas czynności nie jest przypięty. Jego czynności stoją na początku treści, przed pierwszą grupą, czynność główna pierwsza, np. „Sprawdź, którędy wejść” i „Pokaż na mapie” na V2. Na ekranach decyzji, V5 i L1, stoją na końcu treści, po tekście potrzebnym do decyzji. Przypięte zostają górny pasek z „Wstecz”, który może się chować przy przewijaniu, i pasek dolny. W szerokim oknie przyciski mają szerokość kolumny tekstu (NFR-51). | NFR-20, NFR-21, WCAG 2.4.11 [7]; decyzja 53 |
| 11 | Ikona ozdobna obok słowa znika, np. przy „Winda” i „Drzwi”. Zostają ikony, które niosą znaczenie: kształt oznaczenia wejścia i uciążliwość. | NFR-20, NFR-21 |
| 12 | W opcji wyboru opis stoi pod tytułem na całą szerokość, a kółko i ikona tylko w wierszu tytułu. Tekst dzieli wyrazy po polsku i nie stoi w wąskich kolumnach. | NFR-20 |

Żadna reguła nie usuwa informacji wymaganej przez kryteria. Data i źródło przy wejściach zostają widoczne (US17.3), a podpis OpenStreetMap stoi przy każdej mapie (NFR-37).

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
| Realizuje | US1.1, US1.2, US1.3, US10.6, US16.1, początek US3.2 |
| Czynność główna | wyszukanie budynku po symbolu |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Mapa z konturami budynków | dane | US1, NFR-11 | — |
| Bariery ze zgłoszeń przyjętych jako znak uciążliwości: kształt, nie kolor. Słowo obok znaku tylko wtedy, gdy mieści się bez nakładania; symbole budynków mają pierwszeństwo. Znak ma nazwę dla TalkBack, np. „Blokada: schody przy wejściu, budynek A-1” | dane | US1.3, US10.6, NFR-18, NFR-20, NFR-21 | — |
| Dotknięcie znaku bariery | czynność | US1.3, NFR-17 | arkusz bariery na tym ekranie |
| Pole symbolu budynku z podpowiedzią „Wpisz symbol budynku, np. C-13”; przyjmuje też zapis „c13” i symbol z numerem sali, np. „A-1 329” | czynność główna | US1.1, PW-8; sala: decyzja 44 | V2; sala: stan „znaleziona sala” |
| „Legenda”: rozwijana lista znaków mapy i planu, każdy z napisem; na końcu „Źródło: Politechnika Wrocławska” | czynność | NFR-21, NFR-40, decyzja 48 | legenda na tym ekranie |
| Plan piętra budynku szczegółowego na mapie: pomieszczenia z krótkimi podpisami („Winda”, „Toaleta”, „Sala 329”), wejścia, zgłoszenia przyjęte | dane | US16.1, decyzja 44 | — |
| Przełącznik pięter nad planem: etykiety pięter z danych PWr; wybrane piętro widać po obwódce, nie po kolorze | czynność | US16.1, NFR-17, NFR-21 | plan innego piętra |
| Podpis „Źródło: Politechnika Wrocławska” obok podpisu OpenStreetMap, gdy widać plan | dane | NFR-40 | — |
| Przełącznik „Mapa” i „Lista budynków”. Domyślnie „Mapa”, a przy skali czcionki co najmniej 1,5 „Lista budynków”. Aplikacja pamięta ostatni wybór | czynność | NFR-19, NFR-20 | lista na tym ekranie |
| Budynek na liście to jedna karta, która w całości jest jednym przyciskiem. TalkBack czyta symbol, nazwę i bariery, a potem „otwiera opis budynku”. Przycisk „Pokaż na mapie” jest na V2, a nie na liście | czynność | NFR-17, NFR-18, NFR-19, NFR-20; decyzja 54 | V2 |
| Dotknięcie konturu albo budynku na liście | czynność | US2, US3.2, decyzja 48 | V2 jako arkusz na mapie |
| Przyciski „Powiększ” i „Pomniejsz” | czynność | NFR-15 (WCAG 2.5.1, powiększanie dwoma palcami); PW-8 | — |
| Cztery przyciski przesuwania mapy na mapie, w krzyżu: strzałka w górę u góry, w lewo i w prawo po bokach, w dół u dołu. Każdy ma co najmniej 48 dp i nazwę dla TalkBack, np. „Przesuń mapę w górę” | czynność | WCAG 2.5.7 [7], NFR-17, NFR-18; zasada dla osób starszych; PW-8, PW-11 | — |
| Napis „© OpenStreetMap contributors” w jednym wierszu w rogu mapy, jako odnośnik | dane | NFR-37 | strona praw autorskich OSM |
| „ⓘ” w rogu mapy, nazwa dla TalkBack „Informacje o mapie”: źródła mapy, czyli OpenStreetMap z odnośnikiem i „Źródło: Politechnika Wrocławska” | czynność | NFR-18, NFR-37, NFR-40 | arkusz informacji na tym ekranie |
| Pasek dolny | czynność | wymagania wspólne | V4, V5, L1 / L2 |

Wybór wyniku wyszukiwania zaznacza budynek na mapie i otwiera V2. Po „Wstecz” z V2 widać zaznaczony budynek. Plan piętra pojawia się po „Pokaż na planie” na V2 albo po znalezieniu sali.

Arkusz budynku na V1 to ten sam wzorzec co V2, z tym samym układem i kodem. Zmiana V2 zmienia więc też arkusz na V1 (decyzja 53).

Arkusz bariery na dole mapy otwiera dotknięcie znaku. Bariery nie trzeba szukać na liście, a TalkBack dochodzi do niej przez listę budynków (NFR-19):

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Typ bariery tymi samymi napisami co na V2, uciążliwość napisem i ikoną | dane | US1.3, BR-7, NFR-21 | — |
| Miejsce: budynek i wejście albo piętro, „Sprawdzone 12.11.2026” | dane | US2.2, PW-3 | — |
| „Pokaż opis budynku” i „Zamknij” | czynność główna stanu, czynność | US2, PW-5 | V2; „Zamknij”: V1 |

Arkusz znalezionej sali na dole mapy (decyzja 48, punkt 5):

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Symbol budynku, „Sala 329”, funkcja sali i piętro z danych PWr, „Zamknij” | dane | decyzja 44, NFR-40 | „Zamknij”: V1 |
| Kroki tekstem w numerowanej liście, bez rysowania trasy: wejście zalecane, winda, piętro sali, sala z przybliżoną odległością od windy | dane | BR-25, decyzja 44, decyzja 48, decyzja 53 | — |
| „Pokaż opis budynku” | czynność główna stanu | US2, PW-5 | V2 |

Kroki prowadzą od wejścia zalecanego (BR-25) windą, która staje na piętrze tego wejścia i na piętrze sali. W A-1 to winda przy klatkach C i D, bo winda przy klatkach A i B jest w danych tylko na 2 piętrze [6]. Sala na piętrze wejścia ma tylko pierwszy i ostatni krok.

| Stan | Co widać |
| --- | --- |
| Zwykły | mapa z podkładem OpenStreetMap, konturami i barierami |
| Plan piętra | plan wybranego piętra, przełącznik pięter, „Legenda” i podpis źródła (US16.1, NFR-40) |
| Znaleziona sala | plan piętra sali z wyróżnioną salą i arkusz z krokami (decyzja 48) |
| Bariera na mapie | wybrany znak bariery i arkusz z typem, uciążliwością i miejscem (US1.3) |
| Duża czcionka | start na liście budynków; karta budynku to jeden przycisk, a „Pokaż na mapie” jest na V2 (NFR-19, NFR-20) |
| Sala bez drogi | brak wejścia zalecanego albo windy z jego piętra na piętro sali. Zamiast kroków: „Nie znaleźliśmy w danych wejścia i windy, którymi dotrzesz do tej sali bez schodów.” Przycisk „Pokaż opis budynku” (BR-25) |
| Pusty — brak sali | „Nie znaleźliśmy sali »X« w budynku A-1. Sprawdź numer albo otwórz opis budynku.” Przycisk „Pokaż opis budynku” (PW-10) |
| Pierwsze uruchomienie | przed mapą pokazuje się instrukcja V5 (US5.1) |
| Ładowanie | „Wczytujemy mapę kampusu…”; kontury pojawiają się przed podkładem (NFR-11) |
| Pusty — brak wyniku | „Nie znaleźliśmy budynku »X«. Sprawdź symbol, np. C-13, albo wybierz budynek z listy.” Przycisk „Lista budynków” (UC1 3a) |
| Brak sieci | kontury i bariery z telefonu, bez podkładu. Linia „Bez internetu · dane z 12.11.2026” stoi pod przełącznikiem „Mapa” i „Lista budynków”, a pole wyszukiwania się nie przesuwa (NFR-10, NFR-39) |
| Brak sieci przy pierwszym uruchomieniu | „Pierwsze otwarcie aplikacji wymaga internetu. Połącz się z internetem, a mapa wczyta się sama.” (NFR-11) |
| Błąd — brak podkładu | kontury i bariery bez podkładu, napis „Mapa ulic chwilowo się nie wczytuje. Budynki i bariery nadal możesz oglądać.” (NFR-50) |

Dostępność i osoby starsze: lista budynków daje tę samą informację co mapa i jest drogą dla TalkBack (NFR-19). Wyszukiwanie przyjmuje symbol w każdym zapisie, bez wymogu myślnika (US1.1). Kroki do sali są tekstem, więc TalkBack nie potrzebuje planu (NFR-19).

### V2 Karta budynku

| Pole | Treść |
| --- | --- |
| Cel | Osoba sprawdza, którym wejściem wejdzie i jakie bariery są w budynku. |
| Role | wszystkie; przyciski Zbierającego widzi tylko RO-ZBI (US14.1) |
| Realizuje | US2.1, US2.2, US16.1, US16.2, US17.1, US17.3, US10.6, US3.2; start US4, US8.4, US15.1, US17.2 |
| Czynność główna | „Sprawdź, którędy wejść” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Mapa nad arkuszem: kontur budynku wyróżniony, sąsiednie przygaszone, tylko wejścia tego budynku ze znakiem oznaczenia; napis „© OpenStreetMap contributors”. Na karcie mapa nie ma własnych czynności | dane | US2.1, NFR-21, NFR-37, decyzja 44, decyzja 48 | — |
| Nagłówek „Budynek C-13” i nazwa z danych | dane | US2 | — |
| Rząd chipów z ikoną i napisem: „Wejście dostępne”, „Winda”, „Toaleta dostosowana”. Chip jest tylko wtedy, gdy dane to potwierdzają; dotknięcie przewija do grupy | czynność | US2.1, US16.1, NFR-17, NFR-21, decyzja 48 | grupa na tym ekranie |
| Nad grupą „Wejścia” jeden wiersz z datą i źródłem, np. „Z map i opisów, stan z 1.10.2026”, gdy wszystkie wejścia mają to samo źródło i datę. Inaczej data i źródło stoją przy każdym wejściu | dane | US17.3, BR-24, PW-3, decyzja 48 | — |
| Grupa „Wejścia”: najpierw wejście zalecane z napisem „Zalecane” bez piętra: etykieta jest krótka, a piętro podają kroki do sali i opis wejścia | dane | BR-25, US3, decyzja 48, decyzja 53 | — |
| Każde wejście: oznaczenie „Dostępne”, „Niedostępne” albo „Brak danych” tekstem i kształtem ikony, nazwa, opis i szerokość drzwi z danych. Wejście bez nazwy: „Wejście bez nazwy w danych” | dane | US2.1, US17.1, NFR-21, decyzja 44 | — |
| Przy wejściu zalecanym „Pokaż wejście na mapie”; TalkBack czyta „Pokaż wejście na mapie: »nazwa wejścia«”. Inne słowa niż „Pokaż na mapie” przy budynku, bo cel jest inny | czynność | US2.1, NFR-18, WCAG 2.4.6 [1], decyzja 48, decyzja 53 | V1 z zaznaczonym wejściem |
| Przy każdym wejściu „Zgłoś barierę przy wejściu”; TalkBack czyta „Zgłoś barierę przy wejściu: »nazwa wejścia«” | czynność | US4, US14.2, NFR-18, WCAG 2.5.3 [1] | V4 z wybranym wejściem; Gość: L1 |
| Wyjścia ewakuacyjne pod przyciskiem „Pokaż wyjścia ewakuacyjne (2)”; liczba z danych | czynność | US2.1, decyzja 44, decyzja 48 | lista na tym ekranie |
| Zgłoszenia przyjęte pod nagłówkiem „Bariery w budynku”, zaraz po wejściach: typ bariery, uciążliwość, zdjęcie, data weryfikacji jako „Sprawdzone 12.11.2026” | dane | US2.2, BR-17, NFR-21, PW-3, decyzja 54 | — |
| Typ bariery nazwą z klasyfikacji, np. „niesprawna winda”. Gość i Odwiedzający widzą prostsze napisy trzech typów. TB-20: „toaleta zamknięta, zastawiona lub trudna do dojścia”. TB-22: „brak planu dotykowego lub głosowego”. TB-28: „brak obniżonej lady lub miejsca dla wózka”. Te same napisy są na V1 i V3. | dane | BR-7, słownik użytkownika | — |
| Grupa „Winda i toaleta”: na których piętrach jest winda i toaleta dla osób z niepełnosprawnością, z planów PWr (budynek szczegółowy) | dane | US16.1, NFR-40, decyzja 44 | — |
| Grupa „Piętra”: każde piętro z danych PWr od najniższego. Etykieta piętra to przycisk na całą szerokość, który rozwija krótki opis z funkcji pomieszczeń; na początku opisy są zwinięte. TalkBack czyta etykietę i stan „zwinięte” albo „rozwinięte”. Pod etykietą „Pokaż na planie” | dane, czynność | US16.1, NFR-17, NFR-18, decyzja 44, decyzja 54 | opis na tym ekranie; „Pokaż na planie”: V1, plan tego piętra |
| Podpis „Źródło: Politechnika Wrocławska” na dole karty | dane | NFR-40, decyzja 44 | — |
| Budynek podstawowy: napis „Nie mamy planu pięter tego budynku.” zamiast grup „Winda i toaleta” i „Piętra”. Lista barier stoi nad napisem, więc napis nie odsyła do niej | dane | US16.2, BR-G1, PW-6, UC2 2a, decyzja 54 | — |
| Przypięty pas nad paskiem dolnym: „Sprawdź, którędy wejść”, jedyny wypełniony przycisk, wyróżniony też położeniem i rozmiarem, i „Pokaż na mapie”. Przy dużej czcionce oba przyciski stoją pod nazwą budynku i chipami (reguła 10) | czynność główna, czynność | US3, US3.2, PW-2, NFR-20, NFR-21, decyzja 48, decyzja 53 | V3; V1 z zaznaczonym budynkiem |
| Zbierający: „Zmień oznaczenie” przy każdym wejściu | czynność | US17.2, BR-24 | C6 |
| Zbierający: „Zgłoś, że bariery nie ma” przy każdym zgłoszeniu przyjętym | czynność | US8.4, BR-19, PW-12 | C7 |
| Zbierający: „Popraw zgłoszenie” przy własnym zgłoszeniu przyjętym i przy każdym przyjętym zgłoszeniu szybkim | czynność | BR-3, BR-18 | C2 |
| Zbierający: „Oznacz obejrzane piętra” dla całego budynku | czynność | US15.1, PW-12 | C5 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5, L1 / L2 |

V2 wysuwa się z dołu jako arkusz na mapie V1, najpierw do połowy ekranu, jak karta miejsca w Mapach Google (decyzja 48). Przewinięcie treści albo dotknięcie uchwytu rozwija arkusz na cały ekran, więc przeciąganie nie jest jedyną drogą (wymagania wspólne). To jest V2, a nie dodatkowy arkusz przed nim, więc droga do wejścia ma tyle samo dotknięć (US3.2).

Kolejność od góry: nazwa budynku i czynności, „Wejścia”, „Bariery w budynku”, „Winda i toaleta”, „Piętra”. Bariery stoją zaraz po wejściach, bo to najważniejsza informacja karty, a zwinięte opisy pięter skracają kartę przy dużej czcionce (decyzja 54).

Na rozwiniętym arkuszu górny pasek, a w nim „Wstecz”, chowa się przy przewijaniu w dół i wraca przy przewijaniu w górę. Przypięty pas czynności i pasek dolny nie zasłaniają elementu z fokusem: treść przewija się tak, by był widoczny (WCAG 2.4.11 [7]).

„Zgłoś barierę przy wejściu” otwiera krok 1 V4 („Zgłoś barierę”) z napisem „Wejście: »nazwa wejścia«”. Wybrane wejście dołącza się do zgłoszenia jako wskazówka dla Weryfikatora, a miejsce i budynek nadal pochodzą z bieżącej pozycji (BR-8, BR-14). Gość przechodzi najpierw do L1, a po zalogowaniu wraca do V4 z tym samym wejściem (US14.2).

Przycisk „Zgłoś barierę przy wejściu” na V3 działa tak samo.

Fragment mapy i plany pięter pokazują to, co już jest na liście, więc TalkBack ich nie potrzebuje (NFR-19). Gość i Odwiedzający nie widzą przycisków Zbierającego, więc ich karta jest krótka.

| Stan | Co widać |
| --- | --- |
| Zwykły — budynek szczegółowy | fragment mapy, chipy, wejścia, lista barier, winda i toaleta, piętra ze zwiniętymi opisami (decyzja 54) |
| Zwykły — budynek podstawowy | fragment mapy, chipy, wejścia, lista barier, napis o braku planu (US16.2, NFR-48) |
| Bez wejścia zalecanego | budynek nie ma wejścia „Dostępne”: napisu „Zalecane” nie ma, a wejścia „Niedostępne” są pierwsze, każde z powodem z danych (BR-25) |
| Ładowanie | tekst od razu, w miejscu zdjęcia napis „Wczytujemy zdjęcie…” (NFR-12) |
| Pusty — brak zgłoszeń przyjętych | w grupie „Bariery w budynku” nagłówek „Brak zgłoszonych barier”, zdanie „Bariera pojawi się tutaj, gdy ktoś ją zgłosi i zostanie sprawdzona.” i przycisk „Zgłoś barierę” (V4, Gość: L1). Bez wybranego wejścia ten przycisk stoi w treści karty tylko tutaj (UC2 2a, PW-6, decyzja 48) |
| Brak sieci | karta z telefonu z linią „Bez internetu · dane z 12.11.2026”, fragment mapy bez podkładu. Zdjęcia, których nie ma w telefonie, zastępuje napis „Zdjęcie zobaczysz po połączeniu z internetem.” (NFR-10) |
| Brak sieci, budynku nie ma w telefonie | „Tego budynku nie ma jeszcze w telefonie. Połącz się z internetem, aby zobaczyć opis budynku.” Przycisk „Spróbuj ponownie” (NFR-10) |
| Błąd zdjęcia | „Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę.” Przycisk „Spróbuj ponownie” (NFR-50) |

Dostępność i osoby starsze: oznaczenie wejścia jest pierwsze przy wejściu (US17.3) i stoi w spokojnym kontenerze, ale znaczenie niosą kształt ikony i słowo (NFR-21). „Brak danych” ma własną ikonę, inną niż „Niedostępne” (US17.1).

### V3 Którędy wejść {#v3-zaplanuj-wizyte}

| Pole | Treść |
| --- | --- |
| Cel | Osoba wie, którym wejściem wejdzie do budynku bez barier, albo wie, że takiego wejścia nie ma. |
| Role | wszystkie, bez logowania |
| Realizuje | US3.1, US3.2, US2.1, US2.2, US17.3; start US4 |
| Czynność główna | brak; ekran odpowiada na jedno pytanie, a dalej prowadzą „Wstecz” (V2), przyciski przy wejściach i pasek dolny |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Którędy wejść do C-13” | dane | US3, PW-2 | — |
| Zdanie z odpowiedzią, np. „Budynek A-1 ma wejście dostępne: »nazwa wejścia«.”; zdanie podaje wejście zalecane, to samo co na V2 | dane | US3, US3.1, BR-25 | — |
| Każde wejście dostępne: nazwa, oznaczenie tekstem i ikoną. Wejście zalecane jest pierwsze, z napisem „Zalecane” bez piętra, jak na V2 | dane | US2.1, NFR-21, BR-25, decyzja 53 | — |
| Przy każdym wejściu dostępnym „Pokaż wejście na mapie”; TalkBack czyta „Pokaż wejście na mapie: »nazwa wejścia«” | czynność | US2.1, US3, NFR-18, WCAG 2.4.6 [1], decyzja 53 | V1 z zaznaczonym wejściem |
| Przy każdym wejściu dostępnym „Zgłoś barierę przy wejściu”; TalkBack czyta „Zgłoś barierę przy wejściu: »nazwa wejścia«”. Działa jak na V2 | czynność | US4, US14.2, NFR-18, WCAG 2.5.3 [1], decyzja 54 | V4 z wybranym wejściem; Gość: L1 |
| Data i źródło oznaczenia: jeden wiersz nad wejściami albo przy każdym wejściu, jak na V2 | dane | US17.3, BR-24, decyzja 48 | — |
| Zgłoszenia przyjęte przy tym wejściu ze zdjęciem i datą weryfikacji jako „Sprawdzone 12.11.2026” | dane | US2.2, UC4 4c, PW-3 | — |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5, L1 / L2 |

Droga z mapy: dotknięcie budynku (V2), „Sprawdź, którędy wejść” (V3). To 2 dotknięcia, a próg US3.2 to 3.

V3 nie powtarza V2. Karta pokazuje wszystkie wejścia z wejściem zalecanym na górze, a V3 odpowiada jednym zdaniem i pokazuje tylko wejścia dostępne z barierami przy nich.

| Stan | Co widać |
| --- | --- |
| Zwykły | zdanie z odpowiedzią i wejścia dostępne |
| Brak wejścia dostępnego | „Budynek B-4 nie ma wejścia dostępnego. Wybierz »Wstecz«, aby w opisie budynku zobaczyć bariery przy każdym wejściu.” (US3.1, UC4 4a, PW-10) |
| Brak danych o wejściach | „Nie wiemy jeszcze, czy budynek B-4 ma wejście dostępne: brak danych o 2 z 3 wejść. Wybierz »Wstecz«, aby w opisie budynku zobaczyć każde wejście z datą.” (PW-10) Liczby zależą od budynku, także wtedy, gdy danych brak o wszystkich wejściach (UC4 4b, US17.1) |
| Brak sieci | dane z telefonu z linią „Bez internetu · dane z 12.11.2026” (NFR-10) |

Dostępność i osoby starsze: odpowiedź jest pełnym zdaniem na górze ekranu, a nie samym brakiem wyniku (US3.1). TalkBack czyta je jako pierwsze (NFR-18).

### V4 Zgłoszenie szybkie

| Pole | Treść |
| --- | --- |
| Cel | Odwiedzający przekazuje informację o barierze w 3 krokach, bez formularza. |
| Role | RO-ODW i role pracownicze; Gość niezalogowany przechodzi najpierw do L1 (US14.2) |
| Realizuje | US4.1, US4.2, US4.3, US4.4, US4.5, US14.2 |
| Czynność główna | krok 1: „Zrób zdjęcie”, a po powrocie z kroku 2 „Dalej”; krok 2: wybór grupy, w budynku też piętra; krok 3: „Wyślij” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Napis „Krok 1 z 3”, „Krok 2 z 3”, „Krok 3 z 3” | dane | US4.1 | — |
| Nagłówek „Zgłoś barierę”, ten sam napis co na pasku dolnym | dane | US4, PW-14 | — |
| Krok 1: komunikat ostrzegawczy (wymagania wspólne) nad „Zrób zdjęcie”: „Ważne”, wymaganie „Nie fotografuj twarzy ani tablic rejestracyjnych.” i zdanie „Zdjęcie zobaczą inni użytkownicy.” Ten sam komunikat jest w kroku 1 po powrocie z kroku 2 | dane | NFR-25, NFR-21, BR-17, decyzja 54 | — |
| Krok 1: „Zrób zdjęcie” i „Zrób nowe zdjęcie” | czynność | US4.1, BR-8, PW-8 | aparat, potem krok 2 |
| Krok 1 po powrocie z kroku 2: miniatura zdjęcia i „Dalej” nad „Zrób nowe zdjęcie” | czynność główna | US4.1, heurystyka Nielsena 3 [5] | krok 2; zdjęcie i grupa zostają |
| Krok 1 po „Zgłoś barierę przy wejściu” na V2 albo V3: napis „Wejście: »nazwa wejścia«” nad „Zrób zdjęcie” | dane | US4, decyzja 48; znaczenie wejścia: V2 | — |
| Krok 2: pytanie „Czego dotyczy bariera?” i 6 dużych przycisków grup barier, sam tekst: „Wejście i drzwi”, „Schody i windy”, „Przejścia i chodniki”, „Toalety”, „Napisy i oznaczenia”, „Coś innego” | czynność | US4.3, NFR-17, PW-7 | krok 3 albo pytanie o piętro |
| Krok 2, pytanie o piętro: wybrana grupa, pytanie „Na którym piętrze?”, pod nim lista dużych przycisków: „Nie wiem” i piętra od najniższego w górę. W budynku szczegółowym etykiety z danych PWr, w podstawowym „Parter”, „1”, „2”… | czynność | US4.5, BR-14, NFR-17, decyzja 54 | krok 3 |
| Krok 3: miniatura zdjęcia, wybrana grupa oraz piętro albo „Nie wiem”, jeśli było pytanie | dane | US4.1, US4.5 | — |
| Krok 3: napis „Dołączymy miejsce, w którym teraz stoisz.” | dane | US4.1, NFR-27 | — |
| Krok 3: „Wyślij” | czynność główna | US4.1 | potwierdzenie |
| Potwierdzenie: „Dziękujemy. Sprawdzimy zdjęcie, a jeśli bariera się potwierdzi, pokażemy ją na mapie.” | dane | US4.4, PW-5 | — |
| Potwierdzenie: „Wróć do mapy” | czynność | US4.4 | V1 |

Aplikacja nie pyta o typ bariery, uciążliwość ani budynek (US4.3, BR-14). Pozycję zaczyna ustalać po wejściu na ekran, w trakcie kroków 1 i 2 (NFR-27).

O piętro aplikacja pyta tylko w konturze budynku i przy każdej grupie poza „Coś innego” (US4.5, tabela w [regułach](../biznes/reguly.md#pietro-w-zgloszeniu-szybkim)). Zakres numerów pięter w budynku podstawowym, także pięter pod parterem, ustalimy w fazie implementacji.

| Stan | Co widać |
| --- | --- |
| Zwykły | kroki 1–3 i potwierdzenie |
| Pytanie o piętro | po wyborze grupy, na tym samym kroku 2; „Wstecz” wraca do listy grup. Gdy pozycja nie jest jeszcze znana, pytanie pojawia się jak w budynku podstawowym (BR-14) |
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
| Czynność główna | rola pracownicza: wybór czynności z listy „Twoja praca” pod kartą osoby; RO-ODW: brak czynności roli, ekran ma tylko dane konta |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Konto”, a u ról pracowniczych „Konto i praca”, ten sam napis co na pasku dolnym | dane | US14.1, PW-1 | — |
| Karta osoby na górze ekranu: nazwa wyświetlana i lista ról, np. „Twoje role: Odwiedzający, Zbierający” | dane | BR-1, US14.3, decyzja 54 | — |
| Sekcja „Twoja praca”: tylko czynności posiadanych ról, jako lista wierszy. Wiersz to ikona, słowo i strzałka „›” po prawej, a cały wiersz jest jednym celem dotyku. Strzałka znaczy przejście dalej, do kolejnego widoku albo kroku | czynność główna | US14.1, BR-1, PW-1, NFR-17, NFR-18, decyzja 54 | — |
| Zbierający: „Zmierz i zgłoś barierę” | czynność | US6, PW-12 | C1 |
| Zbierający: „Moje zgłoszenia” | czynność | US8 | C3 |
| Weryfikator: „Zgłoszenia do sprawdzenia” | czynność | US10 | R1 |
| Koordynator: „Postęp inwentaryzacji” | czynność | US11 | K1 |
| Koordynator: „Zaproś osobę” | czynność | US12 | K2 |
| Koordynator: „Pobierz eksport” | czynność | US13 | K3 |
| Sekcja „Konto” pod „Twoja praca”: wiersze w tym samym układzie | dane | NFR-30, macierz uprawnień, decyzja 54 | — |
| „Informacja o danych osobowych” ze strzałką „›” | czynność | NFR-30 | stan „Klauzula informacyjna” na tym ekranie |
| „Wycofaj zgodę” ze strzałką „›” | czynność | NFR-30, NFR-29 | potwierdzenie; po potwierdzeniu V1 |
| „Wyloguj się” bez strzałki, bo działa od razu, bez kolejnego kroku | czynność | macierz uprawnień, PW-14 | V1 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5 |

Kolejność od góry: karta osoby, „Twoja praca”, sekcja „Konto”: klauzula, „Wycofaj zgodę”, „Wyloguj się”. Osoba z rolą pracowniczą otwiera L2 głównie po to, by pracować, więc czynności są tuż pod kartą osoby. Czynności to lista wierszy, a nie kafelki ani rząd jednakowych przycisków (decyzja 54).

| Stan | Co widać |
| --- | --- |
| Zwykły — Odwiedzający | nagłówek „Konto”, karta osoby, sekcja „Konto”; bez sekcji „Twoja praca” (US14.3) |
| Zwykły — rola pracownicza | nagłówek „Konto i praca”, karta osoby, sekcja „Twoja praca”, pod nią sekcja „Konto” |
| Klauzula informacyjna | treść klauzuli informacyjnej z podstawą przetwarzania, ta sama co przy rejestracji; „Wstecz” wraca do zwykłego stanu L2 (NFR-30). Administratorem danych jest zespół projektu. Klauzula ma zdanie „Politechnika Wrocławska nie jest administratorem Twoich danych.” i napis „Kontakt z zespołem:” z odnośnikiem „repozytorium projektu na GitHub”, bez słowa „zgłoszenia” (PW-5, decyzja 53). Tekst klauzuli nie był jeszcze sprawdzony przez prawnika |
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
| Budynek szczegółowy: przełącznik „Plan piętra” i „Mapa” nad jedną powierzchnią, domyślnie „Plan piętra”. Plan ma nad sobą wybór piętra, a mapa pokazuje ten sam punkt. | czynność | US6.3, US6.4, NFR-21 | C1, ten sam ekran |
| Przesunięcie punktu na planie wybranego piętra albo na mapie, także przyciskami strzałek: jeden zestaw pod wybranym widokiem | czynność | US6.3, US6.4, NFR-3, NFR-15 | — |
| Napis „Korekta ręczna” po przesunięciu punktu | dane | US6.3, US6.4 | — |
| Napis „© OpenStreetMap contributors”; pod planem „Źródło: Politechnika Wrocławska” | dane | NFR-37, NFR-40 | — |
| „Dalej” | czynność główna | US6 | C2 |

| Stan | Co widać |
| --- | --- |
| Zwykły | uśrednianie, dokładność, satelity, podpowiedź budynku |
| Ładowanie — szukanie satelitów | „Szukamy satelitów. Na otwartej przestrzeni pomiar będzie dokładniejszy.” Przycisk „Powtórz pomiar” (NFR-1, PW-9) |
| Wybór innego budynku | po „Wybierz inny” w miejscu pytania sekcja „Wybierz budynek”: budynki najbliżej punktu, od najbliższego, i „Poza budynkiem”. Wybór zamyka listę i pokazuje „Budynek [symbol]” z „Wybierz inny”. Lista działa bez sieci, z konturów w telefonie (US6.2, BR-G2, NFR-4). |
| Daleko od budynków PWr | „Poza budynkiem”, bez podpowiedzi i bez komunikatu o błędzie (BR-G4, NFR-49) |
| Budynek podstawowy | bez przełącznika „Plan piętra” i „Mapa”, wyboru piętra i korekty na planie. W ich miejscu napis „Nie mamy planu pięter tego budynku.”, jak na V2. Korekta na mapie działa (US6.4, BR-G1, PW-6). |
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
| Konflikt: wersja własna i wersja z serwera naraz. Nad nimi napis „Różni się:” z nazwą pola, np. „Różni się: uciążliwość”, a przy kilku polach „Różnią się:” i ich nazwy. W obu wersjach te pola są pogrubione | dane | US9.3, NFR-21 | — |
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
| W widoku mapy te same przyciski mapy co na V1: „Powiększ”, „Pomniejsz” i strzałki przesuwania w krzyżu | czynność | NFR-15 (WCAG 2.5.1), WCAG 2.5.7 [7]; wymagania V1 | — |
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
| Rola i pseudonim autora, np. „Zbierający, Autor [nr]” | dane | BR-5 | — |
| „Podobne zgłoszenia do 10 m”: typ, zdjęcie, odległość, piętro, status | dane | US10.1, BR-13 | — |
| Zgłoszenie szybkie: wybór „Typ bariery” i „Uciążliwość” z wartościami „Utrudnienie”, „Tylko z pomocą”, „Blokada” | czynność | US10.3, BR-14, PW-13 | — |
| „Zasłoń twarz lub tablicę” na zdjęciu | czynność | BR-15, NFR-25 | — |
| Zmiana oznaczenia wejścia: obecne oznaczenie z datą i źródłem, wybrane oznaczenie, nowe zdjęcie | dane | US17.2, BR-24 | — |
| Zmiana oznaczenia wejścia: tekstem i ikoną, czy oznaczenie jest „Bez zmian od wysłania”, czy „Zmienione po wysłaniu” przez inną przyjętą zmianę | dane | BR-23, BR-24, NFR-21 | — |
| „Bariera usunięta”: zdjęcie poprzedniego zgłoszenia obok nowego | dane | US10.6, BR-19 | — |
| „Bariera usunięta”: „Poprzednie zgłoszenie:” i jego status tekstem i ikoną, np. „Archiwalne” | dane | BR-19, BR-23, NFR-21 | — |
| „Przyjmij” | czynność główna | US10, US10.6 | R1 |
| „Odrzuć” z obowiązkowym polem „Powód odrzucenia”; gotowe powody „Duplikat” ze wskazaniem wcześniejszego zgłoszenia i „Nieaktualne” oraz „Inny powód” z polem „Opisz powód dla autora”. Przy zmianie oznaczenia wejścia bez „Duplikat”, bo duplikat opisuje tę samą barierę. | czynność | US10.2, BR-13, BR-16, BR-23, BR-24, NFR-18 | R1 |

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
| Budynek szczegółowy: „Wejścia sprawdzone na miejscu: 1 z 6” | dane | US11.4, BR-24, PW-3 | — |
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
| Karta bez zgłoszeń przyjętych (US2.4, UC2 2a) | Zdanie, że nikt jeszcze nie zgłosił tu sprawdzonej bariery; karta nie sugeruje, że barier nie ma. | Wynika z P-4 procesu TO-BE: pusta lista nie może brzmieć jak „wszystko dostępne” (N2). |
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
| Napis budynku podstawowego bez barier (recenzja B5) | Napis o braku planu ma jedno zdanie. Lista barier stoi teraz nad nim, więc odsyłacz „Bariery są na liście niżej.” odpadł (decyzja 54). | Przy pustej liście zdanie odsyłałoby do barier, których nie ma, obok napisu „Brak zgłoszonych barier” (UC2 2a). Po zmianie kolejności grup lista nie jest już niżej. |
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
| | US1.4 | V1, V2, V3 | wejścia, ścieżki i legenda |
| | US1.5 | V1 | arkusz bariery |
| Karta budynku | US2.1 | V2, V3 | oznaczenie tekstem i ikoną |
| | US2.2 | V2, V3 | zdjęcie i data weryfikacji |
| | US2.3 | V1, V2 | chipy i kolejność grup |
| | US2.4 | V2 | stan „Pusty” |
| | US2.5 | V1, V2 | miejsce bariery |
| | US2.6 | V2 | wyjścia ewakuacyjne na żądanie |
| Którędy wejść | US3.1 | V3 | stan „brak wejścia dostępnego” |
| | US3.2 | V1, V2, V3 | 2 dotknięcia |
| | US3.3 | V3 | dojście, drzwi, winda; „Pokaż wejście na mapie” |
| Zgłoszenie szybkie | US4.1 | V4 | 3 kroki, pozycja sama |
| | US4.2 | V4 | potwierdzenie bez sieci; wysyłka w tle bez ekranu |
| | US4.3 | V4 | 6 przycisków grup |
| | US4.4 | V4 | potwierdzenie |
| | US4.5 | V4 | pytanie o piętro |
| | US4.6 | V2, V3, V4 | „Zgłoś barierę przy wejściu” i nazwa wejścia w kroku 1 |
| | US4.7 | V4 | „Wstecz” z potwierdzenia |
| | US4.8 | V4 | zdjęcie i grupa po powrocie, pytanie o rezygnację |
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
| | US14.4 | V4, L1 | prośba o logowanie tylko za pierwszym razem |
| Piętra obejrzane | US15.1 | C5 | — |
| | US15.2 | C5 | „Cofnij znacznik” tylko przy własnym |
| | US15.3 | C5 | znacznik w telefonie |
| Plan piętra | US16.1 | V1, V2, C1 | V1: przełącznik i plan; V2: piętra tekstem i „Pokaż na planie” |
| | US16.2 | V2, C2 | napis o braku planu, pole liczby |
| | US16.3 | V2 | grupy „Winda i toaleta” i „Piętra” |
| Oznaczenie wejścia | US17.1 | V2, V3 | trzy oznaczenia; nadanie przy imporcie bez ekranu |
| | US17.2 | V2, C6, R2, C3 | start, zmiana, sprawdzenie, status |
| | US17.3 | V2, V3, C6, R2 | data i źródło |
| Wyszukanie sali | US18.1 | V1 | numer sali w polu wyszukiwania, plan piętra |
| | US18.2 | V1 | kroki do sali |
| | US18.3 | V1 | stany „Sala bez drogi” i „Pusty — brak sali” |

Wszystkie 69 kryteriów ma ekran. Kryteria w całości serwerowe nie występują: każde ma co najmniej widoczny skutek.

## Źródła

1. WCAG 2.1 (W3C), m.in. 2.5.1 „Pointer Gestures”, 3.3.1 „Error Identification”, 3.3.3 „Error Suggestion”: <https://www.w3.org/TR/WCAG21/>
2. Treść komunikatów o błędach: co się stało i jak to naprawić (Nielsen Norman Group): <https://www.nngroup.com/articles/error-message-guidelines/>
3. Android: lokalizacja przybliżona i dokładna, prośba o dokładną lokalizację: <https://developer.android.com/develop/sensors-and-location/location/permissions>
4. Android: prośba o uprawnienie w kontekście, wyjaśnienie z możliwością rezygnacji, odmowa bez ponawiania: <https://developer.android.com/training/permissions/requesting>
5. Nielsen J., 10 Usability Heuristics for User Interface Design (Nielsen Norman Group): <https://www.nngroup.com/articles/ten-usability-heuristics/>
6. Plany pięter PWr w ArcGIS (usługi „Planbudynku_*”, windy w A-1), sprawdzone 28.10.2026: <https://services3.arcgis.com/rrLByHvJ7zuJQDVw/arcgis/rest/services/>
7. WCAG 2.2 (W3C), m.in. 2.4.11 „Focus Not Obscured (Minimum)”, 2.5.7 „Dragging Movements”: <https://www.w3.org/TR/WCAG22/>
8. Android 14: nieliniowe skalowanie czcionki: <https://developer.android.com/about/versions/14/features#non-linear-font-scaling>

Wewnętrzne źródła: [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), [Przypadki użycia](../wymagania/przypadki-uzycia.md), [Role i uprawnienia](../biznes/role.md), [Reguły biznesowe](../biznes/reguly.md), [Wymagania niefunkcjonalne](../wymagania/niefunkcjonalne.md), [Wizja](../biznes/wizja.md).

Dostęp do źródeł: 28.10.2026.
