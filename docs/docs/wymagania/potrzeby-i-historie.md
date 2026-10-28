# Potrzeby i historyjki

Strona prowadzi od potrzeby użytkownika do sprawdzalnego kryterium: potrzeba N → ekran → historyjka US → kryterium akceptacji USx.y → scenariusz. Role opisuje strona [Role i uprawnienia](../biznes/role.md), kroki T1…T29 — [Proces TO-BE](../biznes/procesy/to-be.md). Progi liczbowe są w [wymaganiach niefunkcjonalnych](niefunkcjonalne.md), tu tylko się do nich odwołujemy.

## Potrzeby

| N | Rola (RO-*) | Potrzeba | Status (hipoteza — do potwierdzenia w wywiadach) |
| --- | --- | --- | --- |
| N1 | RO-GOS, RO-ODW | Przed wyjściem z domu wiedzieć, czy wejdę do budynku bez barier i którym wejściem. | hipoteza; pytania 2.1–2.4, 4.1–4.2 i zadanie z części 5 |
| N2 | RO-GOS, RO-ODW | Ufać informacji: widzieć datę sprawdzenia i zdjęcie bariery. | hipoteza; pytania 2.5, 4.3–4.4 |
| N3 | RO-ODW | Zgłosić nową barierę bez trudnego formularza. | hipoteza; pytania 3.4, 4.5–4.6 |
| N4 | RO-GOS, RO-ODW | Korzystać z aplikacji przy słabym wzroku i niepewnych dłoniach. | hipoteza; pytania 3.1–3.4, 4.7–4.8 |
| N5 | RO-ZBI | Szybko i dokładnie, poniżej 1 m, zapisać miejsce bariery. | hipoteza; wywiad opcjonalny, pytanie 10.2 |
| N6 | RO-ZBI | Pracować tam, gdzie nie ma sieci, i nie tracić danych. | hipoteza; wywiad opcjonalny, pytanie 10.3 |
| N7 | RO-ZBI, RO-WER | Przy równoczesnej edycji nie nadpisać wcześniejszej zmiany zapisanej na serwerze. | hipoteza; wywiad opcjonalny, pytanie 10.4 |
| N8 | RO-WER | Odsiać błędy i duplikaty przed publikacją. | hipoteza; wywiad opcjonalny, pytania 10.4–10.5 |
| N9 | RO-KOO | Widzieć postęp inwentaryzacji i przekazywać dane do QGIS. | hipoteza; wywiad opcjonalny, pytania 10.1, 10.6–10.7 |

Numery pytań pochodzą ze [scenariusza wywiadu](../ux/scenariusz-wywiadu.md). Wywiady z osobami 60+ sprawdzają N1–N4, a wywiad ze Zbierającym jest opcjonalny. Potrzeba niepotwierdzona w wywiadach kieruje swoje historyjki do przeglądu (R-7).

## Historyjki

| US | Ekran | N | Priorytet | Historyjka |
| --- | --- | --- | --- | --- |
| US1 | V1 | N1 | Musi być | Jako Gość niezalogowany lub Odwiedzający, chcę znaleźć budynek na mapie po symbolu budynku, aby wiedzieć, gdzie jest. |
| US2 | V2 | N1, N2 | Musi być | Jako Gość niezalogowany lub Odwiedzający, chcę otworzyć kartę budynku, aby zobaczyć wejścia dostępne i bariery. |
| US3 | V3 | N1 | Powinno być | Jako Gość niezalogowany lub Odwiedzający, chcę wybrać budynek i dostać pasujące wejście, aby zaplanować wizytę. |
| US4 | V4 | N3, N4 | Musi być | Jako Odwiedzający, chcę zgłosić barierę zdjęciem i bieżącą pozycją, aby po weryfikacji inni o niej wiedzieli. |
| US5 | V5 | N4 | Powinno być | Jako Gość niezalogowany, który pierwszy raz otwiera aplikację, chcę krótką instrukcję, aby się nie zgubić. |
| US6 | C1 | N5 | Musi być | Jako Zbierający, chcę uśrednić pozycję i wskazać budynek, aby miejsce bariery było dokładne. |
| US7 | C2 | N5 | Musi być | Jako Zbierający, chcę wypełnić formularz zgłoszenia, aby opisać barierę. |
| US8 | C3; zmiana zgłoszenia przyjętego z karty budynku V2: C2, C7 | N6 | Musi być | Jako Zbierający, chcę widzieć status każdego mojego zgłoszenia, aby wiedzieć, co dotarło, a co trzeba poprawić. |
| US9 | C4 | N6, N7 | Musi być | Jako Zbierający, chcę synchronizacji po pracy w trybie offline, aby nie stracić zgłoszeń i nie nadpisać wcześniejszej zmiany zapisanej na serwerze. |
| US10 | R1, R2 | N2, N7, N8 | Musi być | Jako Weryfikator, chcę sprawdzić zgłoszenie i zobaczyć podobne zgłoszenia, aby nie przepuścić błędu ani duplikatu. |
| US11 | K1 | N9 | Musi być | Jako Koordynator, chcę widzieć postęp inwentaryzacji według budynków, aby wiedzieć, gdzie trzeba jeszcze pójść. |
| US12 | K2 | N9 | Powinno być | Jako Koordynator, chcę zaprosić osobę z wybraną rolą, aby dołączyła do inwentaryzacji bez pomocy administratora. |
| US13 | K3 | N9 | Musi być | Jako Koordynator, chcę pobrać eksport, aby otworzyć zgłoszenia przyjęte w QGIS. |
| US14 | L1, L2 | N2, N8 | Musi być | Jako Odwiedzający lub osoba z rolą pracowniczą, chcę się zalogować, aby korzystać z czynności mojej roli. |
| US15 | C5 (z V2 i K1) | N6, N9 | Musi być | Jako Zbierający, chcę oznaczyć piętro obejrzane, aby postęp inwentaryzacji odpowiadał stanowi obchodu. |
| US16 | V2 | N1 | Powinno być | Jako Gość niezalogowany lub Odwiedzający, chcę wybrać piętro budynku szczegółowego, aby zobaczyć na planie piętra windy, toalety i bariery. |
| US17 | V2, V3; zmiana: C6 | N1, N2 | Musi być | Jako Gość niezalogowany lub Odwiedzający, chcę widzieć przy każdym wejściu aktualne oznaczenie z datą i źródłem, aby wiedzieć, czy mogę mu ufać. |

Priorytet to metoda MoSCoW: Musi być, Powinno być, Może być, Nie tym razem. Historyjki „Musi być” tworzą MVP.

- **US14 jest historyjką umożliwiającą US4 i US6–US13.** Logowanie nie ma własnej potrzeby, a dla N3 jest dodatkowym krokiem. Konto służy N2 i N8: chroni dane przed spamem, a o publikacji decyduje tylko osoba z rolą nadaną przez Koordynatora (BR-2).
- **US15 i US16 to nowe historyjki.** US15 daje podstawę postępu z US11, US16 opisuje plan piętra w budynkach szczegółowych.
- **US17 to nowa historyjka (decyzja 36).** Oznaczenie wejścia ma trzy wartości, a Zbierający zmienia je na obchodzie (US17.2). Dlatego US2.1 dopuszcza „brak danych”.

## Kryteria: Gość niezalogowany i Odwiedzający

| AC | Kryterium | Odwołania |
| --- | --- | --- |
| US1.1 | Wyszukanie „C13”, „c-13” i „C-13” znajduje budynek C-13. | — |
| US1.2 | Mapa kampusu i wyszukiwanie działają bez logowania. | RO-GOS |
| US1.3 | Na mapie są tylko zgłoszenia przyjęte. | BR-6 |
| US2.1 | Każde wejście ma jedno z trzech oznaczeń: „dostępne”, „niedostępne” albo „brak danych”, tekstem i ikoną, nie tylko kolorem. | NFR-21, US17.1 |
| US2.2 | Każde zgłoszenie przyjęte na karcie budynku ma zdjęcie i datę weryfikacji. | BR-6 |
| US3.1 | Gdy budynek nie ma wejścia dostępnego, ekran mówi to wprost zdaniem, a nie samym brakiem wyniku. | — |
| US3.2 | Od mapy do wskazania wejścia dostępnego są najwyżej 3 dotknięcia. | M-4 |
| US4.1 | Zgłoszenie szybkie to najwyżej 3 kroki: zdjęcie, grupa barier, wysłanie. Bieżąca pozycja dołącza się sama. | NFR-27 |
| US4.2 | Bez sieci zgłoszenie zapisuje się w telefonie i wysyła się samo po powrocie sieci. | NFR-5, NFR-6 |
| US4.3 | Odwiedzający wybiera jedną z 6 grup barier dużymi przyciskami. Aplikacja nie pyta o typ bariery ani uciążliwość. | NFR-17, US10.3 |
| US4.4 | Odwiedzający po wysłaniu widzi potwierdzenie, że zgłoszenie trafi do weryfikacji. Dalszego statusu zgłoszenia aplikacja mu nie pokazuje. | decyzja 32 |
| US4.5 | Gdy pozycja leży w konturze budynku, a wybrana grupa barier wymaga piętra, aplikacja w kroku 2 pyta o piętro dużymi przyciskami. Przyciski to etykiety pięter z danych PWr albo numery w budynku podstawowym i zawsze „Nie wiem”, które zostawia piętro puste. | BR-14, NFR-17, decyzja 43 |
| US5.1 | Instrukcja pojawia się przy pierwszym uruchomieniu. Można ją pominąć i otworzyć ponownie z pomocy. | — |
| US16.1 | W budynku szczegółowym przełącznik pięter pokazuje etykiety pięter z danych PWr. Wybrane piętro pokazuje plan z windami, schodami, toaletami, wejściami i zgłoszeniami przyjętymi. | NFR-40 |
| US16.2 | W budynku podstawowym przełącznika pięter nie ma, a w miejscu planu widać napis „Plan piętra niedostępny dla tego budynku”. | BR-G1, NFR-48 |

Odwiedzający w MVP nie ma listy własnych zgłoszeń, więc US4.4 kończy jego ścieżkę na potwierdzeniu. Potrzebę takiej listy sprawdzimy w wywiadach.

## Kryteria: Zbierający

| AC | Kryterium | Odwołania |
| --- | --- | --- |
| US6.1 | Uśrednianie pozycji trwa 30 s. Na ekranie widać dokładność pozycji i liczbę satelitów osobno dla każdego systemu GNSS. | NFR-1, NFR-2, NFR-23 |
| US6.2 | Aplikacja podpowiada najbliższy budynek, chyba że punkt jest daleko od budynków PWr (BR-G4). Zbierający go potwierdza, wybiera inny albo zostawia „poza budynkiem”. | NFR-4, BR-G2, BR-G4 |
| US6.3 | W budynku szczegółowym Zbierający przesuwa punkt na planie wybranego piętra. Zgłoszenie dostaje oznaczenie „korekta ręczna”. | NFR-3 |
| US6.4 | W każdym budynku i poza budynkiem Zbierający przesuwa punkt na mapie. Zgłoszenie dostaje oznaczenie „korekta ręczna”. | NFR-3, BR-G1, BR-G2 |
| US6.5 | Ekran pomiaru pokazuje mapę nieba z położeniem satelitów; system każdego satelity jest rozróżniony nie tylko kolorem. | NFR-21 |
| US7.1 | Bez typu bariery, uciążliwości 1–3 i zdjęcia zgłoszenia nie da się zapisać. | — |
| US7.2 | W budynku szczegółowym piętro wybiera się z etykiet z danych PWr. W budynku podstawowym wpisuje się liczbę całkowitą, z dopiskiem „półpiętro” lub bez. | BR-G1 |
| US7.3 | Przy typie TB-29 „inna bariera” opis jest obowiązkowy. | TB-29 |
| US8.1 | Lista pokazuje każde własne zgłoszenie z jednym statusem: lokalne, wysłane, w weryfikacji, przyjęte, odrzucone albo archiwalne. Odrzucone ma powód odrzucenia. | NFR-21, BR-18 |
| US8.2 | Status „wysłane” pojawia się dopiero wtedy, gdy zgłoszenie czeka w kolejce na serwerze. Status „w weryfikacji” pojawia się, gdy wziął je Weryfikator. | US10.4 |
| US8.3 | Zgłoszenie przed decyzją Weryfikatora można edytować. Odrzucone można poprawić i wysłać ponownie. | BR-3 |
| US8.4 | Każdy Zbierający, także inny niż autor, zaczyna na karcie budynku, przy wybranym zgłoszeniu przyjętym, zmianę z oznaczeniem „bariera usunięta”. Do decyzji Weryfikatora zmiana przechodzi te same statusy co zgłoszenie i po synchronizacji ma status „wysłane”. Po przyjęciu jest od razu archiwalna (US10.6). Bariera zostaje na mapie do decyzji Weryfikatora. | BR-3, US10.6, decyzja 33 |
| US9.1 | Bez sieci aplikacja pokazuje wskaźnik braku sieci, a nowe zgłoszenie czeka w kolejce ze statusem „lokalne”. | NFR-5, NFR-7 |
| US9.2 | Po powrocie sieci synchronizacja rusza bez działania użytkownika. | NFR-6, NFR-8 |
| US9.3 | Przy konflikcie wersji aplikacja pokazuje obie wersje, a wybór należy do Zbierającego: własna albo z serwera. Żadna zmiana nie znika po cichu. | NFR-9, BR-3 |
| US15.1 | W budynku szczegółowym Zbierający oznacza wybrane piętro jako obejrzane. W budynku podstawowym oznacza cały budynek jako „obejrzany”. | US11.1 |
| US15.2 | Znacznik cofa Zbierający, który go postawił. Inny Zbierający nie może go cofnąć. | US11.3 |
| US15.3 | Bez sieci znacznik i jego cofnięcie czekają w telefonie jak zgłoszenie i wysyłają się same po powrocie sieci. | NFR-5, NFR-6, decyzja 33 |

Ręczną korektę punktu opisują dwa kryteria: na planie piętra i na mapie. Korekta na planie działa tylko w budynku szczegółowym, więc ma priorytet „Powinno być”, podobnie jak mapa nieba. W MVP punkt poprawia się na mapie.

## Kryteria: Weryfikator i Koordynator

| AC | Kryterium | Odwołania |
| --- | --- | --- |
| US10.1 | Przy sprawdzaniu Weryfikator widzi podobne zgłoszenia w promieniu 10 m: typ, zdjęcie i odległość. | — |
| US10.2 | Odrzucenie, także duplikatu, jest niemożliwe bez powodu odrzucenia. | — |
| US10.3 | Zgłoszenia szybkiego nie da się przyjąć, zanim Weryfikator nie ustali typu bariery i uciążliwości. | decyzja 29 |
| US10.4 | Zgłoszenie wzięte przez Weryfikatora ma status „w weryfikacji” i znika z kolejki pozostałych Weryfikatorów. | decyzja 29 |
| US10.5 | Weryfikator nie widzi w kolejce własnych zgłoszeń ani zmian „bariera usunięta”, które sam złożył. | BR-4, decyzja 33 |
| US10.6 | Po przyjęciu zmiany „bariera usunięta” bariera znika z mapy i karty budynku. Zgłoszenie zostaje w systemie z tym oznaczeniem. | US1.3, decyzja 32 |
| US11.1 | Dla każdego budynku widać odsetek pięter obejrzanych (budynek szczegółowy) albo znacznik „obejrzany” (budynek podstawowy). Obok są liczby zgłoszeń czekających na weryfikację i przyjętych. | US15.1 |
| US11.2 | Gdy znacznik piętra obejrzanego albo jego cofnięcie dotrze na serwer, postęp budynku pokazuje nową wartość, np. 4 z 5 pięter, czyli 80 procent. | US15.3 |
| US11.3 | Koordynator cofa znacznik piętra obejrzanego postawiony przez dowolnego Zbierającego. | decyzja 32 |
| US11.4 | Dla każdego budynku szczegółowego widać liczbę wejść ze źródłem oznaczenia „obchód” wśród wszystkich jego wejść, np. „Wejścia sprawdzone na miejscu: 4 z 6”. | BR-24, M-1, decyzja 38 |
| US12.1 | Po zaproszeniu osoba z podanym adresem e-mail loguje się i ma wybraną rolę. | BR-2 |
| US12.2 | Zaproszenie na adres istniejącego konta dodaje mu rolę, a dotychczasowe role zostają. | BR-1, BR-2 |
| US13.1 | Plik GeoJSON otwiera się w QGIS bez ustawiania układu współrzędnych i ma współrzędne w WGS84. | NFR-43 |
| US13.2 | Plik CSV ma współrzędne w PUWG 1992 (EPSG:2180) i otwiera się w QGIS jako warstwa punktów. | NFR-43 |
| US13.3 | Eksport zawiera tylko bariery bieżące ze zgłoszeń przyjętych, bez barier usuniętych i bez danych autora. | BR-6, NFR-38, decyzja 33 |

US10.1 to lista podobnych zgłoszeń, czyli możliwych duplikatów. Ma priorytet „Musi być”, bo bez niej Weryfikator nie odsieje duplikatów (N8). Pokaz w A-1 opiera się na podobnym zgłoszeniu 5 m dalej (decyzja 37).

Usunięcie bariery nie ma osobnego procesu ani formularza. Przechodzi tę samą drogę co każda zmiana zgłoszenia przyjętego (BR-3), więc mapa nie pokazuje już naprawionej windy (N2).

## Kryteria: oznaczenie wejścia

| AC | Kryterium | Odwołania |
| --- | --- | --- |
| US17.1 | Przy imporcie budynków wejście dostaje oznaczenie „dostępne” albo „niedostępne” z danych PWr albo OpenStreetMap. Wejście bez takich danych ma oznaczenie „brak danych” tekstem i ikoną. | BR-24, NFR-21 |
| US17.2 | Zbierający na obchodzie składa zmianę oznaczenia wejścia z nowym zdjęciem. Sprawdza ją inny Weryfikator, a po przyjęciu nowe oznaczenie widać na karcie budynku. | BR-4, BR-24 |
| US17.3 | Przy każdym wejściu widać datę i źródło oznaczenia: „import” albo „obchód”. | BR-24 |

Zmiana oznaczenia wejścia to zgłoszenie, więc przechodzi te same statusy co każde zgłoszenie. Po przyjęciu jest od razu archiwalna, podobnie jak „bariera usunięta” (BR-24).

## Kryteria: logowanie

| AC | Kryterium | Odwołania |
| --- | --- | --- |
| US14.1 | Bez wymaganej roli aplikacja nie pokazuje ekranu czynności, a serwer odrzuca próbę jej wykonania. | NFR-33 |
| US14.2 | Gość niezalogowany po wybraniu zgłoszenia szybkiego widzi przed zdjęciem prośbę o logowanie albo rejestrację. | RO-GOS |
| US14.3 | Nowe konto z samodzielnej rejestracji ma tylko uprawnienia Odwiedzającego. | BR-1, BR-2 |

Uprawnienia każdej roli podaje macierz na stronie [Role i uprawnienia](../biznes/role.md). Sposób logowania opisują NFR-31 i NFR-32.

## Mapa historii

| Aktywność | Zacząć korzystać | Sprawdzić budynek | Zgłosić barierę szybko | Zmierzyć i opisać barierę | Wysłać i śledzić zgłoszenia | Zweryfikować zgłoszenie | Prowadzić inwentaryzację |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Kto | każda rola | RO-GOS, RO-ODW | RO-ODW | RO-ZBI | RO-ZBI | RO-WER | RO-ZBI, RO-KOO |
| Kroki TO-BE | przed T7 | T20–T23 | T7–T10 | T1–T6 | T6a, T6b, T10–T13, T19 | T14–T19 | T24–T29 |
| **Musi być (MVP)** | US14 logowanie | US1 wyszukiwanie<br>US2 karta budynku<br>US17 oznaczenie wejścia | US4 zgłoszenie szybkie | US6 pomiar bez US6.3, US6.5<br>US7 formularz | US8 statusy, „bariera usunięta”<br>US17.2 zmiana oznaczenia wejścia<br>US9 synchronizacja | US10 z podobnymi zgłoszeniami | US15 piętro obejrzane<br>US11 postęp<br>US13 eksport |
| **— linia MVP —** | | | | | | | |
| **Powinno być** | US5 pomoc | US3 „Którędy wejść”<br>US16 plan piętra | — | US6.3 korekta na planie<br>US6.5 mapa nieba | — | — | US12 zaproszenie |
| **Może być** | wersja angielska, tryb ciemny | kolejne budynki szczegółowe, dane z deklaracji dostępności | — | — | — | — | — |

Kolumny idą w kolejności korzystania z aplikacji, od Gościa niezalogowanego do Koordynatora. Kroki TO-BE każdej aktywności podaje drugi wiersz. Wiersze nad linią MVP muszą działać na pokazie. Pozycje „Może być” nie mają jeszcze historyjek; dostaną je, gdy wejdą do planu sprintu.

**Nie tym razem:** nawigacja po trasie (także w budynkach), iOS, Google Play, sztuczna inteligencja, czat, powiadomienia push, osobny proces usuwania bariery, lista własnych zgłoszeń Odwiedzającego.

## Źródła

- Format historyjki „Jako…, chcę…, aby…” (Mountain Goat Software): <https://www.mountaingoatsoftware.com/agile/user-stories>
- Priorytety MoSCoW (Agile Business Consortium): <https://www.agilebusiness.org/dsdm-project-framework/moscow-prioritisation.html>
- Mapa historii: aktywności w poziomie, linie wydań (Nielsen Norman Group): <https://www.nngroup.com/articles/user-story-mapping/>
- Składnia scenariuszy Gherkin (Cucumber): <https://cucumber.io/docs/gherkin/reference/>

Dostęp do źródeł: 27.10.2026.
