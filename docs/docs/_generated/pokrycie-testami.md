# Pokrycie scenariuszy testami

Jeden wiersz to jeden scenariusz z katalogu `specs/`. Kolumna „Test” wskazuje testy Androida ze znacznikiem
`// Scenariusz:` (moduł, klasa i funkcja testu). Stan „pełny” znaczy, że test sprawdza wszystkie kroki „Wtedy”,
„częściowo” — tylko część z nich, a „brak” — że scenariusz nie ma testu.

Stronę tworzy skrypt, nie edytuj jej ręcznie. Odśwież ją razem z macierzą śledzenia:
`python scripts/check_ids.py --matrix docs/wymagania/sledzenie.md`.

| Stan | Liczba |
| --- | --- |
| Pełny test | 0 |
| Tylko częściowo | 9 |
| Bez testu | 142 |

| Plik | Scenariusz | Kryteria i reguły | Test | Stan |
| --- | --- | --- | --- | --- |
| `budynki-bez-szczegolow.feature` | Karta budynku podstawowego zamiast planu piętra pokazuje napis | BR-G1, US16.2 | — | brak |
| `budynki-bez-szczegolow.feature` | Piętro w budynku podstawowym wpisuje się jako liczbę całkowitą | BR-G1, US7.2 | — | brak |
| `budynki-bez-szczegolow.feature` | Korekta punktu na mapie w każdym budynku i poza budynkiem | BR-G1, BR-G2, US6.4 | — | brak |
| `budynki-bez-szczegolow.feature` | W budynku podstawowym zgłoszenie można złożyć zawsze | BR-G1 | — | brak |
| `budynki-bez-szczegolow.feature` | Poza konturem aplikacja pokazuje stan „Poza budynkiem” i proponuje najbliższy budynek | BR-G2, US6.2 | — | brak |
| `budynki-bez-szczegolow.feature` | Propozycję najbliższego budynku można wybrać albo odrzucić | BR-G2, US6.2 | — | brak |
| `budynki-bez-szczegolow.feature` | Zgłoszenie szybkie poza konturem budynku zapisuje się bez błędu | BR-G2 | — | brak |
| `budynki-bez-szczegolow.feature` | Budynek z uszkodzonymi danymi planów działa jako budynek podstawowy | BR-G3, US16.2 | — | brak |
| `budynki-bez-szczegolow.feature` | Zgłoszenie w budynku po nieudanym imporcie planów | BR-G3 | — | brak |
| `budynki-bez-szczegolow.feature` | Pomiar daleko od budynków PWr nie dostaje propozycji budynku | BR-G4, US6.2 | — | brak |
| `budynki-bez-szczegolow.feature` | Mapa działa daleko od budynków PWr | BR-G4 | — | brak |
| `eksport.feature` | Plik GeoJSON otwiera się w QGIS bez ustawiania układu współrzędnych | US13.1 | — | brak |
| `eksport.feature` | Plik CSV ma współrzędne w PUWG 1992 i otwiera się jako warstwa punktów | US13.2 | — | brak |
| `eksport.feature` | Eksport zawiera tylko bieżące zgłoszenia przyjęte | BR-6, BR-22, US13.3 | — | brak |
| `eksport.feature` | Bariera usunięta nie trafia do eksportu | BR-19, BR-22, US13.3 | — | brak |
| `eksport.feature` | Eksport nie zawiera danych autora | BR-6, US13.3 | — | brak |
| `oznaczenie-wejscia.feature` | Bez roli Zbierającego zmiany oznaczenia wejścia nie da się złożyć | BR-24, US14.1 | — | brak |
| `oznaczenie-wejscia.feature` | Import nadaje wejściu oznaczenie z danych PWr | BR-24, US2.1, US17.1, US17.3 | — | brak |
| `oznaczenie-wejscia.feature` | Import bez danych daje oznaczenie „brak danych” | BR-24, US2.1, US17.1, US17.3 | — | brak |
| `oznaczenie-wejscia.feature` | Wspólna data i źródło stoją raz nad listą wejść | BR-24, US17.3 | — | brak |
| `oznaczenie-wejscia.feature` | Różne źródła stoją przy każdym wejściu | BR-24, US17.3 | — | brak |
| `oznaczenie-wejscia.feature` | Zmiana oznaczenia wejścia przechodzi te same statusy co zgłoszenie | BR-9, BR-24, US17.2 | — | brak |
| `oznaczenie-wejscia.feature` | Zmiany oznaczenia wejścia nie da się zapisać bez zdjęcia | BR-7, BR-24, US17.2 | — | brak |
| `oznaczenie-wejscia.feature` | Przyjęta zmiana zmienia oznaczenie, datę i źródło na karcie budynku | BR-24, US17.2, US17.3 | — | brak |
| `oznaczenie-wejscia.feature` | Zmiana może potwierdzić dotychczasowe oznaczenie | BR-24, US17.2, US17.3 | — | brak |
| `oznaczenie-wejscia.feature` | Odrzucona zmiana nie zmienia oznaczenia wejścia | BR-16, BR-24, US17.2 | — | brak |
| `oznaczenie-wejscia.feature` | Weryfikator nie sprawdza zmiany oznaczenia, którą sam złożył | BR-4, BR-24, US10.5, US17.2 | — | brak |
| `oznaczenie-wejscia.feature` | Po przyjęciu jednej zmiany wejścia drugą czekającą odrzuca się jako „nieaktualne” | BR-23, BR-24, US17.2 | — | brak |
| `pomiar-precyzyjny.feature` | Uśrednianie pozycji trwa 30 s i pokazuje dane o satelitach | US6.1 | — | brak |
| `pomiar-precyzyjny.feature` | Uśrednianie można przerwać i zacząć od nowa | US6.1 | — | brak |
| `pomiar-precyzyjny.feature` | Zbierający decyduje o podpowiedzianym budynku | US6.2 | — | brak |
| `pomiar-precyzyjny.feature` | Podpowiedź budynku działa bez sieci | US6.2 | — | brak |
| `pomiar-precyzyjny.feature` | Korekta punktu na planie piętra w budynku szczegółowym | US6.3 | — | brak |
| `pomiar-precyzyjny.feature` | Mapa nieba rozróżnia systemy GNSS nie tylko kolorem | US6.5 | — | brak |
| `pomiar-precyzyjny.feature` | Piętro w budynku szczegółowym wybiera się z etykiet z danych PWr | BR-7, US7.2 | — | brak |
| `pomiar-precyzyjny.feature` | Bez typu bariery, uciążliwości albo zdjęcia zgłoszenia nie da się zapisać | BR-7, US7.1 | — | brak |
| `pomiar-precyzyjny.feature` | W budynku trzeba podać piętro | BR-7 | — | brak |
| `pomiar-precyzyjny.feature` | Kompletne zgłoszenie zapisuje się ze statusem „lokalne” | BR-7, US7.1 | — | brak |
| `pomiar-precyzyjny.feature` | Typ TB-29 „inna bariera” wymaga opisu | BR-7, US7.3 | — | brak |
| `pomiar-precyzyjny.feature` | Typ TB-29 „inna bariera” z opisem zapisuje się | BR-7, US7.3 | — | brak |
| `postep-inwentaryzacji.feature` | Oznaczenie piętra albo budynku jako obejrzanego | BR-20, US15.1 | — | brak |
| `postep-inwentaryzacji.feature` | Zbierający cofa własny znacznik | BR-20, US15.2 | — | brak |
| `postep-inwentaryzacji.feature` | Inny Zbierający nie cofnie cudzego znacznika | BR-20, US15.2 | — | brak |
| `postep-inwentaryzacji.feature` | Koordynator cofa znacznik dowolnego Zbierającego | BR-20, US11.3 | — | brak |
| `postep-inwentaryzacji.feature` | Znacznik i jego cofnięcie bez sieci czekają w telefonie | BR-9, BR-20, US15.3 | — | brak |
| `postep-inwentaryzacji.feature` | Po powrocie sieci znacznik i jego cofnięcie wysyłają się same | BR-9, BR-20, US15.3 | — | brak |
| `postep-inwentaryzacji.feature` | Postęp budynku szczegółowego i podstawowego z liczbami zgłoszeń | BR-21, US11.1 | — | brak |
| `postep-inwentaryzacji.feature` | Postęp pokazuje nową wartość po dotarciu znacznika na serwer | BR-21, US11.2 | — | brak |
| `postep-inwentaryzacji.feature` | Do postępu nie wchodzą zgłoszenia nieznane serwerowi, archiwalne ani poza budynkiem | BR-21 | — | brak |
| `postep-inwentaryzacji.feature` | Liczba wejść ze źródłem „obchód” w budynku szczegółowym | BR-24, US11.4 | — | brak |
| `postep-inwentaryzacji.feature` | Przyjęta zmiana oznaczenia wejścia zwiększa liczbę wejść sprawdzonych na miejscu | BR-24, US11.4 | — | brak |
| `przegladanie-mapy.feature` | Wyszukanie budynku po symbolu w różnym zapisie | US1.1 | `core/geo · SearchBuildingsTest.us1_1_spelling_finds_c13` | częściowo |
| `przegladanie-mapy.feature` | Mapa i wyszukiwanie działają bez logowania | US1.2 | `core/geo · SearchBuildingsTest.us1_2_real_file_finds_a1` | częściowo |
| `przegladanie-mapy.feature` | Na mapie są tylko zgłoszenia przyjęte | BR-6, US1.3 | — | brak |
| `przegladanie-mapy.feature` | Wejście ma oznaczenie tekstem i ikoną, nie tylko kolorem | US2.1 | — | brak |
| `przegladanie-mapy.feature` | Wejście zalecane stoi pierwsze na karcie budynku | BR-25, US2.1 | — | brak |
| `przegladanie-mapy.feature` | Przy kilku wejściach dostępnych zalecane jest wejście główne | BR-25 | — | brak |
| `przegladanie-mapy.feature` | Przy kilku wejściach dostępnych bez głównego zalecane jest pierwsze z danych | BR-25 | — | brak |
| `przegladanie-mapy.feature` | Budynek bez wejścia dostępnego nie ma wejścia zalecanego | BR-25, US3.1 | — | brak |
| `przegladanie-mapy.feature` | Zgłoszenie przyjęte na karcie budynku ma zdjęcie i datę weryfikacji | BR-17, US2.2 | — | brak |
| `przegladanie-mapy.feature` | Wejście dostępne najwyżej 3 dotknięcia od mapy | US3.2 | `app · AppNavigatorTest.us3_2_v1_v2_v3_back_v2` | częściowo |
| `przegladanie-mapy.feature` | Brak wejścia dostępnego jest powiedziany wprost | US3.1 | — | brak |
| `przegladanie-mapy.feature` | Mapa pokazuje wejścia i ścieżki, a legenda objaśnia znaki słowem | US1.4 | — | brak |
| `przegladanie-mapy.feature` | Dotknięcie znaku bariery pokazuje jej opis z miejscem | BR-14, BR-17, US1.5, US2.5 | — | brak |
| `przegladanie-mapy.feature` | Karta budynku zaczyna się od skrótu faktów, potem wejścia i bariery | US2.3 | — | brak |
| `przegladanie-mapy.feature` | Skrót nie pokazuje faktu bez potwierdzenia w danych | US2.3 | — | brak |
| `przegladanie-mapy.feature` | Karta budynku bez zgłoszeń przyjętych nie sugeruje braku barier | BR-6, US2.4 | — | brak |
| `przegladanie-mapy.feature` | Wyjścia ewakuacyjne rozwijają się na żądanie | US2.6 | — | brak |
| `przegladanie-mapy.feature` | Opis wejścia zalecanego podaje dojście, drzwi i najbliższą windę | BR-25, US3.3 | — | brak |
| `przegladanie-mapy.feature` | Karta budynku szczegółowego opisuje piętra tekstem | US16.3 | — | brak |
| `przegladanie-mapy.feature` | Instrukcja przy pierwszym uruchomieniu | US5.1 | `app · AppNavigatorTest.us5_1_first_launch_opens_v5`<br>`app · AppShellTest.us5_1_first_launch_v5_back_v1`<br>`core/data · FirstLaunchRepositoryTest.us5_1_help_until_marked_seen` | częściowo |
| `przegladanie-mapy.feature` | Instrukcję można otworzyć ponownie z pomocy | US5.1 | `app · AppNavigatorTest.us5_1_bar_help_v5_back_returns`<br>`app · AppNavigatorTest.us5_1_first_launch_back_v1`<br>`app · AppNavigatorTest.us5_1_next_launch_opens_v1`<br>`core/data · FirstLaunchRepositoryTest.us5_1_second_launch_no_help` | częściowo |
| `przegladanie-mapy.feature` | Plan piętra w budynku szczegółowym | US16.1 | — | brak |
| `scenariusz-demo.feature` | Schody przy wejściu głównym A-1 od pomiaru do eksportu | US2.1, US6.1, US6.2, US7.1, US9.2, US10.1, US11.1, US13.1 | — | brak |
| `synchronizacja.feature` | Bez sieci widać wskaźnik, a zgłoszenie czeka ze statusem „lokalne” | BR-9, US9.1 | — | brak |
| `synchronizacja.feature` | Zgłoszenia zapisane bez sieci przetrwają restart telefonu | BR-9, US4.2, US9.1 | — | brak |
| `synchronizacja.feature` | Po powrocie sieci synchronizacja rusza sama | BR-9, US9.2 | — | brak |
| `synchronizacja.feature` | Przerwana synchronizacja nie gubi zgłoszenia i nie tworzy drugiego | BR-9, US9.2 | — | brak |
| `synchronizacja.feature` | Przy konflikcie wersji aplikacja pokazuje obie wersje | BR-10, US9.3 | — | brak |
| `synchronizacja.feature` | Przy konflikcie wersji Zbierający wybiera wersję | BR-10, US9.3 | — | brak |
| `synchronizacja.feature` | Odłożony wybór wersji zostawia zmianę w telefonie | BR-10, US9.3 | — | brak |
| `szukanie-sali.feature` | Wyszukanie sali po budynku i numerze w różnym zapisie | US18.1 | — | brak |
| `szukanie-sali.feature` | Kroki do sali podane tekstem bez rysowania trasy | BR-25, US18.2 | — | brak |
| `szukanie-sali.feature` | Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem | US18.3 | — | brak |
| `uprawnienia.feature` | Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze | US14.1 | — | brak |
| `uprawnienia.feature` | Znacznik piętra i zmianę „bariera usunięta” składa tylko Zbierający | BR-19, BR-20, US14.1 | — | brak |
| `uprawnienia.feature` | Zbierający nie edytuje cudzego zgłoszenia | BR-3, US14.1 | — | brak |
| `uprawnienia.feature` | Samodzielna rejestracja daje tylko uprawnienia Odwiedzającego | BR-1, BR-2, US14.3 | — | brak |
| `uprawnienia.feature` | Osoba z zaproszenia loguje się z wybraną rolą | BR-2, US12.1 | — | brak |
| `uprawnienia.feature` | Zaproszenie na adres istniejącego konta dodaje rolę | BR-1, BR-2, US12.2 | — | brak |
| `uprawnienia.feature` | Rolę pracowniczą nadaje tylko Koordynator | BR-2 | — | brak |
| `uprawnienia.feature` | Osoba z rolami Zbierającego i Weryfikatora nie przyjmie własnego zgłoszenia | BR-1, BR-4 | — | brak |
| `uprawnienia.feature` | Zalogowany Odwiedzający nie jest proszony o logowanie ponownie | US14.4 | — | brak |
| `weryfikacja.feature` | Wzięte zgłoszenie znika z kolejki pozostałych Weryfikatorów | BR-11, US8.2, US10.4 | — | brak |
| `weryfikacja.feature` | Drugi Weryfikator nie obsłuży zgłoszenia wziętego przez pierwszego | BR-11, US10.4 | — | brak |
| `weryfikacja.feature` | Weryfikator nie widzi w kolejce własnych zgłoszeń | BR-1, BR-4, US10.5 | — | brak |
| `weryfikacja.feature` | Weryfikator widzi rolę i pseudonim autora, a nie jego dane osobowe | BR-5 | — | brak |
| `weryfikacja.feature` | Podobne zgłoszenia w promieniu 10 m | BR-13, US10.1 | — | brak |
| `weryfikacja.feature` | Podobne zgłoszenie pokazuje typ, zdjęcie, odległość i piętro | BR-13, US10.1 | — | brak |
| `weryfikacja.feature` | System sam nie odrzuca ani nie łączy podobnych zgłoszeń | BR-13 | — | brak |
| `weryfikacja.feature` | Odrzucenie duplikatu wskazuje wcześniejsze zgłoszenie | BR-13 | — | brak |
| `weryfikacja.feature` | Bez powodu odrzucenia zgłoszenie nie zostaje odrzucone | BR-16, US10.2 | — | brak |
| `weryfikacja.feature` | Zgłoszenia szybkiego nie da się przyjąć bez typu i uciążliwości | BR-14, US10.3 | — | brak |
| `weryfikacja.feature` | Weryfikator ustala typ z innej grupy barier niż wybrana przez Odwiedzającego | BR-14, US10.3 | — | brak |
| `weryfikacja.feature` | Weryfikator nie zmienia danych zgłoszenia z pomiaru precyzyjnego | BR-15 | — | brak |
| `weryfikacja.feature` | Błąd w zgłoszeniu z pomiaru precyzyjnego oznacza odrzucenie z powodem | BR-15 | — | brak |
| `weryfikacja.feature` | Zasłonięcie twarzy na zdjęciu nie tworzy nowej wersji | BR-15 | — | brak |
| `weryfikacja.feature` | Edycja autora w trakcie weryfikacji daje Weryfikatorowi nową wersję | BR-3, US8.3 | — | brak |
| `weryfikacja.feature` | Przyjęte zgłoszenie od razu pojawia się na mapie i karcie budynku | BR-6, BR-17, US1.3, US2.2 | — | brak |
| `weryfikacja.feature` | Po przyjęciu zmiany poprzednie zgłoszenie staje się archiwalne | BR-18 | — | brak |
| `weryfikacja.feature` | Po odrzuceniu zmiany na mapie zostaje poprzednie zgłoszenie | BR-18 | — | brak |
| `weryfikacja.feature` | Po przyjęciu „bariera usunięta” bariera znika z mapy i karty budynku | BR-19, US8.4, US10.6 | — | brak |
| `weryfikacja.feature` | Po odrzuceniu „bariera usunięta” bariera zostaje na mapie | BR-19, US8.4 | — | brak |
| `weryfikacja.feature` | Druga równoległa zmiana jest widoczna jako podobne zgłoszenie | BR-23, US10.1 | — | brak |
| `weryfikacja.feature` | Zmiany zgłoszenia archiwalnego nie da się przyjąć | BR-23 | — | brak |
| `weryfikacja.feature` | Zmianę zgłoszenia archiwalnego Weryfikator odrzuca z powodem „nieaktualne” | BR-23 | — | brak |
| `weryfikacja.feature` | Zmianę odrzuconą jako „nieaktualne” Zbierający wysyła ponownie do bieżącego zgłoszenia | BR-23 | — | brak |
| `weryfikacja.feature` | Po przyjęciu „bariera usunięta” barierę, która nadal istnieje, zgłasza się od nowa | BR-23 | — | brak |
| `zgloszenia-zbierajacego.feature` | Każde własne zgłoszenie ma jeden status tekstem i ikoną | US8.1 | — | brak |
| `zgloszenia-zbierajacego.feature` | Zgłoszenie odrzucone pokazuje powód odrzucenia | BR-16, US8.1 | — | brak |
| `zgloszenia-zbierajacego.feature` | Status „wysłane” dopiero po potwierdzeniu odbioru przez serwer | BR-9, US8.2 | — | brak |
| `zgloszenia-zbierajacego.feature` | Status „w weryfikacji” po wzięciu zgłoszenia przez Weryfikatora | US8.2 | — | brak |
| `zgloszenia-zbierajacego.feature` | Edycja zgłoszenia przed decyzją Weryfikatora | BR-3, US8.3 | — | brak |
| `zgloszenia-zbierajacego.feature` | Poprawa i ponowne wysłanie zgłoszenia odrzuconego | BR-3, BR-16, US8.3 | — | brak |
| `zgloszenia-zbierajacego.feature` | Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie | BR-3, BR-18 | — | brak |
| `zgloszenia-zbierajacego.feature` | Zwykłą zmianę przyjętego zgłoszenia z pomiaru precyzyjnego składa tylko autor | BR-3, BR-18 | — | brak |
| `zgloszenia-zbierajacego.feature` | Przyjęte zgłoszenie szybkie zmienia dowolny Zbierający | BR-18 | — | brak |
| `zgloszenia-zbierajacego.feature` | Inny Zbierający niż autor zgłasza usunięcie bariery | BR-19, US8.4 | — | brak |
| `zgloszenia-zbierajacego.feature` | Usunięcia bariery nie da się zgłosić bez nowego zdjęcia | BR-19, US8.4 | — | brak |
| `zgloszenia-zbierajacego.feature` | Zgłoszenie „bariera usunięta” zapisuje się bez uciążliwości | BR-7, BR-19, US8.4 | — | brak |
| `zgloszenie-szybkie-powrot.feature` | Po wysłaniu „Wstecz” wraca do ekranu, z którego zaczęto zgłoszenie | US4.7 | — | brak |
| `zgloszenie-szybkie-powrot.feature` | Powrót między krokami nie usuwa zdjęcia ani grupy barier | US4.8 | — | brak |
| `zgloszenie-szybkie.feature` | Zgłoszenie szybkie w 3 krokach z pozycją dołączoną automatycznie | BR-8, US4.1 | `app · AppNavigatorTest.us4_1_three_steps_sent_p2` | częściowo |
| `zgloszenie-szybkie.feature` | Zgłoszenie przy wejściu podpowiada wejście, a miejsce bierze z bieżącej pozycji | BR-8, US4.6 | `app · AppNavigatorTest.us4_6_report_at_entrance` | częściowo |
| `zgloszenie-szybkie.feature` | Wybór jednej z 6 grup barier dużymi przyciskami | BR-8, US4.3 | — | brak |
| `zgloszenie-szybkie.feature` | Zgłoszenia szybkiego nie da się wysłać bez zdjęcia albo grupy barier | BR-8, US4.1, US4.3 | — | brak |
| `zgloszenie-szybkie.feature` | Bez zgody na lokalizację aparat się nie otwiera | BR-8, US4.1 | — | brak |
| `zgloszenie-szybkie.feature` | Bez bieżącej pozycji zgłoszenie czeka na ponowną próbę | BR-8, US4.1 | — | brak |
| `zgloszenie-szybkie.feature` | Pozycja ustalona przy ponownej próbie pozwala wysłać zgłoszenie | BR-8, US4.1 | — | brak |
| `zgloszenie-szybkie.feature` | Budynek zgłoszenia szybkiego przypisuje system, a przy grupie „inne” piętro zostaje puste | BR-8, BR-14 | — | brak |
| `zgloszenie-szybkie.feature` | Pytanie o piętro tylko w budynku i przy grupie, która go wymaga | BR-8, BR-14, US4.5 | — | brak |
| `zgloszenie-szybkie.feature` | Odpowiedź „Nie wiem” zostawia piętro puste i pozwala wysłać zgłoszenie | BR-8, US4.5 | — | brak |
| `zgloszenie-szybkie.feature` | Wybrane piętro trafia do zgłoszenia | BR-8, US4.5 | — | brak |
| `zgloszenie-szybkie.feature` | Pozycja poza konturem przy wysłaniu zostawia piętro puste | BR-8, BR-14, US4.5 | — | brak |
| `zgloszenie-szybkie.feature` | Zgłoszenie szybkie bez sieci zapisuje się w telefonie | BR-8, BR-9, US4.2 | — | brak |
| `zgloszenie-szybkie.feature` | Zgłoszenie szybkie zapisane bez sieci wysyła się samo po powrocie sieci | BR-8, BR-9, US4.2 | — | brak |
| `zgloszenie-szybkie.feature` | Potwierdzenie po wysłaniu bez dalszego statusu | BR-8, US4.4 | — | brak |
| `zgloszenie-szybkie.feature` | Potwierdzenie bez sieci mówi o późniejszej wysyłce | BR-8, US4.2, US4.4 | — | brak |
| `zgloszenie-szybkie.feature` | Gość niezalogowany jest proszony o logowanie przed zdjęciem | BR-8, US14.2 | `app · AppNavigatorTest.us14_2_bar_report_guest_l1` | częściowo |
| `zgloszenie-szybkie.feature` | Gość po zalogowaniu wraca do zgłoszenia szybkiego | BR-8, US14.2 | — | brak |
| `zgloszenie-szybkie.feature` | Gość rezygnuje z logowania | BR-8, US14.2 | `app · AppNavigatorTest.uc7_1b_guest_not_now_v1` | częściowo |
