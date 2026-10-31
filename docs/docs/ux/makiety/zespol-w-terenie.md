# Makiety: zespół w terenie

Makiety ekranów C1–C7 dla roli Zbierającego (RO-ZBI), a na C5 także dla Koordynatora (RO-KOO). Elementy, czynności i stany pochodzą z [wymagań do ekranów](../wymagania-ekranow.md#zbierajacy), a napisy ze [słownika użytkownika](../slownik-uzytkownika.md). Konwencje i fragmenty wspólne opisuje strona [Makiety](index.md).

Zbierający pracuje w ruchu i często w słońcu. Dlatego każdy ekran ma jedną czynność główną na pełną szerokość, a wybór wartości to duże ramki zamiast małych pól. Przykłady dotyczą budynku A-1: wejście główne ma schody [1]. Nieznane dane są zastępnikami w nawiasach.

Blok zdjęcia, błąd przy polu, opcja wyboru, wybór piętra, mapa i ramka pozycji na pełną szerokość pochodzą z `_wspolne.iuml`. W pliku `_zespol-w-terenie.iuml` został tylko punkt bariery na środku mapy albo planu. Ma te same przyciski mapy i „ⓘ” co V1 i R1 (decyzja 93).

## C1 Pomiar pozycji

Nagłówek ekranu to „Zmierz i zgłoś barierę”: ten sam napis co na przycisku, który go otwiera (decyzja 93).

### Pomiar trwa

![Makieta C1 w trakcie pomiaru: pozostały czas, dokładność, liczba satelitów według systemów, mapa nieba ze znakami koła, kwadratu i trójkąta z podpisami oraz przycisk Powtórz pomiar](../../_generated/m-c1-pomiar-pozycji.svg)

Czynność główna: brak w trakcie pomiaru; „Dalej” pojawia się po końcu pomiaru, razem z podpowiedzią budynku. Przerwanie i ponowny start to jedno dotknięcie „Powtórz pomiar” (UC8 2a, scenariusz przerwania `@US6.1`), a wyjście bez zapisu — „Wstecz”.

| Element | AC/BR/NFR |
| --- | --- |
| „Pomiar: zostało 12 s” | US6.1 |
| „Dokładność: 0,8 m (szacunek)” | US6.1, NFR-2 |
| „Satelity: GPS 7, Galileo 5, GLONASS…” tekstem, osobno dla każdego systemu | US6.1, NFR-2, NFR-18 |
| „Mapa nieba”: GPS to koło, Galileo kwadrat, GLONASS trójkąt, każdy znak z podpisem systemu | US6.5, NFR-21 |
| „Powtórz pomiar”: przerywa uśrednianie i zaczyna pełne 30 s od nowa, bez limitu | US6.1, UC8 2a, NFR-23 |

### Po pomiarze: podpowiedź budynku

![Makieta C1 po pomiarze: dokładność, satelity, pytanie Czy to budynek A-1 z przyciskami Potwierdź, Wybierz inny i Poza budynkiem, podpis Przesuń mapę, aby punkt stał na miejscu bariery, mapa z punktem na środku, przyciski Powiększ i Pomniejsz, strzałki w krzyżu, przycisk informacji przy podpisie OpenStreetMap i Dalej](../../_generated/m-c1-pomiar-pozycji-podpowiedz.svg)

Czynność główna: „Dalej” — zatwierdzenie miejsca i przejście do C2.

| Element | AC/BR/NFR |
| --- | --- |
| „Czy to budynek A-1?” i „Potwierdź”, „Wybierz inny”, „Poza budynkiem” | US6.2, BR-G2 |
| „Przesuń mapę, aby punkt stał na miejscu bariery.” i punkt bariery na środku mapy | US6.4, NFR-3, decyzja 93 |
| „Powiększ”, „Pomniejsz” i strzałki w krzyżu zamiast przeciągania; te same na V1 i R1 | US6.4, NFR-3, NFR-15, NFR-53 |
| „ⓘ” przy „© OpenStreetMap contributors”: źródła i pełna licencja jak na V1 | NFR-37, NFR-40 |
| „Dalej” pogrubione, na pełną szerokość | US6, NFR-21 |

### Wybór innego budynku

![Makieta C1 po dotknięciu Wybierz inny: sekcja Wybierz budynek z trzema najbliższymi budynkami jako przyciskami i przyciskiem Poza budynkiem, pod nią mapa z punktem na środku, przyciski mapy i Dalej](../../_generated/m-c1-pomiar-pozycji-wybor-budynku.svg)

Czynność główna: wybór budynku z listy. Ten sam ekran C1, bez nowego identyfikatora.

| Element | AC/BR/NFR |
| --- | --- |
| „Wybierz budynek” w miejscu pytania „Czy to budynek A-1?” | US6.2, BR-G2 |
| „Najbliżej punktu, od najbliższego:” i budynki jako przyciski na pełną szerokość, podpowiadany budynek pierwszy | US6.2, NFR-17 |
| „Poza budynkiem” na końcu listy | BR-G2, BR-G4 |
| Lista z konturów zapisanych w telefonie, więc działa bez sieci | NFR-4 |
| Wybór zamyka listę i pokazuje „Budynek A-2” z „Wybierz inny”, jak po potwierdzeniu | US6.2 |

### Korekta w budynku szczegółowym

![Makieta C1 po potwierdzeniu budynku A-1: napis Korekta ręczna, przełącznik z wybranym Plan piętra obok Mapa, wybór piętra, podpis Przesuń mapę, plan piętra z punktem na środku, przyciski mapy, przycisk informacji przy źródle PWr i Dalej](../../_generated/m-c1-pomiar-pozycji-korekta.svg)

Czynność główna: „Dalej”. Plan i mapa dzielą jedną powierzchnię, więc ekran ma jeden zestaw przycisków mapy.

| Element | AC/BR/NFR |
| --- | --- |
| „Budynek A-1” po potwierdzeniu i „Wybierz inny” | US6.2 |
| „Korekta ręczna” po przesunięciu punktu | US6.3, US6.4 |
| Przełącznik „Plan piętra” i „Mapa”, jak „Mapa / Lista” na R1; w budynku szczegółowym wybrany jest „Plan piętra”, co widać po kształcie pola | US6.3, US6.4, NFR-21 |
| „Piętro:” i 5 etykiet z danych PWr, plan wybranego piętra | US6.3, US16.1 |
| „Źródło: Politechnika Wrocławska” z „ⓘ” pod planem | NFR-40 |
| Punkt na środku planu; plan przesuwają przyciski mapy, jedyne na ekranie | US6.3, NFR-3, NFR-15, decyzja 93 |

| Wariant bez osobnej makiety | Co widać |
| --- | --- |
| Wybrana „Mapa” | w miejscu wyboru piętra, planu i podpisu PWr mapa z tym samym punktem i podpisem „© OpenStreetMap contributors” w ramce (US6.4, NFR-37). Ten blok rysuje makieta „Po pomiarze: podpowiedź budynku”. Punkt zostaje na środku, a przyciski mapy są te same. Wybrane piętro zostaje po powrocie do planu. |

### Szukanie satelitów

![Makieta C1 w stanie ładowania: komunikat Szukamy satelitów i przycisk Powtórz pomiar](../../_generated/m-c1-pomiar-pozycji-szukanie.svg)

Czynność główna: „Powtórz pomiar”.

| Element | AC/BR/NFR |
| --- | --- |
| „Szukamy satelitów. Na otwartej przestrzeni pomiar będzie dokładniejszy.” | NFR-1, NFR-50, PW-9 |
| „Powtórz pomiar” | UC8 2a, NFR-23 |

### Daleko od budynków, bez internetu

![Makieta C1 poza budynkiem i bez internetu: wskaźnik Bez internetu z datą synchronizacji, dokładność, satelity, napis Poza budynkiem, mapa z konturami bez mapy ulic i punktem na środku, przyciski mapy i Dalej](../../_generated/m-c1-pomiar-pozycji-poza-budynkiem.svg)

Czynność główna: „Dalej”.

| Element | AC/BR/NFR |
| --- | --- |
| „Bez internetu ·” z ikoną i „Ostatnia synchronizacja: …” | US9.1, NFR-10, NFR-21, decyzja 54 |
| „Poza budynkiem” bez pytania o budynek i bez komunikatu o błędzie | BR-G4, NFR-49 |
| Mapa bez mapy ulic: kontury i punkt z danych aplikacji. Podpis OpenStreetMap zostaje, jak na V1 bez internetu. Kontury pochodzą też z OpenStreetMap, a NFR-37 wymaga podpisu na każdym ekranie z mapą. | NFR-4, NFR-37, NFR-39 |
| Brak planu piętra; korekta tylko na mapie | US6.4, BR-G4 |

Budynek podstawowy wygląda tak samo, ale zamiast „Poza budynkiem” ma pytanie „Czy to budynek …?” z trzema przyciskami (US6.4, BR-G1). Po potwierdzeniu w miejscu wyboru piętra i planu stoi ten sam napis o braku planu co na V2 (BR-G1).

Zwykły stan bez internetu różni się od makiety po podpowiedzi tylko wskaźnikiem i mapą bez ulic.

### Brak zgody na lokalizację

![Makieta C1 bez zgody na lokalizację: komunikat o potrzebie lokalizacji i przycisk Pozwól na lokalizację, bez przycisku powrotu do mapy](../../_generated/m-c1-pomiar-pozycji-brak-zgody.svg)

Czynność główna: przycisk w komunikacie. Rezygnacja przez „Wstecz” do L2, C2 albo C3, bez „Wróć do mapy” (NFR-15).

| Stan | Komunikat i przycisk | AC/BR/NFR |
| --- | --- | --- |
| Brak zgody (makieta) | „Aby zapisać miejsce bariery, potrzebujemy lokalizacji. Sprawdzimy ją tylko teraz i nie będziemy Cię śledzić.” „Pozwól na lokalizację” | NFR-27 |
| Odmowa zgody | „Bez lokalizacji nie zapiszesz miejsca bariery. Możesz ją włączyć w ustawieniach telefonu.” „Otwórz ustawienia” | NFR-27 |
| Zgoda tylko przybliżona | „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. Pozwól na dokładną lokalizację.” „Pozwól na dokładną lokalizację” | NFR-27 |
| Lokalizacja wyłączona | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby zapisać miejsce bariery.” „Otwórz ustawienia” | NFR-27 |

Komunikaty powtarzają V4 z końcówką „aby zapisać miejsce bariery” i stoją w katalogu komunikatów.

## C2 Formularz zgłoszenia

Nagłówek ekranu: „Opisz barierę”, a przy poprawianiu „Popraw zgłoszenie”: to napis przycisku na C3 i V2 (decyzja 93).

### Nowe zgłoszenie w budynku szczegółowym

![Makieta C2 nowego zgłoszenia: budynek A-1 i miejsce z pomiaru z przyciskiem Zmień miejsce, wybór piętra, lista typów bariery, trzy ramki uciążliwości z ikoną i opisem, komunikat Ważne o twarzach i Zrób zdjęcie, pole opisu i Zapisz](../../_generated/m-c2-formularz-zgloszenia.svg)

Czynność główna: „Zapisz”. Formularz nie ma paska dolnego.

| Element | AC/BR/NFR |
| --- | --- |
| „Budynek A-1”, miejsce z C1 i „Zmień miejsce” | US6.2, US7.4 |
| „Piętro”: 5 etykiet z danych PWr, bez wpisywania ręcznie | US7.2, BR-7 |
| „Typ bariery”: lista z typami w 6 grupach barier | US7.1, BR-7 |
| „Uciążliwość” i pod nagłówkiem „Oceń dla osoby, dla której bariera jest najtrudniejsza.” | US7.1, słownik pojęć (skala uciążliwości) |
| „Utrudnienie”, „Tylko z pomocą”, „Blokada”, każda w ramce z ikoną i opisem kryterium skali | US7.1, NFR-17, NFR-21, PW-13 |
| Komunikat „Ważne” z „(!)”: „Nie fotografuj twarzy ani tablic rejestracyjnych.” i „Zdjęcie zobaczą inni użytkownicy.”, jak na V4; pod nim „Zrób zdjęcie” | US7.1, NFR-25, PW-8, PW-13, decyzja 54 |
| „Opis” z dopiskiem o typie „inna bariera” | US7.3 |
| „Zapisz” | BR-7 |

### Błąd walidacji

![Makieta C2 z błędem: komunikat Brakuje typu bariery nad formularzem i ten sam pogrubiony tekst przy polu Typ bariery; piętro, uciążliwość Blokada i zdjęcie już wybrane](../../_generated/m-c2-formularz-zgloszenia-blad.svg)

Czynność główna: „Zapisz”. Zapis nie wykona się, dopóki brakuje wymaganego pola.

| Element | AC/BR/NFR |
| --- | --- |
| Nad formularzem: „Brakuje typu bariery. Uzupełnij to pole, aby zapisać.” | US7.1, NFR-15, PW-10 |
| Ten sam tekst pogrubiony przy polu; fokus TalkBack na tym polu | NFR-15 (WCAG 3.3.1), NFR-18 |
| Po zdjęciu: zastępnik zdjęcia i „Zrób nowe zdjęcie” | US7.1, PW-8 |

Ten sam wzór dotyczy piętra, uciążliwości, zdjęcia i opisu przy typie „inna bariera” (BR-7, US7.3).

Gdy piętro wybrano już na C1 przy korekcie na planie, C2 ma je zaznaczone. Zbierający nie wybiera piętra drugi raz (US6.3, US7.2).

### Edycja w budynku podstawowym

![Makieta C2 w edycji z nagłówkiem Popraw zgłoszenie: budynek jako zastępnik, piętro jako liczba z polem Półpiętro, typ inna bariera, uciążliwość Tylko z pomocą, zrobione zdjęcie i wpisany opis](../../_generated/m-c2-formularz-zgloszenia-edycja.svg)

Czynność główna: „Zapisz”.

| Element | AC/BR/NFR |
| --- | --- |
| Pola z zapisanymi wartościami | US8.3 |
| „Nie mamy planu pięter tego budynku. Wpisz numer piętra, 0 to parter.”, pod nim piętro jako liczba całkowita i pole „Półpiętro” | US7.2, BR-G1, PW-6 |
| Typ „inna bariera” z opisem ze scenariusza `@US7.3` | US7.3 |

| Stan bez makiety | Co się zmienia | AC/BR/NFR |
| --- | --- | --- |
| Poza budynkiem | brak sekcji „Piętro” | BR-G4 |
| Zmiana zgłoszenia przyjętego | nad „Zapisz”: „Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.” | BR-18 |
| Brak internetu | wskaźnik pod nagłówkiem; zapis działa, a na C3 status „W telefonie” | US9.1, PW-12 |
| Błąd zapisu | nad formularzem: „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” i „Spróbuj ponownie” | NFR-7 |

## C3 Moje zgłoszenia

### Lista zgłoszeń

![Makieta C3: przyciski Czeka na wysłanie 2 i Zmierz i zgłoś barierę, potem sześć zgłoszeń, każde ze statusem tekstem i ikoną na początku: W telefonie Bariery już nie ma, Odrzucone z powodem, W weryfikacji ze znakiem Zmiana czeka na wysłanie, Wysłane z przyciskiem Dwie wersje, w tych czterech przycisk Otwórz ze strzałką, Przyjęte schody przy wejściu A-1, Archiwalne zmiana oznaczenia wejścia](../../_generated/m-c3-moje-zgloszenia.svg)

Czynność główna: „Otwórz” przy zgłoszeniu do poprawy. Przycisk powtarza się w pozycjach, więc nie jest pogrubiony. Tak samo jest z „Otwórz” na R1.

| Element | AC/BR/NFR |
| --- | --- |
| „Czeka na wysłanie: 2”, prowadzi do C4 | US9.1 |
| „Zmierz i zgłoś barierę”, prowadzi do C1 | US6, PW-12 |
| Status jako pierwsza linia pozycji, słowem i ikoną; sześć statusów | US8.1, US8.2, NFR-18, NFR-21 |
| Typ, budynek, piętro, data | US8.1 |
| „Powód odrzucenia:” z powodem ze scenariusza `@BR-16` | US8.1, BR-16 |
| „Otwórz” ze strzałką przy zgłoszeniu przed decyzją i odrzuconym; prowadzi do C2 | US8.1, US8.3, BR-3, decyzja 93 |
| Przyjęte i archiwalne bez „Otwórz” | US8.1 |
| „Zmiana czeka na wysłanie” przy niewysłanej edycji | US8.1, BR-9 |
| „Dwie wersje: wybierz jedną”, prowadzi do C4 | US9.3 |
| Rodzaj „Bariery już nie ma”, tu „W telefonie”; po przyjęciu byłby „Archiwalne”, nigdy „Przyjęte” | US8.4, BR-19, PW-12 |
| Rodzaj „Zmiana oznaczenia wejścia”: wejście główne A-1 „Niedostępne”, przyjęta zmiana staje się archiwalna | US17.2, BR-24 |

### Odrzucone jako „nieaktualne”

![Makieta C3 z dwoma odrzuconymi zgłoszeniami: próg ze zdaniem o zmianie przez inną osobę i przyciskiem Wyślij ponownie oraz niesprawna winda z komunikatem, że bariery nie ma już na mapie, i przyciskiem Zmierz i zgłoś barierę](../../_generated/m-c3-moje-zgloszenia-nieaktualne.svg)

Czynność główna: „Wyślij ponownie”.

| Element | AC/BR/NFR |
| --- | --- |
| „Ktoś już zmienił to zgłoszenie. Twoja zmiana trafi do jego aktualnej wersji.”; przy zmianie oznaczenia wejścia: „Oznaczenie tego wejścia już się zmieniło. Sprawdź je i wyślij swoją zmianę ponownie.” | BR-23, BR-24, PW-14 |
| „Wyślij ponownie”; prowadzi do C2, przy zmianie oznaczenia wejścia do C6, a przy „bariera usunięta” do C7, zawsze z aktualną wersją zgłoszenia | BR-23, BR-24, PW-14 |
| „Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa.” i „Zmierz i zgłoś barierę” | BR-23, PW-12 |

### Pusta lista

![Makieta C3 pusta: zdanie Nie masz jeszcze zgłoszeń i przycisk Zmierz i zgłoś barierę](../../_generated/m-c3-moje-zgloszenia-pusty.svg)

Czynność główna: „Zmierz i zgłoś barierę”.

| Stan bez makiety | Co się zmienia | AC/BR/NFR |
| --- | --- | --- |
| Ładowanie | lista z telefonu zostaje, nad nią „Sprawdzamy statusy…” | PW-9 |
| Brak internetu | wskaźnik z „Ostatnia synchronizacja: …”; statusy z dnia synchronizacji | US9.1, NFR-10 |
| Błąd | nad listą: „Nie udało się sprawdzić statusów, ale zgłoszenia są bezpieczne w telefonie. Spróbuj ponownie za chwilę.” i „Spróbuj ponownie” | NFR-5, NFR-50, PW-10 |

## C4 Synchronizacja

### Kolejka w telefonie

![Makieta C4: Czeka na wysłanie 3, w tym 2 zgłoszenia i 1 znacznik piętra, data ostatniej synchronizacji i przycisk Synchronizuj teraz](../../_generated/m-c4-synchronizacja.svg)

Czynność główna: „Synchronizuj teraz”; synchronizacja i tak rusza sama.

| Element | AC/BR/NFR |
| --- | --- |
| „Czeka na wysłanie: 3” i podział na zgłoszenia i znaczniki | US9.1, BR-9 |
| „Ostatnia synchronizacja: …” | NFR-10 |
| „Synchronizuj teraz” | UC10, US9.2 |

| Stan bez makiety | Co się zmienia | AC/BR/NFR |
| --- | --- | --- |
| Pusty | „Wszystko wysłane. Ostatnia synchronizacja: …” | US9.1 |
| Wysyłanie | „Wysyłamy 2 z 5…” | PW-9 |
| Brak internetu | „Brak internetu. Wyślemy wszystko sami, gdy internet wróci.” | US9.1, US9.2 |
| Błąd serwera | „Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później.” | NFR-50 |
| Mało miejsca | nad liczbą: „W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia.” | NFR-7, PW-10 |

### Konflikt wersji

![Makieta C4 z konfliktem: komunikat Dwie wersje, wybierz jedną, napis Różni się: uciążliwość, ramka Moja wersja z pogrubioną uciążliwością Tylko z pomocą i przyciskiem Zostaw moją wersję, ramka Wersja z serwera z pogrubioną uciążliwością Blokada i przyciskiem Weź wersję z serwera, na dole Zdecyduję później](../../_generated/m-c4-synchronizacja-konflikt.svg)

Czynność główna: wybór wersji — „Zostaw moją wersję” albo „Weź wersję z serwera”, oba przyciski tej samej wagi.

| Element | AC/BR/NFR |
| --- | --- |
| „Dwie wersje: wybierz jedną. Druga wersja zostanie usunięta.” | US9.3, BR-10, PW-12 |
| Obie wersje jedna pod drugą; uciążliwość 2 i 3 jak w scenariuszu `@US9.3` | US9.3, NFR-20, NFR-22 |
| „Różni się: uciążliwość” pod komunikatem, nad obiema wersjami; w obu wersjach pole „Uciążliwość:” pogrubione. Przy innym polu napis podaje jego nazwę, np. „Różni się: [pole]”, a przy kilku polach „Różnią się: [pole], [pole]” | US9.3, NFR-21 |
| Przycisk wyboru pod każdą wersją | US9.3, BR-10 |
| „Zdecyduję później”: zmiana czeka w telefonie | BR-10 |

Przy zgłoszeniu przyjętym komunikat ma drugie zdanie z katalogu komunikatów (UC11 3b). W orientacji poziomej wersje stoją obok siebie (NFR-22).

## C5 Obejrzane piętra

### Zbierający, budynek szczegółowy

![Makieta C5 dla budynku A-1: pięć pięter; piętro 1 Obejrzane z Twój znacznik i Cofnij znacznik, piętro 2 Obejrzane ze Znacznik innej osoby, piętro 3 Obejrzane z Czeka na wysłanie, piętra 4 i 5 Nieobejrzane z przyciskiem Oznacz jako obejrzane](../../_generated/m-c5-obejrzane-pietra.svg)

Czynność główna: „Oznacz jako obejrzane” przy piętrze bez znacznika. Przycisk powtarza się w wierszach, więc nie jest pogrubiony; wyróżnia go położenie w wierszu, jak „Weź do sprawdzenia” na R1 (NFR-21).

| Element | AC/BR/NFR |
| --- | --- |
| Symbol budynku pod nagłówkiem | US15.1 |
| Lista pięter z etykietami z danych PWr | US15.1, US16.1 |
| „Obejrzane” z ikoną albo „Nieobejrzane” samym tekstem | US15.1, PW-11 |
| „Twój znacznik” albo „Znacznik innej osoby”; u Koordynatora „Znacznik: Zbierający, Autor [nr]” | US15.2, BR-5 |
| „Czeka na wysłanie” przy znaczniku w telefonie | US15.3 |
| „Cofnij znacznik” tylko przy własnym znaczniku | US15.2, PW-12 |

### Zbierający, potwierdzenie cofnięcia

![Makieta C5 dla Zbierającego: przy piętrze 1 z Twój znacznik pytanie Cofnąć znacznik piętra 1, Postęp budynku się zmniejszy, przyciski Cofnij znacznik i Zostaw znacznik; piętro 2 ze Znacznik innej osoby bez przycisku, piętro 3 z Oznacz jako obejrzane](../../_generated/m-c5-obejrzane-pietra-cofniecie.svg)

Czynność główna: „Cofnij znacznik”, po pytaniu o skutek (decyzja 93).

| Element | AC/BR/NFR |
| --- | --- |
| „Cofnąć znacznik piętra [1]? Postęp budynku się zmniejszy.” pod własnym znacznikiem | US15.2, BR-20, PW-12 |
| „Cofnij znacznik” pogrubione i „Zostaw znacznik” | US15.2, PW-12 |
| Przy znaczniku innej osoby brak „Cofnij znacznik” | US15.2 |

### Budynek podstawowy, bez internetu

![Makieta C5 dla budynku podstawowego bez internetu: wskaźnik Bez internetu, komunikat Znacznik zapisany w telefonie, wiersz Cały budynek Obejrzany z Twój znacznik, Czeka na wysłanie i Cofnij znacznik](../../_generated/m-c5-obejrzane-pietra-podstawowy-brak-sieci.svg)

Czynność główna: „Oznacz jako obejrzane”, a po oznaczeniu „Cofnij znacznik” jako jedyny przycisk.

| Element | AC/BR/NFR |
| --- | --- |
| Jeden wiersz „Cały budynek” | US15.1, BR-20 |
| „Obejrzany”: forma dla budynku taka sama jak w K1 | US15.1, słownik użytkownika |
| „Znacznik zapisany w telefonie. Wyślemy go, gdy wróci internet.” | US15.3, BR-9 |
| Cofnięcie bez internetu działa tak samo | US15.3, BR-20 |

### Koordynator, potwierdzenie cofnięcia

![Makieta C5 dla Koordynatora: przy znacznikach Znacznik: Zbierający, Autor nr; przy piętrze 2 pytanie Cofnąć znacznik piętra 2 i przyciski Cofnij znacznik oraz Zostaw znacznik; bez przycisków Oznacz jako obejrzane](../../_generated/m-c5-obejrzane-pietra-koordynator.svg)

Czynność główna: „Cofnij znacznik” przy każdym znaczniku, z potwierdzeniem.

| Element | AC/BR/NFR |
| --- | --- |
| „Cofnij znacznik” przy każdym znaczniku | US11.3, BR-20 |
| „Znacznik: Zbierający, Autor [nr]”: rola i pseudonim autora jak w R2 | US15.2, BR-5 |
| „Cofnąć znacznik piętra [2]? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający.” | BR-20, PW-12 |
| „Cofnij znacznik” i „Zostaw znacznik” | BR-20, PW-12 |
| Brak „Oznacz jako obejrzane” | BR-20 |

Pytanie stoi przy wierszu, którego dotyczy, więc osoba widzi piętro i skutek naraz. Błąd zapisu ma nad listą komunikat „Nie udało się zapisać znacznika w telefonie. Spróbuj ponownie, a jeśli to nie pomoże, zwolnij miejsce.” i „Spróbuj ponownie” (NFR-7).

## C6 Zmień oznaczenie wejścia

### Przed wyborem

![Makieta C6 dla wejścia głównego A-1: obecne oznaczenie Niedostępne z datą i źródłem, dwie duże ramki Dostępne i Niedostępne, komunikat Ważne o twarzach, Zrób zdjęcie, informacja o sprawdzeniu przez inną osobę i Wyślij do sprawdzenia](../../_generated/m-c6-zmien-oznaczenie-wejscia.svg)

Czynność główna: „Wyślij do sprawdzenia”.

| Element | AC/BR/NFR |
| --- | --- |
| Budynek A-1 i „Wejście główne” | US17.2 |
| Obecne oznaczenie „Niedostępne” z ikoną, datą i źródłem jak na V2 [1] | US17.3, NFR-21, PW-3 |
| Dwie duże ramki „Dostępne” i „Niedostępne”, bez „Brak danych” | US17.2, BR-24, NFR-17 |
| Komunikat „Ważne” o twarzach i „Zrób zdjęcie”; zdjęcie obowiązkowe | US17.2, BR-24, NFR-25, PW-8, decyzja 54 |
| „Zmianę sprawdzi inna osoba. Do tego czasu karta budynku pokazuje obecne oznaczenie.” | BR-4, BR-24 |

### Potwierdzenie obecnego oznaczenia

![Makieta C6 po wyborze Niedostępne, czyli obecnej wartości: pod wyborem zdanie Potwierdzasz obecne oznaczenie, zmieni się data i źródło, nowe zdjęcie wejścia i Zrób nowe zdjęcie](../../_generated/m-c6-zmien-oznaczenie-wejscia-obecna.svg)

Czynność główna: „Wyślij do sprawdzenia”.

| Element | AC/BR/NFR |
| --- | --- |
| Wybrana ramka: znak wyboru i pogrubienie, nie kolor | NFR-21 |
| „Potwierdzasz obecne oznaczenie. Zmieni się data i źródło.” | BR-24, UC18 2a |
| Nowe zdjęcie i „Zrób nowe zdjęcie” | US17.2, PW-8 |

| Stan bez makiety | Co się zmienia | AC/BR/NFR |
| --- | --- | --- |
| Błąd walidacji | nad formularzem i przy polu: „Dodaj zdjęcie wejścia, aby wysłać zmianę.” albo „Wybierz: dostępne czy niedostępne.” | BR-24 |
| Brak internetu | po wysłaniu na C3: „Zmiana zapisana w telefonie. Wyślemy ją, gdy wróci internet.” i status „W telefonie” | US9.1 |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” | NFR-7 |

## C7 Zgłoś, że bariery nie ma

![Makieta C7: przyjęte zgłoszenie niesprawna winda z budynkiem, piętrem, miejscem, zdjęciem i datą sprawdzenia, bez pól do edycji; sekcja Nowe zdjęcie miejsca z komunikatem Ważne i Zrób zdjęcie; informacja, że bariera zostanie na mapie do sprawdzenia, i Wyślij do sprawdzenia](../../_generated/m-c7-bariery-nie-ma.svg)

Czynność główna: „Wyślij do sprawdzenia”.

| Element | AC/BR/NFR |
| --- | --- |
| Zgłoszenie przyjęte: typ TB-9 ze scenariusza `@US8.4`, piętro, miejsce, zdjęcie, „Sprawdzone …”; bez edycji | BR-19 |
| „Nowe zdjęcie miejsca”, komunikat „Ważne” o twarzach i „Zrób zdjęcie”; zdjęcie obowiązkowe | US8.4, BR-19, NFR-25, PW-8, decyzja 54 |
| „Bariera zostanie na mapie, dopóki Weryfikator nie sprawdzi zgłoszenia.” | US8.4 |
| Brak typu i uciążliwości do wyboru | BR-19, BR-7 |

| Stan bez makiety | Co się zmienia | AC/BR/NFR |
| --- | --- | --- |
| Błąd walidacji | nad formularzem i przy zdjęciu: „Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.” | BR-19 |
| Brak internetu | po wysłaniu na C3: „Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet.” i status „W telefonie” | US8.4, US9.1 |
| Błąd zapisu | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” | NFR-7 |

## Źródła

1. Deklaracja dostępności budynku A-1 (wejście główne ze schodami): <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow/budynki-a/a1>
2. PlantUML Salt: <https://plantuml.com/salt>

Dostęp do źródeł: 28.10.2026.
