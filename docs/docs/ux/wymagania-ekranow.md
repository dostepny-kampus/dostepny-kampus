# Wymagania do ekranów

Strona opisuje każdy ekran, zanim powstanie makieta. Element ekranu istnieje tylko wtedy, gdy wynika z kryterium akceptacji, reguły, przypadku użycia albo wymagania niefunkcjonalnego, które mówi o nim wprost (decyzja 16). Historyjki i kryteria są na stronie [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), uprawnienia w [macierzy ról](../biznes/role.md#macierz-uprawnien).

Kolumna „Odwołanie” podaje te identyfikatory. Skrypt `check_ids.py` sprawdza, że każdy wiersz ma w niej ID US, AC, BR, NFR albo UC lub odsyłacz „wymagania wspólne”.

Napisy przycisków i treść komunikatów w cudzysłowie pochodzą ze [słownika użytkownika](slownik-uzytkownika.md) (propozycje PW-1…PW-15) i są hipotezą do sprawdzenia w wywiadach. Cudzysłów »…« wewnątrz takiego napisu to na ekranie „…” ([zasady napisów](slownik-uzytkownika.md#zasady-napisow)). Opis elementu bez cudzysłowu używa pojęć ze [słownika pojęć](../biznes/slownik.md). Kolumna „Dokąd” podaje ekran docelowy i jest wejściem do mapy nawigacji.

## Lista ekranów

| Ekran | Nazwa robocza | Nagłówek na ekranie | Role z dostępem | Historyjki | Nowy w fazie B |
| --- | --- | --- | --- | --- | --- |
| V1 | Mapa kampusu | „Mapa kampusu” | wszystkie | US1, US3.3, US10.6, US16.1, US18 | — |
| V2 | Karta budynku | symbol, np. „A-1”, i nazwa budynku | wszystkie; czynności Zbierającego tylko RO-ZBI | US2, US3, US16, US17; start US8.4, US15, US17.2 | — |
| V4 | Zgłoszenie szybkie | „Zgłoś barierę” | RO-ODW i role pracownicze; RO-GOS przechodzi do L1 | US4, US14.2 | — |
| V5 | Pomoc | „Jak korzystać z aplikacji” | wszystkie | US5 | — |
| L1 | Prośba o logowanie | „Konto” | RO-GOS | US14.2, US14.3 | tak |
| L2 | Konto | „Konto”; role pracownicze: „Konto i praca” | RO-ODW i role pracownicze | US14.1, US14.3 | tak |
| C1 | Pomiar pozycji | „Zmierz i zgłoś barierę” | RO-ZBI | US6 | — |
| C2 | Formularz zgłoszenia | „Opisz barierę”; przy poprawianiu i zmianie zgłoszenia przyjętego „Popraw zgłoszenie” | RO-ZBI | US7, US8.3 | — |
| C3 | Moje zgłoszenia | „Moje zgłoszenia” | RO-ZBI, tylko własne, bez zgłoszeń szybkich | US8, US9.1 | — |
| C4 | Synchronizacja i wybór wersji | „Synchronizacja” | RO-ZBI | US9 | — |
| C5 | Piętra obejrzane | „Obejrzane piętra” | RO-ZBI, RO-KOO | US15, US11.3 | tak |
| C6 | Zmiana oznaczenia wejścia | „Zmień oznaczenie wejścia” | RO-ZBI | US17.2 | tak |
| C7 | Usunięcie bariery | „Zgłoś, że bariery nie ma” | RO-ZBI | US8.4 | tak |
| R1 | Kolejka weryfikacji | „Zgłoszenia do sprawdzenia” | RO-WER | US10.4, US10.5 | — |
| R2 | Sprawdzenie zgłoszenia | „Sprawdzenie zgłoszenia” | RO-WER | US10, US17.2 | — |
| K1 | Postęp inwentaryzacji | „Postęp inwentaryzacji” | RO-KOO | US11 | — |
| K2 | Zaproszenie | „Zaproś osobę” | RO-KOO | US12 | — |
| K3 | Eksport | „Pobierz eksport” | RO-KOO | US13 | — |

Nazwa robocza służy dokumentom i diagramom, a nagłówek to napis, który osoba czyta w aplikacji (PW-2, PW-14). Symbol „C-13” w nagłówku V2 to przykład. Ekranu V3 „Którędy wejść” nie ma: odpowiedź podaje karta V2, a szczegóły wejścia arkusz wejścia na V1 ([V3](#v3-zaplanuj-wizyte)).

Odwiedzający ma uprawnienie „Wysyłka po powrocie sieci”, ale nie ma ekranu C4. Jego zgłoszenie szybkie wysyła się w tle, a on widzi tylko potwierdzenie (US4.2, US4.4).

## Wymagania wspólne

Dotyczą każdego ekranu. Przy ekranach podajemy tylko to, co do nich dochodzi.

| Wymaganie | Odwołanie |
| --- | --- |
| Nagłówek z nazwą ekranu i przyciskiem „Wstecz”: strzałka w lewo i słowo, zawsze w tym samym miejscu. Przy przewijaniu w dół może się schować razem z górnym paskiem i wraca przy przewijaniu w górę (reguła 10). Wyjątki: V1 jako ekran startowy i karta V2, którą zamyka „Zamknij” w jej nagłówku. | NFR-52, US2.8; WCAG 3.2.3 [1]; PW-11 |
| Wskaźnik braku sieci to jedna linia: ikona i napis „Bez internetu · dane z 12.11.2026”. Linia zostaje, bo dane w telefonie mogą być nieaktualne. Data to dzień ostatniego pobrania danych tego ekranu, a na V1 całej mapy. Gdy w telefonie nie ma jeszcze danych, linia ma samo „Bez internetu”. Zbierający widzi w tej linii „Ostatnia synchronizacja: 12.11.2026, 10:42” zamiast „dane z …”. Przy dużej czcionce linia łamie się po słowach, a data z godziną zostaje razem w jednym wierszu. | US9.1, NFR-10, NFR-20, NFR-21; PW-4; decyzje 54, 94 |
| Pasek dolny na V1, V2, V5 i L2: „Mapa” (V1), „Zgłoś barierę” (V4), „Pomoc” (V5), „Konto” (L1 albo L2). Osoba z co najmniej jedną rolą pracowniczą widzi na czwartej pozycji, w tym samym miejscu, napis „Konto i praca” (PW-1). Pozycja prowadzi do L2. L1 nie ma paska, także otwarty z paska: to ekran decyzji, a „Nie teraz” wraca do mapy. Na formularzach i krokach zgłoszenia paska nie ma. | US1, US4, US5.1, US14, US14.1; [mapa nawigacji](mapa-nawigacji.md), P1 |
| Pasek dolny nigdy się nie chowa, także przy przewijaniu. Przy przewijaniu w dół może się schować tylko górny pasek, w którym jest „Wstecz”. | NFR-52 |
| W oknie o szerokości co najmniej 600 dp, np. w telefonie w poziomie, pasek nawigacji stoi z boku ekranu: te same cztery pozycje, napisy i kolejność co w pasku dolnym. Obowiązuje przy każdej czcionce. Pasek z boku też nigdy się nie chowa i nie zabiera wysokości okna, więc nie wlicza się do 25% elementów przypiętych. Węższe okno ma pasek dolny. | NFR-22, NFR-51, NFR-52; decyzja 69 |
| Przy dużej czcionce elementy przypięte razem zajmują najwyżej 25% wysokości okna w bieżącej orientacji (próg zespołu). Czynności, które by się nie zmieściły, stoją w treści według reguły 10 sekcji [Duża czcionka](#duza-czcionka). | NFR-52, NFR-20 |
| Arkusze bariery, wejścia, toalety i schodów, karta sali i arkusz „Który budynek?” na V1 zamyka obrysowany przycisk ze słowem „Zamknij” w nagłówku, bez znaku „×”. Tak samo zamyka się arkusz „Legenda” spod „ⓘ” oraz karta V2 na każdej wysokości. Górnego paska z „Wstecz” karta V2 nie ma. | NFR-53, NFR-17, NFR-18, US2.8; PW-11; decyzje 53, 115 |
| Komunikat ostrzegawczy przed czynnością: obrysowane koło ze znakiem „!”, pogrubione słowo „Ważne”, wymaganie i jedno zdanie z powodem. Ramka, koło i słowo mają kolor roli `error` z [tokenów](tokeny.md#zasady), ale znaczenie niosą kształt i słowo. Trójkąt jest zarezerwowany dla uciążliwości „Utrudnienie”, więc komunikat go nie używa. | NFR-16, NFR-21; PW-13; decyzja 54 |
| Jedna czynność główna na ekranie: pierwsza w kolejności czytania wśród czynności ekranu i jedyna wypełniona, nigdy wyróżniona samym kolorem. Czynność powtarzana w każdej pozycji listy (R1, C5, K1) i wybór jednego z dwóch równorzędnych wariantów (C4) nie mają przycisku wypełnionego; wyróżnia je położenie. | NFR-21, zasada dla osób starszych |
| Czynność drugorzędna obok czynności głównej jest obrysowana: ramka 1 dp `outline`, napis `primary`. „Spróbuj ponownie” w treści ekranu to kompaktowy przycisk obrysowany przy lewej krawędzi treści, np. na C2, C3, C5, C6 i V2. „Zrób zdjęcie” i „Zrób nowe zdjęcie” obok przypiętej czynności głównej (C2, C6, V4 po zrobieniu zdjęcia) to przycisk obrysowany na całą szerokość treści. W V4 przed zdjęciem „Zrób zdjęcie” jest czynnością główną i jest wypełnione. Aplikacja nie ma przycisku tonalnego: jego kształt ma z tłem kontrast poniżej 3:1, więc każdy przycisk jest wypełniony albo obrysowany. | NFR-21, NFR-15; WCAG 1.4.11 [1] |
| Pozycja listy zgłoszeń, budynków, pięter albo znaczników, np. na C3, R1, K1 i C5, to karta pozycji listy: tło `surfaceContainerLowest`, ramka 1 dp `outline`, zaokrąglenie `zaokraglenia.duze`, wcięcie treści `odstepy.m`. Czynności karty stoją pod jej treścią w odstępie `odstepy.s`, a karty listy dzieli `odstepy.m`. Ten sam rodzaj pozycji wygląda tak samo na każdym ekranie. | US8.1, US10.4, US11.1, US15.1, NFR-16, NFR-21; [tokeny](tokeny.md#wymiary) |
| Czynność w karcie pozycji listy to kompaktowy przycisk obrysowany przy lewej krawędzi treści, np. „Otwórz”, „Weź do sprawdzenia”, „Oznacz jako obejrzane” i „Cofnij znacznik”. Ma szerokość napisu, wysokość co najmniej `cele_dotyku.minimum` i kształt kapsuły. Ramka ma 1 dp w kolorze `outline`, a napis styl `labelLarge` i kolor `primary` na każdym ekranie. Czynność, która otwiera inny ekran, ma po napisie strzałkę w prawo. Przycisk przy dużej czcionce rośnie z napisem. Przyciski przypiętego pasa czynności na dole ekranu się nie zmieniają: mają całą szerokość pasa. | NFR-17, NFR-16, NFR-20, NFR-21, US8.1, US10.4, US11.3, US15.1; [tokeny](tokeny.md#kontrast) |
| Informacja ekranu, która nie jest pozycją listy, stoi w panelu informacyjnym, np. karta osoby na L2, komunikat na L1, krok instrukcji na V5 i wynik czynności pracownika. Panel ma tło `surfaceContainerLowest`, zaokrąglenie `zaokraglenia.duze`, wcięcie treści `odstepy.m` i odstęp między elementami `odstepy.s`, bez ramki. Ramka oddziela sąsiednie pozycje listy, a panel stoi pojedynczo. Napisy na mapie, np. o zmianie terenu, to pływające kontrolki mapy, nie panele. | US5.1, US14.1, US14.2, NFR-16, NFR-20 |
| Przyciski w przypiętym pasie czynności na dole ekranu mają razem całą szerokość pasa. Jeden przycisk zajmuje całą szerokość, także wtedy, gdy jego napis jest krótki, np. „Zapisz” na C2 i „Wyślij do sprawdzenia” na C7. Dwa przyciski stoją jeden pod drugim, każdy na całą szerokość, czynność główna wyżej. W szerokim oknie szerokością pasa jest szerokość kolumny tekstu (reguła 10). | NFR-17, NFR-52, NFR-51 |
| Cel dotyku co najmniej 48 × 48 dp. | NFR-17 |
| „ⓘ” na każdym ekranie z mapą (V1, C1, R1) to ten sam element. TalkBack czyta go jako „Legenda i źródła mapy”. Otwiera arkusz „Legenda” z „Zamknij”: znaki widoczne na tej mapie, każdy z napisem, a na końcu „Źródła danych” z treścią licencyjną jak na V1. Osobnego przycisku „Legenda” na mapie nie ma. | US1.4, NFR-21, NFR-37, NFR-40, NFR-18 |
| Wybór na mapie oznacza [pinezka z podpisem przy punkcie albo grubszy obrys obiektu powierzchniowego](#zaznaczenie-na-mapie). | US1.5, US3.3, US16.1, NFR-18, NFR-21 |
| Wybór piętra na V4 i C2 to jeden wspólny element. TalkBack czyta go jako wybór jednej opcji i mówi, które piętro jest wybrane. Wybrane piętro widać po obwódce i znaku, nie po samym kolorze. Każda opcja ma cel co najmniej 48 dp. Element ma dwa układy. Na V4 pytanie o piętro jest jedyną treścią kroku, więc opcje stoją w liście przycisków na całą szerokość. Na C2 piętro jest jednym z pól formularza, więc opcje to chipy w zawijanym wierszu. | NFR-17, NFR-18, US4.5, US7.2 |
| Kontrast tekstu, ikon statusu, ramek pól i fokusu według progów NFR-16. | NFR-16 |
| Tekst czytelny przy czcionce 200%, bez ucinania i nakładania. Przy skali czcionki co najmniej 1,5 obowiązują reguły z sekcji [Duża czcionka](#duza-czcionka). | NFR-20 |
| Status, oznaczenie wejścia i uciążliwość zawsze tekstem i ikoną. Uciążliwość 1–3 ma dla wszystkich ról napisy „Utrudnienie”, „Tylko z pomocą”, „Blokada”. Low-fi jest szare, a kolor nigdy nie jest jedynym nośnikiem znaczenia: na znakach mapy i w legendzie barwa powtarza kształt i słowo. Poza mapą uciążliwość to ten sam barwny znak co na mapie i w legendzie, jak oznaczenie wejścia. „Utrudnienie” jest żółte, „Tylko z pomocą” pomarańczowe, a „Blokada” ciemnoczerwona; w ciemnym motywie znak ma barwy ciemnej mapy (`mapa.kolory.znak_…`). Obok znaku stoi słowo uciążliwości zwykłym tekstem. Tła, ramki i plakietki znak nie ma, więc nie wygląda jak przycisk. | NFR-21; PW-13; US1.3, US2.1; [tokeny](tokeny.md#zasady) |
| Poza mapą oznaczenie wejścia to ten sam znak co na mapie i w legendzie. Dotyczy to karty budynku, listy wejść, arkusza wejścia i „Pomocy”. „Dostępne” ma koło ze znakiem ✓, „Niedostępne” ośmiokąt ze znakiem ✕, a „Brak danych” cichy romb ze znakiem ?. Obok znaku stoi słowo oznaczenia zwykłym tekstem. Plakietki, czyli barwnego tła, ramki i osobnej ikony, oznaczenie nie ma, więc nie wygląda jak przycisk. Znak ma rozmiar ikony w tekście i rośnie z czcionką. Znaczenie niosą kształt i słowo, nie sama barwa. | US2.1, US17.1, NFR-21; [tokeny](tokeny.md) |
| Każdy element interaktywny ma polską nazwę, rolę i stan dla TalkBack. | NFR-18 |
| Żadna czynność nie wymaga gestu złożonego: szczypania i innych gestów dwoma palcami, przesunięcia po ścieżce, przeciągania mapy, punktu albo karty ani długiego naciśnięcia. Każda taka czynność ma odpowiednik w dotknięciu przycisku. Zwykłe przewijanie listy palcem zostaje, a kartę rozwija też dotknięcie. | NFR-53; WCAG 2.5.1 [1], 2.5.7 [7] |
| Ekran działa w orientacji pionowej i poziomej. W szerokim oknie układ spełnia NFR-51. | NFR-22, NFR-51 |
| Brak limitów czasu na odpowiedź użytkownika. | NFR-23 |
| Komunikat o błędzie mówi, co się stało i co zrobić, bez żargonu technicznego, w najwyżej 2 zdaniach. Aplikacja mówi o sobie „my”, np. „Wczytujemy”, „Nie znaleźliśmy”. | NFR-42, NFR-50 [2]; WCAG 3.3.1, 3.3.3 [1]; PW-9, PW-10 |
| Gość i Odwiedzający nie widzą słów „zgłoszenie”, „karta budynku”, „synchronizacja”, „sieć”. Zamiast nich są napisy ze słownika użytkownika, np. „zdjęcie bariery”, „opis budynku”, „Bez internetu”. | NFR-42; PW-4, PW-5 |
| Nazwa budynku stoi zawsze obok jego symbolu, więc nie powtarza symbolu. Gdy nazwa z danych zawiera symbol swojego budynku, aplikacja pokazuje ją bez symbolu i bez łącznika przy nim. Przykłady: „A-1 / Budynek główny” to „Budynek główny”, a „Dom Studencki T-6 "Alcatraz"” to „Dom Studencki "Alcatraz"”. Gdy po symbolu zostają mniej niż 4 litery, np. „M-6 bis”, nazwa zostaje jak w danych. Nazwa z samych symboli, np. „T-22” przy konturze „T-18/T-22”, to brak nazwy. Kontur wspólny bez planów pięter pokazuje w nagłówku karty wszystkie swoje symbole, np. „T-18 / T-22”. Cudzysłowy zostają jak w danych. Nazwę bez symbolu niosą dane budynków (pole `label`), więc aplikacja i serwer czytają tę samą. Każdy ekran i TalkBack podają tę samą nazwę. | US1.1, US2, NFR-18; WCAG 3.2.4 |
| Bez wymaganej roli ekranu nie ma w aplikacji, a serwer odrzuca żądanie. | US14.1, NFR-33 |
| Po wysłaniu, zapisie albo decyzji formularz i kroki przed nim znikają z historii „Wstecz”. Dotyczy to V4 po „Wyślij”, C1 i C2 po „Zapisz”, C6 i C7 po „Wyślij do sprawdzenia”, C4 po wyborze wersji i R2 po decyzji. „Wstecz” z ekranu, który się wtedy otwiera, wraca tam, skąd zaczęła się praca, np. z C3 do L2 albo V2, z R1 do L2. | NFR-54, US4.7; WCAG 3.2.3 [1]; [mapa nawigacji](mapa-nawigacji.md), P2 |
| Zdjęcie bariery rezerwuje od początku wczytywania ramkę 4:3 na całą szerokość (do 240 dp wysokości) na tle `surfaceContainer`. Obraz mieści się wewnątrz bez przycinania. Podczas wczytywania i po błędzie ramka trzyma tę samą minimalną wysokość, więc treść pod zdjęciem nie przesuwa się po załadowaniu. Przy dużej czcionce stan błędu może urosnąć z napisem, aby nie ucinać „Spróbuj ponownie”. | NFR-12, NFR-20, NFR-50; US1.5, US2.2, US8.4 |

Układ ekranów w szerokim oknie z NFR-51 nie ma jeszcze makiet i zostanie zaprojektowany osobno. Do tego czasu obowiązuje sama miara NFR-51, a ekrany przewijają się jak w pionie. Wyjątek to pasek nawigacji z boku ekranu (decyzja 69). Nie ma osobnych makiet, a jego wygląd sprawdzimy na zrzucie ekranu przy czcionce 200%.

### Zaznaczenie na mapie {#zaznaczenie-na-mapie}

Wybrany budynek albo salę na planie wskazuje grubszy kontur. Na V1 wybrane wejście, barierę, windę, toaletę dostosowaną albo schody wskazuje pinezka, gdy arkusz tego punktu jest otwarty.

Czubek pinezki stoi w punkcie znaku, także wtedy, gdy znaki się stykają. Znak zachowuje rozmiar i obwódkę. Przy nieznanym piętrze zostaje znak zapytania.

Pod znakiem stoi nazwa wejścia (lub „Wejście”), typ bariery albo nazwa znaku na planie. Pinezka i podpis znikają po zamknięciu arkusza oraz po zmianie piętra, jeśli wybrany znak znika z mapy. Kształt i podpis pokazują wybór także bez koloru (NFR-21).

TalkBack czyta wybór wraz z danymi znaku (NFR-18, NFR-19).

- „Zaznaczone wejście: Wejście dostosowane, budynek A-1”
- „Zaznaczona bariera: Blokada, schody przy wejściu, budynek A-1”
- „Zaznaczona winda: 2 piętro, budynek A-1”

Znaczniki kroków do sali pozostają kółkami z numerami i nie otwierają arkuszy. Na R1 pozostaje pinezka zgłoszenia opisana w [wymaganiach R1](#r1-kolejka-weryfikacji).

### Duża czcionka {#duza-czcionka}

Reguły włączają się, gdy skala czcionki w telefonie wynosi co najmniej 1,5. Reguły 1 i 9 obowiązują przy każdej czcionce. Android 14 powiększa duży tekst mniej niż mały, więc zrzuty do kontroli NFR-20 robimy z tym samym skalowaniem [8].

| Nr | Reguła | Odwołanie |
| --- | --- | --- |
| 1 | Data i źródło oceny wejść to jeden napis bez ikony nad wejściami, np. „Z map i opisów, stan z 18.12.2023”. Przy dużej czcionce napis może się zawinąć. Gdy wejścia mają różne źródła, taki napis stoi przy każdym wejściu. | US17.3, BR-24, decyzja 48 |
| 2 | „Źródło: Politechnika Wrocławska” stoi raz na dole V2. Na mapie V1 nie stoi, nawet przy planie piętra: jest w legendzie pod „ⓘ”, a na mapie zostaje jeden podpis OpenStreetMap. Grupy pięter go nie powtarzają. | NFR-40, NFR-37, decyzja z 20.11.2026 |
| 3 | Podpis OpenStreetMap jest odnośnikiem w rogu mapy. Otwarty arkusz znaku na V1 może go zasłonić wraz z „ⓘ”. Przycisk „Zamknij” odsłania podpis i „ⓘ”. Pełna informacja o licencji jest pod „ⓘ” na mapie. | NFR-37 |
| 4 | Napisy na mapie i na mapie nieba (C1) rosną z czcionką, najwyżej 1,4 raza. Mapa chowa napisy, które się nie mieszczą, i nie nakłada ich. | NFR-20 |
| 5 | Karta V2 otwiera się od razu na cały ekran, bez mapy, i ma dwie wysokości: cały ekran i zamknięta. Przeciągnięcie w dół zamyka ją od razu. W nagłówku stoi „Zamknij”. Pod nazwą budynku stoi wiersz o wejściach ze zdaniem z odpowiedzią. Pod nim są „Pokaż wejście na mapie” (wypełniony) i „Pokaż budynek na mapie” (obrysowany), przed chipami. Tak samo w oknie co najmniej 600 dp i po wyborze z listy przy TalkBack | NFR-20, NFR-51, NFR-52, NFR-19, US3.2, US2.9 |
| 6 | Arkusz wejścia otwiera się na cały ekran, bez mapy, z „Zamknij” w nagłówku. Tak samo w oknie co najmniej 600 dp i po „Pokaż wejście na mapie” z karty V2 otwartej od razu na cały ekran. Fokus TalkBack staje na nagłówku arkusza, a cała treść arkusza czyta się bez mapy. „Zamknij” arkusza otwartego z karty V2 wraca do tej karty, a arkusza otwartego znakiem na mapie do mapy | NFR-19, NFR-20, NFR-51, NFR-18, US3.3 |
| 7 | V1 otwiera się na liście budynków. Widok mapy V1 się nie przewija: mapa wypełnia miejsce między polem wyszukiwania a paskiem dolnym. „Pokaż budynek na mapie” na V2 otwiera widok mapy, nie listę, i zapamiętuje „Mapa” jako ostatni wybór. | NFR-19, NFR-20, US2.9 |
| 8 | Pasek dolny ma zawsze jeden rząd z czterema pozycjami, a napis może mieć dwa wiersze. Pasek nie chowa się przy przewijaniu. Wszystkie napisy paska mają jeden rozmiar. Rosną z czcionką telefonu, dopóki najdłuższe słowo wszystkich napisów mieści się w pozycji, ale co najmniej 1,3 raza. Pozycja ma odstęp 8 dp od sąsiedniej pozycji i od krawędzi ekranu. Po jednoliterowym słowie stoi twarda spacja, więc „Konto i praca” łamie się jako „Konto / i praca”, a „i praca” liczy się jako jedno słowo. To [odstępstwo od pełnych 200%](dostepnosc.md#odstepstwo-pasek-dolny). Pasek z boku w oknie co najmniej 600 dp ma te same zasady napisów. Ma stałą szerokość, napis do dwóch wierszy, jeden rozmiar napisów i odstęp 8 dp. Gdy cztery pozycje nie mieszczą się w wysokości okna, pasek z boku się przewija. | NFR-20, NFR-51, NFR-52, wymagania wspólne (pasek dolny); decyzja 53, decyzja 69 |
| 9 | Widoczny napis mieści się w przycisku bez dzielenia wyrazów, a pełną nazwę czyta TalkBack, np. „Zgłoś barierę przy wejściu” i „Zgłoś barierę przy wejściu: Wejście Cesarskie”. Nazwa dla TalkBack zaczyna się od widocznych słów. | NFR-18, WCAG 2.5.3 [1] |
| 10 | Elementy przypięte to górny pasek, pas czynności i pasek dolny. Gdy razem zajęłyby ponad 25% wysokości okna w bieżącej orientacji, pas czynności nie jest przypięty. Jego czynności stoją na początku treści, przed pierwszą grupą, czynność główna pierwsza. V2 nie ma pasa czynności przy żadnej czcionce. Na ekranach decyzji, V5, L1, C4 i R2, stoją na końcu treści, po tekście potrzebnym do decyzji. Przypięte zostają górny pasek, w którym jest „Wstecz”, i pasek dolny. Pasek nawigacji z boku ekranu w oknie co najmniej 600 dp nie wlicza się do tych 25% (wymagania wspólne). Górny pasek może się chować przy przewijaniu. W szerokim oknie przyciski mają szerokość kolumny tekstu (NFR-51). | NFR-52, NFR-20, NFR-51 |
| 11 | Ikona ozdobna obok słowa znika, np. przy „Winda”, „Drzwi”, „Pokaż na planie” i w wierszach L2. Zostają ikony, które niosą znaczenie albo stan: oznaczenie wejścia, uciążliwość, strzałki, ptaszek wybranej pozycji przełącznika (C1, R1), ołówek przy „Popraw zgłoszenie” i pasek dolny. Na V1 zostaje też lupa w polu, a na V1, C1 i R1 sterowanie mapą ([zasady ikon](slownik-uzytkownika.md#metafory-i-ikony)). | NFR-20, NFR-21 |
| 12 | W opcji wyboru opis stoi pod tytułem na całą szerokość, a kółko i ikona tylko w wierszu tytułu. Tekst dzieli wyrazy po polsku i nie stoi w wąskich kolumnach. | NFR-20 |
| 13 | Karta sali otwiera się na cały ekran bez mapy. Na górze jest tylko „Wstecz”. Pod nagłówkiem stoją rodzaj sali z piętrem i wiersz budynku, dalej lista „Jak dojść” bez „Pokaż po kolei” i strzałek kroków. Wiersz kroku wejścia albo windy otwiera arkusz wejścia albo windy. Treść przewija się między górnym paskiem a paskiem dolnym, a „Pokaż na mapie” stoi na jej końcu (reguła 10). „Pokaż na mapie” pokazuje plan piętra z wyróżnioną salą, a „Wstecz” wraca do karty. | NFR-20, US18.1, US18.2, decyzja z 19.11.2026 |

Żadna reguła nie usuwa informacji wymaganej przez kryteria. Data i źródło przy wejściach zostają widoczne (US17.3), a podpis OpenStreetMap stoi przy każdej mapie (NFR-37).

Stany wspólne dla ekranów ról pracowniczych, które potrzebują serwera (R1, R2, K1, K2, K3). L1 ma własne stany, bez słowa „serwer”:

| Stan | Komunikat |
| --- | --- |
| Brak sieci | „Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie.” Przycisk „Spróbuj ponownie” (UC14 1a, UC15 1a, UC16 1b, UC17 1a, NFR-50). |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Spróbuj ponownie za kilka minut.” Przycisk „Spróbuj ponownie” (NFR-50, PW-10) |
| Brak uprawnień | Gdy rolę odebrano w trakcie pracy: „Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie.” Przycisk „Wróć do mapy” (V1). (US14.1, PW-10) |

## Gość niezalogowany i Odwiedzający

### V1 Mapa kampusu

| Pole | Treść |
| --- | --- |
| Cel | Osoba znajduje budynek na kampusie. |
| Role | wszystkie, bez logowania (US1.2) |
| Realizuje | US1.1…US1.8, US2.3, US2.5, US2.7, US3.3, US10.6, US16.1, US18.1…US18.3, początek US3.2 |
| Czynność główna | wyszukanie budynku po symbolu |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Mapa z konturami budynków. Pierwszy widok to budynek A-1 i sąsiednie budynki w powiększeniu 17, przy którym symbole budynków są czytelne | dane | US1, NFR-11, decyzja 80 | — |
| Budynki mają dwa poziomy, widoczne przed dotknięciem. Budynek szczegółowy (z planem pięter) ma pełne tło i gruby kontur. Budynek podstawowy, także po nieudanym imporcie planów, ma blade tło i cienki ciągły kontur. Poziomy różnią się jasnością tła i grubością konturu, nie samym odcieniem. Kontur obu ma co najmniej 3:1 do lądu mapy w obu motywach. Budynek podstawowy nie wygląda na wyłączony: ma podpis symbolu i działa po dotknięciu jak szczegółowy. Zaznaczenie budynku i przygaszenie sąsiadów są takie same dla obu poziomów. Przygaszony budynek PWr zostaje w swojej barwie, bledszy: tło ma krycie 40%, kontur `pwr_kontur` krycie 70% i tę samą grubość. Szczegółowy i podstawowy różnią się więc także przygaszone. Kontur przygaszonego budynku ma co najmniej 3:1 do lądu i do swojego tła w obu motywach. Gdy widać plan piętra budynku, jego tło i kontur z OpenStreetMap są ukryte (wiersz o wyglądzie planu) | dane | US16.1, US16.2, BR-G1, BR-G3, NFR-16, NFR-21 | — |
| Ścieżki dostosowane z mapy kampusu PWr jako piaskowa wstęga z cienkimi turkusowymi brzegami (`sciezka_wnetrze`, `sciezka_brzeg`). Leżą nad ulicami, a pod nazwami ulic, rzek i dzielnic oraz pod budynkami PWr, od powiększenia 15,5; przy przybliżeniu wstęga jest szersza (`mapa.sciezka`). Piasku nie ma nigdzie indziej na mapie, więc ścieżka nie wygląda jak ulica, chodnik ani budynek. Brzeg ma co najmniej 3:1 do lądu, zieleni i ulicy w obu motywach. Chodniki OpenStreetMap zostają bez zmian; od ścieżki odróżnia je barwa, a znaczenie ścieżki podaje legenda. Ścieżka nie jest celem dotyku i nie ma napisu na mapie; jej znaczenie podaje pozycja „Ścieżka dostosowana” w legendzie. Słowo „Dostępne” zostaje przy znaku wejścia, ścieżka go nie powtarza. Bez sieci ścieżki pochodzą z ostatniej kopii z serwera, jak budynki | dane | US1.4, NFR-10, NFR-16, NFR-21, NFR-40 | — |
| Pochylnie z mapy kampusu PWr jako znak: zaokrąglony kwadrat w kolorze `tusz` z klinem w kolorze `papier`, z obwódką w kolorze `papier`, jak znaki planu. Znak stoi nad ścieżką dostosowaną, a pod znakami wejść i barier, od powiększenia 16,25; rośnie z 22 dp do 30 dp przy powiększeniu 17,5. Słowo „Pochylnia” obok znaku od powiększenia 17,5, tylko wtedy, gdy mieści się bez nakładania; symbole budynków i słowa barier mają pierwszeństwo. Pochylnia nie jest celem dotyku; jej znaczenie podaje pozycja „Pochylnia” w legendzie. Bez sieci pochylnie pochodzą z ostatniej kopii z serwera, jak ścieżki | dane | US1.4, NFR-10, NFR-16, NFR-21, NFR-40 | — |
| Bariery ze zgłoszeń przyjętych jako znak uciążliwości: znaczenie niesie kształt, a barwa je powtarza: „Utrudnienie” żółte, „Tylko z pomocą” pomarańczowe, „Blokada” ciemnoczerwona. Jasność maleje z uciążliwością, więc kolejność widać też w odcieniach szarości. Znak ma obwódkę w kolorze tła mapy i kontur w kolorze tekstu mapy. „Blokada” jest widoczna przy każdym powiększeniu, „Utrudnienie” i „Tylko z pomocą” od powiększenia 16. Przy zaznaczonym budynku jego bariery są widoczne przy każdym powiększeniu, jak jego wejścia. Przy małym powiększeniu bariera nie znika z aplikacji: lista budynków i karta budynku pokazują wszystkie bariery bez względu na powiększenie mapy. Słowo obok znaku od powiększenia 17, nie przy krawędzi mapy i tylko wtedy, gdy mieści się bez nakładania; symbole budynków mają pierwszeństwo. Znak ma nazwę dla TalkBack, np. „Blokada: schody przy wejściu, budynek A-1”. Bariera zgłoszona przy wejściu ma punkt wejścia (BR-8). Położenie jej znaku zależy od tego, czy widać znak tego wejścia (wiersz o znakach wejść). Gdy znaku wejścia nie widać, znak bariery stoi w punkcie wejścia, więc przy oddaleniu nie odjeżdża od budynku. Gdy znak wejścia jest widoczny, znak bariery stoi tuż obok niego, na zewnątrz ściany, prostopadle do najbliższej ściany konturu; w narożniku wklęsłym nad wejściem. Środki znaków dzieli 30 dp, połowa znaku wejścia i połowa znaku bariery, więc znaki się stykają i nie zasłaniają. Znak zmienia położenie w chwili pojawienia się albo zniknięcia znaku wejścia. Kilka barier przy jednym wejściu daje jeden znak, tej o najwyższej uciążliwości; wszystkie są w arkuszu wejścia. Znak wejścia i jego oznaczenie się nie zmieniają | dane | US1.3, US2.2, US10.6, BR-8, NFR-16, NFR-17, NFR-18, NFR-19, NFR-20, NFR-21; decyzja z 19.11.2026 | — |
| Dotknięcie znaku bariery otwiera arkusz i pokazuje pinezkę z typem bariery pod znakiem | czynność | US1.5, NFR-17 | arkusz bariery na tym ekranie |
| Dotknięcie znaku wejścia, także cichego znaku „Brak danych”, otwiera arkusz wejścia. Obszar dotyku znaku to kwadrat 48 dp wokół jego punktu, a gdy obraz jest większy, sam obraz. Tak samo jest przy znaku bariery i znaku planu. Dotknięcie znaku nie zaznacza budynku i nie rusza kamery. Znak działa też przy otwartej karcie budynku | czynność | US2.1, US3.3, NFR-17 | arkusz wejścia na tym ekranie |
| Obraz znaku pod palcem wygrywa z obszarem dotyku znaku obok. Dzięki temu stykające się znaki wejścia i bariery dotyka się osobno (NFR-17, US1.5, US2.1). Gdy palec nie trafia w żaden obraz, liczy się kolejność obiektów w zasięgu dotknięcia. Wygrywa pierwszy z nich: bariera, wejście, znak planu (winda, toaleta dostosowana, schody), sala z numerem na planie, budynek. Dotknięcie obok znaku, poza jego obszarem dotyku, trafia w budynek albo w pustą mapę. Dotknięcie pustej mapy zamyka arkusz bariery, wejścia, toalety i schodów | czynność | US1.5, US2.1, US16.1, NFR-17, NFR-53 | arkusz tego obiektu; pusta mapa: V1 bez arkusza |
| Wejścia do budynków z danych jako znaki: ✓ dla „Dostępne”, ✗ dla „Niedostępne” i romb ze znakiem ? dla „Brak danych”. Znaki różnią się kształtem; „Dostępne” ma też barwę zielononiebieską, a „Niedostępne” i „Brak danych” mają kolor tekstu mapy. Znak ma obwódkę w kolorze tła mapy i kontur w kolorze tekstu mapy. Na mapie znak „Brak danych” jest cichy: pusty romb (wnętrze w kolorze papieru mapy, kontur i znak ? w kolorze tekstu) o wielkości 20 dp zamiast 28 dp. Znak nie znika, bo jego brak wyglądałby jak brak wejścia, a jego obszar dotyku jest taki sam jak przy innych znakach wejść. W legendzie, na V2 i w arkuszu wejścia „Brak danych” ma zwykłą wielkość i słowo. Bez zaznaczonego budynku znaki wszystkich wejść są widoczne od powiększenia 17, także wtedy, gdy żadne wejście nie jest zaznaczone. Przy mniejszym powiększeniu znaków wejść nie ma, żeby nie zasłaniały symboli budynków. Przy zaznaczonym budynku znaki mają tylko jego wejścia, przy każdym powiększeniu. Sąsiednie budynki są wtedy przygaszone i zachowują podpisy | dane | US1.4, US2.1, US2.8, NFR-16, NFR-17, NFR-21; decyzje 134, 153, 162 | — |
| Wejście z otwartym arkuszem jest zaznaczone według [wspólnej zasady](#zaznaczenie-na-mapie). Arkusz wejścia otwiera „Pokaż wejście na mapie”, dotknięcie wiersza na liście „Wejścia” na V2 albo dotknięcie znaku wejścia na mapie. Pinezka i podpis są widoczne przez cały czas, gdy arkusz jest otwarty. Znikają razem z arkuszem, czymkolwiek go zamknięto: „Zamknij”, „Wstecz”, wierszem budynku albo dotknięciem pustej mapy. Wejście bez nazwy w danych ma podpis „Wejście”. Opisu położenia z BR-27 podpis nie ma, więc mieści się przy dużym kroju. Pozostałe wejścia zostają widoczne. Po przycisku i wierszu listy kamera ustawia się na tym wejściu, nie na budynku: wejście stoi na środku części mapy nad arkuszem wejścia. „Pokaż wejście na mapie” wybiera na V1 widok „Mapa”, jak „Pokaż budynek na mapie”. TalkBack czyta np. „Zaznaczone wejście: »Wejście dostosowane«, budynek A-1”, z budynkiem ekranu, z którego użyto przycisku, a bez nazwy „Zaznaczone wejście: Wejście, budynek A-1”. Razem z zaznaczeniem otwiera się arkusz tego wejścia | dane | US3.3, US2.1, NFR-19, NFR-18; decyzje 134, 135 | arkusz wejścia na tym ekranie |
| Pole symbolu budynku z podpowiedzią „Szukaj budynku, np. C-13”; przyjmuje też zapis „c13” oraz symbol budynku i numer sali w dowolnej kolejności, rozdzielone odstępem, przecinkiem albo ukośnikiem, ze słowem „sala” albo bez, np. „A-1 329”, „329 A-1”, „sala 329 A-1”, „A-1/329”. Przy zwykłym kroju podpowiedź ma jeden wiersz, a gdy się nie mieści, kończy się wielokropkiem. Przy kroju od 150% pusta podpowiedź łamie się na kilka wierszy, żeby była czytelna w całości. Pole z tekstem ma zawsze jeden wiersz, więc przy zwykłym kroju mapa nie skacze przy wpisywaniu. Pole jest białe (`surfaceContainerLowest`) z ramką `outline` 2 dp (`obramowania.pole`). Szapka nie ma cienia: od mapy oddziela ją linia 1 dp `outlineVariant` | czynność główna | US1.1, US18.1, PW-8, NFR-16 | V2; sala: stan „znaleziona sala” |
| „×” w polu po wpisaniu tekstu; nazwa dla TalkBack „Wyczyść pole” | czynność | US1.1, NFR-53, NFR-17, NFR-18 | pole puste, mapa |
| Wyniki pod polem od pierwszego wpisanego znaku: budynki, których symbol zaczyna się od wpisanego tekstu. Wielkość liter i myślnik nie mają znaczenia, jak przy pełnym symbolu: „A” daje A-1, A-2 i dalsze, a „a1” daje A-1, A-10, A-11. Kolejność symboli jest naturalna, więc pełny symbol stoi pierwszy. Od trzeciego znaku wyniki obejmują też budynki, których nazwa ma słowo zaczynające się od wpisanego tekstu. Wielkość liter i polskie znaki nie mają znaczenia: „glow” i „głów” dają A-1 „Budynek główny”. Najpierw stoją budynki dopasowane po symbolu, potem po nazwie. Każdy wynik to karta z symbolem, nazwą budynku według wymagań wspólnych i „Budynek C-13”. Wybór symbolu ze wspólnego konturu bez planów pięter, np. „T-18” albo „T-22”, otwiera wspólną kartę V2 z oboma symbolami w nagłówku. Cała karta jest jednym przyciskiem. Budynek z innego terenu ma dopisek terenu. Lista przewija się nad klawiaturą | czynność główna | US1.1, NFR-17, NFR-18 | V2 |
| Wyniki sal: gdy tekst ma pełny symbol budynku i cyfrę, w dowolnej kolejności, np. „A-1 3” albo „3 A-1”, wyniki to sale tego budynku. Numer sali zaczyna się od wpisanych cyfr. Sale stoją po piętrach od najniższego, a na piętrze według numeru. Pełny numer, np. „A-1 329”, daje jedną salę. Wynik sali to karta jak wynik budynku: ten sam znak z symbolem budynku i „Sala 329”. Pod numerem stoi rodzaj pomieszczenia z danych PWr z piętrem, np. „Sala wykładowa · 2 piętro”. Rodzaju „Inne” nie ma, zostaje samo piętro. Cała karta jest jednym przyciskiem; TalkBack czyta „Sala 329, budynek A-1, sala wykładowa, 2 piętro” | czynność główna | US18.1, NFR-17, NFR-18, decyzja z 19.11.2026 | stan „Znaleziona sala” |
| Sam numer sali bez budynku, np. „329” albo „sala 329”: aplikacja nie szuka sali, a pod polem zamiast wyników stoi „Dopisz budynek do numeru sali. Na przykład: A-1 329.” | dane | US18.1, NFR-50, decyzja z 19.11.2026 | — |
| Arkusz „Legenda” spod „ⓘ”: znaki mapy i planu, każdy z napisem. Budynki mają dwie pozycje: „Budynek z planem pięter” i „Budynek bez planu”. Ścieżki mają pozycję „Ścieżka dostosowana” z odcinkiem wstęgi w barwach mapy, a pochylnie pozycję „Pochylnia” ze znakiem z mapy. Znaki wejść i barier mają w legendzie te same kształty i barwy co na mapie. Kolejność pozycji: dwa poziomy budynków, „Ścieżka dostosowana”, „Pochylnia”, trzy wejścia, trzy bariery, pozycje planu. Gdy widać plan, ma też pozycje planu: sala, korytarz, klatka schodowa, winda, toaleta dostosowana i zgłoszenie bez piętra („?”). Na końcu „Źródła danych” (wiersz o „ⓘ”). Gdy treść się nie mieści, na dole stoi „Przewiń w dół ↓” | czynność | US1.4, NFR-21, NFR-40 | arkusz „Legenda” na tym ekranie |
| Plan piętra pojawia się sam przy powiększeniu co najmniej 17. Dostaje go budynek szczegółowy na środku mapy albo najbliżej środka, nie dalej niż 60 m. Plan ma naraz jeden budynek; inne budynki PWr są przygaszone jak przy zaznaczeniu budynku: w swojej barwie, bledsze. Plan przechodzi na inny budynek, gdy ten jest bliżej środka o więcej niż 10 m | dane | US16.1, NFR-11 | plan piętra na tym ekranie |
| Plan i kolumna pięter znikają przy powiększeniu poniżej 16,5, gdy na środku nie ma budynku szczegółowego, i po zmianie terenu. Różnica między 17 a 16,5 chroni plan przed miganiem | czynność | US16.1, US1.6, NFR-23 | V1 bez planu |
| Plan przy powiększeniu planu trzyma budynek otwartej karty albo arkusza. Dotyczy to karty budynku V2, arkusza wejścia, karty sali i arkuszy windy, toalety dostosowanej i schodów. Budynek szczegółowy dostaje plan do 60 m od środka części mapy widocznej nad kartą albo arkuszem. Karta i arkusz zasłaniają dół mapy, więc od tego środka liczą się też kadr kamery i położenie przycisków. Dla budynku podstawowego planu sąsiada ani kolumny pięter nie ma. Karta albo arkusz na cały ekran (duża czcionka) nie ma mapy, więc tej zasady nie ma | dane | US16.1, US16.2, US2.8, US18.1, US18.2, BR-G1; decyzje z 19.11.2026 i 21.11.2026 | — |
| Przesunięcie wybranego budynku dalej niż 60 m albo pomniejszenie poniżej 16,5 chowa plan. Karta budynku, arkusz wejścia i karta sali zostają otwarte bez planu sąsiada; wyróżnienie sali znika razem z planem i wraca z nim. Arkusz windy, toalety dostosowanej i schodów zamyka się, bo jego znaku nie ma już na mapie; tak samo po wyborze innego piętra. Zamknięcie każdej karty i każdego arkusza, czymkolwiek, zostawia widok mapy bez ruchu i od razu przywraca wybór planu od środka mapy. „Pokaż na planie” z V2 zamyka kartę i zostawia wybór od środka | czynność | US16.1, US18.2, NFR-15, NFR-56; decyzje z 19.11.2026 i 21.11.2026 | — |
| Bez otwartej karty środek mapy wewnątrz budynku podstawowego (poza budynkiem szczegółowym) chowa plan sąsiada i kolumnę pięter. Po wyjściu poza ten kontur o ponad 5 m albo wejściu w budynek szczegółowy plan sąsiada może wrócić. Ten zapas 5 m chroni plan przed miganiem przy krawędzi | dane | US16.2, BR-G1, NFR-11; decyzja z 19.11.2026 | — |
| Na start plan pokazuje piętro wejścia zalecanego z warstwy wejść PWr; gdy dane go nie podają, parter. Ostatnio wybrane piętro budynku aplikacja pamięta do zamknięcia: powrót do tego budynku pokazuje to piętro. Znaleziona sala i „Pokaż na planie” z V2 pokazują swoje piętro i zapisują je jako ostatnio wybrane | dane | US16.1, US18.1, US18.2, BR-25 | — |
| Plan piętra: pomieszczenia w kolorach według funkcji (sala, korytarz, klatka schodowa w kreski, toaleta dostosowana w kropki, winda). Winda, toaleta dostosowana i schody mają same znaki bez podpisu na mapie; ich opis daje dotknięcie znaku i legenda pod „ⓘ”. Wejścia stoją tylko na swoim piętrze, obok zgłoszenia przyjęte. Ściana zewnętrzna to ściana piętra z planu PWr, czyli obrys wszystkich pomieszczeń piętra, nie kontur OpenStreetMap. Rysuje ją jedna linia 2,4 dp w barwie konturu budynku PWr. Tło i kontur tego budynku z OpenStreetMap są ukryte, dopóki widać jego plan. Obrys zaznaczonego budynku też idzie po ścianie piętra. Szczeliny między pomieszczeniami mają barwę korytarza, a nie barwę budynku ani podkładu. Ściany pomieszczeń mają 0,75 dp przy powiększeniu 17 i rosną liniowo do 1,5 dp przy 20. Symbol budynku jest ukryty, dopóki plan tego budynku jest widoczny | dane | US16.1, NFR-16, NFR-21, NFR-40; decyzja 44 | — |
| Znaki planu: winda, toaleta dostosowana i schody. Wszystkie mają ten sam rozmiar: 20 dp przy powiększeniu 17, 24 dp przy 18 i 28 dp od 19, liniowo między progami. Ważność niesie jasność, nie rozmiar: winda i toaleta dostosowana mają jasny znak na ciemnym polu, schody ciemny znak na jasnym polu. Winda i toaleta leżą nad schodami. Znak ma obwódkę w kolorze tła mapy i kontur w kolorze tekstu mapy, jak znaki wejść i barier. Obszar dotyku znaku ma co najmniej 48 dp przy każdym rozmiarze obrazu | dane | US16.1, NFR-16, NFR-17, NFR-21 | — |
| Numery sal od powiększenia 19, poziomo, z kontrastem co najmniej 4,5:1 w obu motywach. Kilka pomieszczeń z tym samym numerem ma jeden numer, na największym; pomieszczenia mniejsze niż 20 m² i bez numeru nie mają numeru | dane | US16.1, US18.1, NFR-21 | — |
| Zgłoszenie przyjęte z piętrem stoi tylko na swoim piętrze. Zgłoszenie bez piętra stoi na każdym piętrze ze znakiem „?” i słowem „piętro nieznane” dla TalkBack | dane | US16.1, BR-14, NFR-18 | arkusz bariery na tym ekranie |
| Kolumna pięter przy lewej krawędzi mapy pod przyciskiem terenu, jak przyciski w windzie: napis „Piętro”, nad nim najwyższe piętro, wybrane w kółku, nie samym kolorem. Etykiety z danych PWr skrócone do numeru, np. „2”; TalkBack czyta pełną etykietę „2 piętro”. Kolumna jest zawsze rozwinięta, gdy widać plan, bez zwijania do jednego przycisku: osoba widzi wszystkie piętra i wybiera jednym dotknięciem. Kończy się nad podpisem OpenStreetMap. Przy otwartym panelu terenu kolumny nie ma. Gdy się nie mieści, przewija się, a napis „Piętro” i wybrane piętro są zawsze widoczne. Zmiana piętra nie przesuwa mapy. Kolumna nie zasłania budynku po ruchu aplikacji. „Pokaż na planie” i „Pokaż budynek na mapie” stawiają budynek szczegółowy na środku części mapy obok kolumny. Powiększenie zostaje 17, bo poniżej 16,5 plan znika. Budynek szerszy niż ta część mapy wystaje w prawo, nie pod kolumnę. Gest osoby nie przesuwa mapy z powodu kolumny | czynność | US16.1, US1.3, US1.4, NFR-17, NFR-18, NFR-20, NFR-21 | plan innego piętra |
| Budynek podstawowy przy powiększeniu co najmniej 17 bez otwartej karty V2: nad mapą zdanie „Nie mamy planu pięter tego budynku.”, bez kolumny pięter. W otwartej karcie V2 zdanie jest już w jej treści | dane | US16.2, BR-G1; decyzja z 19.11.2026 | — |
| Dotknięcie windy na planie pokazuje pinezkę z podpisem „Winda” pod znakiem i otwiera arkusz z nagłówkiem „Winda” i „Zamknij”. Pod nim stoi wiersz budynku „Budynek A-1” jak w arkuszu toalety (V2 na niskiej wysokości). Pod wierszem budynku stoi piętro osobnym wierszem, na przykład „Parter”. Dalej klatki, przy których stoi winda, tylko gdy są w danych, i „Jedzie na: …” z piętrami windy, przy bieżącym piętrze „(tutaj jesteś)”. Przy dużej czcionce arkusz zajmuje cały ekran, jak karta sali. Na końcu arkusza obrysowany „Zgłoś barierę przy windzie”, jak w arkuszu toalety | czynność | US2.8, US16.1, US18.2, NFR-20, US4.6; decyzja z 19.11.2026 | wiersz: V2 na niskiej wysokości; arkusz windy na tym ekranie |
| Dotknięcie toalety dostosowanej na planie otwiera arkusz toalety i pokazuje pinezkę z podpisem „Toaleta dostosowana” pod znakiem. Dotknięcie znaku nie zaznacza budynku i nie rusza kamery | czynność | US16.1, NFR-17 | arkusz toalety na tym ekranie |
| Dotknięcie schodów na planie otwiera arkusz schodów i pokazuje pinezkę z podpisem „Schody” pod znakiem. Dotknięcie znaku nie zaznacza budynku i nie rusza kamery | czynność | US16.1, NFR-17 | arkusz schodów na tym ekranie |
| Dotknięcie pomieszczenia z numerem w danych PWr na planie otwiera kartę sali, tę samą co wynik wyszukiwania. Działa od powiększenia 19, przy którym widać numery sal. Korytarze, klatki schodowe i pomieszczenia bez numeru nie reagują; dotknięcie ich trafia w budynek. Znak windy, schodów i toalety ma pierwszeństwo | czynność | US18.1, NFR-17, decyzja z 19.11.2026 | karta sali na tym ekranie |
| Po pojawieniu się planu i po zmianie piętra TalkBack mówi raz, np. „Plan budynku A-1, parter. Piętra: piwnica, parter, 1, 2, 3” | dane | NFR-18, NFR-19 | — |
| Źródło danych planów i wejść PWr („Źródło: Politechnika Wrocławska”) stoi w legendzie pod „ⓘ” przy każdej skali czcionki. Pod podpisem OpenStreetMap na mapie V1 nie stoi (reguła 2 sekcji [Duża czcionka](#duza-czcionka)) | dane | NFR-40, decyzja z 20.11.2026 | — |
| Przycisk tekstowy po prawej w wierszu tytułu „Mapa kampusu”: „Pokaż listę” na mapie, „Pokaż mapę” na liście. Ma kolor `primary`, ikonę listy albo mapy obok słowa i cel 48 dp. Przy skali czcionki co najmniej 1,5 ikona znika, a przycisk stoi pod tytułem. Domyślnie mapa, a przy tej skali lista. Aplikacja pamięta ostatni widok także po zamknięciu. Przełącznik segmentowy zostaje na C1 i R1, gdzie zmienia widok wewnątrz treści, a tu zmienia się cały ekran | czynność | NFR-19, NFR-20, NFR-17 | lista albo mapa na tym ekranie |
| Lista budynków pod polem symbolu: wszystkie budynki w kolejności katalogu, naturalnej po symbolu, np. A-1, A-2 … A-10. Pole symbolu zawęża listę tym samym wyszukiwaniem co nad mapą: od pierwszego znaku po początku symbolu (wielkość liter i myślnik bez znaczenia). Od trzeciego znaku dopasowuje też początek słów nazwy: „c13” zostawia C-13, a samo „C” budynki od tej litery | dane | NFR-19, US1.1, decyzja 90 | — |
| Dotknięcie wolnego miejsca na liście zdejmuje fokus z pola i chowa klawiaturę. Tekst w polu i filtr zostają. Dotknięcie wiersza od razu otwiera budynek. Przeciągnięcie przewija listę | czynność | US3.1, NFR-17, NFR-19 | — |
| Budynek na liście to jedna karta, która w całości jest jednym przyciskiem. Karta pokazuje symbol, nazwę z danych, a gdy nazwa jest, pod nią „Budynek A-6”, także przy czcionce 200%. Pod nimi bariery: znak, uciążliwość, typ i miejsce, albo „Brak zgłoszonych barier”. Gdy w telefonie nie ma jeszcze danych o barierach, karta nie mówi nic o barierach, także nie „Brak zgłoszonych barier”. TalkBack czyta symbol, nazwę i bariery, a potem „otwiera opis budynku”. Przycisk „Pokaż budynek na mapie” jest na karcie V2 otwartej od razu na cały ekran, a nie na liście | czynność | US2.5, US2.7, US2.9, NFR-10, NFR-17, NFR-18, NFR-19, NFR-20; decyzja 90 | V2 |
| Dotknięcie konturu wyróżnia kontur i otwiera kartę V2 na niskiej wysokości. Powiększenie mapy się nie zmienia. Mapa rusza się tylko w jednym przypadku: przed pojawieniem się karty cały kontur był widoczny na mapie, a karta zasłoniłaby jego część. Wtedy mapa przesuwa się w górę tak, żeby dolny brzeg konturu stał 16 dp nad kartą. Przesuwa się najwyżej tak daleko, żeby górny brzeg konturu został co najmniej 16 dp pod górą wolnej części mapy. Przesunięcie jest tylko pionowe i trwa 300 ms, razem z kartą i z tą samą krzywą. W pozostałych przypadkach mapa stoi: kontur ucięty brzegiem ekranu, mapa przybliżona do wnętrza budynku, kontur widoczny nad kartą. Osoba sama wybrała widok i powiększenie, więc mapa poprawia tylko to, co zasłoniła sama karta. Ruch, o który osoba nie prosiła, gubi jej miejsce na ekranie, zwłaszcza przy powiększeniu ekranu. Ponowne dotknięcie tego samego budynku nie rusza mapy, bo ta sama czynność nie przesuwa mapy drugi raz. Tak samo jest przy kolejnym budynku przy otwartej karcie i po wyborze w arkuszu „Który budynek?”. Przy otwartej karcie kontur jest widoczny, gdy cały leży w granicach mapy, także pod kartą. Wybór budynku z wyszukiwania przesuwa budynek na środek wolnej części mapy nad kartą. Po wyborze z listy budynków mapa nie rusza się (wiersz niżej). Rodzaj ruchu po wyszukiwaniu zależy tylko od położenia budynku względem widoku. Cały kontur widoczny: przesunięcie (300 ms). Środek budynku najwyżej jedną szerokość wolnej mapy od środka widoku: przesunięcie (400 ms). Dalej na tym samym terenie: przelot, czyli oddalenie, przesunięcie i przybliżenie jednym ruchem (ok. 1 s). Na innym terenie: ten sam przelot (najwyżej 1,5 s). Dalszy cel przyspiesza przelot, a nie zamienia go na skok. Krzywa przesunięcia to krzywa MapLibre, cubic-bezier(0; 0; 0,25; 1). Po ruchu mapa jest blisko budynku. Kontur zajmuje połowę szerokości albo wysokości wolnej części mapy, najwyżej przy powiększeniu 19. Liczy się wymiar, który pierwszy dojdzie do połowy. Gdy osoba ma już większe powiększenie, a kontur się mieści, powiększenie zostaje. Kontur, który się nie mieści, oddala mapę tylko tyle, żeby zmieścił się w wolnej części mapy. Górny pasek z polem wyszukiwania i przełącznikiem „Mapa / Lista budynków” leży nad mapą. Pasek odjeżdża w górę za krawędź ekranu, a karta wjeżdża od dołu. Oba ruszają się w tym samym czasie i z tą samą krzywą co mapa. Mapa nie rusza, zanim karta ma wysokość. Cel ruchu liczy się raz, z wysokości karty po pomiarze, więc karta, która potem urośnie, nie rusza mapy drugi raz. Gdy mapa stoi, ruszają się w 300 ms z krzywą przesunięcia mapy. Mapa przy tym nie zmienia wysokości. Po zamknięciu karty pasek wraca z góry, a karta zjeżdża w dół. Otwarta karta nie wjeżdża od nowa, tylko na początku ruchu zmienia treść na nowy budynek. Przy systemowym „Usuń animacje” mapa i karta od razu pokazują stan końcowy | czynność | US2.8, US3.2, NFR-56; WCAG 2.3.3, 3.2.5 [1]; decyzja 115 | V2 na niskiej wysokości |
| Dotknięcie budynku na liście przy zwykłej czcionce i bez TalkBack: V1 przełącza się na mapę z zaznaczonym budynkiem. Mapa pokazuje od razu widok, w którym kończy się ruch z wiersza wyżej: budynek na środku wolnej części mapy nad kartą. Mapy przed chwilą nie było na ekranie, więc ruch od jej dawnego widoku nie pokazywałby, skąd i dokąd. Jego długość zależałaby też od przypadkowego dawnego widoku. Rusza się tylko karta: wjeżdża od dołu w 300 ms z krzywą przesunięcia mapy. Mapa nie jest wtedy pusta: kontur budynku i podkład widać najpóźniej razem z końcem wjazdu karty. Przy systemowym „Usuń animacje” karta stoi od razu. Przy skali czcionki co najmniej 1,5 albo z TalkBack karta V2 otwiera się od razu na cały ekran | czynność | US2.8, US3.2, NFR-19, NFR-20, NFR-56; decyzja 115 | V2 na niskiej wysokości; duża czcionka i TalkBack: V2 na cały ekran |
| Dotknięcie konturu wspólnego kilku symboli bez planów pięter, np. „T-18/T-22”: wspólna karta obu symboli bez arkusza „Który budynek?” | czynność | US2, US3.2, decyzja 85 | V2, wspólna karta |
| Dotknięcie wewnątrz konturu jednego budynku otwiera ten budynek, także gdy blisko jest inny kontur. Znak bariery, wejścia albo planu w zasięgu dotknięcia ma pierwszeństwo przed konturem (wiersz o kolejności obiektów). Dotknięcie tuż obok konturu, poza konturami, liczy się jako dotknięcie konturu. Zasięg to co najmniej połowa celu dotyku, czyli 24 dp od konturu. Gdy dotknięcie obok konturów ma w zasięgu dwa kontury, otwiera się arkusz „Który budynek?” z ich symbolami | czynność | US2, NFR-17, decyzja 85 | V2 albo arkusz „Który budynek?” |
| Przyciski „Powiększ” i „Pomniejsz” | czynność | NFR-53; WCAG 2.5.1 [1]; PW-8 | — |
| Cztery przyciski przesuwania mapy na mapie, w krzyżu: strzałka w górę u góry, w lewo i w prawo po bokach, w dół u dołu. Każdy ma co najmniej 48 dp i nazwę dla TalkBack, np. „Przesuń mapę w górę” | czynność | NFR-53, NFR-17, NFR-18; WCAG 2.5.7 [7]; PW-8, PW-11 | — |
| Przyciski przesuwania i powiększania stoją po prawej, obok przycisku terenu. Gdy brakuje miejsca obok, cały zestaw stoi pod przyciskiem terenu. Przy zmianie nazwy terenu układ zmienia się bez nakładania przycisków | czynność | NFR-20, NFR-53 | — |
| Mapa zostaje na wybranym terenie: cały widok mapy, nie tylko jego środek, leży w prostokącie terenu. Prostokąt jest opisany na granicy terenu (BR-G5), powiększony o 100 m z każdej strony. Prostokąt nie jest rozszerzany do proporcji mapy, bo pas miasta bez budynków PWr nie należy do terenu. Najmniejsze powiększenie to takie, przy którym prostokąt wypełnia mapę w krótszym wymiarze. Bardziej oddalić mapy się nie da, a wzdłuż dłuższego wymiaru mapę się przesuwa. Strzałki przesuwania, przeciąganie palcem i „Pomniejsz” zatrzymują się, gdy brzeg mapy dojdzie do brzegu prostokąta. Mapa nie rysuje granicy terenu i nie wybiela otoczenia: podkład w całym prostokącie wygląda tak samo, a budynki PWr wyróżnia ich kolor. Brzeg terenu osoba poznaje po tym, że mapa dalej nie jedzie, a ulice i place wokół zostają punktami orientacyjnymi | czynność | US1.7, BR-G5, NFR-39, NFR-53 | — |
| Przycisk terenu w lewym górnym rogu mapy: znak „⊙”, nazwa wybranego terenu, np. „Kampus główny”, i znak rozwinięcia „▾”. Nazwa dla TalkBack „Teren: Kampus główny. Zmień teren” | czynność | US1.6, US1.7, NFR-17, NFR-18 | panel „Teren na mapie” na tym ekranie |
| Panel „Teren na mapie” pod przyciskiem terenu: nagłówek z „Zamknij”, który się nie przewija, i lista terenów. Każdy teren to jeden przycisk z nazwą, liczbą budynków (symboli budynków) i odległością od wybranego terenu, np. „Wittigowo · 9 budynków · 2 km”. Wybrany teren ma znak „✓” i napis „wybrany”. Po otwarciu fokus TalkBack stoi na nagłówku, a po zamknięciu wraca na przycisk terenu. „Wstecz” i dotknięcie obok zamykają panel. Panel i „Legenda” nie są otwarte razem | czynność | US1.7, NFR-17, NFR-18, NFR-20, NFR-21 | V1 z wybranym terenem |
| Po wyborze terenu z listy mapa pokazuje wszystkie budynki terenu w części mapy, której nie zasłaniają przyciski mapy, z marginesem. Powiększenie jest nie większe niż 17, przy którym symbole budynków są czytelne. Środek prostokąta terenu nie wystarcza: teren z kilku części, np. Wittigowo, ma środek między częściami i ekran byłby pusty. Gdy budynki wąskiego terenu nie mieszczą się obok przycisków przy najmniejszym powiększeniu terenu (wiersz o prostokącie terenu), stoją na środku całej szerokości mapy. Przyciski mogą wtedy zasłonić brzeg skrajnego budynku. Wygrywa zasada, że wszystkie budynki terenu są na ekranie, a widok nie wychodzi poza teren. Mapa jedzie do tego widoku jednym ruchem według tej samej reguły co do budynku z wyszukiwania (wiersz o dotknięciu konturu). Nowy widok w zasięgu ekranu to przesunięcie, a dalszy to przelot, którego czas rośnie z drogą (najwyżej 1,5 s). Krótki przelot przez 2 km wygląda jak skok, a ciągły ruch pokazuje, w którą stronę leży nowy teren. Pani Halina (PE-1) nie zgubi wtedy miasta. Panel zamyka się w chwili startu ruchu. Przy systemowym „Usuń animacje” mapa od razu stoi na nowym terenie | czynność | US1.7, BR-G5, NFR-56 | — |
| Gdy strzałka przesuwania albo gest dochodzi do brzegu prostokąta terenu i mapa nie przesuwa się dalej, TalkBack ogłasza raz: „Tu kończy się teren Kampus główny. Inny teren wybierzesz przyciskiem «Zmień teren» u góry mapy.” Ogłoszenie powtarza się dopiero po odsunięciu widoku od brzegu albo po zmianie terenu. Na ekranie nic się nie pojawia. Teren zmienia się przyciskiem terenu, a budynek z innego terenu otwiera się z wyszukiwania albo z listy budynków (US1.8) | dane | US1.7, NFR-18, NFR-53 | — |
| Wynik wyszukiwania budynku z innego terenu ma dopisek z nazwą terenu, np. „Wittigowo · inny teren”. Wybranie go zmienia teren, a mapa przelatuje na ten teren jak do dalekiego budynku (wiersz o dotknięciu konturu). Wybranie budynku z innego terenu na liście też zmienia teren, ale bez przelotu: mapa od razu stoi na budynku (wiersz o dotknięciu budynku na liście). Na czas przelotu granica starego terenu nie zatrzymuje mapy. Mapa w przelocie nigdy nie jest pusta: podkład starego terenu zostaje na ekranie, dopóki kafelki nowego terenu w widoku nie są wczytane. Potem podkład nowego terenu przenika w 200 ms (Material 3, `short4`) i nie wskakuje kawałkami. Szara mapa bez podkładu wygląda jak błąd aplikacji albo brak sieci, a osoba traci punkty orientacyjne. Kafelki pobiera się tylko w prostokątach starego i nowego terenu, nie dla miasta po drodze (NFR-39). Przelot kończy się w kadrze z wiersza o dotknięciu konturu, liczonym raz, z wysokości karty po pomiarze. Przy wyłączonych animacjach pojawia się od razu, bez przenikania. Po końcu przelotu, a po wyborze z listy po wjeździe karty V2, pojawia się napis o zmianie terenu (wiersz niżej). Napis nie pojawia się, dopóki mapa jeszcze jedzie. Zamknięcie karty V2, „Zamknij”, dotknięcie pustej mapy i „Wstecz” nie zmieniają terenu: zostaje ten, na który mapa przeszła, albo ten, który osoba wybrała potem. Na teren startowy mapa wraca tylko po jego wyborze przez osobę. Lista budynków ma nagłówki terenów z liczbą budynków, np. „Wittigowo · 9” | czynność | US1.8, NFR-10, NFR-18, NFR-19, NFR-39, NFR-56 | V1 z tym terenem; dalej jak przy każdym wyniku |
| Napis o zmianie terenu z nazwą terenu, odległością i kierunkiem, np. „Pokazuję teren: Wittigowo · 2 km na południowy wschód”. Odległość i kierunek liczą się od terenu wybranego przed zmianą, jak na liście terenów. Napis jest potrzebny, bo teren zmienia się bez wyboru terenu przez osobę. Sam przelot nie mówi, że mapa pokazuje inną część miasta, a TalkBack nie powie tego wcale (NFR-56). Napis stoi nad podpisem OpenStreetMap, a przy otwartej karcie V2 tuż nad kartą. Tam patrzy osoba, a napis nie zasłania budynku ani przycisków mapy, także tych z fokusem. Ma wygląd przycisków mapy i panelu „Legenda”: tło `surfaceContainerLowest`, cień i zaokrąglone rogi. Barwy odwrotne odpadają, bo w ciemnym motywie jasne tło świeci. Pojawia się po przelocie, a po wyborze z listy po wjeździe karty V2; przy wyłączonych animacjach bez stopniowego pojawiania się. TalkBack czyta go raz jako komunikat stanu, po nagłówku karty V2. Ten sam napis pokazany znów po zamknięciu panelu nie jest czytany drugi raz. Napis nie znika sam (NFR-23). Gaśnie po wyborze terenu i wtedy, gdy osoba przesunie albo przybliży mapę przyciskami albo gestem, a napis był w tej chwili widoczny. Ruch mapy przy zasłoniętym napisie go nie gasi | dane | US1.8, NFR-18, NFR-23, NFR-56; WCAG 4.1.3 [1], 2.4.11 [7] | — |
| Gdy klawiatura jest otwarta, przyciski mapy, czyli „Powiększ”, „Pomniejsz” i strzałki przesuwania, są schowane, bo mapa nad klawiaturą jest za niska na cały zestaw. Po zamknięciu klawiatury przyciski wracają. Podpis OpenStreetMap zostaje widoczny. Dotknięcie mapy przy otwartej klawiaturze albo wynikach pod polem tylko je zamyka. Nie zaznacza budynku ani znaku pod palcem i nie otwiera karty ani arkusza. Tekst w polu zostaje. Przesunięcie mapy palcem też zamyka klawiaturę i przesuwa mapę | czynność | NFR-53, NFR-37, US3.1, NFR-17; decyzja 91, decyzja z 19.11.2026 | — |
| Napis „© OpenStreetMap” w jednym wierszu w rogu mapy, jako odnośnik przy każdej skali czcionki. Tekst ma 12 sp (`mapa.podpis_osm_sp`, wyjątek od najmniejszego tekstu) w kroju aplikacji i obszar dotyku o wysokości co najmniej 48 dp | dane | NFR-37 | strona praw autorskich OSM |
| „ⓘ” w prawym dolnym rogu mapy, w wierszu z podpisem OpenStreetMap, nazwa dla TalkBack „Legenda i źródła mapy”. Otwiera arkusz „Legenda”. Przycisk jest samą ikoną, aby pozostawić miejsce na podpis OpenStreetMap. Ostatnia sekcja arkusza ma nagłówek „Źródła danych”. Pod nim „Mapa ulic i kontury budynków: © OpenStreetMap contributors” z odnośnikiem i nazwą licencji ODbL. Dalej „Podkład mapy: OpenFreeMap © OpenMapTiles”, obie nazwy jako odnośniki do stron projektów (ADR-0014). Dalej „Wejścia i plany pięter: Źródło: Politechnika Wrocławska” z datą stanu danych | czynność | US1.4, NFR-18, NFR-37, NFR-40 | V1, panel „ⓘ” |
| Pasek dolny | czynność | wymagania wspólne | V4, V5, L1 / L2 |

Wybór wyniku wyszukiwania zaznacza budynek na mapie i otwiera kartę V2 na niskiej wysokości. Przy skali czcionki co najmniej 1,5 karta otwiera się od razu na cały ekran. „Wstecz” albo „Zamknij” zamyka kartę i zdejmuje zaznaczenie. Tryb i tekst wyszukiwania zostają jak przed kartą, a mapa stoi w widoku z chwili zamknięcia (decyzje 83, 116).

Plan piętra pojawia się sam po przybliżeniu budynku szczegółowego, po „Pokaż na planie” na V2 albo po znalezieniu sali. Plan to stan V1, nie osobny ekran: „Wstecz” telefonu go nie zmienia. Po „Pokaż na planie” karta jest zamknięta i żaden budynek nie jest zaznaczony. Plan ma wtedy samą ścianę piętra, bez obrysu zaznaczenia.

Arkusz budynku na V1 to ten sam wzorzec co V2, z tym samym układem i kodem. Zmiana V2 zmienia więc też arkusz na V1 (decyzja 53).

Przejście do karty budynku z arkusza innego obiektu to zawsze ten sam wiersz budynku pod nagłówkiem arkusza. Wiersz ma znak budynku, napis „Budynek A-1” i strzałkę w prawo.

Cały wiersz to jeden cel dotyku, co najmniej 48 dp wysokości. TalkBack czyta „Budynek A-1, otwiera opis budynku”, jak budynek na liście budynków (NFR-17, NFR-18, WCAG 2.5.3 [1]).

Wiersz budynku mają arkusze wejścia, toalety, schodów i bariery, karta sali i arkusz „Który budynek?”.

Znak budynku znaczy tylko „otwórz kartę tego budynku”, więc nie stoi w wynikach wyszukiwania ani na liście budynków. Tam wybór przenosi mapę do budynku, a to inna czynność (decyzja z 18.11.2026).

Dotknięcie wiersza budynku zamyka arkusz i otwiera kartę V2 na niskiej wysokości. Mapa nie jedzie do budynku, tylko zachowuje się jak po dotknięciu konturu: stoi, chyba że karta zasłoniłaby cały widoczny kontur (wiersz o dotknięciu konturu). Osoba patrzyła na obiekt obok budynku, więc przelot gubiłby jej miejsce na mapie (US2.8, NFR-56).

Gdy arkusz otwarto z karty tego budynku, karta wraca na tę samą wysokość i do tego samego miejsca przewinięcia. Przy skali czcionki co najmniej 1,5 karta otwiera się rozwinięta. Karta otwarta wierszem zamyka się jak karta otwarta konturem.

Arkusz bariery na dole mapy otwiera dotknięcie znaku. Bariery nie trzeba szukać na liście, a TalkBack dochodzi do niej przez listę budynków (NFR-19):

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Typ bariery tymi samymi napisami co na V2, uciążliwość napisem i ikoną | dane | US1.5, NFR-21 | — |
| Miejsce: budynek i wejście albo piętro, zdjęcie, „Sprawdzone 12.11.2026” | dane | US1.5, US2.5, PW-3 | — |
| Wiersz budynku pod nagłówkiem i „Zamknij”. Bariera poza budynkiem ma w miejscu napis „Poza budynkiem” i nie ma wiersza budynku, bo nie ma budynku do pokazania | czynność | US1.5, PW-5, BR-G2 | wiersz: V2 na niskiej wysokości; „Zamknij”: V1; „Wstecz” arkusza otwartego przy karcie budynku: ta karta |

Arkusz wejścia na dole mapy otwiera dotknięcie znaku wejścia, a z karty V2 dotknięcie wiersza na liście „Wejścia” i „Pokaż wejście na mapie”. O wejściu nie trzeba czytać w opisie budynku (US2.1, US3.3):

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Znak oznaczenia obok nagłówka „Wejście” i „Zamknij”. Nagłówek nie powtarza budynku, bo podaje go wiersz budynku pod nim. Pod wierszem słowo oznaczenia: „Dostępne”, „Niedostępne” albo „Brak danych”, a przy wejściu głównym dopisek „wejście główne” | dane, czynność | US2.1, NFR-21, NFR-53 | „Zamknij”: V1; „Wstecz” arkusza otwartego z karty budynku albo przy niej: ta karta na tej samej wysokości i w tym samym miejscu przewinięcia |
| Wiersz budynku „Budynek A-1” pod nagłówkiem | czynność | US2.8, US3.3, NFR-18 | V2 na niskiej wysokości; otwarty z karty: ta karta na tej samej wysokości i w tym samym miejscu przewinięcia |
| Nazwa wejścia i uwaga tylko wtedy, gdy są w danych | dane | US2.1 | — |
| Pod słowem oznaczenia data i źródło oznaczenia, np. „Z map i opisów, stan z 18.12.2023”; przy wejściu zalecanym przed nimi „Zalecane.” | dane | US17.3, BR-24, BR-25, PW-3 | — |
| Przy „Brak danych” zdanie „Nie mamy danych o dostępności tego wejścia.”. Mówi, czego brakuje, i nie ocenia ludzi | dane | US2.1, US17.1 | — |
| Część „Bariery przy tym wejściu” zaraz pod oznaczeniem, źródłem i nazwą wejścia, przed „Dojście”: zgłoszenia przyjęte przy tym wejściu. Każde ma typ, uciążliwość, zdjęcie i datę weryfikacji jako „Sprawdzone 12.11.2026”. Typ bariery stoi krojem nagłówków części, jak „Drzwi”, a nie krojem nagłówka arkusza. Bez takich zgłoszeń części nie ma, a reszta arkusza się nie zmienia | dane | US2.2, US3.3, UC4 5a, PW-3, PW-13, decyzja z 19.11.2026 | — |
| Części „Dojście”, „Drzwi” z szerokością, np. „Szerokość drzwi: 90 cm”, i „Za drzwiami”. Każde wejście ma te części z nagłówkami, zalecane i każde inne tak samo. Część bez danych nie stoi; szerokość drzwi 0 to brak danych. Przy wejściu z planów PWr „Dojście” to opis z uwag. Przy wejściu bez nazwy w danych „Dojście” najpierw podaje opis położenia, np. „Od strony C-20, od północy.”, a pod nim opis z uwag (BR-27) | dane | US3.3, US16.3, BR-25, BR-27 | — |
| „Za drzwiami” podaje najbliższą windę z planu PWr i odległość od wejścia w linii prostej, zaokrągloną do 5 metrów. Przykład: „Winda przy klatkach C i D, ok. 30 m od wejścia.”. Bez klatek w danych zdanie brzmi „Winda, ok. 30 m od wejścia.”. Drugie zdanie podaje zakres pięter windy z odmianą, np. „Jeździ od piwnicy do 3 piętra.” (decyzja z 18.11.2026). Odmieniamy nazwy „piwnica”, „parter” i „N piętro”. Inną nazwę piętra zdanie wylicza jak arkusz windy, np. „Jedzie na: piwnica, parter, antresola.”. Bez windy na planie budynku części nie ma | dane | US3.3, US16.3, BR-25 | — |
| „Zgłoś barierę przy wejściu”, obrysowany, na końcu arkusza; TalkBack czyta „Zgłoś barierę przy wejściu: »nazwa wejścia«”, z cudzysłowem tylko przy nazwie z danych. Działa jak na V2 | czynność | US4.6, US14.2, NFR-18, WCAG 2.5.3 [1] | V4 z wybranym wejściem; Gość: L1 |

Arkusz toalety i arkusz schodów na dole mapy otwiera dotknięcie znaku na planie piętra (US16.1):

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Toaleta dostosowana” albo „Schody” i „Zamknij”. Pod nagłówkiem wiersz budynku, a pod nim etykieta piętra z danych PWr, np. „Parter” | dane, czynność | US16.1, NFR-53 | wiersz: V2 na niskiej wysokości; „Zamknij”: V1; „Wstecz” arkusza otwartego przy karcie budynku: ta karta |
| Przy toalecie uwaga, gdy jest w danych | dane | US16.1 | — |
| „Zgłoś barierę przy toalecie” albo „Zgłoś barierę przy schodach”, obrysowany, na końcu arkusza; TalkBack czyta napis z budynkiem i piętrem, np. „Zgłoś barierę przy toalecie: Budynek A-1, parter” | czynność | US4.6, US14.2, NFR-18, WCAG 2.5.3 [1] | V4 z wybranym obiektem; Gość: L1 |

Arkusze wejścia, toalety i schodów mają obrysowany „Zamknij” w nagłówku, jak arkusz bariery. Po otwarciu fokus TalkBack stoi na nagłówku, a przy skali czcionki 200% treść arkusza się przewija.

Przycisk zgłoszenia w arkuszu toalety, schodów albo windy otwiera krok 1 V4 z napisem obiektu. Miejscem zgłoszenia jest punkt obiektu, a piętrem piętro planu, na którym osoba dotknęła znaku. Winda obsługuje kilka pięter, więc zgłoszenie dostaje piętro oglądanego planu (US4.6, BR-8, BR-14).

Arkusz wejścia nie ma czynności głównej: odpowiedź już podała karta V2. Innych wejść budynku arkusz nie pokazuje: są na liście „Wejścia” karty V2.

Arkusz ma wysokość treści, najwyżej pół ekranu; treść, która się nie mieści, przewija się pod nagłówkiem z „Zamknij”. Kamera stawia zaznaczone wejście na środku części mapy nad arkuszem przy jego rzeczywistej wysokości, więc pinezkę widać przy każdej treści (US3.3, NFR-19, NFR-53).

Przy dużej czcionce arkusz wejścia zajmuje cały ekran (reguła 6 sekcji [Duża czcionka](#duza-czcionka)). Arkusz zamykają „Zamknij”, „Wstecz” telefonu i dotknięcie pustej mapy (NFR-18, NFR-20, NFR-53).

Arkusz wejścia otwarty z karty budynku, dotknięciem wiersza na liście „Wejścia” albo przez „Pokaż wejście na mapie”, stoi w miejscu karty. Tak samo znak bariery, wejścia, toalety albo schodów dotknięty przy otwartej karcie budynku otwiera swój arkusz w miejsce karty.

„Zamknij” zamyka wtedy arkusz razem z kartą i zostawia mapę. „Wstecz” telefonu wraca do karty tego budynku na tej samej wysokości i w tym samym miejscu przewinięcia.

Arkusz otwarty znakiem na mapie bez karty zamykają „Zamknij” i „Wstecz” na mapę. Dotknięcie pustej mapy zamyka arkusz bez karty (US3.3, NFR-15, NFR-53).

Arkusz „Który budynek?” na dole mapy otwiera dotknięcie obok dwóch różnych konturów, poza nimi (decyzja 85):

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Który budynek?” i „Zamknij” | dane, czynność | NFR-53, NFR-17 | „Zamknij”: V1 |
| Wiersz budynku w ramce na pełną szerokość dla każdego konturu w zasięgu: znak budynku, „Budynek A-1” i strzałka w prawo. Pod napisem stoi nazwa budynku, gdy jest. Cały wiersz to jeden cel dotyku, a TalkBack czyta „Budynek A-1”, nazwę i „otwiera opis budynku” | czynność główna stanu | US2, NFR-17, NFR-18 | V2 tego budynku na niskiej wysokości |

Karta sali na dole mapy otwiera się po wyniku wyszukiwania sali albo po dotknięciu sali na planie (US18.1, US18.2).

Gdy wejścia budynku są już w telefonie, karta otwiera się od razu z drogą i potem nie zmienia wysokości. Pierwsza sala budynku po uruchomieniu aplikacji czeka na serwer. Do tego czasu zamiast kroków stoi „Wczytujemy drogę…” (NFR-10, NFR-56).

Dotknięcie sali na planie nie zmienia powiększenia, a mapa stoi. Gdy karta zasłoni część sali, mapa przesuwa się w górę jak przy konturze budynku: pionowo, w 300 ms, sala 16 dp nad kartą (US2.8, NFR-56).

Po wyniku wyszukiwania sali i po wyborze kroku ruch zależy od planu. Z planem piętra tego budynku na mapie mapa tylko przesuwa punkt na środek wolnej części nad kartą, a powiększenie zostaje. Bez tego planu mapa przelatuje do punktu z powiększeniem 19, przy którym widać numery sal (US18.2, NFR-56).

Jedna czynność osoby rusza mapę najwyżej raz. Zmiana wysokości karty, np. po wczytaniu drogi, nie rusza mapy. Zamknięcie karty zostawia widok bez ruchu (NFR-56).

Otwarcie nie zaczyna pokazu drogi. Mapa pokazuje plan piętra sali z wyróżnioną salą. Karta od razu pokazuje całą listę „Jak dojść”, bez wybranego kroku.

Przy zwykłej czcionce karta ma wysokość treści: nagłówek, wiersz budynku i cała lista „Jak dojść” bez przewijania, plan piętra z salą nad kartą (US18.1).

Przy zwykłej czcionce nad kartą sali nie ma wiersza tytułu z „Pokaż listę” ani pola z numerem sali. Odjeżdżają w górę jak nad V2, także w trybie „po kolei” i pod arkuszem kroku, a mapa sięga górnej krawędzi. Po zamknięciu karty pasek wraca z tym samym tekstem w polu (US18.1, NFR-53).

Przy [dużej czcionce](#duza-czcionka) karta stoi na cały ekran według reguły 13 (decyzja z 19.11.2026).

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nagłówek „Sala 329” i „Zamknij”, pod nim rodzaj pomieszczenia z danych PWr i piętro, np. „Sala wykładowa · 2 piętro”; rodzaju „Inne” nie ma, zostaje samo piętro. Pod nimi wiersz budynku | dane, czynność | US18.1, NFR-40 | wiersz: V2 na niskiej wysokości; „Zamknij”: V1 |
| Nagłówek listy „Jak dojść”, a po prawej przycisk tekstowy „Pokaż po kolei”. Przycisk otwiera tryb „po kolei” na kroku 1 (wiersz niżej) | czynność | US18.2, NFR-17, NFR-18; decyzja z 20.11.2026 | stan „Kroki po kolei” |
| Kroki tekstem w numerowanych wierszach, bez rysowania trasy: numer w kółku, nazwa kroku i zdanie. Wiersze stoją w ramce grupy jak lista „Wejścia” na V2: tło `surfaceContainerLowest`, ramka 1 dp `outline`, dzielniki `outlineVariant` z odstępem 4 dp. Wiersz ma wcięcie 4 dp w pionie i 8 dp w poziomie. Tekst kroków dzieli więc 8 dp nad i pod kreską jak w innych listach w ramce grupy, a tło wybranego kroku nie dotyka tekstu. Nazwa kroku ma krój `titleMedium`, zdanie `bodyLarge`, a wiersz kończy „›”. Kółko numeru jest jak znak kroku na planie: tło `papier`, kontur `tusz`, cyfra 18 sp. Przy dużej czcionce zdanie stoi pod numerem i nazwą, na całą szerokość ramki. Zbyt długie słowo nazwy dzieli się z łącznikiem, jak tytuł arkusza. Wejście zalecane: nazwa wejścia, jego piętro i położenie, np. „Parter, od strony A-4, od północy.”. Winda: odległość od wejścia i piętro sali, np. „Ok. 30 m od wejścia. Jedź na 2 piętro.”. Sala: piętro i odległość od windy, np. „2 piętro, ok. 85 m od windy.”. Odległość jest w linii prostej i zaokrąglona do 5 metrów. Piętro wejścia pochodzi z warstwy wejść planu PWr. Gdy go brak, krok przyjmuje parter i mówi to, np. „Parter (brak piętra w danych), od strony A-4.”. Sala na piętrze wejścia ma dwa kroki: wejście i salę z odległością od wejścia | dane | US18.2, BR-25 | — |
| Dotknięcie wiersza kroku albo numeru kroku na planie wybiera ten krok w liście. Wybrany wiersz ma znak „wybrane” jak piętro: tło `primaryContainer`, obwódkę 2 dp `onPrimaryContainer` i wypełnione kółko numeru. Dzielniki przy nim znikają. W wybranym kroku wejścia albo windy stoi „Szczegóły wejścia ›” albo „Szczegóły windy ›” z celem dotyku 48 dp. Odsyłacz otwiera arkusz wejścia albo windy. Ten arkusz ma w nagłówku „‹ Sala 329” zamiast „Zamknij” (TalkBack: „Wróć do sali 329”). Przycisk wraca do karty sali na tym samym kroku i w tym samym trybie | czynność | US18.2, US3.3, US16.1, NFR-17, NFR-18, NFR-21; decyzje z 19.11.2026 i 20.11.2026 | arkusz wejścia albo windy na tym ekranie |
| Tryb „po kolei” pokazuje jeden krok naraz. Pod nagłówkiem „Sala 329” z „Zamknij” stoją „Krok 1 z 3” i odsyłacz „Wszystkie kroki”, a pod nimi pasek postępu. Niżej jest tylko bieżący krok z wypełnionym kółkiem i odsyłaczem szczegółów. Na dole stoją „‹ Wstecz” i „Dalej ›”, wyłączone na pierwszym i ostatnim kroku. „Wszystkie kroki” wraca do listy z tym samym krokiem wybranym. Karta jest niska, więc mapa zostaje na mniej więcej połowie ekranu | czynność | US18.2, NFR-17, NFR-18, NFR-21; decyzja z 20.11.2026 | stan „Znaleziona sala” z wybranym krokiem |
| Na planie numery kroków w kółkach 32 dp, bez linii trasy: 1 przy wejściu, 2 przy windzie, 3 w sali, każdy tylko na swoim piętrze. Kółko ma tło `papier` i kontur `tusz`, a wybrany krok wypełnienie `primary`. Sala ma wypełnienie `p_wybrana` i kontur `p_wybrana_kontur` przez cały czas, gdy karta jest otwarta; pinezki nie ma | dane | US18.1, US18.2, NFR-17, NFR-21, decyzja z 19.11.2026 | — |

Wybór kroku przestawia plan na piętro kroku, a mapa rusza się według reguł karty sali opisanych wyżej. „Zamknij” kończy pokaz i zamyka kartę.

Kroki prowadzą od wejścia zalecanego (BR-25) windą, która staje na piętrze tego wejścia i na piętrze sali (US18.2). W A-1, z wejściami na parterze, to winda przy klatkach C i D, bo winda przy A i B staje tylko na 2 piętrze [6]. Sala na piętrze wejścia ma tylko krok wejścia i krok sali, a odległość liczy się od wejścia.

| Stan | Co widać |
| --- | --- |
| Zwykły | mapa z podkładem OpenStreetMap, konturami i barierami. Podkład ma przygaszone barwy, więc budynki PWr i znaki są najwyraźniejsze, a zieleń, woda i ulice zostają rozpoznawalne; w motywie ciemnym podkład jest ciemny (NFR-16) |
| Plan piętra | plan jednego budynku przy powiększeniu co najmniej 17 i kolumna pięter po lewej pod przyciskiem terenu. Inne budynki są przygaszone, jest „ⓘ” z legendą i źródłem danych (US16.1, NFR-40) |
| Budynek bez planu | Przy powiększeniu od 17 środek mapy leży w konturze budynku, który lista z serwera określa jako podstawowy. Bez karty V2 nad mapą widać zdanie „Nie mamy planu pięter tego budynku.” Kolumny pięter nie ma (US16.2, BR-G1). |
| Winda na planie | pinezka z podpisem „Winda” i arkusz z piętrami, na które jedzie winda (US16.1, US18.2) |
| Znaleziona sala | plan piętra sali z wyróżnioną salą, numery kroków na planie bez linii trasy i karta sali z całą listą „Jak dojść”, bez wybranego kroku. Wiersza tytułu z „Pokaż listę” i pola z numerem sali nad kartą nie ma, jak nad V2; wracają po zamknięciu karty (US18.1, US18.2) |
| Kroki po kolei | po „Pokaż po kolei”. Karta pokazuje jeden krok z „Krok 1 z 3”, paskiem postępu, „‹ Wstecz” i „Dalej ›”. Plan pokazuje piętro kroku, a mapa jego punkt. Wiersza tytułu i pola nad kartą nie ma jak w stanie „Znaleziona sala” (US18.2) |
| Sam numer sali | pod polem „Dopisz budynek do numeru sali. Na przykład: A-1 329.” zamiast wyników (US18.1, NFR-50) |
| Bariera na mapie | pinezka z podpisem typu bariery i arkusz z typem, uciążliwością, miejscem i zdjęciem (US1.5) |
| Wejście na mapie | arkusz wejścia z oznaczeniem, datą i źródłem, dojściem, drzwiami, windą za drzwiami i barierami przy wejściu, ten sam przy każdym wejściu (US2.1, US3.3, US17.3) |
| Toaleta albo schody na planie | pinezka z nazwą znaku i arkusz z nazwą znaku, budynkiem i piętrem (US16.1) |
| Wybór terenu | panel „Teren na mapie” z listą terenów (US1.7) |
| Brzeg terenu | mapa kończy się na brzegu prostokąta terenu, bez linii granicy i wyblaknięcia; przy dojściu do brzegu jeden komunikat TalkBack (US1.7, BR-G5) |
| Wybór budynku | arkusz „Który budynek?” z wierszem budynku dla każdego z dwóch różnych konturów w zasięgu dotknięcia obok nich (US2, decyzja 85) |
| Duża czcionka | start na liście budynków; karta budynku to jeden przycisk. „Pokaż budynek na mapie” jest na V2 otwartej od razu na cały ekran (NFR-19, NFR-20, US2.9) |
| Sala bez drogi | brak wejścia zalecanego albo windy z piętra wejścia na piętro sali. Zamiast kroków: „Nie znaleźliśmy w danych wejścia i windy, którymi dotrzesz do tej sali bez schodów.” Wiersz budynku zostaje pod nagłówkiem (US18.3, BR-25) |
| Pusty — brak sali | „Nie znaleźliśmy sali »X« w budynku A-1. Sprawdź numer albo otwórz opis budynku.” Pod komunikatem wiersz budynku „Budynek A-1” (US18.3, PW-10) |
| Pierwsze uruchomienie | przed mapą pokazuje się instrukcja V5 (US5.1) |
| Ładowanie | „Wczytujemy mapę kampusu…”; kontury pojawiają się przed podkładem (NFR-11) |
| Pusty — brak wyniku, gdy do wpisanego tekstu nie pasuje ani symbol, ani nazwa budynku | „Nie znaleźliśmy budynku »X«. Sprawdź zapis, np. C-13, albo wybierz budynek z listy.” Przycisk „Pokaż listę” (UC1 3a) |
| Brak sieci | kontury i bariery z telefonu. Nowego podkładu aplikacja bez sieci nie pobiera. Fragmenty mapy ulic obejrzane wcześniej mogą się pokazać z pamięci telefonu (decyzja 84). Linia „Bez internetu · dane z 12.11.2026” stoi pod polem symbolu, a pole się nie przesuwa (NFR-10, NFR-39) |
| Brak sieci przy pierwszym uruchomieniu | mapa z konturami i wejściami z danych aplikacji, bez podkładu, barier i planów pięter. Wyszukiwanie po symbolu działa. Linia „Bez internetu” bez daty, bo nic jeszcze nie pobrano, stoi pod polem symbolu. Bariery, plany i podkład wczytują się same po powrocie sieci (NFR-10, NFR-39) |
| Błąd — brak podkładu | kontury i bariery bez podkładu, napis „Mapa ulic chwilowo się nie wczytuje. Budynki i bariery nadal możesz oglądać.” (NFR-50) |

Dostępność i osoby starsze: lista budynków daje tę samą informację co mapa i jest drogą dla TalkBack (NFR-19). Wyszukiwanie przyjmuje symbol w każdym zapisie, bez wymogu myślnika (US1.1). Kroki do sali są tekstem, więc TalkBack nie potrzebuje planu (NFR-19).

### V2 Karta budynku

| Pole | Treść |
| --- | --- |
| Cel | Osoba sprawdza, którym wejściem wejdzie i jakie bariery są w budynku. |
| Role | wszystkie; przyciski Zbierającego widzi tylko RO-ZBI (US14.1) |
| Realizuje | US2.1…US2.9, US3.1, US3.2, US16.1…US16.3, US17.1, US17.3, US10.6; start US3.3, US4, US4.6, US8.4, US15.1, US17.2 |
| Czynność główna | „Pokaż wejście na mapie”; bez wejścia zalecanego brak |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Mapa nad kartą na niskiej wysokości: kontur budynku wyróżniony, sąsiednie przygaszone w swojej barwie, bledsze, z podpisami symboli. Są tylko wejścia tego budynku ze znakiem oznaczenia i napis „© OpenStreetMap”. Wiersza tytułu z „Pokaż listę”, pola symbolu ani przycisku informacji nad kartą nie ma. TalkBack nie czyta mapy pod kartą, bo tę samą treść ma lista (NFR-19). Przy karcie na cały ekran mapy nie widać | dane | US1.4, US2.1, US2.8, NFR-19, NFR-21, NFR-37; decyzje 151, 153 | — |
| Mapę nad kartą da się przesuwać i powiększać. Nad kartą stoją te same przyciski co na V1: „Powiększ”, „Pomniejsz” i strzałki przesuwania. Gdy nad kartą zostaje mniej niż 300 dp mapy, przycisków nie ma | czynność | NFR-53, NFR-17; decyzja 151 | — |
| Po zaznaczeniu budynku kamera ustawia się raz. Po wyborze z wyszukiwania albo z listy budynek trafia do wolnej części mapy. Cały kontur budynku i jego wejścia mieszczą się wtedy w części mapy, której nie zasłaniają karta, przyciski ani napis o zmianie terenu. Po dotknięciu konturu mapa rusza się tylko tak, jak mówi wiersz V1 o dotknięciu konturu, więc kontur ucięty brzegiem ekranu zostaje ucięty. Potem widok zmienia tylko osoba | dane | US1.4, US2.8, NFR-53; decyzja 151 | — |
| Karta na niskiej wysokości: uchwyt, nagłówek, wiersz o wejściach ze zdaniem z odpowiedzią i „Pokaż wejście na mapie”. Pod kreską wystaje początek pełnego opisu: cały nagłówek „Wejścia” i ucięty następny wiersz. Widać więc, że treść idzie dalej. Osobnego przycisku do rozwinięcia nie ma. Dolnego przycisku „Zamknij” i chipów na niej nie ma, a wiersz o wejściach zastępuje chip „Wejście dostępne”. Bez internetu nad nagłówkiem stoi plakietka „Bez internetu”, w tym samym miejscu na każdej wysokości (decyzja 153). Karta ma wysokość gotowej treści od pierwszej klatki. Zanim wczyta się opis, miejsce nagłówka i wiersza o wejściach jest puste, bez zaślepek, a przyciski stoją już na swoim miejscu | dane, czynność | US2.8, US3.2, NFR-10, NFR-53, NFR-56 | „Zamknij”: V1 bez zaznaczenia, na tym samym terenie i w tym samym widoku |
| Dotknięcie wystającego nagłówka „Wejścia” na niskiej wysokości od razu rozwija kartę, jak dotknięcie uchwytu. Cel dotyku nagłówka ma co najmniej 48 dp, a TalkBack czyta go jako przycisk | czynność | US2.8, NFR-17, NFR-18, NFR-53 | ta sama karta rozwinięta |
| V2 to jedna karta z dwiema wysokościami: niską i rozwiniętą. Na rozwiniętej pod kreską stoi pełny opis: chipy, „Wejścia”, „Bariery w budynku”, „Winda i toaleta”, „Piętra” i źródło. Rozwinięta karta sięga do góry ekranu, a pełny opis się przewija. Pod krótką treścią zostaje puste miejsce, a uchwyt rozwiniętej karty stoi zawsze w tym samym miejscu. Po dotknięciu budynku i po wyborze z listy karta otwiera się na niskiej wysokości | dane | US2, US2.8, NFR-53 | — |
| Karta idzie za palcem. Po puszczeniu staje na bliższej z dwóch wysokości, a szybki rzut przenosi ją na wysokość w stronę rzutu. Przeciągnięcie w dół z niskiej wysokości zamyka kartę. Uchwyt u góry karty to przycisk: na niskiej karcie dotknięcie od razu ją rozwija, a na rozwiniętej wraca na niską. Wszystko da się więc zrobić bez przeciągania. TalkBack czyta uchwyt jako „Wysokość karty” z bieżącą wysokością | czynność | US2.8, NFR-17, NFR-18, NFR-53; WCAG 2.5.7 [7] | ta sama karta na innej wysokości; z niskiej w dół: V1 bez zaznaczenia |
| Dotknięcie innego budynku na mapie zastępuje treść karty, a karta zostaje na swojej wysokości. Dotknięcie pustej mapy zamyka kartę | czynność | US2.8, NFR-17 | V2 innego budynku; V1 bez zaznaczenia |
| „Wstecz” telefonu zwija kartę do niskiej wysokości, jeśli osoba wcześniej rozwinęła ją z niskiej (decyzja 115). Z niskiej karty „Wstecz” zamyka kartę. Gdy karta otworzyła się od razu na cały ekran, „Wstecz” wraca do ekranu sprzed V2. Nie prowadzi przez historię oglądanych budynków. Karta otwarta z listy budynków zamyka się do listy, także po „Zamknij”, przeciągnięciu w dół i dotknięciu pustej mapy. Zapamiętany wybór „Lista budynków” się nie zmienia. Zamknięcie karty w każdy sposób nie rusza mapy: położenie i powiększenie zostają takie jak w chwili zamknięcia. Osoba patrzy na miejsce, które widziała nad kartą, a powrót do widoku sprzed karty przesunąłby mapę bez jej prośby. Wyjątkiem jest mały teren, taki jak „T-9 Atol”, na którym widok bez karty wyszedłby poza prostokąt terenu. Tam pierwszeństwo ma granica terenu: mapa przesuwa się tylko o tyle, żeby cały widok wrócił do prostokąta. Powiększenie się nie zmienia (decyzja z 19.11.2026) | czynność | NFR-15, NFR-19, NFR-39, NFR-56, US2.8; WCAG 2.3.3, 3.2.5 [1]; decyzja 118 | V1 bez zaznaczenia, na tym samym terenie i w tym samym widoku |
| Nagłówek: po lewej symbol budynku, np. „A-1”, stylem `headlineLarge` (36 sp, waga 800) w kolorze `primary`, a pod nim nazwa budynku stylem `titleLarge` (22 sp) według wymagań wspólnych, bez powtórzonego symbolu. Symbol i nazwa mają ten sam rozmiar na każdej wysokości karty, więc niska karta zasłania jak najmniej mapy. Po prawej obrysowany „Zamknij”, jak w legendzie i arkuszu bariery, na każdej wysokości. Gdy nazwy brak, nagłówek ma sam symbol, bez drugiego wiersza. TalkBack czyta „Budynek A-1, Budynek główny”, a bez nazwy „Budynek C-4”. Po otwarciu karty fokus TalkBack staje na nagłówku | dane, czynność | US2, NFR-18, NFR-53; decyzja 152 | „Zamknij”: V1 bez zaznaczenia, na tym samym terenie i w tym samym widoku |
| Gdy treść karty jest przewinięta, pod częścią stojącą w miejscu stoi cienka linia w kolorze kreski nad pełnym opisem. Ta część to nagłówek z „Zamknij”, pasek „Wstecz” albo sam uchwyt karty. Wiersz ucięty górną krawędzią chowa się wtedy pod linią, a nie urywa się w pustym miejscu. Bez przewinięcia linii nie ma | dane | US2.8, NFR-53 | — |
| Wiersz o wejściach pod nagłówkiem: znak z mapy i słowa „Wejście dostępne” albo „Brak wejścia dostępnego”, a pod nimi zdanie z odpowiedzią. Przy wejściu dostępnym zdanie podaje wejście zalecane, to samo co w grupie „Wejścia”, np. „Zalecane: »Wejście dostosowane«. Drzwi 120 cm, winda ok. 30 m za drzwiami.”. Część o drzwiach i część o windzie stoją tylko wtedy, gdy są dla nich dane. Odległość do windy liczy się jak w arkuszu wejścia: w linii prostej, zaokrąglona do 5 metrów. Plany PWr nie mają tej odległości, więc przy nich zdanie kończy się na drzwiach. Przy wejściu „Wejście” z opisem położenia zdanie podaje ten opis bez cudzysłowu, np. „Zalecane wejście od strony C-20, od północy. Drzwi 100 cm.”. Bez opisu położenia zdanie podaje nazwę. Cudzysłów stoi tylko przy nazwie z danych, a „Wejście” i „Wejście główne”, które nadaje import (BR-27), są bez cudzysłowu: „Zalecane: Wejście.” albo „Zalecane: Wejście główne.” (decyzja z 19.11.2026). Gdy przy wejściu zalecanym jest przyjęta bariera „Utrudnienie” albo „Tylko z pomocą”, zdanie kończy się np. „Przy wejściu jest bariera: tylko z pomocą.”, ze słowem uciążliwości małą literą. Przy kilku takich barierach słowo podaje najwyższą uciążliwość. Blokada zabiera wejściu „Zalecane” (BR-25), więc zdanie o niej nie mówi. Drugi napis ma ośmiokąt i stoi, gdy każde wejście jest „Niedostępne”. Zdanie pod nim podaje liczbę wejść z danych, np. „Żadne z 3 wejść nie jest dostępne.”. Przy jednym wejściu zdanie brzmi „Jedyne wejście nie jest dostępne.”. Ten sam napis stoi, gdy każde wejście „Dostępne” ma blokadę. Zdanie brzmi wtedy „Jedyne wejście dostępne ma blokadę.”, a przy kilku takich wejściach np. „Każde z 2 wejść dostępnych ma blokadę.”. Gdy wejść nie ma w danych albo żadne nie jest „Dostępne”, a choć jedno ma „Brak danych”, wiersza ze znakiem nie ma. Pod nazwą stoi wtedy cichy napis „Brak danych o wejściach”, bez zdania. TalkBack czyta wiersz zaraz po nagłówku, jako jeden tekst. Na każdej wysokości wiersz zostaje pod nagłówkiem jako podsumowanie grupy „Wejścia” | dane | US2.8, US2.1, US3, US3.1, US3.3, BR-25, BR-27, NFR-18, NFR-21 | — |
| Rząd chipów z ikoną i napisem: „Winda”, „Toaleta dostosowana”, tylko w pełnym opisie pod kreską, przed grupą „Wejścia”. Chipa „Wejście dostępne” nie ma, bo ten fakt podaje wiersz o wejściach. Chip jest tylko wtedy, gdy dane to potwierdzają; dotknięcie przewija do grupy | czynność | US2.3, US2.8, NFR-17, NFR-21 | grupa na tym ekranie |
| Pod grupą „Wejścia” jeden wiersz z datą i źródłem, np. „Z map i opisów, stan z 1.10.2026”. Przy planach PWr napis brzmi np. „Z planów Politechniki Wrocławskiej, stan z 10.1.2024”. Wiersz stoi raz, gdy wszystkie wejścia mają to samo źródło i datę. Inaczej data i źródło stoją przy każdym wejściu | dane | US17.3, BR-24, PW-3, decyzja 48, decyzja z 19.11.2026 | — |
| Grupa „Wejścia”: najpierw wejście zalecane z napisem „Zalecane” bez piętra: etykieta jest krótka, a piętro podają kroki do sali i opis wejścia. Po nim pozostałe wejścia w grupach oznaczeń: „Dostępne”, „Niedostępne”, „Brak danych”. W każdej grupie najpierw wejścia z nazwą w danych, alfabetycznie po polsku, a liczby w nazwie według wartości („Wejście 2” przed „Wejście 10”). Na końcu grupy stoją wejścia z nazwą nadaną przez import, w kolejności z danych. Osoba szuka wejścia po literze z tabliczki, więc czyta listę jak spis (decyzja z 18.11.2026) | dane | BR-25, US2.1, US3, decyzja 48, decyzja 53 | — |
| Wejścia stoją w jednym białym bloku pod nagłówkiem grupy jako lista. Między wejściami jest cienka linia z równymi odstępami nad nią i pod nią. Znaki oznaczenia stoją w jednej kolumnie przy lewej krawędzi, więc oznaczenia czyta się jednym spojrzeniem w dół | dane | US2.1, NFR-21 | — |
| Każde wejście: oznaczenie „Dostępne”, „Niedostępne” albo „Brak danych” znakiem z mapy i słowem (wymagania wspólne), nazwa, opis i szerokość drzwi z danych. Wejście z planów PWr ma nazwę, opis dojścia i szerokość drzwi według BR-27, a szerokości 0 nie ma. Wejście bez nazwy w danych, także spoza planów PWr, ma nazwę „Wejście”, a główne z OpenStreetMap „Wejście główne” (BR-27). Pierwszy wiersz pod nazwą to opis położenia, np. „Od strony C-20, od północy.”, drugi to opis z uwag, gdy jest, potem szerokość drzwi. Opis położenia ma styl `bodyMedium` w kolorze `onSurface`, gęściej niż uwaga, ale nie pogrubiony jak nazwa z danych. Uwaga i szerokość drzwi mają kolor `onSurfaceVariant`. Między opisem położenia, uwagą i szerokością drzwi jest odstęp `odstepy.xs`. Wejście z przyjętą barierą ma pod szerokością drzwi wiersz ze znakiem uciążliwości z mapy, słowem uciążliwości i typem bariery, np. „Tylko z pomocą: ciężkie drzwi” albo „Blokada: wejście zamknięte”. Przy kilku barierach wiersz podaje tę o najwyższej uciążliwości. Wejście z blokadą nie ma napisu „Zalecane” (BR-25), a jego oznaczenie zostaje. Stoi w grupie „Dostępne” na zwykłym miejscu, bez przesuwania na koniec listy | dane | US2.1, US2.2, US17.1, NFR-21, BR-25, BR-27, decyzja 44 | — |
| Wiersz wejścia na liście jest dotykalny w całości, ze strzałką po prawej. Dotknięcie pokazuje to wejście na mapie V1 i otwiera jego arkusz. Cel dotyku to cały wiersz, co najmniej 48 dp wysokości. TalkBack czyta wiersz jako jeden tekst z czynnością „Pokaż wejście na mapie”. Czynności przy wejściu, w tym „Zgłoś barierę przy wejściu”, są w arkuszu wejścia, więc lista zostaje krótka. Wyjątkiem jest „Zmień oznaczenie” Zbierającego pod wierszem | czynność | US3.3, US4.6, NFR-17, NFR-18, WCAG 2.5.3 [1] | V1 z zaznaczonym wejściem i jego arkuszem. „Wstecz” w arkuszu: ta karta, ta sama wysokość i przewinięcie listy. „Zamknij” arkusza: V1 bez karty |
| Dwa lub więcej wejść bez oznaczenia i bez nazwy w danych stoją jako jeden wiersz, gdy żadne wejście budynku nie ma oznaczenia ani nazwy. Wiersz ma znak „Brak danych” i napis „3 wejścia”: 2–4 „wejścia”, 5 i więcej „wejść”. Pod nim stoi „Nikt jeszcze nie sprawdził tych wejść.” i jeden odnośnik „Zgłoś barierę przy wejściu”, bo nie ma jednego wejścia do otwarcia. Jednakowe bloki czytałyby się jak błąd. Jedno takie wejście to zwykły wiersz, a gdy choć jedno wejście ma oznaczenie albo nazwę, lista jest zwykła | dane, czynność | US2.1, US17.1, US4.6, NFR-21 | V4 budynku bez wybranego wejścia; Gość: L1 |
| Wyjścia ewakuacyjne pod przyciskiem „Pokaż wyjścia ewakuacyjne (2)”; liczba z danych. Na początku lista jest zwinięta, a przycisk ma strzałkę w dół. Po rozwinięciu przycisk ma napis „Ukryj wyjścia ewakuacyjne (2)” i strzałkę w górę, więc napis mówi, co zrobi następne dotknięcie. TalkBack czyta napis i stan „zwinięte” albo „rozwinięte”. Wyjście ewakuacyjne z planów PWr rozpoznaje BR-27; nie stoi na liście wejść | czynność | US2.6, BR-27, NFR-17, NFR-18 | lista na tym ekranie |
| Rozwinięte wyjścia stoją pod przyciskiem w takim samym białym bloku z ramką jak lista „Wejścia”, z cienką linią między wyjściami | dane | US2.6, NFR-21 | — |
| Wiersz wyjścia na rozwiniętej liście wygląda jak wiersz wejścia bez nazwy, ale bez oznaczenia i strzałki. U góry pogrubione „Wyjście ewakuacyjne”, stylem nazwy wejścia. Pod nim zwykłym stylem opisu dojścia reszta opisu z danych, od wielkiej litery i z kropką na końcu. Opis „Wyjście ewakuacyjne z bufetu (pomieszczenie 42)” daje więc „Z bufetu (pomieszczenie 42).”. Opis, który nie zaczyna się od „Wyjście ewakuacyjne”, stoi pod pogrubionym napisem w całości. Niżej „Szerokość drzwi: 90 cm”, gdy szerokość jest w danych; szerokości 0 nie ma (BR-27). Wiersz nie jest dotykalny i nie pokazuje wyjścia na mapie | dane | US2.6, BR-27, NFR-21; decyzja z 19.11.2026 | — |
| Zgłoszenia przyjęte pod nagłówkiem „Bariery w budynku”, zaraz po wejściach: typ bariery, uciążliwość, miejsce, zdjęcie, data weryfikacji jako „Sprawdzone 12.11.2026” | dane | US2.2, US2.3, US2.5, BR-17, NFR-21, PW-3 | — |
| Typ bariery nazwą z klasyfikacji, np. „niesprawna winda”. Gość i Odwiedzający widzą prostsze napisy trzech typów. TB-20: „toaleta zamknięta, zastawiona lub trudna do dojścia”. TB-22: „brak planu dotykowego lub głosowego”. TB-28: „brak obniżonej lady lub miejsca dla wózka”. Te same napisy są na V1 i w arkuszu wejścia. | dane | BR-7, słownik użytkownika | — |
| Grupa „Winda i toaleta”: na których piętrach jest winda i toaleta dla osób z niepełnosprawnością, z planów PWr (budynek szczegółowy) | dane | US16.3, NFR-40 | — |
| Tekst wiersza „Winda” i „Toaleta dostosowana”: piętra z pomieszczeniem tej grupy od najniższego, etykietami z danych. Gdy na piętrze jest kilka, liczba stoi w nawiasie, np. „Na piętrach: Piwnica, 1 piętro (2) i 2 piętro.”. Gdy są na każdym piętrze: „Na każdym piętrze: Piwnica – 3 piętro.”. Inaczej po liście „Nie ma na piętrach: Parter i 3 piętro.”; etykiety bez odmiany, bo przychodzą z danych. Toaleta ma przed listą „Dla osób z niepełnosprawnością.”. Wiersza bez takiego pomieszczenia nie ma, a bez obu wierszy nie ma grupy | dane | US16.3, US2.3, NFR-40 | — |
| Grupa „Piętra”: każde piętro z danych PWr od najniższego. Etykieta piętra to przycisk na całą szerokość, który rozwija krótki opis z funkcji pomieszczeń; na początku opisy są zwinięte. TalkBack czyta etykietę i stan „zwinięte” albo „rozwinięte”. Pod etykietą „Pokaż na planie” | dane, czynność | US16.3, US16.1, NFR-17, NFR-18 | opis na tym ekranie; „Pokaż na planie”: V1, plan tego piętra bez zaznaczonego budynku |
| Opis piętra: funkcje pomieszczeń z danych PWr poza korytarzem, klatką schodową, windą i toaletami. Najwyżej cztery, od najczęstszej, z liczbą w nawiasie przy kilku, np. „Sala dydaktyczna (12), laboratorium (3), portiernia.”. Piętro bez takich pomieszczeń ma samą etykietę i „Pokaż na planie”, bez rozwijania | dane | US16.3, NFR-19 | — |
| Podpis „Źródło: Politechnika Wrocławska” na dole karty | dane | NFR-40, decyzja 44 | — |
| Budynek podstawowy: napis „Nie mamy planu pięter tego budynku.” w grupie „Piętra” zamiast opisów pięter; grupy „Winda i toaleta” nie ma. Lista barier stoi nad napisem, więc napis nie odsyła do niej | dane | US16.2, BR-G1, PW-6 | — |
| Plan jeszcze nieznany: telefon nie dostał z serwera listy budynków ani planu tego budynku, np. przy pierwszym uruchomieniu bez internetu. W grupie „Piętra” napis „Plan pięter sprawdzimy, gdy telefon połączy się z internetem.”; grupy „Winda i toaleta” nie ma. Napis o braku planu stoi tylko przy budynku, który serwer podał jako podstawowy, bo bez danych z serwera nie wiadomo, czy plan jest | dane | US16.2, BR-G1, NFR-10, PW-6 | — |
| „Pokaż wejście na mapie”: jedyny wypełniony przycisk, w treści pod wierszem o wejściach, na każdej wysokości karty. Pierwsza czynność w kolejności czytania, bez przypiętego pasa. Pokazuje wejście zalecane na mapie V1 i otwiera jego arkusz. TalkBack czyta „Pokaż wejście na mapie: »nazwa wejścia«”, z cudzysłowem tylko przy nazwie z danych. Bez wejścia zalecanego przycisku nie ma, bo na mapie nie ma czego pokazać | czynność główna | US3, US3.2, US3.3, US2.8, BR-25, NFR-18, NFR-21, NFR-52 | V1 z zaznaczonym wejściem zalecanym i jego arkuszem. „Wstecz” w arkuszu: ta karta, ta sama wysokość i przewinięcie. „Zamknij” arkusza: V1 bez karty |
| „Pokaż budynek na mapie”: obrysowany przycisk tylko na karcie otwartej od razu na cały ekran. Stoi pod „Pokaż wejście na mapie”, a bez wejścia zalecanego pod wierszem o wejściach. Nad mapą go nie ma. Zmienia zapamiętany wybór V1 na „Mapa” | czynność | US2.9, NFR-19, NFR-20 | V1 w widoku mapy z zaznaczonym budynkiem, bez karty |
| Zbierający: obrysowany przycisk „Zmień oznaczenie” pod treścią wiersza każdego wejścia, także przy „Brak danych”. Stoi poza celem dotyku wiersza, więc nie pokazuje wejścia na mapie, a TalkBack czyta go jako osobny przycisk po wierszu. Zbierający stoi przy wejściu, więc zmienia oznaczenie bez otwierania arkusza | czynność | US17.2, BR-24, NFR-17, NFR-18 | C6 |
| Zbierający: „Zgłoś, że bariery nie ma” przy każdym zgłoszeniu przyjętym | czynność | US8.4, BR-19, PW-12 | C7 |
| Zbierający: „Popraw zgłoszenie” przy własnym zgłoszeniu przyjętym i przy każdym przyjętym zgłoszeniu szybkim | czynność | BR-3, BR-18 | C2 |
| Zbierający: „Oznacz obejrzane piętra” dla całego budynku | czynność | US15.1, PW-12 | C5 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5, L1 / L2 |

V2 to jedna karta nad mapą V1, bez osobnego pełnego ekranu, z dwiema wysokościami: niską i rozwiniętą (decyzja 115). Uchwyt zmienia wysokość bez przeciągania (NFR-53). Niska karta podaje już odpowiedź o wejściu, więc odpowiedź widać po 1 dotknięciu od mapy (US3.2).

Połowy ekranu nie ma, bo przy budynkach z planami PWr różniła się od niskiej karty o kilka wierszy.

Karta V2 otwiera się od razu na cały ekran przy skali czcionki co najmniej 1,5 i w oknie co najmniej 600 dp (NFR-20, NFR-51). Tak samo jest w poziomie i po wyborze budynku z listy przy TalkBack (NFR-19, NFR-22). Ma wtedy dwie wysokości, cały ekran i zamknięta, i tylko wtedy „Pokaż budynek na mapie” (US2.9).

Wiersz o wejściach ze zdaniem z odpowiedzią stoi nad przyciskami: „Pokaż wejście na mapie” wypełniony, „Pokaż budynek na mapie” obrysowany (NFR-20, NFR-21).

Kolejność od góry: nazwa budynku, wiersz o wejściach, czynności, chipy, „Wejścia”, „Bariery w budynku”, „Winda i toaleta”, „Piętra”. Bariery stoją zaraz po wejściach, bo to najważniejsza informacja karty, a zwinięte opisy pięter skracają kartę przy dużej czcionce (US2.3, US16.3).

Na całym ekranie nagłówek karty z „Zamknij” stoi u góry, a treść przewija się pod nim. Pasek dolny nie zasłania elementu z fokusem: treść przewija się tak, by był widoczny (WCAG 2.4.11 [7]).

„Zgłoś barierę przy wejściu” w arkuszu wejścia otwiera krok 1 V4 („Zgłoś barierę”) z napisem „Wejście: »nazwa wejścia«”. Cudzysłów stoi tylko przy nazwie z danych, więc wejście główne bez nazwy ma napis „Wejście: Wejście główne”. Miejscem zgłoszenia jest punkt wybranego wejścia, a budynek i piętro pochodzą od wejścia (US4.6, BR-8, BR-14).

Gość przechodzi najpierw do L1, a po zalogowaniu wraca do V4 z tym samym wejściem (US14.2).

Odnośnik „Zgłoś barierę przy wejściu” w wierszu kilku wejść bez danych otwiera krok 1 V4 bez wybranego wejścia. Miejscem jest wtedy bieżąca pozycja (BR-8).

Mapa nad kartą i plany pięter pokazują to, co już jest na liście, więc TalkBack ich nie potrzebuje (NFR-19). Gość i Odwiedzający nie widzą przycisków Zbierającego, więc ich karta jest krótka.

| Stan | Co widać |
| --- | --- |
| Niska wysokość | mapa z wyróżnionym budynkiem i przyciskami mapy, a pod nią karta. Na karcie: uchwyt, nagłówek z „Zamknij”, wiersz o wejściach ze zdaniem z odpowiedzią i „Pokaż wejście na mapie”. Pod kreską wystaje nagłówek „Wejścia” z uciętym następnym wierszem, bez chipów. Fokus TalkBack na nagłówku, a wiersz o wejściach czytany zaraz po nim. Po zmianie terenu nad kartą stoi napis o zmianie terenu, czytany po nagłówku (US2.8, US1.8, US3.2, NFR-18, NFR-53) |
| Niska wysokość — brak wejścia dostępnego | wiersz „Brak wejścia dostępnego” z ośmiokątem i zdaniem o liczbie wejść. Przycisku „Pokaż wejście na mapie” nie ma. Nagłówek „Wejścia” wystaje tak samo (US3.1, BR-25) |
| Niska wysokość — blokada przy każdym wejściu dostępnym | wiersz „Brak wejścia dostępnego” z ośmiokątem i zdaniem „Jedyne wejście dostępne ma blokadę.”. Przycisku „Pokaż wejście na mapie” nie ma. Wiersz tego wejścia na liście ma oznaczenie „Dostępne” i wiersz „Blokada: [typ bariery]” (US3.1, BR-25) |
| Niska wysokość — brak danych o wejściach | cichy napis „Brak danych o wejściach”, bez przycisku wypełnionego; nagłówek „Wejścia” wystaje tak samo. Brak danych nie znaczy, że budynek jest niedostępny (US3.1, US17.1) |
| Rozwinięta | pod kreską pełny opis: chipy, „Wejścia”, „Bariery w budynku”, „Winda i toaleta”, „Piętra” i źródło. Karta zawsze sięga do góry ekranu i się przewija, a pod krótką treścią zostaje puste miejsce (US2.8, NFR-53) |
| Cały ekran od razu | duża czcionka, okno co najmniej 600 dp, poziomo albo wybór z listy przy TalkBack. Karta bez mapy: wiersz o wejściach ze zdaniem z odpowiedzią nad przyciskami „Pokaż wejście na mapie” (wypełniony) i „Pokaż budynek na mapie” (obrysowany). Ma dwie wysokości: cały ekran i zamknięta. Po zmianie terenu napis o zmianie terenu czeka na mapę: pojawia się i TalkBack czyta go po „Pokaż budynek na mapie” (US2.9, US1.8, NFR-20, NFR-22) |
| Zwykły — budynek szczegółowy | pełny opis: chipy, wejścia, lista barier, winda i toaleta, piętra ze zwiniętymi opisami (US2.3, US16.3) |
| Zwykły — budynek podstawowy | pełny opis: chipy, wejścia, lista barier, napis o braku planu (US16.2, NFR-48) |
| Bez wejścia zalecanego | budynek nie ma wejścia „Dostępne”: napisu „Zalecane” nie ma, a wejścia „Niedostępne” są pierwsze, każde z powodem z danych (BR-25) |
| Ładowanie | tekst od razu, w miejscu zdjęcia napis „Wczytujemy zdjęcie…” (NFR-12) |
| Pusty — brak zgłoszeń przyjętych | w grupie „Bariery w budynku” nagłówek „Brak zgłoszonych barier”, zdanie „Bariera pojawi się tutaj, gdy ktoś ją zgłosi i zostanie sprawdzona.” i przycisk „Zgłoś barierę” (V4, Gość: L1). Bez wybranego wejścia ten przycisk stoi w treści karty tylko tutaj (US2.4, UC2 2a, PW-6) |
| Bez danych o barierach | telefon nie ma jeszcze danych o barierach budynku: karta nie mówi nic o barierach, także nie „Brak zgłoszonych barier” (US2.7, NFR-10) |
| Wejście bez danych | wejście z oznaczeniem „Brak danych”: cichy romb z mapy i słowo, bez napisu „Zalecane”; data i źródło oceny jak przy innych wejściach (US17.1, US2.1) |
| Bez wejść w danych | budynek nie ma w danych żadnego wejścia odwiedzających. W grupie „Wejścia” stoi wtedy zdanie „Nie mamy jeszcze danych o wejściach do tego budynku.”. Pod grupą nie ma daty i źródła, a w grupie przycisków. Pustej ramki nie ma, a brak danych nie znaczy, że budynek jest niedostępny (US17.1, US3.1) |
| Brak sieci | karta z telefonu z plakietką „Bez internetu · dane z 12.11.2026” nad nagłówkiem, na każdej wysokości karty. Daty nie ma, dopóki dane pochodzą tylko z pakietu aplikacji (decyzja 84). Mapa bez nowego podkładu, a obejrzany wcześniej może się pokazać z pamięci telefonu. Zdjęcia, których nie ma w telefonie, zastępuje napis „Zdjęcie zobaczysz po połączeniu z internetem.” (NFR-10) |
| Brak sieci, budynku nie ma w telefonie | „Tego budynku nie ma jeszcze w telefonie. Połącz się z internetem, aby zobaczyć opis budynku.” Przycisk „Spróbuj ponownie” (NFR-10) |
| Błąd zdjęcia | „Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę.” Przycisk „Spróbuj ponownie” (NFR-50) |

Dostępność i osoby starsze: oznaczenie wejścia jest pierwsze przy wejściu (US17.3). To znak z mapy i słowo zwykłym tekstem, bez plakietki (wymagania wspólne), a znaczenie niosą kształt znaku i słowo (NFR-21).

„Brak danych” ma własny znak, cichy romb, inny niż ośmiokąt „Niedostępne” (US17.1).

### V3 Którędy wejść (wycofany) {#v3-zaplanuj-wizyte}

Ekranu V3 nie ma, a jego identyfikator nie wraca do innego ekranu.

Odpowiedź, którym wejściem wejść, podaje wiersz o wejściach na [karcie V2](#v2-karta-budynku) (US3.1, US3.2).

Dojście, drzwi, windę za drzwiami i bariery przy wejściu pokazuje arkusz wejścia na [V1](#v1-mapa-kampusu), otwierany przez „Pokaż wejście na mapie” i wiersz na liście „Wejścia”. Inne wejścia budynku są na tej liście (US3.3).

### V4 Zgłoszenie szybkie

| Pole | Treść |
| --- | --- |
| Cel | Odwiedzający przekazuje informację o barierze w 3 krokach, bez formularza. |
| Role | RO-ODW i role pracownicze; Gość niezalogowany przechodzi najpierw do L1 (US14.2) |
| Realizuje | US4.1…US4.9, US14.2, US14.4 |
| Czynność główna | krok 1: „Zrób zdjęcie”, a po powrocie z kroku 2 „Dalej”; krok 2: wybór grupy, w budynku też piętra; krok 3: „Wyślij” |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Napis „Krok 1 z 3”, „Krok 2 z 3”, „Krok 3 z 3” i pod nim pasek z 3 odcinków; wykonane odcinki są wypełnione | dane | US4.1, NFR-21 | — |
| Nagłówek „Zgłoś barierę”, ten sam napis co na pasku dolnym | dane | US4, PW-14 | — |
| Krok 1: komunikat ostrzegawczy (wymagania wspólne) nad „Zrób zdjęcie”: „Ważne”, wymaganie „Nie fotografuj twarzy ani tablic rejestracyjnych.” i zdanie „Zdjęcie zobaczą inni użytkownicy.” Ten sam komunikat jest w kroku 1 po powrocie z kroku 2 | dane | NFR-25, NFR-21, BR-17, decyzja 54 | — |
| Krok 1: „Zrób zdjęcie” i „Zrób nowe zdjęcie” | czynność | US4.1, BR-8, PW-8 | aparat, potem krok 2 |
| Krok 1 po powrocie z kroku 2: miniatura zdjęcia i „Dalej” nad „Zrób nowe zdjęcie”. Przy skali czcionki co najmniej 1,5 „Dalej” zostaje przypięte na dole, a „Zrób nowe zdjęcie” stoi na końcu treści | czynność główna | US4.8, NFR-52 | krok 2; zdjęcie i grupa zostają |
| Krok 1 po „Zgłoś barierę przy wejściu” w arkuszu wejścia: napis „Wejście: »nazwa wejścia«” nad „Zrób zdjęcie”. Przy wejściu bez nazwy w danych napis ma też opis położenia, np. „Wejście: Wejście, od strony E-3, od południa”, bo kilka wejść może się nazywać „Wejście” | dane | US4.6, BR-27; znaczenie wejścia: V2 | — |
| Krok 1 po przycisku zgłoszenia w arkuszu toalety, schodów albo windy: nagłówek arkusza z budynkiem i piętrem, np. „Toaleta dostosowana: Budynek A-1, parter”, nad „Zrób zdjęcie” | dane | US4.6, BR-8 | — |
| Krok 2: pytanie „Czego dotyczy bariera?” i 6 przycisków grup barier na całą szerokość ekranu, każdy co najmniej 48 dp wysokości. Napisy bez ikon: „Wejście i drzwi”, „Schody i windy”, „Przejścia i chodniki”, „Toalety”, „Napisy i oznaczenia”, „Coś innego” | czynność | US4.3, NFR-17, PW-7 | krok 3 albo pytanie o piętro |
| Krok 2, pytanie o piętro: wybrana grupa, pytanie „Na którym piętrze?”. Pod nim przyciski na całą szerokość ekranu: „Nie wiem” i piętra od najniższego. W budynku szczegółowym etykiety z danych PWr, w podstawowym piętra od -1 do 10 w tej samej postaci: „Piwnica”, „Parter”, „1 piętro” … „10 piętro” | czynność | US4.5, BR-14, NFR-17 | krok 3 |
| Krok 3: miniatura zdjęcia, wybrana grupa oraz piętro albo „Nie wiem”, jeśli było pytanie | dane | US4.1, US4.5 | — |
| Krok 3: napis „Dołączymy miejsce, w którym teraz stoisz.” Przy wybranym wejściu zamiast niego stoi napis z kroku 1 „Wejście: »nazwa wejścia«” | dane | US4.1, US4.6, NFR-27 | — |
| Krok 3: „Wyślij” | czynność główna | US4.1 | potwierdzenie |
| Potwierdzenie: „Dziękujemy. Sprawdzimy zdjęcie, a jeśli bariera się potwierdzi, pokażemy ją na mapie.” | dane | US4.4, PW-5 | — |
| Potwierdzenie: „Wróć do mapy” | czynność | US4.4 | V1 |

Aplikacja nie pyta o typ bariery, uciążliwość ani budynek (US4.3, BR-14). Pozycję zaczyna ustalać po wejściu na ekran, w trakcie kroków 1 i 2 (NFR-27).

Przy wybranym wejściu pozycji nie ustala, o zgodę na lokalizację nie prosi i o piętro nie pyta. Stany o lokalizacji, pozycji i terenie PWr wtedy nie występują (US4.6, BR-8).

O piętro aplikacja pyta tylko w konturze budynku i przy każdej grupie poza „Coś innego” (US4.5, tabela w [regułach](../biznes/reguly.md#pietro-w-zgloszeniu-szybkim)). W budynku podstawowym i przed ustaleniem pozycji piętra od -1 do 10 mają postać „Piwnica”, „Parter”, „1 piętro” i dalsze, także w podsumowaniu kroku 3. W budynku szczegółowym są to piętra z danych PWr, a zgłoszenie dostaje identyfikator piętra zamiast numeru.

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
| Poza terenem PWr | bieżąca pozycja leży poza granicami wszystkich terenów PWr: „Jesteś poza terenem PWr. Zdjęcie bariery wyślesz tylko na terenie Politechniki.” Przyciski „Spróbuj ponownie” i „Zrezygnuj” (PW-5). Zdjęcie i grupa zostają do ponownej próby (US4.9, BR-G5, NFR-23) |
| Potwierdzenie rezygnacji | pojawia się przed usunięciem zrobionego zdjęcia. Wywołuje je „Zrezygnuj” w stanie „brak pozycji” albo „Wstecz” w kroku 1 po zrobieniu zdjęcia. Komunikat: „Jeśli zrezygnujesz, usuniemy zrobione zdjęcie i niczego nie wyślemy. Aby je zachować, wybierz »Wróć do zdjęcia«.” Przyciski „Tak, zrezygnuj” i „Wróć do zdjęcia”, tej samej wielkości. „Tak, zrezygnuj” usuwa zdjęcie, a zgłoszenie nie powstaje, jak przy rezygnacji z logowania (UC7 1b). Po „Zrezygnuj” aplikacja otwiera mapę (V1), a po „Wstecz” ekran sprzed V4. „Wróć do zdjęcia” wraca tam, skąd osoba przyszła, a zdjęcie i grupa zostają (US4.8; heurystyki Nielsena 3 i 5 [5]) |
| Brak sieci | potwierdzenie: „Zapisaliśmy zdjęcie w telefonie. Wyślemy je sami, gdy wróci internet. Potem sprawdzimy zdjęcie, a jeśli bariera się potwierdzi, pokażemy ją na mapie.” Przycisk „Wróć do mapy” (US4.2, US4.4, UC7 7a) |
| Błąd zapisu | „W telefonie brakuje miejsca, więc nie zapisaliśmy zdjęcia bariery. Usuń niepotrzebne zdjęcia albo filmy i spróbuj ponownie.” Przycisk „Spróbuj ponownie”. Zdjęcie i grupa zostają (NFR-5, NFR-7) |

Dostępność i osoby starsze: każdy krok ma jedną czynność, a przyciski grup wypełniają szerokość ekranu (US4.3, NFR-17). Odwiedzający nie traci zdjęcia przy braku pozycji ani przy błędzie, a zdjęcie znika dopiero po potwierdzeniu rezygnacji.

„Wstecz” w krokach 2 i 3 wraca o krok, a zdjęcie i grupa zostają. W kroku 1 ze zdjęciem „Dalej” prowadzi znowu do kroku 2 bez nowego zdjęcia. „Wstecz” w kroku 1 po zrobieniu zdjęcia najpierw pyta o potwierdzenie rezygnacji (US4.8).

„Wstecz” z potwierdzenia wraca do ekranu, z którego otwarto V4, a nie do kroku 3 (US4.7, wymagania wspólne).

#### Jak ograniczamy przerwanie przez logowanie

US14.2 wymaga logowania przed zdjęciem, czyli w środku zadania. W granicach obecnych kryteriów ograniczamy to przerwanie tak:

| Środek | Odwołanie |
| --- | --- |
| Prośba o logowanie pojawia się od razu po „Zgłoś barierę”, zanim osoba coś zrobi. Nie traci zdjęcia ani wybranej grupy. | US14.2 |
| L1 mówi, co będzie dalej: „Po zalogowaniu od razu zrobisz zdjęcie. Całość to 3 krótkie kroki.” | US14.2, US4.1 |
| Po zalogowaniu albo rejestracji aplikacja sama wraca do kroku 1 V4, bez ponownego wybierania „Zgłoś barierę”. | US14.2, UC7 1a |
| Aplikacja pamięta zalogowanie, więc przerwanie zdarza się tylko przy pierwszym zgłoszeniu. | US14.4, NFR-35 |
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
| Instrukcja w 3 krokach: „Znajdź budynek”, „Co znaczy »Dostępne«” i „Zgłoś barierę”. Każdy krok ma tytuł i najwyżej 3 zdania. Obraz jest tylko wtedy, gdy pokazuje prawdziwy element aplikacji, np. przycisk, pole wyszukiwania albo element opisu budynku, i ma opis dla TalkBack | dane | US5.1, NFR-18 | — |
| Numer kroku, np. „Krok 2 z 3”, i pod nim pasek z tylu odcinków, ile jest kroków; wykonane odcinki są wypełnione | dane | US5.1, NFR-21, decyzja 89 | — |
| Nagłówek „Jak korzystać z aplikacji” | dane | US5.1 | — |
| Zdanie w instrukcji: „Aby zgłosić barierę, potrzebne jest konto. Możesz je założyć teraz w »Konto«.” | dane | US5.1, US14.2, PW-5 | — |
| Zdanie w instrukcji: „Napis »Dostępne« przy wejściu znaczy, że wejdziesz bez pokonywania stopni: z poziomu chodnika, pochylnią albo podnośnikiem.” | dane | US5.1, US2.1, BR-24, PW-15 | — |
| „Dalej”, a w ostatnim kroku „Zacznij” | czynność główna | US5.1 | następny krok albo V1 |
| „Pomiń instrukcję” | czynność | US5.1, PW-8 | V1 |
| „Wstecz”: poprzedni krok instrukcji; w pierwszym kroku przy pierwszym uruchomieniu działa jak „Pomiń instrukcję” | czynność | US5.1, wymagania wspólne | poprzedni krok albo V1; przy otwarciu z paska pierwszy krok wraca do ekranu, z którego otwarto V5 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, L1 / L2 |

| Stan | Co widać |
| --- | --- |
| Pierwsze uruchomienie | instrukcja otwiera się sama przed V1; „Wstecz” w pierwszym kroku działa jak „Pomiń instrukcję” (US5.1) |
| Otwarcie z pomocy | ta sama instrukcja z paska dolnego; nie otwiera się już sama (US5.1) |

Zdanie o napisie „Dostępne” objaśnia go raz, bo osoba starsza może odczytać to słowo jako „otwarte” (recenzja B6, spór 19).

Treść instrukcji jest w telefonie, więc ekran nie ma stanu braku sieci. Dostępność i osoby starsze: przejście dalej przyciskiem zamiast przesunięcia palcem (NFR-53); brak automatycznego przewijania (NFR-23).

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
| Błąd | „Logowanie się nie udało albo zostało przerwane. Sprawdź internet i spróbuj ponownie.” Przyciski „Spróbuj ponownie” i „Nie teraz” (NFR-50) |
| Blokada konta | stan strony logowania w przeglądarce, nie ekranu L1: „Konto jest chwilowo zablokowane po kilku błędnych hasłach. Spróbuj ponownie później.” Treść ustawia się w Usłudze logowania (NFR-31, NFR-32) |

Dostępność i osoby starsze: strona logowania w przeglądarce też spełnia WCAG 2.1 AA (NFR-15). „Nie teraz” jest tak samo duże, jak „Zaloguj się”, więc rezygnacja nie wymaga szukania.

### L2 Konto

| Pole | Treść |
| --- | --- |
| Cel | Zalogowana osoba widzi swoje role i przechodzi do czynności swojej roli. |
| Role | RO-ODW i role pracownicze |
| Realizuje | US14.1, US14.3, US14.4 |
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
| „Wyloguj się” bez strzałki, bo działa od razu, bez kolejnego kroku | czynność | US14.4, PW-14 | V1 |
| Pasek dolny | czynność | wymagania wspólne | V1, V4, V5 |

Kolejność od góry: karta osoby, „Twoja praca”, sekcja „Konto”: klauzula, „Wycofaj zgodę”, „Wyloguj się”. Osoba z rolą pracowniczą otwiera L2 głównie po to, by pracować, więc czynności są tuż pod kartą osoby. Czynności to lista wierszy, a nie kafelki ani rząd jednakowych przycisków (decyzja 54).

| Stan | Co widać |
| --- | --- |
| Zwykły — Odwiedzający | nagłówek „Konto”, karta osoby, sekcja „Konto”; bez sekcji „Twoja praca” (US14.3) |
| Zwykły — rola pracownicza | nagłówek „Konto i praca”, karta osoby, sekcja „Twoja praca”, pod nią sekcja „Konto” |
| Klauzula informacyjna | treść klauzuli informacyjnej z podstawą przetwarzania, ta sama co przy rejestracji; „Wstecz” wraca do zwykłego stanu L2 (NFR-30). Administratorem danych jest zespół projektu. Klauzula ma zdanie „Politechnika Wrocławska nie jest administratorem Twoich danych.” i napis „Kontakt z zespołem:” z odnośnikiem „repozytorium projektu na GitHub”, bez słowa „zgłoszenia” (PW-5, decyzja 53). Tekst klauzuli nie był jeszcze sprawdzony przez prawnika |
| Potwierdzenie wycofania zgody | „Wylogujemy Cię i w ciągu miesiąca usuniemy Twoje dane osobowe. Wysłane zdjęcia barier zostaną, ale bez Twoich danych.” Przyciski „Wycofaj zgodę” i „Zostaw zgodę” (PW-5). Po potwierdzeniu osoba jest wylogowana, a aplikacja otwiera V1, jak po „Wyloguj się” (NFR-29) |
| Brak sieci | role z ostatniego logowania; ekrany czynności pokazują własne stany braku sieci (NFR-10) |

Dostępność i osoby starsze: klauzula jest najwyżej 2 dotknięcia od mapy: „Konto” (u ról pracowniczych „Konto i praca”), „Informacja o danych osobowych” (NFR-30). Klauzula jest stanem L2, a nie osobnym ekranem, bo NFR-30 wymaga jej w profilu.

## Zbierający

### C1 Pomiar pozycji

| Pole | Treść |
| --- | --- |
| Cel | Zbierający zapisuje dokładne miejsce bariery i wskazuje budynek. |
| Role | RO-ZBI |
| Realizuje | US6.1, US6.2, US6.3, US6.4, US6.5 |
| Czynność główna | „Dalej” — zatwierdzenie miejsca |
| Czynność główna przypięta | tak (reguła 10) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Postęp uśredniania 30 s, np. „Pomiar: zostało 12 s” | dane | US6.1 | — |
| Dokładność pozycji w metrach z dopiskiem „szacunek”, np. „Dokładność: 0,8 m (szacunek)” | dane | US6.1, NFR-2 | — |
| Liczba satelitów osobno dla każdego systemu GNSS, np. „Satelity: GPS 7, Galileo 5” | dane | US6.1, NFR-2 | — |
| Mapa nieba; system satelity rozróżniony kształtem i podpisem, pod mapą legenda kształtów | dane | US6.5, NFR-21 | — |
| „Powtórz pomiar” | czynność | UC8 2a, NFR-23 | początek pomiaru |
| Podpowiedź budynku: „Czy to budynek C-13?”, pod pytaniem nazwa budynku z danych i odległość od punktu, i „Potwierdź”, „Wybierz inny”, „Poza budynkiem” | czynność | US6.2, BR-G2 | „Wybierz inny”: lista budynków na C1 |
| Budynek szczegółowy: przełącznik „Plan piętra” i „Mapa” nad jedną powierzchnią, domyślnie „Plan piętra”. Plan ma nad sobą wybór piętra, a mapa pokazuje ten sam punkt. | czynność | US6.3, US6.4, NFR-21 | C1, ten sam ekran |
| Punkt stoi na środku planu albo mapy. Nad nimi podpis „Przesuń mapę, aby punkt stał na miejscu bariery.” Plan i mapę przesuwają te same przyciski co na V1: „Powiększ”, „Pomniejsz” i strzałki w krzyżu, a punkt zostaje na środku | czynność | US6.3, US6.4, NFR-3, NFR-53; wymagania V1; decyzja 51, decyzja 93 | — |
| „ⓘ” na mapie i planie: legenda znaków tej mapy, źródła i pełna licencja mapy jak na V1 | czynność | US1.4, NFR-37, NFR-40 | C1, arkusz „Legenda” |
| Napis „Korekta ręczna” po przesunięciu punktu | dane | US6.3, US6.4 | — |
| Napis „© OpenStreetMap contributors”; pod planem „Źródło: Politechnika Wrocławska” | dane | NFR-37, NFR-40 | — |
| „Dalej”; pojawia się po końcu uśredniania, razem z podpowiedzią budynku | czynność główna | US6, US6.1 | C2 |

| Stan | Co widać |
| --- | --- |
| Zwykły | uśrednianie, dokładność, satelity, podpowiedź budynku |
| Po pomiarze | napis „Pomiar zakończony”, dokładność i satelity z końca uśredniania, „Powtórz pomiar”, podpowiedź budynku i „Dalej” (US6.1, US6.2) |
| Ładowanie — szukanie satelitów | „Szukamy satelitów. Na otwartej przestrzeni pomiar będzie dokładniejszy.” Przycisk „Powtórz pomiar” (NFR-1, PW-9) |
| Powrót po przerwie | ekran był niewidoczny dłużej niż 5 s w trakcie uśredniania, np. po wizycie w ustawieniach, albo lokalizację wyłączono: pomiar zaczyna się od nowa, a nad nim jest „Pomiar był przerwany, więc zaczęliśmy go od nowa.” Krótsza przerwa, np. obrót ekranu, pomiaru nie przerywa. Gotowy wynik zostaje (NFR-23, NFR-27) |
| Wybór innego budynku | po „Wybierz inny” w miejscu pytania sekcja „Wybierz budynek”: budynki najbliżej punktu, od najbliższego, każdy z nazwą i odległością, i „Poza budynkiem”. Wybór zamyka listę i pokazuje „Budynek [symbol]” z „Wybierz inny”. Lista działa bez sieci, z konturów w telefonie (US6.2, BR-G2, NFR-4). |
| Daleko od budynków PWr | „Poza budynkiem”, bez podpowiedzi i bez komunikatu o błędzie (BR-G4, NFR-49) |
| Budynek podstawowy | bez przełącznika „Plan piętra” i „Mapa”, wyboru piętra i korekty na planie. W ich miejscu napis „Nie mamy planu pięter tego budynku.”, jak na V2. Korekta na mapie działa (US6.4, BR-G1, PW-6). |
| Brak zgody na lokalizację | „Aby zapisać miejsce bariery, potrzebujemy lokalizacji. Sprawdzimy ją tylko teraz i nie będziemy Cię śledzić.” Przycisk „Pozwól na lokalizację” (NFR-27) |
| Odmowa zgody | „Bez lokalizacji nie zapiszesz miejsca bariery. Możesz ją włączyć w ustawieniach telefonu.” Przycisk „Otwórz ustawienia”. Aplikacja nie ponawia prośby sama (NFR-27, BR-8) |
| Zgoda tylko na przybliżoną lokalizację | „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. Pozwól na dokładną lokalizację.” Przycisk „Pozwól na dokładną lokalizację” (BR-8, US6.1) |
| Lokalizacja wyłączona w telefonie | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby zapisać miejsce bariery.” Przycisk „Otwórz ustawienia” |
| Stany zgody, wspólnie | ten sam układ co w V4, z jednym nagłówkiem „Zmierz i zgłoś barierę”. Bez przycisku „Wróć do mapy”: rezygnacja przez „Wstecz” do ekranu, z którego otwarto C1, czyli L2, C2 albo C3 (NFR-15) |
| Brak sieci | pomiar i podpowiedź działają; mapa bez nowego podkładu, a fragmenty obejrzane wcześniej mogą się pokazać z pamięci telefonu (NFR-4, NFR-39) |

Dostępność: liczby dokładności i satelitów są tekstem, a nie tylko wykresem (NFR-18). Uśrednianie można przerwać i powtórzyć bez limitu (NFR-23). TalkBack ogłasza szukanie satelitów, „Pomiar zakończony”, podpowiedź budynku i błędy, a odliczanie co sekundę nie jest ogłaszane (NFR-18).

### C2 Formularz zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Zbierający opisuje barierę tak, by Weryfikator mógł ją ocenić. |
| Role | RO-ZBI |
| Realizuje | US7.1, US7.2, US7.3, US7.4, US8.3 |
| Czynność główna | „Zapisz” |
| Czynność główna przypięta | tak (reguła 10) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Budynek i miejsce z C1. Miejsce słowami: najbliższe wejście z danych z odległością i dokładność pomiaru, np. „ok. 3 m od Wejścia Cesarskiego, dokładność 0,8 m”; bez współrzędnych | dane | US7.4, US6.1, NFR-19 | — |
| „Zmień miejsce”: powrót do C1, wpisane pola zostają | czynność | US7.4 | C1 |
| Piętro: wybór z etykiet z danych PWr (budynek szczegółowy), chipy wspólnego wyboru piętra | czynność | US7.2 | — |
| Piętro: liczba całkowita w polu z etykietą „Numer piętra” i pole „Półpiętro” (budynek podstawowy); nad polem „Nie mamy planu pięter tego budynku. Wpisz numer piętra, 0 to parter.” | czynność | US7.2, BR-G1, NFR-15, PW-6 | — |
| „Typ bariery”, pogrupowany w 6 grup barier. Pole otwiera panel na cały ekran. U góry są nagłówek „Typ bariery” i „Zamknij”, które nie przewijają się z listą. Pod nimi są grupy jako nagłówki i typy. Po otwarciu fokus jest na nagłówku, po zamknięciu wraca na pole | czynność | US7.1, BR-7, NFR-18, NFR-53 | — |
| „Uciążliwość”: „Utrudnienie”, „Tylko z pomocą”, „Blokada”, każda wartość z opisem i ikoną; pod nagłówkiem „Oceń dla osoby, dla której bariera jest najtrudniejsza.” | czynność | US7.1, NFR-21, PW-13 | — |
| Zdjęcie, co najmniej jedno: „Zrób zdjęcie” i „Zrób nowe zdjęcie”. Nad nimi komunikat ostrzegawczy jak w kroku 1 V4: „Ważne”, „Nie fotografuj twarzy ani tablic rejestracyjnych.” i „Zdjęcie zobaczą inni użytkownicy.” | czynność | US7.1, NFR-25, PW-8, decyzja 54 | aparat |
| Opis; obowiązkowy przy TB-29 „inna bariera” | czynność | US7.3 | — |
| Przy zmianie zgłoszenia przyjętego: „Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.” | dane | BR-18 | — |
| „Zapisz” | czynność główna | BR-7, NFR-54 | C3 |

Poza budynkiem pola piętra nie ma (BR-G4). C2 otwiera się z C1, z C3 przy edycji (US8.3) i z V2 przy zmianie zgłoszenia przyjętego (BR-18).

| Stan | Co widać |
| --- | --- |
| Zwykły — nowe zgłoszenie | puste pola, miejsce z C1 |
| Edycja | pola z zapisanymi wartościami (US8.3) |
| Wyjście bez zapisu | po „Wstecz” z niezapisanymi zmianami: „Wyjść bez zapisu? Wpisane pola nie zostaną zapisane.” Przyciski „Wyjdź bez zapisu” i „Wróć do formularza” (US7.4, NFR-23) |
| Błąd walidacji | po „Zapisz” nad formularzem pierwszy brak, np. „Brakuje typu bariery. Uzupełnij to pole, aby zapisać.”, a przy każdym brakującym polu jego tekst: „Brakuje uciążliwości. Uzupełnij to pole, aby zapisać.”, „Brakuje zdjęcia. Zrób zdjęcie, aby zapisać.”, „Brakuje opisu. Przy typie „inna bariera” opis jest obowiązkowy.” (PW-10); opis z samych spacji to brak opisu. Po pierwszej próbie braki znikają na bieżąco, gdy pole zostanie uzupełnione; zapis się nie wykonuje (US7.1, US7.3) |
| Brak sieci | zapis działa, zgłoszenie dostaje status „lokalne”, na ekranie „W telefonie” (US9.1, PW-12) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” (NFR-7). Gdy zdjęcia nie pasują do treści: „Nie zapisaliśmy zgłoszenia. Zrób zdjęcie jeszcze raz i zapisz ponownie.” Gdy telefon nie zna wersji poprawianego zgłoszenia: „Nie zapisaliśmy poprawki, bo telefon nie zna aktualnej wersji zgłoszenia. Otwórz „Moje zgłoszenia”, poczekaj na statusy i popraw je jeszcze raz.” (BR-10). Wpisane pola zostają (NFR-50) |

Dostępność: błąd walidacji jest tekstem przy polu i w podsumowaniu, a fokus TalkBack przechodzi do pierwszego brakującego pola (NFR-15, WCAG 3.3.1).

### C3 Moje zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Zbierający wie, które zgłoszenia dotarły, a które trzeba poprawić. |
| Role | RO-ZBI, tylko własne zgłoszenia z pomiaru precyzyjnego i własne zmiany; zgłoszeń szybkich lista nie pokazuje (US4.4) |
| Realizuje | US8.1, US8.2, US8.3, US9.1, US9.5, US8.4 (status zmiany), US17.2 (status zmiany) |
| Czynność główna | „Otwórz” przy zgłoszeniu do poprawy |
| Czynność główna przypięta | nie: czynność powtarza się w pozycjach listy |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Zgłoszenie: typ, budynek, piętro, data | dane | US8.1 | — |
| Rodzaj: zgłoszenie, „bariera usunięta” (na ekranie „Bariery już nie ma”) albo zmiana oznaczenia wejścia; przy zmianie oznaczenia „Nowe oznaczenie:” tekstem i ikoną | dane | US8.4, US17.2, NFR-21 | — |
| Jeden status tekstem i ikoną: „W telefonie” (lokalne), „Nie wysłano”, „Wysłane”, „W weryfikacji”, „Przyjęte”, „Odrzucone”, „Archiwalne”. Przy konflikcie wersji status wersji z serwera, nie „Nie wysłano”: zmiana nie jest odrzucona, czeka na wybór | dane | US8.1, US8.2, NFR-21, PW-12, BR-10 | — |
| Przy odrzuconym: powód odrzucenia | dane | US8.1, BR-16 | — |
| Przy „Nie wysłano”: powód. Przy odebranej roli „Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie.”, w pozostałych przypadkach „Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie.” | dane | US9.5, US14.1, BR-26, PW-10 | — |
| Znak „Zmiana czeka na wysłanie” przy niewysłanej edycji | dane | US8.1, BR-9, [reguły, cykl życia zgłoszenia](../biznes/reguly.md) | — |
| Znak „Dwie wersje: wybierz jedną” jako czynność karty ze strzałką przy konflikcie wersji, bez powodu odmowy | czynność | US9.3, BR-10 | C4 |
| Przy zgłoszeniu przed decyzją, odrzuconym albo „Nie wysłano” „Otwórz” jako czynność karty ze strzałką (wymagania wspólne). Zgłoszenia przyjęte i archiwalne nie są przyciskami. Przy konflikcie wersji „Otwórz” nie ma do wyboru wersji w C4, bo poprawka go nie rozstrzyga | czynność główna | US8.1, US8.3, US9.5, BR-3, BR-26, BR-10 | C2; zmiana oznaczenia wejścia: C6; „bariera usunięta”: C7 |
| Przy powodzie „nieaktualne”: „Ktoś już zmienił to zgłoszenie. Twoja zmiana trafi do jego aktualnej wersji.” (przy zmianie oznaczenia wejścia: „Oznaczenie tego wejścia już się zmieniło. Sprawdź je i wyślij swoją zmianę ponownie.”) i przycisk „Wyślij ponownie” | czynność | BR-23, BR-24, PW-14 | C2; zmiana oznaczenia wejścia: C6; „bariera usunięta”: C7 |
| Przy „nieaktualne” po usunięciu bariery: „Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa.” Przycisk „Zmierz i zgłoś barierę” | dane, czynność | BR-23, PW-12 | C1 |
| Stan synchronizacji, np. „Czeka na wysłanie: 3” | czynność | US9.1 | C4 |
| „Zmierz i zgłoś barierę” | czynność | US6, PW-12 | C1 |

| Stan | Co widać |
| --- | --- |
| Zwykły | lista własnych zgłoszeń, najnowsze na górze (US8.1) |
| Pusty | „Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę.” Przycisk „Zmierz i zgłoś barierę” do C1 (UC9 0a, PW-12) |
| Ładowanie | lista z telefonu zostaje widoczna; obok napis „Sprawdzamy statusy…” |
| Brak sieci | lista z telefonu ze statusami z dnia ostatniej synchronizacji; nowe zgłoszenia „W telefonie” (US9.1) |
| Błąd | „Nie udało się sprawdzić statusów, ale zgłoszenia są bezpieczne w telefonie. Spróbuj ponownie za chwilę.” Przycisk „Spróbuj ponownie” (NFR-5, NFR-50, PW-10) |

Dostępność: status jest słowem, a ikona go tylko wzmacnia (NFR-21). TalkBack czyta status przed typem bariery (NFR-18).

### C4 Synchronizacja i wybór wersji

| Pole | Treść |
| --- | --- |
| Cel | Zbierający wie, co czeka na wysłanie, i nie traci żadnej wersji zgłoszenia. |
| Role | RO-ZBI |
| Realizuje | US9.1, US9.2, US9.3, US9.4, US9.5 |
| Czynność główna | przy konflikcie: wybór wersji; bez konfliktu: „Synchronizuj teraz” |
| Czynność główna przypięta | nie: przy konflikcie to ekran decyzji, wybór stoi na końcu treści, pod obiema wersjami (NFR-52) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| „Czeka na wysłanie: 3” i pod nim podział, np. „2 zgłoszenia i 1 znacznik piętra”. Liczą się zgłoszenia „W telefonie” i zmiany znaczników pięter. Zgłoszeń „Nie wysłano” i czekających na wybór wersji licznik nie obejmuje; te mają znak na C3 | dane | US9.1, US9.5, BR-9, BR-10, BR-26 | — |
| Przy wygasłym zalogowaniu: „Logowanie wygasło. Zaloguj się, a wyślemy zgłoszenia same.” i przycisk „Zaloguj się” | dane, czynność | US9.4, US14.4, BR-26, PW-10 | L1 |
| „Ostatnia synchronizacja: 12.11.2026, 10:42”: chwila ostatniej wysyłki, która doszła do końca kolejki; zostaje po ponownym uruchomieniu aplikacji. Przed pierwszą taką wysyłką wiersza nie ma | dane | NFR-10, US9.1 | — |
| „Synchronizuj teraz”; synchronizacja i tak rusza sama | czynność | UC10, US9.2 | — |
| Ostrzeżenie „W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia.” | dane | NFR-7, PW-10 | — |
| Konflikt: na panelu informacyjnym napis „Dwie wersje: wybierz jedną. Druga wersja zostanie usunięta.”; przy zgłoszeniu przyjętym drugie zdanie „Przyjęte zgłoszenie zostanie, a Twoja wersja, jeśli ją wybierzesz, trafi do sprawdzenia jako nowe.” (UC11 3b) | dane | US9.3, BR-10, PW-12 | — |
| Konflikt: wersja własna i wersja z serwera naraz. Nad nimi napis „Różni się:” z nazwą pola, np. „Różni się: uciążliwość”, a przy kilku polach „Różnią się:” i ich nazwy. W obu wersjach te pola są pogrubione. Każda wersja stoi na karcie pozycji listy, jak zgłoszenie na C3 (wymagania wspólne) | dane | US9.3, NFR-21 | — |
| „Zostaw moją wersję” i „Weź wersję z serwera” | czynność główna | US9.3, BR-10, NFR-54 | C3 |
| „Zdecyduję później”; zmiana czeka w telefonie | czynność | BR-10 | C3 |

| Stan | Co widać |
| --- | --- |
| Zwykły — kolejka | liczba czekających i data ostatniej synchronizacji |
| Pusty | „Wszystko wysłane. Ostatnia synchronizacja: 12.11.2026, 10:42.” (US9.1) |
| Ładowanie | „Wysyłamy 2 z 5…” |
| Brak sieci | „Bez internetu. Wyślemy wszystko sami, gdy internet wróci.” (US9.1, US9.2, decyzja 54) |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później.” (NFR-50, US9.4) |
| Wygasłe zalogowanie | „Logowanie wygasło. Zaloguj się, a wyślemy zgłoszenia same.” Zgłoszenia czekają (US9.4) |
| Konflikt | napis o skutku, obie wersje i trzy przyciski wyboru (US9.3) |

Dostępność: w pionie wersje stoją jedna pod drugą, w poziomie obok siebie, by przy czcionce 200% nic się nie ucinało (NFR-20, NFR-22).

### C5 Piętra obejrzane

| Pole | Treść |
| --- | --- |
| Cel | Zbierający oznacza piętro albo budynek jako obejrzany, a Koordynator cofa błędny znacznik. |
| Role | RO-ZBI: oznacza i cofa własne; RO-KOO: cofa każdy znacznik |
| Realizuje | US15.1, US15.2, US15.3, US11.3 |
| Czynność główna | RO-ZBI: „Oznacz jako obejrzane”; RO-KOO: „Cofnij znacznik” |
| Czynność główna przypięta | nie: czynność powtarza się przy piętrach |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Symbol budynku | dane | US15.1 | — |
| Budynek szczegółowy: lista pięter z etykietami z danych PWr, od najniższego, jak pytanie o piętro w V4 | dane | US15.1, US16.1, US4.5 | — |
| Budynek podstawowy: jeden wiersz „Cały budynek” | dane | US15.1, BR-20 | — |
| Przy wierszu: „Obejrzane” tekstem i ikoną albo „Nieobejrzane” samym tekstem | dane | US15.1, PW-11 | — |
| Przy znaczniku u Zbierającego: „Twój znacznik” albo „Znacznik innej osoby”; u Koordynatora: „Znacznik: Zbierający, Autor [nr]” | dane | US15.2, BR-5 | — |
| Przy znaczniku w telefonie: „Czeka na wysłanie” | dane | US15.3 | — |
| Zbierający: „Oznacz jako obejrzane” przy wierszu bez znacznika | czynność główna | US15.1 | — |
| Zbierający: „Cofnij znacznik” tylko przy własnym znaczniku, z potwierdzeniem | czynność | US15.2, PW-12 | C5, potwierdzenie |
| Koordynator: „Cofnij znacznik” przy każdym znaczniku, z potwierdzeniem | czynność główna | US11.3, BR-20 | C5, potwierdzenie; do K1 przez „Wstecz” |

C5 otwiera Zbierający z karty budynku (V2), a Koordynator z postępu (K1). Koordynator nie stawia znaczników (BR-20), więc nie widzi „Oznacz jako obejrzane”.

| Stan | Co widać |
| --- | --- |
| Zwykły | lista pięter albo wiersz „Cały budynek” |
| Potwierdzenie cofnięcia | pod znacznikiem, u Zbierającego: „Cofnąć znacznik: 2 piętro? Postęp budynku się zmniejszy.”; u Koordynatora: „Cofnąć znacznik: 2 piętro? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający.” Przyciski „Cofnij znacznik” i „Zostaw znacznik” (US15.2, US11.3, BR-20, PW-12) |
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
| Czynność główna przypięta | tak (reguła 10) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Budynek i nazwa wejścia | dane | US17.2 | — |
| Pod podpisem „Obecne oznaczenie” oznaczenie tekstem i ikoną, z datą i źródłem jak na V2 | dane | US17.3, NFR-21, PW-3 | — |
| Dwa duże przyciski: „Dostępne” i „Niedostępne” | czynność | US17.2, BR-24, NFR-17 | — |
| Przy wyborze obecnej wartości: „Potwierdzasz obecne oznaczenie. Zmieni się data i źródło.” | dane | BR-24, UC18 2a | — |
| Zdjęcie wejścia, obowiązkowe: „Zrób zdjęcie” i „Zrób nowe zdjęcie”, z komunikatem ostrzegawczym jak w C2 | czynność | US17.2, BR-24, NFR-25, PW-8, decyzja 54 | aparat |
| Napis „Zmianę sprawdzi inna osoba. Do tego czasu karta budynku pokazuje obecne oznaczenie.” | dane | BR-4, BR-24 | — |
| „Wyślij do sprawdzenia” | czynność główna | US17.2, NFR-54 | C3 |

„Brak danych” nie jest do wyboru, bo nadaje je tylko import (BR-24).

| Stan | Co widać |
| --- | --- |
| Zwykły | obecne oznaczenie, wybór i zdjęcie |
| Błąd walidacji | „Dodaj zdjęcie wejścia, aby wysłać zmianę.” albo „Wybierz: dostępne czy niedostępne.” (BR-24) |
| Brak sieci | wysłanie działa i otwiera C3 z komunikatem „Zmiana zapisana w telefonie. Wyślemy ją, gdy wróci internet.” i statusem „W telefonie”, jak po zapisie C2 (US9.1, NFR-54) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” (NFR-7) |

### C7 Usunięcie bariery

| Pole | Treść |
| --- | --- |
| Cel | Zbierający zgłasza, że bariery ze zgłoszenia przyjętego już nie ma, np. winda działa. |
| Role | RO-ZBI, także dla cudzych zgłoszeń (BR-19) |
| Realizuje | US8.4 |
| Czynność główna | „Wyślij do sprawdzenia” |
| Czynność główna przypięta | tak (reguła 10) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Zgłoszenie przyjęte: typ, piętro, miejsce, zdjęcie, data weryfikacji; bez edycji | dane | BR-19 | — |
| Nowe zdjęcie miejsca, obowiązkowe: „Zrób zdjęcie” i „Zrób nowe zdjęcie”, z komunikatem ostrzegawczym jak w C2 | czynność | US8.4, BR-19, NFR-25, PW-8, decyzja 54 | aparat |
| Napis „Bariera zostanie na mapie, dopóki Weryfikator nie sprawdzi zgłoszenia.” | dane | US8.4 | — |
| „Wyślij do sprawdzenia” | czynność główna | US8.4, NFR-54 | C3 |

Uciążliwości ani typu się nie wybiera: przechodzą z poprzedniego zgłoszenia albo nie są wymagane (BR-19).

| Stan | Co widać |
| --- | --- |
| Zwykły | dane poprzedniego zgłoszenia i miejsce na nowe zdjęcie |
| Błąd walidacji | „Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.” (BR-19) |
| Brak sieci | wysłanie działa i otwiera C3 z komunikatem „Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet.” i statusem „W telefonie”, jak po zapisie C2 (US8.4, US9.1, NFR-54) |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” (NFR-7) |

## Weryfikator

### R1 Kolejka weryfikacji

| Pole | Treść |
| --- | --- |
| Cel | Weryfikator wybiera zgłoszenie do sprawdzenia. |
| Role | RO-WER |
| Realizuje | US10.4, US10.5, US10.7 |
| Czynność główna | „Weź do sprawdzenia” |
| Czynność główna przypięta | nie: czynność powtarza się w pozycjach listy |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Nad listą nagłówek „Do sprawdzenia”. Lista zgłoszeń „wysłane” od najdawniej wysłanego: typ albo grupa barier, budynek, rodzaj, data wysłania | dane | US10.4, BR-14 | — |
| Przełącznik „Mapa” i „Lista” w sekcji „Do sprawdzenia”; domyślnie wybrana „Lista”. Wybór nie jest zapamiętywany: R1 zawsze otwiera się na liście. Lista daje tę samą informację. | czynność | UC14, NFR-19 | — |
| W widoku mapy pod przełącznikiem mapa wysokości 340 dp przy każdej skali czcionki, na całą szerokość treści. To ta sama mapa kampusu co na V1: podkład, budynki z symbolami i ścieżki dostosowane. Znaków wejść, barier i planu piętra, przycisku terenu ani pola wyszukiwania nie ma, bo R1 pokazuje zgłoszenia. Po wybraniu „Mapa” kamera obejmuje wszystkie znaki zgłoszeń, a przy jednym zgłoszeniu stoi na nim w powiększeniu 17. Gest palcem w ramce mapy przesuwa i przybliża mapę, poza ramką przewija stronę. TalkBack czyta mapę jako „Mapa kampusu z 4 zgłoszeniami do sprawdzenia. Te same zgłoszenia są w widoku Lista.” | dane | US10.4, NFR-18, NFR-19, NFR-53 | — |
| Napis „© OpenStreetMap contributors” na mapie i „ⓘ” jak na V1. Arkusz „Legenda” ma pozycje „Budynek z planem pięter”, „Budynek bez planu”, „Ścieżka dostosowana” i „Zgłoszenie do sprawdzenia”. Na końcu stoją „Źródła danych” z treścią licencyjną jak na V1 | dane, czynność | US1.4, NFR-21, NFR-37, NFR-40; decyzja 93 | R1, arkusz „Legenda” |
| W widoku mapy te same przyciski mapy co na V1: „Powiększ”, „Pomniejsz” i strzałki przesuwania w krzyżu | czynność | NFR-53; WCAG 2.5.1 [1], 2.5.7 [7]; wymagania V1 | — |
| W widoku mapy każde zgłoszenie ma jeden znak „zgłoszenie do sprawdzenia” w swoim punkcie, z nazwą dla TalkBack, np. „Zgłoszenie do sprawdzenia: próg, budynek C-13”. Znak to kartka z zagiętym prawym górnym rogiem i dwiema poziomymi liniami, z ostrzem w dół, którego czubek stoi w punkcie zgłoszenia. Kartka ma kolor `tusz`, a linie, róg i obwódka kolor `papier`, jak znaki na mapie V1. Kształt różni się od znaków wejść, barier i planu, więc znaczenia nie niesie barwa. Znak ma 31 × 44 dp, a obszar dotyku co najmniej 48 dp | dane | NFR-17, NFR-18, NFR-21 | — |
| W widoku mapy dotknięcie znaku wybiera to zgłoszenie, a jego znak rośnie do 36 × 52 dp i wskazuje punkt jak pinezka według [wspólnej zasady](#zaznaczenie-na-mapie). Obok znaku stoi typ albo grupa barier zgłoszenia, napisem jak symbol budynku na mapie, a TalkBack dodaje „wybrane”. Pod mapą stoi to zgłoszenie z tymi samymi danymi co na liście i „Weź do sprawdzenia”. Bez wybranego znaku pod mapą nic nie ma, bo te same zgłoszenia są w widoku „Lista”. Dotknięcie pustej mapy zdejmuje wybór, a dotknięcie innego znaku go zmienia. Zgłoszenie zajęte przez kogoś innego znika z mapy razem ze swoim znakiem | czynność | UC14, NFR-17, NFR-19, NFR-53; BR-11 | R2 |
| Sekcja „Wzięte przeze mnie” ze zgłoszeniami „w weryfikacji” tego Weryfikatora; w każdej pozycji „Otwórz” jako czynność karty ze strzałką (wymagania wspólne) | czynność | US10.7, BR-11, UC14 2b, NFR-21 | R2 |
| „Weź do sprawdzenia” jako czynność karty (wymagania wspólne) | czynność główna | US10.4, BR-11 | R2 |

Własnych zgłoszeń, także zmian „bariera usunięta” i zmian oznaczenia wejścia, lista nie pokazuje (US10.5, BR-4).

| Stan | Co widać |
| --- | --- |
| Zwykły | lista albo mapa zgłoszeń |
| Pusty | „Nie ma zgłoszeń do sprawdzenia. Zajrzyj tu później.” Bez przełącznika „Mapa” i „Lista” (UC14 1b, PW-10) |
| Ładowanie | „Wczytujemy zgłoszenia…” |
| Zgłoszenie zajęte | „Ktoś inny wziął już to zgłoszenie. Wybierz inne.” Zgłoszenie znika z listy (UC14 2a, BR-11) |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne (UC14 1a, NFR-50) |

### R2 Sprawdzenie zgłoszenia

| Pole | Treść |
| --- | --- |
| Cel | Weryfikator przyjmuje zgłoszenie albo odrzuca je z powodem, bez przepuszczenia błędu i duplikatu. |
| Role | RO-WER, tylko cudze zgłoszenia wzięte przez siebie (BR-4, BR-11) |
| Realizuje | US10.1, US10.2, US10.3, US10.6, US10.8, US17.2 |
| Czynność główna | „Przyjmij” |
| Czynność główna przypięta | nie: ekran decyzji, „Przyjmij” i „Odrzuć” stoją na końcu treści, po „Podobne zgłoszenia do 10 m” (BR-13, NFR-52) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Na górze rodzaj zgłoszenia, np. „Zgłoszenie z pomiaru”, „Zgłoszenie szybkie”, „Bariery już nie ma”, „Zmiana oznaczenia wejścia” | dane | US10.4 | — |
| Dane zgłoszenia: typ, uciążliwość napisem z wymagań wspólnych, budynek, piętro, miejsce słowami jak w C2; bez edycji przy pomiarze precyzyjnym | dane | BR-15, US6.1, NFR-19, PW-13 | — |
| Zdjęcie | dane | US10 | — |
| Rola i pseudonim autora, np. „Zbierający, Autor [nr]” | dane | BR-5 | — |
| „Podobne zgłoszenia do 10 m”: typ, zdjęcie, odległość, piętro, status | dane | US10.1, BR-13 | — |
| Zgłoszenie szybkie: „Czego dotyczy:” i grupa barier wybrana przez autora | dane | BR-14, US4.3 | — |
| Zgłoszenie szybkie: wybór „Typ bariery” i „Uciążliwość” z wartościami „Utrudnienie”, „Tylko z pomocą”, „Blokada”. Lista typów otwiera się na grupie wybranej przez autora, a pozostałe grupy są pod nią | czynność | US10.3, BR-14, PW-13 | — |
| „Zasłoń twarz lub tablicę” na zdjęciu otwiera to zdjęcie na cały ekran do zasłaniania; zasłonięcie nie tworzy nowej wersji zgłoszenia | czynność | BR-15, NFR-25 | zasłanianie zdjęcia |
| Zasłanianie: nad zdjęciem napis „Dotknij zdjęcia w miejscu twarzy albo tablicy.” Dotknięcie zdjęcia stawia w tym miejscu prostokąt zasłony stałej wielkości. Dotknięcie prostokąta go zaznacza; zaznaczony ma grubszą obwódkę i napis „Zaznaczona”, nie sam kolor. Bez przeciągania i szczypania | czynność | NFR-53, NFR-21 | — |
| Zasłanianie: „Większy”, „Mniejszy” i „Usuń zasłonę” dla zaznaczonego prostokąta, pod zdjęciem; każdy cel co najmniej 48 × 48 dp | czynność | NFR-17, NFR-53 | — |
| Zasłanianie: TalkBack czyta każdy prostokąt z numerem i położeniem słowami, np. „Zasłona 1 z 2, u góry po lewej, zaznaczona” | dane | NFR-18 | — |
| Zasłanianie: „Zapisz zdjęcie” jako czynność główna w przypiętym pasie na dole ekranu, jak „Zapisz” na C2. Wypala zasłony w pikselach zdjęcia i wysyła je, a oryginał nie opuszcza telefonu. Po zapisie R2 pokazuje nowe zdjęcie. „Wstecz” wraca do R2 bez zmian | czynność | BR-15, NFR-25 | R2 |
| Zmiana oznaczenia wejścia: obecne oznaczenie z datą i źródłem, wybrane oznaczenie, nowe zdjęcie | dane | US17.2, BR-24 | — |
| Zmiana oznaczenia wejścia: tekstem i ikoną, czy oznaczenie jest „Bez zmian od wysłania”, czy „Zmienione po wysłaniu” przez inną przyjętą zmianę | dane | US10.8, BR-23, BR-24, NFR-21 | — |
| „Bariera usunięta”: zdjęcie poprzedniego zgłoszenia obok nowego, z podpisami „Zdjęcie z poprzedniego zgłoszenia” i „Nowe zdjęcie miejsca” | dane | US10.8, US10.6, BR-19 | — |
| „Bariera usunięta”: „Poprzednie zgłoszenie:” i jego status tekstem i ikoną, np. „Archiwalne” | dane | US10.8, BR-19, BR-23, NFR-21 | — |
| „Przyjmij” | czynność główna | US10, US10.6, NFR-54 | R1 |
| „Odrzuć” z obowiązkowym polem „Powód odrzucenia”; gotowe powody „Duplikat” ze wskazaniem wcześniejszego zgłoszenia i „Nieaktualne” oraz „Inny powód” z polem „Opisz powód dla autora”. Przy zmianie oznaczenia wejścia bez „Duplikat”, bo duplikat opisuje tę samą barierę. | czynność | US10.2, BR-13, BR-16, BR-23, BR-24, NFR-18, NFR-54 | R1 |

| Stan | Co widać |
| --- | --- |
| Zwykły | dane, zdjęcie, podobne zgłoszenia, decyzja |
| Pusty — brak podobnych | „W promieniu 10 m nie ma podobnych zgłoszeń.” (US10.1) |
| Ładowanie | „Wczytujemy zgłoszenie…”, dopóki nie przyjdą dane; potem tekst od razu, zdjęcia z napisem „Wczytujemy zdjęcie…” |
| Błąd walidacji — przyjęcie | „Ustal typ bariery i uciążliwość, aby przyjąć zgłoszenie.” (US10.3) |
| Błąd walidacji — odrzucenie | „Podaj powód odrzucenia, aby autor wiedział, co poprawić.” (US10.2) |
| Nowa wersja | „Autor zmienił zgłoszenie. Sprawdź nową wersję przed decyzją.” Pod komunikatem „Zmienione:” i nazwy zmienionych pól, np. „Zmienione: uciążliwość” (US10.8, BR-3, BR-15, UC14 5e, PW-10) |
| Brak sieci | „Decyzja wymaga internetu, a zgłoszenie czeka w »Wziętych przeze mnie«. Połącz się z internetem i podejmij decyzję ponownie.” (BR-11, PW-10) |
| Zasłanianie — zapisywanie | „Zapisujemy zdjęcie…” zamiast „Zapisz zdjęcie” |
| Zasłanianie — brak sieci przy zapisie | „Zapisanie zdjęcia wymaga internetu. Połącz się z internetem i zapisz ponownie.” Zasłony zostają (PW-10) |
| Błąd serwera, brak uprawnień | stany wspólne |

Dostępność: podobne zgłoszenia mają odległość tekstem, np. „5 m”, a nie tylko punkt na mapie (NFR-19). Przy skali czcionki co najmniej 1,5 dwa zdjęcia „bariera usunięta” stoją jedno pod drugim (NFR-20).

## Koordynator

### K1 Postęp inwentaryzacji

| Pole | Treść |
| --- | --- |
| Cel | Koordynator wie, gdzie zespół musi jeszcze pójść. |
| Role | RO-KOO |
| Realizuje | US11.1, US11.2, US11.3, US11.4 |
| Czynność główna | „Otwórz” przy budynku — otwarcie jego pięter |
| Czynność główna przypięta | nie: czynność powtarza się w pozycjach listy |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Lista budynków w kolejności katalogu z danych PWr: symbol, a pod nim nazwa budynku | dane | US11.1; decyzje 16, 94 | — |
| Budynek szczegółowy: „4 z 5 pięter, 80%” | dane | US11.1, US11.2, BR-21 | — |
| Budynek podstawowy: „Obejrzany” tekstem i ikoną albo „Nieobejrzany” samym tekstem | dane | US11.1, PW-11 | — |
| Liczby: „Czeka na sprawdzenie: 3” i „Przyjęte: 4”, jak w scenariuszu `@US11.1` | dane | US11.1, BR-21 | — |
| Budynek szczegółowy: „Wejścia sprawdzone na miejscu: 1 z 6” | dane | US11.4, BR-24, PW-3 | — |
| „Otwórz” jako czynność karty ze strzałką (wymagania wspólne). Karta budynku nie jest osobnym celem dotyku | czynność główna | US11.3, NFR-21 | C5 |
| „Odśwież” i data z godziną odczytu, np. „Stan z 12.11.2026, 10:42”; bez samoczynnego odświeżania | czynność | US11.2 | — |

Sortowania ani filtrów nie ma, bo nie wynikają z żadnego kryterium (decyzja 16). Budynki szczegółowe nie idą więc na początek listy (decyzja 94). Wiersz o wejściach pozwala odczytać miarę M-1 z [wizji](../biznes/wizja.md) bez liczenia na kartach budynków (decyzja 38). Wejście liczy się dopiero po przyjęciu zmiany oznaczenia, bo wtedy dostaje źródło „obchód” (BR-24).

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
| Czynność główna przypięta | tak (reguła 10) |

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
| Sukces — rola już jest | „Ta osoba ma już rolę Weryfikatora. Nic nie zmieniliśmy.” (US12.2) |
| Błąd walidacji | „Adres e-mail jest niepełny, np. brakuje znaku @. Popraw go i wyślij ponownie.” albo „Wybierz rolę.” Zaproszenie się nie wysyła (US12.1, PW-10) |
| Brak sieci, błąd serwera, brak uprawnień | stany wspólne; błąd serwera obejmuje Usługę logowania |

### K3 Eksport

| Pole | Treść |
| --- | --- |
| Cel | Koordynator pobiera plik z barierami do QGIS. |
| Role | RO-KOO |
| Realizuje | US13.1, US13.2, US13.3 |
| Czynność główna | „Pobierz plik” |
| Czynność główna przypięta | tak (reguła 10) |

| Element | Rodzaj | Odwołanie | Dokąd |
| --- | --- | --- | --- |
| Wybór formatu: „GeoJSON (WGS84)” albo „CSV (PUWG 1992, EPSG:2180)” | czynność | US13.1, US13.2 | — |
| Napis „Plik zawiera tylko obecne, sprawdzone bariery, bez danych autorów.” | dane | US13.3, BR-22 | — |
| „Pobierz plik” | czynność główna | US13 | plik w pobranych plikach telefonu |
| Na Androidzie 8–9 „Pobierz plik” otwiera systemowe okno wyboru miejsca z podpowiedzianą nazwą pliku; zamknięcie okna bez zapisu wraca do K3 bez komunikatu | czynność | US13, NFR-41 | plik w wybranym miejscu |

| Stan | Co widać |
| --- | --- |
| Zwykły | wybór formatu i opis zawartości |
| Ładowanie | „Przygotowujemy plik…” |
| Pusty | „Nie ma jeszcze przyjętych barier. Pobrany plik będzie pusty.” Pobranie działa (UC17 2a) |
| Sukces | „Plik zapisany w pobranych plikach.” Na Androidzie 8–9: „Plik zapisany w wybranym miejscu.” (NFR-41) |
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
| Komunikat „brak wejścia dostępnego” (US3.1, UC4 4a, 4b) | Dwa różne napisy w wierszu o wejściach na V2: „Brak wejścia dostępnego” i „Brak danych o wejściach”. Treść w tabeli elementów V2. | „Brak danych” to co innego niż „niedostępne” (US17.1). Senior nie może zrezygnować z wizyty przez brak danych. |
| Brak wyniku wyszukiwania (UC1 3a) | Zdanie z przykładem symbolu i przycisk „Pokaż listę”. | Komunikat mówi, co zrobić, a lista działa też z TalkBack (NFR-19). |
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
| | US1.4 | V1, V2 | wejścia, ścieżki i legenda |
| | US1.5 | V1 | arkusz bariery |
| | US1.6 | V1 | przycisk terenu z nazwą |
| | US1.7 | V1 | panel „Teren na mapie”, brzeg terenu, komunikat TalkBack przy granicy |
| | US1.8 | V1 | wynik i lista z innego terenu, nagłówki terenów |
| Karta budynku | US2.1 | V2, V1 | oznaczenie tekstem i ikoną; V1: arkusz wejścia |
| | US2.2 | V2, V1 | zdjęcie i data weryfikacji; V1: bariery w arkuszu wejścia |
| | US2.3 | V1, V2 | chipy i kolejność grup |
| | US2.4 | V2 | stan „Pusty” |
| | US2.5 | V1, V2 | miejsce bariery |
| | US2.6 | V2 | wyjścia ewakuacyjne na żądanie |
| | US2.7 | V1, V2 | karta listy i stan „Bez danych o barierach” |
| | US2.8 | V1, V2 | karta nad mapą z dwiema wysokościami |
| | US2.9 | V2, V1 | „Pokaż budynek na mapie” na karcie otwartej od razu na cały ekran |
| Którędy wejść | US3.1 | V2 | wiersz o wejściach „Brak wejścia dostępnego” ze zdaniem |
| | US3.2 | V1, V2 | 1 dotknięcie: odpowiedź na niskiej karcie |
| | US3.3 | V2, V1 | „Pokaż wejście na mapie”; arkusz wejścia: dojście, drzwi, winda; inne wejścia na liście „Wejścia” |
| Zgłoszenie szybkie | US4.1 | V4 | 3 kroki, pozycja sama |
| | US4.2 | V4 | potwierdzenie bez sieci; wysyłka w tle bez ekranu |
| | US4.3 | V4 | 6 przycisków grup |
| | US4.4 | V4 | potwierdzenie |
| | US4.5 | V4 | pytanie o piętro |
| | US4.6 | V2, V1, V4 | „Zgłoś barierę przy wejściu” i nazwa wejścia w kroku 1; przyciski zgłoszenia w arkuszach toalety, schodów i windy |
| | US4.7 | V4 | „Wstecz” z potwierdzenia |
| | US4.8 | V4 | zdjęcie i grupa po powrocie, pytanie o rezygnację |
| | US4.9 | V4 | stan „Poza terenem PWr” |
| Pomoc | US5.1 | V5 | — |
| Pomiar pozycji | US6.1 | C1 | — |
| | US6.2 | C1 | podpowiedź budynku |
| | US6.3 | C1 | korekta na planie |
| | US6.4 | C1 | korekta na mapie |
| | US6.5 | C1 | mapa nieba |
| Formularz | US7.1 | C2 | walidacja |
| | US7.2 | C2 | pole piętra |
| | US7.3 | C2 | opis przy TB-29 |
| | US7.4 | C2, C1 | „Zmień miejsce” z polami, pytanie przed wyjściem bez zapisu |
| Moje zgłoszenia | US8.1 | C3 | status, powód odrzucenia, znak zmiany, kolejność, „Otwórz”, bez zgłoszeń szybkich |
| | US8.2 | C3 | statusy „wysłane” i „w weryfikacji”; moment zmiany ustala serwer |
| | US8.3 | C3, C2 | edycja |
| | US8.4 | V2, C7, C3 | start, formularz, status |
| Synchronizacja | US9.1 | C3, C4 | wskaźnik braku sieci na każdym ekranie |
| | US9.2 | C4 | samo uruchomienie bez ekranu |
| | US9.3 | C4 | wybór wersji i nazwy pól, którymi się różnią |
| Weryfikacja | US10.1 | R2 | podobne zgłoszenia |
| | US10.2 | R2 | powód odrzucenia |
| | US10.3 | R2 | typ i uciążliwość |
| | US10.4 | R1, R2, C3 | dane pozycji i kolejność; zniknięcie z kolejki ustala serwer |
| | US10.5 | R1 | filtr robi serwer |
| | US10.6 | R2, V1, V2 | skutek przyjęcia |
| | US10.7 | R1 | sekcja „Wzięte przeze mnie” |
| | US10.8 | R2 | zmiana od wysłania, para zdjęć, zmienione pola |
| Postęp | US11.1 | K1 | — |
| | US11.2 | K1 | data odczytu i „Odśwież”; przeliczenie robi serwer |
| | US11.3 | K1, C5 | cofnięcie z potwierdzeniem i podpisem autora znacznika |
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
| | US15.2 | C5 | „Cofnij znacznik” tylko przy własnym, z potwierdzeniem |
| | US15.3 | C5 | znacznik w telefonie |
| Plan piętra | US16.1 | V1, V2, C1 | V1: przełącznik, plan i wyróżniony budynek z planem pięter; V2: piętra tekstem i „Pokaż na planie” |
| | US16.2 | V1, V2, C2 | V1: blady budynek bez planu i zdanie nad mapą; V2: napis o braku planu; C2: pole liczby |
| | US16.3 | V2 | grupy „Winda i toaleta” i „Piętra” |
| Oznaczenie wejścia | US17.1 | V2, V1 | trzy oznaczenia; nadanie przy imporcie bez ekranu |
| | US17.2 | V2, C6, R2, C3 | start, zmiana, sprawdzenie, status |
| | US17.3 | V2, V1, C6, R2 | data i źródło |
| Wyszukanie sali | US18.1 | V1 | numer sali w polu wyszukiwania, wyniki sal, plan piętra, dotknięcie sali na planie |
| | US18.2 | V1 | karta sali, „Jak dojść”, kroki po kolei |
| | US18.3 | V1 | stany „Sala bez drogi” i „Pusty — brak sali” |

Wszystkie 75 kryteriów mają ekran. Kryteria w całości serwerowe nie występują: każde ma co najmniej widoczny skutek.

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
