# Wymagania niefunkcjonalne

| Kategoria | Charakterystyka ISO/IEC 25010:2023 [1], [2] | Wymagania |
| --- | --- | --- |
| Dokładność pozycji | przydatność funkcjonalna (functional suitability) | NFR-1…NFR-4 |
| Praca offline i synchronizacja | niezawodność (reliability) | NFR-5…NFR-10 |
| Wydajność | efektywność wykonania (performance efficiency) | NFR-11…NFR-14 |
| Dostępność i użyteczność interfejsu | zdolność interakcji (interaction capability) | NFR-15…NFR-24, NFR-51 |
| RODO | bezpieczeństwo (security), poza modelem: zgodność z prawem | NFR-25…NFR-30 |
| Bezpieczeństwo | bezpieczeństwo (security) | NFR-31…NFR-36 |
| Licencje danych | poza modelem: zgodność z licencjami | NFR-37…NFR-40 |
| Przenośność | elastyczność (flexibility), zgodność (compatibility) | NFR-41…NFR-43 |
| Utrzymywalność | utrzymywalność (maintainability) | NFR-44…NFR-47 |
| Odporność | niezawodność (reliability), bezpieczeństwo użytkowania (safety) | NFR-48…NFR-50 |

Każde wymaganie ma miarę i sposób sprawdzenia. Dopisek „(próg zespołu)” oznacza wartość ustaloną przez zespół bez normy ani przepisu; potwierdzamy ją w testach. Numery w nawiasach kwadratowych odsyłają do sekcji „Źródła”.

**Telefon referencyjny** — fizyczny telefon zespołu z Androidem, używany do pomiarów i na demo. **Teren otwarty** — miejsce bez budynków i drzew w promieniu około 20 m.

## Dokładność pozycji

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-1 | Dokładność pozycji | Pozycja bariery w terenie otwartym jest dokładna po 30 s uśredniania pozycji. | Mediana błędu poziomego < 1 m (cel z N5, do potwierdzenia pomiarem). Żaden pomiar > 3 m (próg zespołu). | 10 pomiarów w terenie otwartym nad punktem o znanych współrzędnych (punkt osnowy albo wyraźny narożnik na ortofotomapie z Geoportalu). | N5, US6.1, [33] |
| NFR-2 | Dokładność pozycji | Ekran C1 pokazuje szacowaną dokładność pozycji i liczbę satelitów według systemów. Wartość jest opisana jako szacunek, nie gwarancja. | Odświeżanie co ≤ 1 s (próg zespołu). Przy wartości widnieje informacja, że to szacunek. | Obserwacja ekranu C1 na telefonie referencyjnym, porównanie z wynikiem NFR-1. | US6.1; system podaje promień ufności 68% [9] |
| NFR-3 | Dokładność pozycji | Wewnątrz budynku pozycję z GNSS poprawia się ręcznie: na mapie w każdym budynku, a w budynku szczegółowym także na planie piętra. | Ręczna korekta dostępna zawsze. Zgłoszenie po korekcie ma oznaczenie „korekta ręczna”. | Scenariusze US6.3 i US6.4 w budynku A-1 i w budynku podstawowym. | US6.3, US6.4, BR-G1; błąd przy ścianie 5–15 m (hipoteza) |
| NFR-4 | Dokładność pozycji | Podpowiedź najbliższego budynku działa bez sieci. | Podpowiedź ≤ 2 s od końca uśredniania, w trybie samolotowym (próg zespołu). | Test na telefonie referencyjnym w trybie samolotowym przy 5 budynkach szczegółowych. | US6.2, N6 |

## Praca offline i synchronizacja

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-5 | Offline | Zgłoszenie i zdjęcie zapisane w telefonie przetrwają zamknięcie aplikacji, wymuszone zatrzymanie i restart telefonu. | 0 utraconych zgłoszeń na 50. | 50 zgłoszeń ze zdjęciem bez sieci, zatrzymanie aplikacji, restart, powrót sieci, porównanie z serwerem. | N6, US4.2, US9.1 |
| NFR-6 | Synchronizacja | Po powrocie sieci synchronizacja rusza sama. | ≤ 60 s, gdy aplikacja jest na ekranie (próg zespołu). W tle — w oknie pracy wyznaczonym przez system [11], najpóźniej przy następnym otwarciu aplikacji. | Pomiar czasu od włączenia sieci do statusu „wysłane” w C3; w tle — status po ponownym otwarciu. | N6, US9.2 |
| NFR-7 | Offline | Kolejka do synchronizacji mieści pracę kilku dni terenowych. | ≥ 200 zgłoszeń z jednym zdjęciem każde. Ostrzeżenie, gdy w telefonie zostaje < 100 MB (próg zespołu). | Test z 200 zgłoszeniami bez sieci, potem pełna synchronizacja. | N6 |
| NFR-8 | Synchronizacja | Ponowne wysłanie tej samej operacji nie tworzy drugiego zgłoszenia. | 0 duplikatów przy 20 przerwanych synchronizacjach. | Przerwanie połączenia w trakcie synchronizacji, ponowienie, liczenie zgłoszeń na serwerze. | N6, N8 |
| NFR-9 | Synchronizacja | Konflikt wersji nigdy nie kończy się cichym nadpisaniem. | 100% konfliktów trafia na ekran wyboru wersji (C4). | Równoległa edycja tego samego zgłoszenia z dwóch telefonów. | N7, US9.3 |
| NFR-10 | Offline | Ostatnio pobrane budynki i bariery są dostępne bez sieci, z datą aktualizacji. | Karta wcześniej otwartego budynku otwiera się offline i pokazuje datę synchronizacji. | Test w trybie samolotowym na V1 i V2. | N1, N2, N6 |

## Wydajność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-11 | Wydajność | Aplikacja startuje szybko i od razu pokazuje mapę kampusu z konturami budynków i barierami. | Zimny start < 5 s (próg „wolnego startu” w statystykach Androida [8]). Mapa ≤ 3 s od pierwszego ekranu, gdy dane są w telefonie; ≤ 5 s przy pierwszym uruchomieniu przez sieć komórkową (próg zespołu). | Mediana z 10 zimnych startów i 10 otwarć mapy na telefonie referencyjnym. | N1, N4, US1 |
| NFR-12 | Wydajność | Karta budynku pokazuje tekst od razu, zdjęcia doczytuje później. | Tekst ≤ 1 s od dotknięcia (dane w telefonie), ≤ 3 s przez sieć (próg zespołu). | Mediana z 10 pomiarów dla A-1, C-13 i budynku podstawowego. | N1, N2, US2 |
| NFR-13 | Wydajność | Serwer odpowiada szybko na odczyty. | 95% odczytów budynków i barier ≤ 500 ms przy 20 równoczesnych użytkownikach (próg zespołu). | Test obciążeniowy na środowisku demo. | N1 |
| NFR-14 | Wydajność | Zdjęcie przed wysłaniem jest zmniejszane, żeby przejść przez słaby zasięg. | Plik ≤ 1 MB, dłuższy bok ≤ 2048 px (próg zespołu). | Sprawdzenie rozmiaru 20 zdjęć po stronie serwera. | N3, N6, NFR-7 |

## Dostępność i użyteczność interfejsu

Aplikacja dotyczy barier dla osób z niepełnosprawnościami, więc sama musi być dostępna. Bazą jest WCAG 2.1 AA [3] i rozdział 11 normy EN 301 549 [4]. Ustawa o dostępności cyfrowej [5] obowiązuje aplikacje podmiotów publicznych; dotyczy nas, jeśli aplikację przejmie PWr (do potwierdzenia).

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-15 <span id="nfr-15"></span> | Dostępność UI | Aplikacja i strona logowania i rejestracji spełniają WCAG 2.1 na poziomach A i AA według rozdziału 11 normy EN 301 549. NFR-16…NFR-23 i NFR-51 doprecyzowują kryteria najważniejsze dla N4. | 0 niespełnionych kryteriów A i AA na wszystkich ekranach. | Skaner dostępności, przegląd według listy kryteriów z rozdziału 11 EN 301 549, przegląd z TalkBack. | N4, US14, [3], [4] |
| NFR-16 <span id="nfr-16"></span> | Dostępność UI | Tekst, ikony statusu, ramki pól i wskaźnik fokusu mają wystarczający kontrast z tłem. | Tekst ≥ 4,5:1. Tekst duży ≥ 3:1. Duży to ≥ 24 sp albo pogrubiony ≥ 19 sp (18 pt i 14 pt z WCAG, przeliczenie zespołu). Ikony statusu, ramki pól i fokus: ≥ 3:1. | Skaner dostępności na każdym ekranie, kontrola par kolorów w palecie, ręczny przegląd ikon statusu. | N4, WCAG 1.4.3 i 1.4.11 [3]; próg Androida 18 sp [6] jest łagodniejszy niż WCAG, więc go nie stosujemy |
| NFR-17 <span id="nfr-17"></span> | Dostępność UI | Elementy dotykowe są duże. | Cel dotyku ≥ 48 × 48 dp. To więcej niż 44 px z kryterium AAA WCAG 2.5.5. | Skaner dostępności na każdym ekranie. | N4, wytyczne Androida [6]; WCAG 2.1 AA nie stawia tu progu |
| NFR-18 <span id="nfr-18"></span> | Dostępność UI | Każdy element interaktywny ma po polsku nazwę, rolę i stan dla czytnika ekranu TalkBack. | 100% elementów z etykietą. Ścieżki US1–US5 przechodzone wyłącznie z TalkBack. | Skaner dostępności i ręczny przegląd z TalkBack. | N4, WCAG 1.1.1 i 4.1.2 [3], [4] |
| NFR-19 <span id="nfr-19"></span> | Dostępność UI | Informacja z mapy jest dostępna także jako lista budynków i barier. | Każdy budynek i bariera z V1 osiągalne z listy. | Ścieżka US1 z TalkBack bez dotykania mapy. | N1, N4, WCAG 1.1.1 [3] |
| NFR-20 <span id="nfr-20"></span> | Dostępność UI | Interfejs działa przy powiększonej czcionce. | Przy 200% (Android 14+) brak uciętego tekstu i nakładania, wszystkie funkcje osiągalne. Na starszych wersjach — przy maksimum systemu. | Przegląd wszystkich ekranów przy 200%, testy zrzutów ekranu. | N4, WCAG 1.4.4 [3], [7] |
| NFR-21 <span id="nfr-21"></span> | Dostępność UI | Informacja nie jest przekazywana wyłącznie kolorem. | Status zgłoszenia, oznaczenie wejścia (także „brak danych”) i uciążliwość bariery 1–3 mają tekst i ikonę lub kształt. | Przegląd ekranów w trybie odcieni szarości. | N4, US2.1, US17.1, WCAG 1.4.1 [3] |
| NFR-22 <span id="nfr-22"></span> | Dostępność UI | Aplikacja działa w orientacji pionowej i poziomej. | 100% ekranów w obu orientacjach bez utraty treści. | Przegląd ekranów po obrocie telefonu. | N4, WCAG 1.3.4 [3] |
| NFR-23 <span id="nfr-23"></span> | Dostępność UI | Brak limitów czasu na odpowiedź użytkownika. | 0 akcji przerywanych przez upływ czasu. Uśrednianie 30 s można przerwać i powtórzyć. | Przegląd ścieżek US1–US9. | N4, WCAG 2.2.1 [3] |
| NFR-24 <span id="nfr-24"></span> | Użyteczność | Osoby starsze z grupy docelowej oceniają aplikację co najmniej przeciętnie. Zbierający nie biorą udziału w tym teście. | Średni wynik SUS ≥ 68 przy ≥ 5 uczestnikach z grupy docelowej (M-3). | Ankieta SUS po teście z myśleniem na głos. | N4, [32] |
| NFR-51 <span id="nfr-51"></span> | Dostępność UI | Na tablecie, rozłożonym telefonie składanym i telefonie w poziomie, gdy okno ma szerokość ≥ 600 dp, tekst nie rozciąga się na całą szerokość. Na ekranach z mapą mapa zajmuje pozostałą szerokość. Aplikacja nie wymusza orientacji. | Okno o szerokości ≥ 600 dp, czyli klasy „średnia” i szersze [35]: wiersz tekstu ma najwyżej 75 znaków ze spacjami [36]. Przyciski, pola i arkusze mają szerokość kolumny tekstu, także przy dużej czcionce [38]. Wszystkie funkcje z telefonu działają w obu orientacjach i na podzielonym ekranie. Poza mapą nie ma przewijania w dwóch kierunkach. | Przegląd ekranów na emulatorze o zmiennym rozmiarze okna z profilami telefonu składanego i tabletu [39]: obie orientacje i podzielony ekran. Punkty poziomu 3 wytycznych adaptacyjnych [37]: `Config_Changes`, `Config_Combinations`, `Multi-Window_Functionality`. Punkty poziomu 2 o kolumnach tekstu i szerokości elementów [38]. | N4, WCAG 1.3.4 i 1.4.10 [3]; od API 36 system pomija blokadę orientacji na ekranach o najmniejszej szerokości ≥ 600 dp [40] |

## RODO

Rozporządzenie RODO [14] zalicza dane o lokalizacji do danych osobowych (art. 4 pkt 1). Oznaczenie „(do potwierdzenia)” wskazuje naszą interpretację przepisu.

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-25 | RODO | Zdjęcia w zgłoszeniach przyjętych nie pokazują rozpoznawalnych twarzy ani tablic rejestracyjnych. Weryfikator zasłania je albo odrzuca zgłoszenie z powodem (BR-15, BR-16). | 0 rozpoznawalnych twarzy i tablic w zdjęciach zgłoszeń przyjętych. Przypomnienie przy robieniu zdjęcia. | Punkt listy kontrolnej R2, przegląd 20 losowych zdjęć zgłoszeń przyjętych. | RODO art. 5 ust. 1 lit. c [14], wizerunek: art. 81 prawa autorskiego [15]; tablice (do potwierdzenia) |
| NFR-26 | RODO | Serwer usuwa metadane zdjęcia (miejsce, model telefonu, czas) przed udostępnieniem. | 0 znaczników lokalizacji w pobranych zdjęciach. | Pobranie 20 zdjęć i odczyt metadanych. | RODO art. 5 ust. 1 lit. c, art. 25 [14] |
| NFR-27 | RODO | Aplikacja pobiera lokalizację tylko na ekranie, przy pomiarze precyzyjnym (C1) lub zgłoszeniu szybkim (V4). Nie zapisuje śladu ruchu. | Brak uprawnienia do lokalizacji w tle. 0 zapisanych pozycji poza punktem bariery. | Przegląd uprawnień w ustawieniach telefonu, przegląd danych na serwerze. | RODO art. 4 pkt 1, art. 5 ust. 1 lit. c [14], [12] |
| NFR-28 | RODO | O autorze serwer przechowuje tylko identyfikator konta, nazwę wyświetlaną i rolę. E-mail i hasło zostają w Usłudze logowania. | 0 danych autora w odpowiedziach publicznych. 0 zewnętrznych narzędzi analitycznych i reklamowych. | Test kontraktu publicznych odpowiedzi, przegląd zależności aplikacji. | RODO art. 5 ust. 1 lit. c, art. 25 [14], US1.2 |
| NFR-29 | RODO | Dane są przechowywane tylko tak długo, jak to potrzebne. | Zgłoszenia odrzucone i ich zdjęcia: usunięcie po 90 dniach (próg zespołu, do potwierdzenia). Żądanie usunięcia danych osobowych: realizacja ≤ 1 miesiąc, także ręcznie przez administratora; zgłoszenia zostają bez autora. | Test usuwania z przesuniętą datą, usunięcie konta testowego. | RODO art. 5 ust. 1 lit. e, art. 12 ust. 3, art. 17 [14] |
| NFR-30 | RODO | Przed założeniem konta użytkownik widzi klauzulę informacyjną i podstawę przetwarzania. | Klauzula przed rejestracją i w profilu (≤ 2 dotknięcia, próg zespołu). Zgodę można wycofać. | Przejście ścieżki rejestracji i profilu. | RODO art. 6, 7 ust. 3, 13 [14]; podstawa: zgoda w projekcie studenckim, zadanie publiczne po wdrożeniu w PWr (do potwierdzenia) |

## Bezpieczeństwo

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-31 | Bezpieczeństwo | Logowanie odbywa się standardem OAuth 2.0 i OpenID Connect, z kodem autoryzacji i PKCE, w przeglądarce systemowej. | 0 pól hasła w aplikacji. | Przegląd ekranów, test ścieżki logowania. | US14, decyzja 7, [16], [17], [18], [19] |
| NFR-32 | Bezpieczeństwo | Konto jest chronione przed odgadywaniem hasła. | Po 5 nieudanych próbach blokada czasowa konta (próg zespołu). | Test 6 błędnych logowań. | [23] |
| NFR-33 | Bezpieczeństwo | Serwer sprawdza rolę przy każdym niepublicznym żądaniu. | Brak roli albo cudze zgłoszenie przy uprawnieniu „własne”: odpowiedź 403. Macierz operacja × rola pokryta testami w 100%. | Testy kontraktu dla każdej operacji i roli. | US14.1, OWASP API1:2023 i API5:2023 [21] |
| NFR-34 | Bezpieczeństwo | Publiczne operacje (bez logowania) mają limit liczby żądań. | ≤ 60 żądań/min z jednego adresu; powyżej: 429 z czasem ponowienia. Zdjęcie ≤ 5 MB (próg zespołu). | Test serii żądań i zbyt dużego pliku. | OWASP API4:2023 [21], [20] |
| NFR-35 | Bezpieczeństwo | Aplikacja i repozytorium nie zawierają sekretów; aplikacja jest klientem publicznym. Tokeny leżą w zaszyfrowanym magazynie systemu i nie trafiają do logów. | 0 sekretów w pakiecie aplikacji i w historii repozytorium. 0 tokenów w logach z pełnej ścieżki testowej. | Skan sekretów w CI przy każdym PR, przegląd konfiguracji klienta, przegląd logów i magazynu danych po teście. | [18] §8.5, [22], decyzja 6 |
| NFR-36 | Bezpieczeństwo | Poza środowiskiem deweloperskim komunikacja jest szyfrowana. | 100% ruchu przez HTTPS (TLS ≥ 1.2). Wersja do demo publicznego odrzuca ruch nieszyfrowany. | Próba połączenia bez szyfrowania. | [22] |

## Licencje danych

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-37 | Licencje | Atrybucja OpenStreetMap jest stale widoczna na mapie, z odnośnikiem do strony praw autorskich. Jest też w plikach eksportu. | Napis „© OpenStreetMap contributors” na 100% ekranów z mapą (V1, C1, R1), nieukryty. | Przegląd ekranów i plików eksportu. | ODbL [24], [25], [26] |
| NFR-38 | Licencje | Bariery są osobną warstwą, powiązaną z budynkiem tylko symbolem budynku. | 0 geometrii budynków z OSM w eksporcie barier. | Przegląd plików eksportu. | ODbL share-alike [24]; interpretacja (do potwierdzenia) |
| NFR-39 | Licencje | Podkład mapy z serwerów OSM jest używany tylko online, zgodnie z ich polityką. Bez sieci mapa pokazuje kontury, plany pięter i bariery z danych aplikacji. | Własny identyfikator aplikacji w żądaniach. Pamięć podręczna według nagłówków serwera, bez nich ≥ 7 dni. 0 pobrań z wyprzedzeniem i do użytku offline. | Przegląd żądań do serwera kafelków, test w trybie samolotowym. | [27], decyzja 27 |
| NFR-40 | Licencje | Plan piętra podaje źródło danych PWr, a same dane planów nie trafiają do publicznego repozytorium (jest tylko skrypt importu). Fakty z deklaracji dostępności podajemy własnymi słowami, z odnośnikiem. | Podpis „Źródło: Politechnika Wrocławska” na 100% widoków planu. 0 plików z danymi planów w historii repozytorium. | Przegląd ekranów V1, V2 i C1 dla 5 budynków szczegółowych, kontrola w CI przy każdym PR. | decyzje 15 i 17, [34] |

## Przenośność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-41 | Przenośność | Aplikacja działa od Androida 8.0 (API 26) do wersji docelowej (API 36). | Instalacja i ścieżki US1–US4 działają na API 26 i API 36. | Testy w CI na emulatorach z wersją minimalną i docelową, telefon referencyjny. | N4, N5, decyzja 28; ok. 96% urządzeń w kwietniu 2026 [13]; stan satelitów od API 24 [10] |
| NFR-42 | Przenośność | Interfejs jest po polsku. Teksty są oddzielone od kodu, więc angielski można dodać bez zmian w logice. | 100% tekstów po polsku, 0 tekstów zapisanych w kodzie. Daty w formacie polskim. | Analiza statyczna w CI, przegląd ekranów. | N4, produkt §6 |
| NFR-43 | Zgodność | Eksport jest poprawny technicznie i czytelny w QGIS. | GeoJSON przechodzi walidację RFC 7946 i otwiera się bez ustawiania układu. CSV otwiera się po wskazaniu EPSG:2180. Oba pliki w UTF-8, polskie znaki bez zniekształceń. | Walidacja pliku GeoJSON, otwarcie obu plików w QGIS. | N9, US13.1–US13.2, [30], [31] |

## Utrzymywalność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-44 | Utrzymywalność | Kontrakt API w OpenAPI 3.1 powstaje przed kodem. Błędy mają format RFC 9457. | 100% operacji i pól z opisem. Zmiana łamiąca zgodność wstecz blokuje scalenie. | Walidacja kontraktu i porównanie z poprzednią wersją w CI. | decyzje 4, 9 i 22, [28], [29] |
| NFR-45 | Utrzymywalność | Każde kryterium AC i każda reguła BR ma wykonywalny scenariusz akceptacyjny po polsku. | 100% AC i BR pokryte co najmniej 1 scenariuszem. | Skrypt spójności identyfikatorów w CI. | produkt §8, decyzje 16 i 24 |
| NFR-46 | Utrzymywalność | Każdy PR przechodzi budowanie, testy i analizę statyczną. | Czerwony wynik blokuje scalenie. PR funkcjonalny ≤ 300 linii. | Reguły gałęzi w repozytorium. | produkt §9, decyzje 5 i 8 |
| NFR-47 | Utrzymywalność | Lista budynków szczegółowych jest konfiguracją, nie kodem. | Dodanie budynku: 1 wpis w konfiguracji, 0 zmian w kodzie. | Próbne dodanie budynku C-6. | produkt §6a |

## Odporność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-48 | Odporność | Budynek bez planów pięter — brak danych albo nieudany import — działa jako budynek podstawowy (BR-G1, BR-G3). | 100% budynków podstawowych otwiera kartę i przyjmuje zgłoszenie bez błędu. Przy uszkodzonych danych 1 z 5 budynków szczegółowych pozostałe 4 mają plany, a import kończy się raportem. | Scenariusze BR-G1 i BR-G3, automatyczny przegląd kart wszystkich budynków, import z celowo uszkodzonymi danymi. | BR-G1, BR-G3, N3 |
| NFR-49 | Odporność | Punkt poza konturem budynku, także daleko od budynków PWr, to stan normalny (BR-G2, BR-G4). | 0 komunikatów błędu. Propozycja najbliższego budynku, którą można odrzucić. Daleko od budynków PWr zgłoszenie zapisuje się jako „poza budynkiem”. | Scenariusze BR-G2 (punkt między budynkami) i BR-G4 (punkt na ulicy obok kampusu). | BR-G2, BR-G4, N3, N5 |
| NFR-50 | Odporność | Awaria serwera lub źródeł zewnętrznych (podkład OSM, ArcGIS PWr) nie zatrzymuje aplikacji. Aplikacja pokazuje dane z telefonu, a zgłoszenia czekają w kolejce. Dane źródeł są importowane przy wdrożeniu. | 0 awarii aplikacji. Ścieżki US1, US2 i US4 przechodzą. Brak podkładu nie blokuje konturów i list. Komunikat bez żargonu technicznego. | Test z wyłączonym serwerem i z zablokowanym dostępem do źródeł zewnętrznych. | N1, N6, NFR-5, NFR-39, decyzja 17 |

## Źródła

1. [ISO/IEC 25010:2023 — model jakości produktu](https://www.iso.org/standard/78176.html)
2. [Zmiany w ISO/IEC 25010:2023 (arc42)](https://quality.arc42.org/articles/iso-25010-update-2023)
3. [WCAG 2.1 (W3C)](https://www.w3.org/TR/WCAG21/)
4. [EN 301 549 V3.2.1 (ETSI)](https://www.etsi.org/deliver/etsi_en/301500_301599/301549/03.02.01_60/en_301549v030201p.pdf)
5. [Ustawa o dostępności cyfrowej stron internetowych i aplikacji mobilnych podmiotów publicznych](https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20190000848)
6. [Android: dostępność aplikacji — cele dotyku i kontrast](https://developer.android.com/guide/topics/ui/accessibility/apps)
7. [Android 14: skalowanie czcionki do 200%](https://developer.android.com/about/versions/14/features)
8. [Android vitals: czas startu aplikacji](https://support.google.com/googleplay/android-developer/answer/9844486)
9. [Android: Location — dokładność jako promień ufności 68%](https://developer.android.com/reference/android/location/Location)
10. [Android: GnssStatus (od API 24)](https://developer.android.com/reference/android/location/GnssStatus)
11. [Android: tryb Doze i praca w tle](https://developer.android.com/training/monitoring-device-state/doze-standby)
12. [Android: uprawnienia do lokalizacji](https://developer.android.com/develop/sensors-and-location/location/permissions)
13. [Udział wersji Androida (apilevels.com)](https://apilevels.com/)
14. [RODO — rozporządzenie (UE) 2016/679](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
15. [Ustawa o prawie autorskim i prawach pokrewnych](https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU19940240083)
16. [RFC 6749 — OAuth 2.0](https://www.rfc-editor.org/rfc/rfc6749)
17. [RFC 7636 — PKCE](https://www.rfc-editor.org/rfc/rfc7636)
18. [RFC 8252 — OAuth 2.0 dla aplikacji natywnych](https://www.rfc-editor.org/rfc/rfc8252)
19. [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
20. [RFC 6585 — kod 429 Too Many Requests](https://www.rfc-editor.org/rfc/rfc6585)
21. [OWASP API Security Top 10 2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)
22. [OWASP MASVS](https://mas.owasp.org/MASVS/)
23. [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/)
24. [Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/)
25. [OpenStreetMap: prawa autorskie i licencja](https://www.openstreetmap.org/copyright)
26. [OSMF: wytyczne atrybucji](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines)
27. [OSMF: polityka korzystania z kafelków](https://operations.osmfoundation.org/policies/tiles/)
28. [OpenAPI Specification 3.1](https://spec.openapis.org/oas/v3.1.0)
29. [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
30. [RFC 7946 — GeoJSON](https://www.rfc-editor.org/rfc/rfc7946)
31. [EPSG:2180 — PUWG 1992](https://epsg.io/2180)
32. [System Usability Scale — średni wynik 68 (MeasuringU)](https://measuringu.com/sus/)
33. [Geoportal krajowy](https://www.geoportal.gov.pl/)
34. [Dział Dostępności PWr — deklaracje dostępności](https://dostepnosc.pwr.edu.pl/)
35. [Android: klasy rozmiaru okna](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)
36. [Android: podstawowe wytyczne jakości aplikacji — długość wiersza](https://developer.android.com/docs/quality-guidelines/core-app-quality)
37. [Android: wytyczne jakości aplikacji adaptacyjnych, poziom 3](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3)
38. [Android: wytyczne jakości aplikacji adaptacyjnych, poziom 2](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-2)
39. [Android: wytyczne jakości aplikacji adaptacyjnych — urządzenia testowe](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality)
40. [Android 16: orientacja i proporcje ignorowane na dużych ekranach](https://developer.android.com/about/versions/16/behavior-changes-16)
