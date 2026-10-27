# Słownik pojęć

Jedno pojęcie ma jedną nazwę. Aplikacja, kod i dokumenty używają tylko nazw z kolumny „Termin”.

## Pojęcia

| Termin | Definicja | Nie mylić z / synonimy zakazane |
| --- | --- | --- |
| administrator obiektu | Osoba wyznaczona z Działu Zarządzania Obiektami lub Działu Domów Studenckich PWr, która w procesie AS-IS wypełnia ankietę o budynku. | Nie mylić z: Koordynator, administrator usługi logowania. Zakazane: zarządca budynku, gospodarz budynku. |
| aktor | Rola (RO-*) albo system zewnętrzny, który w przypadku użycia wymienia dane z systemem; jedyny zewnętrzny aktor to Usługa logowania. | Nie mylić z: rola (aktorem bywa też system), persona. Zakazane: użytkownik (jako nazwa aktora). |
| bariera | Przeszkoda lub ograniczenie w budynku albo w jego otoczeniu, które utrudnia lub uniemożliwia osobie ze szczególnymi potrzebami dotarcie do miejsca lub skorzystanie z niego (por. ustawa z 19 lipca 2019 r., art. 2 pkt 1). | Nie mylić z: zgłoszenie. Zakazane: problem, usterka, obiekt. |
| bariera usunięta | Oznaczenie zmiany zgłoszenia przyjętego, którym Zbierający zgłasza, że bariery już nie ma; po przyjęciu bariera znika z mapy, a oba zgłoszenia są archiwalne (BR-19). | Nie mylić z: zgłoszenie odrzucone. Zakazane: bariera zlikwidowana, bariera nieaktualna. |
| brak danych | Oznaczenie wejścia, o którego dostępności import nie dał informacji ani z danych PWr, ani z OpenStreetMap; aplikacja pokazuje je tekstem i ikoną (US17.1, BR-24). | Nie mylić z: „niedostępne” (wejście, przez które nie da się wejść bez stopni). Zakazane: nieznane, brak informacji, nie wiadomo. |
| budynek podstawowy | Budynek PWr bez planów pięter w aplikacji, który ma kontur, wejścia i kartę budynku, a piętro zgłoszenia wpisuje się ręcznie. | Nie mylić z: budynek szczegółowy. Zakazane: budynek uproszczony, zwykły, niepełny. |
| budynek szczegółowy | Budynek z planami pięter z danych PWr, sprawdzany przez zespół na miejscu; na start A-1, B-1, C-13, D-1 i D-2. | Nie mylić z: budynek podstawowy. Zakazane: budynek detalowy, pełny, pilotażowy, „top-5”. |
| CSV | Tekstowy plik tabelaryczny, który otwiera arkusz kalkulacyjny i QGIS; w eksporcie zawiera współrzędne PUWG 1992. | Zakazane: Excel, arkusz (jako nazwa formatu). |
| Cyfrowy Przewodnik PWr | Serwis PWr z opisami 25 budynków (wejścia, windy, toalety, zdjęcia) i z Interaktywną Mapą Kampusu (położenie i plany pięter wybranych budynków). To źródło danych w procesie AS-IS, nie część aplikacji. | Nie mylić z: mapa kampusu (ekran aplikacji), deklaracja dostępności. Dopuszczalny skrót po rozwinięciu: Przewodnik. |
| data weryfikacji | Data przyjęcia zgłoszenia, widoczna przy barierze na karcie budynku (BR-17). | Nie mylić z: data aktualizacji deklaracji dostępności, data znacznika piętra obejrzanego. Zakazane: data publikacji, data zatwierdzenia. |
| deklaracja dostępności | Opis dostępności strony, aplikacji lub siedziby podmiotu publicznego (art. 10 ustawy o dostępności cyfrowej); na PWr to opis każdego budynku. | Nie mylić z: raport o stanie zapewniania dostępności (ustawa z 19 lipca 2019 r., art. 11), certyfikat dostępności, plan poprawy dostępności, audyt ekspercki. |
| dokładność pozycji | Szacowana w metrach odległość, w jakiej od wyznaczonego punktu leży rzeczywiste miejsce bariery; cel to poniżej 1 m. | Nie mylić z: pomiar precyzyjny. Zakazane: precyzja, błąd GPS, rozrzut, accuracy. |
| dostęp alternatywny | Inny sposób obsługi osoby ze szczególnymi potrzebami, gdy podmiot publiczny nie może zapewnić dostępności, np. wsparcie innej osoby (art. 7 ustawy z 19 lipca 2019 r.). | Nie mylić z: wejście dostępne. Zakazane: obejście, rozwiązanie zastępcze. |
| duplikat | Zgłoszenie, które według Weryfikatora opisuje tę samą barierę co wcześniejsze zgłoszenie. | Nie mylić z: podobne zgłoszenie, konflikt wersji. Zakazane: dubel, kopia, powtórzenie. |
| eksport | Pobranie przez Koordynatora zgłoszeń przyjętych jako pliku GeoJSON lub CSV do otwarcia w QGIS. | Nie mylić z: synchronizacja. Zakazane: raport, zrzut, kopia zapasowa, wydruk. |
| GeoJSON | Otwarty format pliku z danymi mapowymi, który QGIS otwiera bez konwersji; w eksporcie zawiera współrzędne WGS84. | Zakazane: JSON, plik mapy. |
| GNSS | Wspólna nazwa satelitarnych systemów ustalania położenia, takich jak GPS, Galileo i GLONASS. | Nie mylić z: GPS (jeden z systemów). Zakazane: GPS jako nazwa ogólna. |
| Gość niezalogowany | Rola RO-GOS: każdy, kto bez konta ogląda mapę kampusu, karty budynków i plany pięter. | Nie mylić z: Odwiedzający (ma konto i składa zgłoszenia). Dopuszczalny skrót po rozwinięciu: Gość. Zakazane: użytkownik anonimowy, niezalogowany użytkownik. |
| grupa barier | Jedna z sześciu grup klasyfikacji barier, np. „komunikacja pionowa”. | Nie mylić z: typ bariery. Zakazane: kategoria, dział. |
| historyjka | Krótki opis potrzeby użytkownika w formie „Jako…, chcę…, aby…” z identyfikatorem US1… | Nie mylić z: przypadek użycia, kryterium akceptacji. Zakazane: user story, wymaganie funkcjonalne (jako synonim). |
| historyjka umożliwiająca | Historyjka bez potrzeby N, z której wynikałaby wprost; udostępnia czynności innych historyjek i służy ich potrzebom, w projekcie US14 (logowanie: N2, N8). | Nie mylić z: historyjka. Zakazane: historyjka pomocnicza, historyjka techniczna, enabler. |
| inwentaryzacja | Systematyczny obchód budynków i terenu kampusu, który ma zebrać i zweryfikować zgłoszenia o wszystkich barierach. | Nie mylić z: audyt dostępności, deklaracja dostępności. Zakazane: spis, przegląd, monitoring. |
| karta budynku | Ekran z opisem jednego budynku: wejścia z oznaczeniem wejścia, windy, zgłoszenia przyjęte ze zdjęciami i data ostatniej weryfikacji. | Zakazane: profil budynku, szczegóły budynku, strona budynku. |
| kolejka weryfikacji | Zgłoszenia ze statusem „wysłane”, które czekają, aż weźmie je Weryfikator (ekran R1); własnych zgłoszeń Weryfikator w niej nie widzi. | Nie mylić z: zgłoszenia lokalne czekające w telefonie na synchronizację. Zakazane: skrzynka, inbox, lista do akceptacji. |
| konflikt wersji | Sytuacja, w której serwer ma inną wersję zgłoszenia niż zmiana wysyłana z telefonu, np. po edycji na dwóch telefonach autora; Zbierający wybiera, którą zachować (BR-10). | Nie mylić z: duplikat. Zakazane: kolizja, błąd zapisu, nadpisanie. |
| kontur budynku | Zarys budynku widziany z góry na mapie kampusu, pobrany z OpenStreetMap. | Zakazane: obrys, poligon, wielokąt, footprint. |
| Koordynator | Rola RO-KOO: osoba, która kieruje inwentaryzacją: śledzi postęp, wysyła zaproszenia i wykonuje eksport. | Nie mylić z: koordynator ds. dostępności (stanowisko na PWr). Zakazane: administrator, kierownik, admin. |
| koordynator ds. dostępności | Stanowisko na PWr: kierownik Działu Dostępności wyznaczony na podstawie art. 14 ustawy z 19 lipca 2019 r.; występuje w procesie AS-IS. | Nie mylić z: Koordynator (rola RO-KOO w aplikacji). Zakazane: pełnomocnik ds. dostępności. |
| korekta ręczna | Przesunięcie przez Zbierającego punktu bariery na planie piętra albo na mapie po uśrednianiu pozycji; zgłoszenie dostaje takie oznaczenie (US6.3, US6.4, NFR-3). | Nie mylić z: pomiar precyzyjny, wersja zgłoszenia. Zakazane: kalibracja, poprawka GPS. |
| kryterium akceptacji | Sprawdzalny warunek historyjki z identyfikatorem USx.y, np. US6.1; z każdego kryterium powstaje scenariusz. | Nie mylić z: reguła biznesowa (BR), wymaganie niefunkcjonalne (NFR), definition of done (warunki ukończenia każdego zadania). Zakazane: AC bez numeru historyjki, warunek akceptacji. |
| mapa historii | Tabela aktywności użytkownika w kolejności procesu, w której historyjki dzieli według priorytetu linia MVP. | Nie mylić z: mapa kampusu, proces TO-BE, backlog (lista zadań). Zakazane: story map. |
| mapa nieba | Widok położenia satelitów na niebie w czasie uśredniania pozycji, z systemem GNSS każdego satelity (US6.5). | Nie mylić z: mapa kampusu. Zakazane: skyplot, radar satelitów. |
| MVP | Minimalna wersja produktu: historyjki z priorytetem „Musi być”, które muszą działać na pokazie. | Nie mylić z: prototyp, tryb demonstracyjny. Zakazane: wersja podstawowa, wersja beta. |
| Odwiedzający | Rola RO-ODW: osoba z kontem, np. słuchacz Politechniki Trzeciego Wieku, która ogląda mapę i składa zgłoszenia szybkie. | Nie mylić z: Gość niezalogowany (bez konta), senior (persona, nie rola). Zakazane: gość (jako nazwa tej roli), visitor. |
| OpenStreetMap | Otwarta, tworzona społecznie mapa świata; źródło konturów budynków i wejść na licencji ODbL. | Dopuszczalny skrót po rozwinięciu: OSM. |
| osoba ze szczególnymi potrzebami | Osoba, która przez swoje cechy lub okoliczności potrzebuje dodatkowych działań lub środków, aby pokonać barierę i uczestniczyć w życiu na równi z innymi (art. 2 pkt 3 ustawy z 19 lipca 2019 r.). | Nie mylić z: osoba z niepełnosprawnością (pojęcie węższe). Zakazane: inwalida, niepełnosprawny. |
| oznaczenie wejścia | Jedna z trzech wartości przy wejściu: „dostępne”, „niedostępne” albo „brak danych”, z datą i źródłem oznaczenia. Datą jest dzień importu albo dzień przyjęcia zmiany oznaczenia wejścia; wejście z oznaczeniem „dostępne” to wejście dostępne (US2.1, US17.3, BR-24). | Nie mylić z: status zgłoszenia, znacznik, oznaczenie zgłoszenia („korekta ręczna”, „bariera usunięta”). Zakazane: status wejścia, stan wejścia. |
| piętro | Część budynku na jednej wysokości; w budynku szczegółowym ma etykietę z planów PWr, w podstawowym liczbę (0 to parter) i ewentualnie dopisek „półpiętro”. | Dopuszczalne w cytatach z przepisów: kondygnacja. Zakazane: poziom (w znaczeniu piętra), level. |
| piętro obejrzane | Piętro budynku szczegółowego, które Zbierający oznaczył jako przejrzane w całości; w budynku podstawowym znacznik „obejrzany” dotyczy całego budynku. | Nie mylić z: weryfikacja. Zakazane: piętro sprawdzone, piętro zweryfikowane, piętro zinwentaryzowane. |
| plan piętra | Rysunek jednego piętra budynku szczegółowego z pomieszczeniami, windami, schodami, toaletami i wejściami, pochodzący z danych PWr. | Nie mylić z: mapa kampusu. Zakazane: mapa piętra, rzut, schemat, mapa wewnętrzna. |
| podobne zgłoszenie | Zgłoszenie leżące w promieniu 10 m od sprawdzanego, pokazywane Weryfikatorowi jako możliwy duplikat. | Nie mylić z: duplikat (o duplikacie decyduje Weryfikator). Zakazane: zgłoszenie sąsiednie, kandydat na duplikat, podpowiedź duplikatów. |
| podpowiedź budynku | Propozycja najbliższego budynku po uśrednianiu pozycji, którą Zbierający potwierdza, zmienia albo odrzuca (US6.2, BR-G2). | Nie mylić z: podobne zgłoszenie. Zakazane: automatyczne przypisanie budynku, geokodowanie. |
| pomiar precyzyjny | Ustalenie miejsca bariery przez Zbierającego: uśrednianie pozycji, wybór budynku i w razie potrzeby ręczna korekta punktu na planie piętra albo na mapie. | Nie mylić z: zgłoszenie szybkie, dokładność pozycji. Zakazane: pomiar dokładny, namiar, lokalizacja GPS. |
| postęp inwentaryzacji | Dla każdego budynku odsetek pięter obejrzanych albo znacznik „obejrzany” (budynek podstawowy) oraz liczba zgłoszeń czekających na weryfikację i przyjętych. | Zakazane: statystyki, progres, stan prac, raport. |
| powód odrzucenia | Obowiązkowe krótkie uzasadnienie, które Weryfikator podaje przy odrzuceniu zgłoszenia; zmianę zgłoszenia już archiwalnego odrzuca z powodem „nieaktualne” (BR-23). | Zakazane: przyczyna odrzucenia, komentarz, uwaga. |
| poza budynkiem | Położenie bariery poza konturem każdego budynku PWr, np. krawężnik na chodniku; to stan normalny, nie błąd. | Zakazane: na zewnątrz, outdoor, błąd lokalizacji. |
| poziom szczegółowości | Jeden z trzech sposobów obsługi miejsca w aplikacji: budynek szczegółowy, budynek podstawowy albo poza budynkiem. | Nie mylić z: uciążliwość bariery. Zakazane: poziom detali, poziom opracowania, tier. |
| przełącznik pięter | Element karty budynku szczegółowego do wyboru piętra, którego plan piętra widać; budynek podstawowy go nie ma (BR-G1, US16.1). | Nie mylić z: wybór piętra w formularzu zgłoszenia. Zakazane: wybór poziomu, selektor pięter, lista kondygnacji. |
| przypadek użycia | Opis krok po kroku, jak aktor osiąga cel w systemie, z przepływem głównym i przepływami alternatywnymi; ma identyfikator UC1… | Nie mylić z: historyjka, scenariusz. Zakazane: use case, scenariusz użycia. |
| PUWG 1992 | Państwowy płaski układ współrzędnych w metrach dla map Polski (EPSG:2180, w przepisach PL-1992); w nim jest eksport CSV. | Nie mylić z: PUWG 2000 (EPSG:2176–2179). Zakazane: układ 92. |
| PWr | Politechnika Wrocławska. | Zakazane: PWR, Polibuda. |
| QGIS | Bezpłatny program do przeglądania i analizy danych mapowych, w którym Koordynator otwiera eksport. | Zakazane: GIS jako nazwa programu. |
| raport o stanie zapewniania dostępności | Ustawowy raport podmiotu publicznego, składany co 4 lata do 31 marca i publikowany w BIP (art. 11 ustawy z 19 lipca 2019 r.). | Nie mylić z: deklaracja dostępności, eksport, audyt ekspercki. Zakazane: raport dostępności (bez dopowiedzenia). |
| rola | Zestaw czynności dostępnych dla osoby w aplikacji: RO-GOS, RO-ODW, RO-ZBI, RO-WER albo RO-KOO. | Nie mylić z: uprawnienie (jedna czynność z macierzy uprawnień). Zakazane: typ konta, grupa, poziom dostępu. |
| skarga na brak dostępności | Środek, z którego osoba korzysta, gdy podmiot publiczny nie zrealizował jej wniosku o zapewnienie dostępności (art. 32 ustawy z 19 lipca 2019 r.). | Nie mylić z: zgłoszenie, wniosek o zapewnienie dostępności. Zakazane: reklamacja, zażalenie. |
| status zgłoszenia | Etap obsługi zgłoszenia: lokalne (tylko w telefonie), wysłane (na serwerze w kolejce), w weryfikacji (wzięte przez jednego Weryfikatora), przyjęte, odrzucone albo archiwalne. Archiwalne to zgłoszenie zastąpione przyjętą zmianą albo zgłoszeniem „bariera usunięta” oraz samo zgłoszenie „bariera usunięta” albo zmiana oznaczenia wejścia po przyjęciu przez Weryfikatora. | Zakazane: stan, faza, oczekujące, nowe, zatwierdzone. |
| symbol budynku | Oznaczenie budynku PWr z litery i numeru, zapisywane z dywizem, np. C-13. | Zakazane: numer budynku, kod, ref, sygnatura. |
| synchronizacja | Wymiana danych między telefonem a serwerem: wysłanie zgłoszeń zapisanych w telefonie i pobranie zmian z serwera. | Nie mylić z: eksport. Zakazane: upload, odświeżanie, sync. |
| tryb offline | Praca aplikacji bez internetu, w której zgłoszenia zapisują się w telefonie i czekają na synchronizację. | Dopuszczalne jako warunek testu: tryb samolotowy. Zakazane: tryb lokalny, tryb bez sieci. |
| typ bariery | Pozycja z klasyfikacji barier (TB-1…TB-29) wybierana w zgłoszeniu. | Nie mylić z: grupa barier. Zakazane: kategoria, rodzaj, klasa. |
| uciążliwość bariery | Stopień w skali 1–3, jak bardzo bariera utrudnia skorzystanie z miejsca, gdzie 3 oznacza blokadę. | Nie mylić z: poziom szczegółowości. Zakazane: ocena, waga, priorytet, ważność, severity. |
| Usługa logowania | System zewnętrzny, który przechowuje hasła i adresy e-mail, prowadzi logowanie i rejestrację oraz zakłada konta z zaproszenia; jedyny zewnętrzny aktor w przypadkach użycia. | Nie mylić z: serwer aplikacji. Zakazane: dostawca tożsamości, serwer logowania, nazwa produktu zamiast nazwy roli w systemie. |
| uśrednianie pozycji | Zbieranie przez 30 sekund odczytów położenia z GNSS i wyznaczenie z nich jednego, dokładniejszego punktu. | Nie mylić z: dokładność pozycji. Zakazane: kalibracja, uśrednianie GPS, fix. |
| wejście dostępne | Wejście, przez które osoba na wózku dostanie się do budynku bez pokonywania stopni: z poziomu terenu, pochylnią lub podnośnikiem. | Nie mylić z: wejście główne. Zakazane: wejście dla niepełnosprawnych, wejście przystosowane, wejście bez barier. |
| wejście główne | Podstawowe wejście do budynku, którym wchodzi większość osób; nie musi być wejściem dostępnym. | Nie mylić z: wejście dostępne. Zakazane: wejście frontowe, wejście reprezentacyjne. |
| wersja zgłoszenia | Treść zgłoszenia po zapisanej edycji przed decyzją Weryfikatora; Weryfikator ocenia aktualną wersję, a konflikt wersji porównuje dwie wersje (BR-3, BR-10). | Nie mylić z: zmiana zgłoszenia przyjętego (to nowe zgłoszenie, BR-18). Zakazane: rewizja, wersja robocza, draft. |
| weryfikacja | Sprawdzenie zgłoszenia przez Weryfikatora zakończone przyjęciem albo odrzuceniem z powodem odrzucenia. | Nie mylić z: pomiar precyzyjny. Zakazane: moderacja, akceptacja, recenzja, zatwierdzanie. |
| Weryfikator | Rola RO-WER: pracownik, który przyjmuje albo odrzuca zgłoszenia, wskazuje duplikaty i ustala typ oraz uciążliwość zgłoszeń szybkich. | Zakazane: moderator, recenzent, sprawdzający, kontroler. |
| WGS84 | Światowy układ współrzędnych geograficznych w stopniach, używany przez GNSS i mapy internetowe (EPSG:4326); w nim jest eksport GeoJSON. | Nie mylić z: PUWG 1992. Zakazane: współrzędne GPS, układ GPS. |
| wniosek o zapewnienie dostępności | Żądanie osoby ze szczególnymi potrzebami, by podmiot publiczny zapewnił dostępność; termin 14 dni, wyjątkowo do 2 miesięcy (art. 30–31 ustawy z 19 lipca 2019 r.). | Nie mylić z: zgłoszenie (zgłoszenie w aplikacji nie jest wnioskiem z ustawy), skarga na brak dostępności. Zakazane: wniosek (w znaczeniu zgłoszenia w aplikacji). |
| zaproszenie | Wpisanie przez Koordynatora adresu e-mail i roli, po którym ta osoba dostaje konto z tą rolą. | Nie mylić z: rejestracja (Odwiedzający zakłada konto sam). Zakazane: dodanie użytkownika, invite. |
| Zbierający | Rola RO-ZBI: student lub pracownik, który na miejscu wykonuje pomiar precyzyjny i składa zgłoszenia, także w trybie offline. | Dopuszczalna forma żeńska w opisie demo: Zbierająca. Zakazane: zbieracz, inwentaryzator, ankieter, kolektor. |
| zgłoszenie | Informacja w systemie o jednej barierze: typ, uciążliwość, miejsce, piętro, zdjęcie i opis; zgłoszeniem jest też zmiana oznaczenia wejścia (BR-24). | Nie mylić z: bariera. Zakazane: raport, obiekt, wpis, rekord, punkt, ticket. |
| zgłoszenie archiwalne | Zgłoszenie przyjęte, które zastąpiła przyjęta zmiana, oraz zgłoszenie „bariera usunięta” po jego przyjęciu przez Weryfikatora razem z poprzednim, a także przyjęta zmiana oznaczenia wejścia. Nie widać go na mapie, karcie budynku ani w eksporcie, zostaje w historii (BR-18, BR-19, BR-22, BR-24). | Nie mylić z: zgłoszenie odrzucone (usuwane po 90 dniach). Zakazane: zgłoszenie usunięte, nieaktywne, historyczne. |
| zgłoszenie przyjęte | Zgłoszenie po pozytywnej weryfikacji; tylko takie zgłoszenia widzi Odwiedzający na mapie. | Zakazane: zatwierdzone, potwierdzone, zaakceptowane, opublikowane. |
| zgłoszenie szybkie | Zgłoszenie osoby zalogowanej (np. Odwiedzającego) w najwyżej 3 krokach: zdjęcie, bieżąca pozycja i grupa barier. Budynek przypisuje system z konturu, w którym leży pozycja, piętro zostaje puste, a typ i uciążliwość ustala Weryfikator (BR-14). | Nie mylić z: pomiar precyzyjny. Zakazane: szybki raport, zgłoszenie uproszczone, sygnał. |
| zmiana oznaczenia wejścia | Zgłoszenie Zbierającego z oznaczeniem „dostępne” albo „niedostępne” dla jednego wejścia i nowym zdjęciem tego wejścia. Przechodzi weryfikację jak każde zgłoszenie; po przyjęciu jest archiwalne, a wejście ma nowe oznaczenie (BR-24). | Nie mylić z: zmiana zgłoszenia przyjętego, „bariera usunięta”. Zakazane: edycja wejścia, aktualizacja statusu wejścia. |
| zmiana zgłoszenia przyjętego | Nowe zgłoszenie powiązane z przyjętym, które po przyjęciu je zastępuje (BR-18). Składa ją autor, a przy zgłoszeniu szybkim i „bariera usunięta” dowolny Zbierający; autorem nowego zgłoszenia jest składający zmianę. | Nie mylić z: wersja zgłoszenia (edycja przed decyzją Weryfikatora). Zakazane: korekta zgłoszenia, aktualizacja zgłoszenia. |
| znacznik | Zapis, że piętro budynku szczegółowego jest piętrem obejrzanym albo że budynek podstawowy jest „obejrzany”; stawia go Zbierający, a cofa on sam albo Koordynator (BR-20). | Nie mylić z: oznaczenie zgłoszenia, np. „korekta ręczna”; oznaczenie wejścia; znacznik dźwiękowy (TB-22). Dopuszczalne dla czynności: oznaczenie piętra obejrzanego. Zakazane: oznaczenie (w znaczeniu zapisu), flaga, ptaszek, checkbox. |
| zwolnienie zgłoszenia | Oddanie wziętego zgłoszenia do kolejki weryfikacji bez decyzji; poza MVP, zgłoszenie w weryfikacji obsługuje do decyzji ten sam Weryfikator (BR-11, R-18). | Nie mylić z: odrzucenie zgłoszenia. Zakazane: oddanie zgłoszenia, cofnięcie do kolejki. |
| źródło oznaczenia | Pochodzenie oznaczenia wejścia widoczne przy wejściu razem z datą: „import” (dane PWr albo OpenStreetMap przy imporcie budynków) albo „obchód” (przyjęta zmiana oznaczenia wejścia) (US17.3, BR-24). | Nie mylić z: źródło danych planu piętra (NFR-40). Zakazane: pochodzenie danych, autor oznaczenia. |

Terminy są w mianowniku liczby pojedynczej. Nazwy ról piszemy wielką literą. Nowe pojęcie trafia do tabeli przed pierwszym użyciem w innym dokumencie.

## Klasyfikacja barier

| ID | Grupa | Typ bariery | Kiedy wybrać | Podstawa |
| --- | --- | --- | --- | --- |
| TB-1 | wejście i drzwi | schody przy wejściu | do wejścia prowadzą tylko stopnie, bez pochylni i podnośnika | Standard PWr 4.1, 4.3; deklaracja A-1 |
| TB-2 | wejście i drzwi | wąskie drzwi | drzwi po otwarciu węższe niż 90 cm (w dwuskrzydłowych: skrzydło czynne) | Standard PWr 4.4, 5.3 |
| TB-3 | wejście i drzwi | ciężkie drzwi | drzwi trudno otworzyć jedną ręką: duża siła, brak automatu, gałka zamiast klamki | Standard PWr 4.4, 5.3 |
| TB-4 | wejście i drzwi | brak miejsca przed drzwiami | przed drzwiami lub w przedsionku nie da się obrócić wózka (około 150 × 150 cm) | Standard PWr 4.4, 5.3 |
| TB-5 | wejście i drzwi | urządzenie poza zasięgiem | domofon, czytnik kart lub przycisk otwierania wyżej niż 110 cm | Standard PWr 5.4 |
| TB-6 | komunikacja pionowa | schody bez poręczy | brak poręczy po obu stronach albo poręcz nie wychodzi poza pierwszy i ostatni stopień | Standard PWr 4.3, 8 |
| TB-7 | komunikacja pionowa | stopnie na piętrze | różnica wysokości wewnątrz piętra bez pochylni lub podnośnika, np. półpiętro | Standard PWr 5.2; deklaracja A-1 |
| TB-8 | komunikacja pionowa | brak windy | budynek nie ma windy albo winda nie dociera na dane piętro | Standard PWr 9; ustawa z 19 lipca 2019 r., art. 6 pkt 1 lit. b |
| TB-9 | komunikacja pionowa | niesprawna winda | winda jest, ale nie działa albo jest wyłączona | Standard PWr 9.1 |
| TB-10 | komunikacja pionowa | winda nieprzystosowana | kabina za mała dla wózka, przyciski wyżej niż 110 cm lub bez alfabetu Braille'a, drzwi zamykają się za szybko | Standard PWr 9.1, 9.2 |
| TB-11 | komunikacja pionowa | pochylnia nieprzystosowana | pochylnia zbyt stroma (na zewnątrz ponad 6%), śliska albo bez oznaczenia początku i końca | Standard PWr 1, 5.2 |
| TB-12 | komunikacja pionowa | podnośnik wymagający pomocy | podnośnik lub platforma schodowa nie działa, wymaga klucza, karty albo obsługi przez pracownika | Standard PWr 4.2, 9.4 |
| TB-13 | komunikacja pozioma | próg | próg, listwa lub wycieraczka wystaje ponad 2 cm | Standard PWr 4.4, 5.3 |
| TB-14 | komunikacja pozioma | wąskie przejście | przejście węższe niż 90 cm albo zastawione meblami lub sprzętem | Standard PWr 3, 5.2, 5.6 |
| TB-15 | komunikacja pozioma | przeszkoda w przejściu | element wystaje ze ściany ponad 10 cm albo przestrzeń pod schodami jest niezabezpieczona | Standard PWr 5.2, 8 |
| TB-16 | komunikacja pozioma | nawierzchnia | nierówna kostka, szczeliny ponad 5 mm, duży spadek albo śliska posadzka | Standard PWr 1, 5.1 |
| TB-17 | komunikacja pozioma | krawężnik | wysoki krawężnik bez obniżenia na trasie pieszej, np. od miejsca parkingowego do chodnika | Standard PWr 1, 2 |
| TB-18 | toalety | brak toalety dostępnej | na piętrze nie ma toalety dla osób z niepełnosprawnościami | Standard PWr 6 |
| TB-19 | toalety | toaleta nieprzystosowana | za mało miejsca na wózek, brak uchwytów przy misce, umywalka lub przyciski poza zasięgiem | Standard PWr 6 |
| TB-20 | toalety | toaleta niedostępna w praktyce | toaleta dostępna zamknięta na klucz, zastawiona albo z barierą na dojściu (hipoteza) | Standard PWr 6 |
| TB-21 | orientacja i informacja | oznakowanie | brak lub nieczytelny symbol budynku, numery pięter, kierunki albo numery sal | Standard PWr 4.1, 5.1 |
| TB-22 | orientacja i informacja | brak planu tyflograficznego | brak informacji o rozkładzie pomieszczeń w formie dotykowej lub głosowej (tyflomapa, znacznik dźwiękowy) | ustawa z 19 lipca 2019 r., art. 6 pkt 1 lit. c; Standard PWr 4.5 |
| TB-23 | orientacja i informacja | brak kontrastu | krawędzie stopni, szklane drzwi i ściany albo drzwi nie odróżniają się od tła | Standard PWr 4.3, 4.4, 5.1, 8 |
| TB-24 | orientacja i informacja | brak oznaczeń dotykowych | brak pól ostrzegawczych przed schodami albo ścieżki dotykowej w dużym holu | Standard PWr 4.3, 4.5, 8 |
| TB-25 | orientacja i informacja | brak wsparcia słuchu | sala lub punkt obsługi bez pętli indukcyjnej albo systemu FM | Standard PWr 5.6; deklaracja C-13 |
| TB-26 | inne | brak miejsca parkingowego | brak miejsca dla osób z niepełnosprawnościami blisko wejścia dostępnego | Standard PWr 2 |
| TB-27 | inne | brak środków ewakuacji | brak krzesła ewakuacyjnego, miejsca oczekiwania albo informacji, gdzie są | ustawa z 19 lipca 2019 r., art. 6 pkt 1 lit. e; Standard PWr 7 |
| TB-28 | inne | niedostępne miejsce obsługi | brak obniżonej części lady albo miejsca dla wózka w sali | Standard PWr 4.5, 5.6 |
| TB-29 | inne | inna bariera | żaden typ nie pasuje; opis jest wtedy obowiązkowy | — |

Wartości liczbowe pochodzą ze Standardu PWr dla budynków nowych i remontowanych. W starszych budynkach służą jako punkt odniesienia, nie wyrok. W zgłoszeniu szybkim Odwiedzający wybiera tylko grupę barier (6 dużych przycisków). Typ bariery i uciążliwość ustala wtedy Weryfikator, a przy pomiarze precyzyjnym wybiera je Zbierający.

## Skala uciążliwości

| Stopień | Nazwa | Kryterium | Przykład |
| --- | --- | --- | --- |
| 1 | utrudnienie | Osoba, dla której bariera jest najtrudniejsza, pokona ją samodzielnie, ale z dodatkowym wysiłkiem lub ryzykiem. | Brak kontrastu na krawędzi stopni (TB-23), brak numerów sal (TB-21). |
| 2 | poważne utrudnienie | Ta osoba pokona barierę tylko z pomocą: innej osoby, która jej nie przenosi, albo urządzenia obsługiwanego przez pracownika. | Próg 4 cm (TB-13), platforma schodowa na klucz (TB-12). |
| 3 | blokada | Ta osoba nie pokona bariery wcale albo tylko wtedy, gdy ktoś ją przeniesie. | Schody przy wejściu bez pochylni (TB-1), niesprawna jedyna winda (TB-9). |

Uciążliwość opisuje barierę w jej miejscu. Inne wejście do budynku nie obniża uciążliwości, tylko pojawia się na karcie budynku jako wejście dostępne. Kryteria są hipotezą do potwierdzenia w wywiadach.

## Źródła

- Ustawa z 19 lipca 2019 r. o zapewnianiu dostępności osobom ze szczególnymi potrzebami: <https://api.sejm.gov.pl/eli/acts/DU/2019/1696/text.html>
- Ustawa z 4 kwietnia 2019 r. o dostępności cyfrowej stron internetowych i aplikacji mobilnych podmiotów publicznych, art. 10: <https://api.sejm.gov.pl/eli/acts/DU/2019/848/text.html>
- Plan działania na rzecz poprawy dostępności PWr na lata 2025–2028 (słownik, tabela 1; administrator obiektu, koordynator ds. dostępności): <https://bip.pwr.edu.pl/d/gGBUKOTtQKxVvBFh6GDxYBxZIQX14QU8OS39DQQpVVm8QRQxpXhsJFXkSGQcBBxNZBUREenlUWx8/plan_poprawy_dostepnosci_2025-28.pdf>
- Cyfrowy Przewodnik PWr, lista 25 budynków: <https://przewodnik.pwr.edu.pl/pl/budynki>
- Rozporządzenie Rady Ministrów z 15 października 2012 r. w sprawie państwowego systemu odniesień przestrzennych: <https://api.sejm.gov.pl/eli/acts/DU/2012/1247/text.html>
- Standard dostępności architektoniczno-urbanistycznej Politechniki Wrocławskiej (2023): <https://dostepnosc.pwr.edu.pl/baza-wiedzy/standardy-dostepnosci-pwr/standard-dostepnosci-architektonicznej-i-urbanistycznej>
- Opis standardu na stronie Działu Dostępności: <https://dzd.pwr.edu.pl/en/accessibility/architectural-accessibility/architectural-urban-accessibility-standard>
- Deklaracje dostępności budynków PWr: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow>
- Deklaracja dostępności budynku A-1: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow/budynki-a/a1>
- Deklaracja dostępności budynku C-13: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow/budynki-c/c13>
- „Historyczna inauguracja. Ruszyła Politechnika Trzeciego Wieku”, PWr: <https://pwr.edu.pl/uczelnia/aktualnosci/seniorzy-na-pwr-ruszyla-politechnika-trzeciego-wieku-13912.html>
- EPSG:2180: <https://epsg.io/2180>
- EPSG:4326: <https://epsg.io/4326>
