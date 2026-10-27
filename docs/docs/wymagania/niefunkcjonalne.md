# Wymagania niefunkcjonalne

| Kategoria | Charakterystyka ISO/IEC 25010:2023 [1], [2] | Wymagania |
| --- | --- | --- |
| Dokładność pozycji | przydatność funkcjonalna (functional suitability) | NFR-1…NFR-4 |
| Praca offline i synchronizacja | niezawodność (reliability) | NFR-5…NFR-10 |
| Wydajność | efektywność wykonania (performance efficiency) | NFR-11…NFR-15 |
| Dostępność interfejsu | zdolność interakcji (interaction capability) | NFR-16…NFR-26 |
| RODO | bezpieczeństwo (security), poza modelem: zgodność z prawem | NFR-27…NFR-32 |
| Bezpieczeństwo | bezpieczeństwo (security) | NFR-33…NFR-39 |
| Licencje danych | poza modelem: zgodność z licencjami | NFR-40…NFR-44 |
| Przenośność | elastyczność (flexibility), zgodność (compatibility) | NFR-45…NFR-47 |
| Utrzymywalność | utrzymywalność (maintainability) | NFR-48…NFR-51 |
| Odporność | niezawodność (reliability), bezpieczeństwo użytkowania (safety) | NFR-52…NFR-57 |

Każde wymaganie ma miarę i sposób sprawdzenia. Jeśli kolumna „Źródło potrzeby” nie wskazuje normy ani przepisu, próg jest naszą propozycją do weryfikacji w testach. Numery w nawiasach kwadratowych odsyłają do sekcji „Źródła”.

**Telefon referencyjny** — fizyczny telefon zespołu z Androidem, używany do pomiarów i na demo. **Teren otwarty** — miejsce bez budynków i drzew w promieniu około 20 m.

## Dokładność pozycji

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-1 | Dokładność pozycji | Pozycja bariery na zewnątrz jest dokładna po 30 s uśredniania. | Mediana błędu poziomego < 1 m, żaden pomiar > 3 m. Cel do potwierdzenia pomiarem. | 10 pomiarów w terenie otwartym nad punktem o znanych współrzędnych (punkt osnowy albo wyraźny narożnik na ortofotomapie z Geoportalu). | N5, US6 AC1, [33] |
| NFR-2 | Dokładność pozycji | Ekran C1 pokazuje szacowaną dokładność i liczbę satelitów według systemów. Wartość jest opisana jako szacunek, nie gwarancja. | Odświeżanie co ≤ 1 s. Przy wartości widnieje informacja, że to szacunek. | Obserwacja ekranu C1 na telefonie referencyjnym, porównanie z wynikiem NFR-1. | US6 AC1; system podaje promień ufności 68 % [9] |
| NFR-3 | Dokładność pozycji | Wewnątrz budynku pozycję satelitarną poprawia się ręcznie: na planie piętra (poziom 1) albo na mapie (poziom 2). | Ręczna korekta dostępna zawsze. Zapis po korekcie ma oznaczenie „korekta ręczna”. | Scenariusz US6 AC3 w budynku A-1 i w budynku bez planu. | US6 AC3, BR-G1; błąd przy ścianie 5–15 m (hipoteza) |
| NFR-4 | Dokładność pozycji | Podpowiedź najbliższego budynku działa bez sieci. | Podpowiedź ≤ 2 s od końca uśredniania, w trybie samolotowym. | Test na telefonie referencyjnym w trybie samolotowym przy 5 budynkach szczegółowych. | US6 AC2, N6 |

## Praca offline i synchronizacja

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-5 | Offline | Zapis i zdjęcie zapisane w telefonie przetrwają zamknięcie aplikacji, wymuszone zatrzymanie i restart telefonu. | 0 utraconych zapisów na 50. | 50 zapisów ze zdjęciem bez sieci, zatrzymanie aplikacji, restart, powrót sieci, porównanie z serwerem. | N6, US4 AC2, US9 AC1 |
| NFR-6 | Synchronizacja | Po powrocie sieci kolejka wysyła się sama. | ≤ 60 s, gdy aplikacja jest na ekranie. W tle — w oknie pracy wyznaczonym przez system [11], najpóźniej przy następnym otwarciu aplikacji. | Pomiar czasu od włączenia sieci do statusu „wysłane” w C3; w tle — status po ponownym otwarciu. | N6, US9 AC2 |
| NFR-7 | Offline | Kolejka do wysłania mieści pracę kilku dni terenowych. | ≥ 200 zapisów z jednym zdjęciem każdy. Ostrzeżenie, gdy w telefonie zostaje < 100 MB. | Test z 200 zapisami bez sieci, potem pełna wysyłka. | N6 |
| NFR-8 | Synchronizacja | Ponowna wysyłka tej samej operacji nie tworzy drugiego zapisu. | 0 duplikatów przy 20 przerwanych wysyłkach. | Przerwanie połączenia w trakcie wysyłki, ponowienie, liczenie zapisów na serwerze. | N6, N8 |
| NFR-9 | Synchronizacja | Konflikt wersji nigdy nie kończy się cichym nadpisaniem. | 100 % konfliktów trafia na ekran wyboru wersji (C4). | Równoległa edycja tego samego zapisu z dwóch telefonów. | N7, US9 AC3 |
| NFR-10 | Offline | Ostatnio pobrane budynki i bariery są dostępne bez sieci, z datą aktualizacji. | Karta wcześniej otwartego budynku otwiera się offline i pokazuje datę synchronizacji. | Test w trybie samolotowym na V1 i V2. | N1, N2, N6 |

## Wydajność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-11 | Wydajność | Aplikacja startuje szybko. | Zimny start < 5 s (próg „wolnego startu” w statystykach Androida). | Mediana z 10 zimnych startów na telefonie referencyjnym. | N1, N4, [8] |
| NFR-12 | Wydajność | Mapa kampusu z konturami budynków i barierami otwiera się szybko. | ≤ 3 s od startu, gdy dane są w telefonie. ≤ 5 s przy pierwszym uruchomieniu przez sieć komórkową. | Mediana z 10 pomiarów na telefonie referencyjnym. | N1, US1 |
| NFR-13 | Wydajność | Karta budynku pokazuje tekst od razu, zdjęcia doczytuje później. | Tekst ≤ 1 s od dotknięcia (dane w telefonie), ≤ 3 s przez sieć. | Mediana z 10 pomiarów dla A-1, C-13 i budynku bez planu. | N1, N2, US2 |
| NFR-14 | Wydajność | Serwer odpowiada szybko na odczyty. | 95 % odczytów budynków i barier ≤ 500 ms przy 20 równoczesnych użytkownikach. | Test obciążeniowy na środowisku demo. | N1 |
| NFR-15 | Wydajność | Zdjęcie przed wysyłką jest zmniejszane, żeby przejść przez słaby zasięg. | Plik ≤ 1 MB, dłuższy bok ≤ 2048 px. | Sprawdzenie rozmiaru 20 zdjęć po stronie serwera. | N3, N6, NFR-7 |

## Dostępność interfejsu

Aplikacja dotyczy barier dla osób z niepełnosprawnościami, więc sama musi być dostępna. Bazą jest WCAG 2.1 AA [3] i rozdział 11 normy EN 301 549 [4]. Ustawa o dostępności cyfrowej [5] obowiązuje aplikacje podmiotów publicznych; dotyczy nas, jeśli aplikację przejmie PWr (do potwierdzenia).

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-16 | Dostępność UI | Tekst ma wystarczający kontrast z tłem. | ≥ 4,5:1; tylko tekst duży (18 pt albo pogrubiony 14 pt z WCAG, przeliczone na ≥ 24 sp albo pogrubiony ≥ 19 sp): ≥ 3:1. | Automatyczny skaner dostępności na każdym ekranie, kontrola par kolorów w palecie. | N4, WCAG 1.4.3 [3]; próg Androida 18 sp [6] jest łagodniejszy niż WCAG, więc go nie stosujemy |
| NFR-17 | Dostępność UI | Ikony statusu, ramki pól i wskaźnik fokusu są widoczne. | Kontrast ≥ 3:1. | Skaner dostępności, ręczny przegląd ikon statusu. | N4, WCAG 1.4.11 [3] |
| NFR-18 | Dostępność UI | Elementy dotykowe są duże. | Cel dotyku ≥ 48 × 48 dp. To więcej niż 44 px z kryterium AAA WCAG 2.5.5. | Skaner dostępności na każdym ekranie. | N4, wytyczne Androida [6]; WCAG 2.1 AA nie stawia tu progu |
| NFR-19 | Dostępność UI | Każdy element interaktywny ma po polsku nazwę, rolę i stan dla czytnika ekranu TalkBack. | 100 % elementów z etykietą. Ścieżki US1–US5 przechodzone wyłącznie z TalkBack. | Skaner dostępności i ręczny przegląd z TalkBack. | N4, WCAG 1.1.1 i 4.1.2 [3], [4] |
| NFR-20 | Dostępność UI | Informacja z mapy jest dostępna także jako lista budynków i barier. | Każdy budynek i bariera z V1 osiągalne z listy. | Ścieżka US1 z TalkBack bez dotykania mapy. | N1, N4, WCAG 1.1.1 [3] |
| NFR-21 | Dostępność UI | Interfejs działa przy powiększonej czcionce. | Przy 200 % (Android 14+) brak uciętego tekstu i nakładania, wszystkie funkcje osiągalne. Na starszych wersjach — przy maksimum systemu. | Przegląd wszystkich ekranów przy 200 %, testy zrzutów ekranu. | N4, WCAG 1.4.4 [3], [7] |
| NFR-22 | Dostępność UI | Informacja nie jest przekazywana wyłącznie kolorem. | Status, dostępność wejścia i ocena 1–3 mają tekst i ikonę lub kształt. | Przegląd ekranów w trybie odcieni szarości. | N4, US2 AC1, WCAG 1.4.1 [3] |
| NFR-23 | Dostępność UI | Aplikacja działa w orientacji pionowej i poziomej. | 100 % ekranów w obu orientacjach bez utraty treści. | Przegląd ekranów po obrocie telefonu. | N4, WCAG 1.3.4 [3] |
| NFR-24 | Dostępność UI | Brak limitów czasu na odpowiedź użytkownika. | 0 akcji przerywanych przez upływ czasu. Uśrednianie 30 s można przerwać i powtórzyć. | Przegląd ścieżek US1–US9. | N4, WCAG 2.2.1 [3] |
| NFR-25 | Dostępność UI | Strona logowania i rejestracji spełnia NFR-16…NFR-22. | Te same progi co w aplikacji. | Skaner dostępności i przegląd z TalkBack. | N4, US14 |
| NFR-26 | Użyteczność | Seniorzy i zbierający oceniają aplikację co najmniej przeciętnie. | Średni wynik SUS ≥ 68 przy ≥ 5 użytkownikach. | Ankieta SUS po teście z myśleniem na głos. | N4, [32] |

## RODO

Rozporządzenie RODO [14] zalicza dane o lokalizacji do danych osobowych (art. 4 pkt 1). Oznaczenie „(do potwierdzenia)” wskazuje naszą interpretację przepisu.

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-27 | RODO | Opublikowane zdjęcia nie pokazują rozpoznawalnych twarzy ani tablic rejestracyjnych. Weryfikator zasłania je albo odrzuca zdjęcie. | 0 rozpoznawalnych twarzy i tablic w opublikowanych zdjęciach. Przypomnienie przy robieniu zdjęcia. | Punkt listy kontrolnej R2, przegląd 20 losowych opublikowanych zdjęć. | RODO art. 5 ust. 1 lit. c [14], wizerunek: art. 81 prawa autorskiego [15]; tablice (do potwierdzenia) |
| NFR-28 | RODO | Serwer usuwa metadane zdjęcia (miejsce, model telefonu, czas) przed udostępnieniem. | 0 znaczników lokalizacji w pobranych zdjęciach. | Pobranie 20 zdjęć i odczyt metadanych. | RODO art. 5 ust. 1 lit. c, art. 25 [14] |
| NFR-29 | RODO | Aplikacja pobiera lokalizację tylko na ekranie, przy ustalaniu pozycji (C1) lub zgłoszeniu (V4). Nie zapisuje śladu ruchu. | Brak uprawnienia do lokalizacji w tle. 0 zapisanych pozycji poza punktem bariery. | Przegląd uprawnień w ustawieniach telefonu, przegląd danych na serwerze. | RODO art. 4 pkt 1, art. 5 ust. 1 lit. c [14], [12] |
| NFR-30 | RODO | O autorze serwer przechowuje tylko identyfikator konta, nazwę wyświetlaną i rolę. E-mail i hasło zostają u dostawcy tożsamości. | 0 danych autora w odpowiedziach publicznych. 0 zewnętrznych narzędzi analitycznych i reklamowych. | Test kontraktu publicznych odpowiedzi, przegląd zależności aplikacji. | RODO art. 5 ust. 1 lit. c, art. 25 [14], US1 AC2 |
| NFR-31 | RODO | Dane są przechowywane tylko tak długo, jak to potrzebne. | Odrzucone zapisy i ich zdjęcia: usunięcie po 90 dniach (do potwierdzenia). Żądanie usunięcia danych osobowych: realizacja ≤ 1 miesiąc, także ręcznie przez administratora; bariery zostają bez autora. | Test usuwania z przesuniętą datą, usunięcie konta testowego. | RODO art. 5 ust. 1 lit. e, art. 12 ust. 3, art. 17 [14] |
| NFR-32 | RODO | Przed założeniem konta użytkownik widzi klauzulę informacyjną i podstawę przetwarzania. | Klauzula przed rejestracją i w profilu (≤ 2 dotknięcia). Zgodę można wycofać. | Przejście ścieżki rejestracji i profilu. | RODO art. 6, 7 ust. 3, 13 [14]; podstawa: zgoda w projekcie studenckim, zadanie publiczne po wdrożeniu w PWr (do potwierdzenia) |

## Bezpieczeństwo

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-33 | Bezpieczeństwo | Logowanie odbywa się standardem OAuth 2.0 i OpenID Connect, z kodem autoryzacji i PKCE, w przeglądarce systemowej. | 0 pól hasła w aplikacji. | Przegląd ekranów, test ścieżki logowania. | US14, decyzja 7, [16], [17], [18], [19] |
| NFR-34 | Bezpieczeństwo | Konto jest chronione przed odgadywaniem hasła. | Po 5 nieudanych próbach blokada czasowa konta. | Test 6 błędnych logowań. | [23] |
| NFR-35 | Bezpieczeństwo | Serwer sprawdza rolę przy każdym niepublicznym żądaniu. | Brak roli albo cudze zgłoszenie przy uprawnieniu „własne”: odpowiedź 403. Macierz operacja × rola pokryta testami w 100 %. | Testy kontraktu dla każdej operacji i roli. | US14 AC1, OWASP API1:2023 i API5:2023 [21] |
| NFR-36 | Bezpieczeństwo | Publiczne operacje (bez logowania) mają limit liczby żądań. | ≤ 60 żądań/min z jednego adresu; powyżej: 429 z czasem ponowienia. Zdjęcie ≤ 5 MB. | Test serii żądań i zbyt dużego pliku. | OWASP API4:2023 [21], [20] |
| NFR-37 | Bezpieczeństwo | Aplikacja i repozytorium nie zawierają sekretów. Aplikacja jest klientem publicznym. | 0 sekretów w pakiecie aplikacji i w historii repozytorium. | Skan sekretów w CI przy każdym PR, przegląd konfiguracji klienta. | [18] §8.5, [22], decyzja 6 |
| NFR-38 | Bezpieczeństwo | Tokeny są przechowywane w zaszyfrowanym magazynie systemu i nie trafiają do logów. | 0 tokenów w logach z pełnej ścieżki testowej. | Przegląd logów po teście, przegląd magazynu danych. | [22] |
| NFR-39 | Bezpieczeństwo | Poza środowiskiem deweloperskim komunikacja jest szyfrowana. | 100 % ruchu przez HTTPS (TLS ≥ 1.2). Wersja do demo publicznego odrzuca ruch nieszyfrowany. | Próba połączenia bez szyfrowania. | [22] |

## Licencje danych

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-40 | Licencje | Atrybucja OpenStreetMap jest stale widoczna na mapie, z odnośnikiem do strony praw autorskich. Jest też w plikach eksportu. | Napis „© OpenStreetMap contributors” na 100 % ekranów z mapą (V1, C1, R1), nieukryty. | Przegląd ekranów i plików eksportu. | ODbL [24], [25], [26] |
| NFR-41 | Licencje | Bariery są osobną warstwą, powiązaną z budynkiem tylko symbolem. | 0 geometrii budynków z OSM w eksporcie barier. | Przegląd plików eksportu. | ODbL share-alike [24]; interpretacja (do potwierdzenia) |
| NFR-42 | Licencje | Podkład mapy z serwerów OSM jest używany tylko online, zgodnie z ich polityką. Bez sieci mapa pokazuje kontury, plany pięter i bariery z danych aplikacji. | Własny identyfikator aplikacji w żądaniach. Pamięć podręczna według nagłówków serwera, bez nich ≥ 7 dni. 0 pobrań z wyprzedzeniem i do użytku offline. | Przegląd żądań do serwera kafelków, test w trybie samolotowym. | [27], decyzja 27 |
| NFR-43 | Licencje | Plan piętra i karta budynku podają źródło danych PWr. Fakty z deklaracji dostępności są podane własnymi słowami, z odnośnikiem. | Podpis „Źródło: Politechnika Wrocławska” na 100 % widoków planu. | Przegląd ekranów V2 i C1 dla 5 budynków szczegółowych. | decyzje 15 i 17, [34] |
| NFR-44 | Licencje | Dane planów PWr nie trafiają do publicznego repozytorium. Repozytorium zawiera tylko skrypt importu. | 0 plików z danymi planów w historii repozytorium. | Kontrola w CI przy każdym PR. | decyzje 15 i 17 |

## Przenośność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-45 | Przenośność | Aplikacja działa od Androida 8.0 (API 26) do wersji docelowej (API 36). | Instalacja i ścieżki US1–US4 działają na API 26 i API 36. | Testy w CI na emulatorach z wersją minimalną i docelową, telefon referencyjny. | N4, N5, decyzja 28; ok. 96 % urządzeń w kwietniu 2026 [13]; stan satelitów od API 24 [10] |
| NFR-46 | Przenośność | Interfejs jest po polsku. Teksty są oddzielone od kodu, więc angielski można dodać bez zmian w logice. | 100 % tekstów po polsku, 0 tekstów zapisanych w kodzie. Daty w formacie polskim. | Analiza statyczna w CI, przegląd ekranów. | N4, produkt §6 |
| NFR-47 | Zgodność | Eksport jest poprawny technicznie i czytelny w QGIS. | GeoJSON przechodzi walidację RFC 7946 i otwiera się bez ustawiania układu. CSV otwiera się po wskazaniu EPSG:2180. Oba pliki w UTF-8, polskie znaki bez zniekształceń. | Walidacja pliku GeoJSON, otwarcie obu plików w QGIS. | N9, US13 AC1–AC2, [30], [31] |

## Utrzymywalność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-48 | Utrzymywalność | Kontrakt API w OpenAPI 3.1 powstaje przed kodem. Błędy mają format RFC 9457. | 100 % operacji i pól z opisem. Zmiana łamiąca zgodność wstecz blokuje scalenie. | Walidacja kontraktu i porównanie z poprzednią wersją w CI. | decyzje 4, 9 i 22, [28], [29] |
| NFR-49 | Utrzymywalność | Każde kryterium AC i każda reguła BR ma wykonywalny scenariusz akceptacyjny po polsku. | 100 % AC i BR pokryte co najmniej 1 scenariuszem. | Skrypt spójności identyfikatorów w CI. | produkt §8, decyzje 16 i 24 |
| NFR-50 | Utrzymywalność | Każdy PR przechodzi budowanie, testy i analizę statyczną. | Czerwony wynik blokuje scalenie. PR funkcjonalny ≤ 300 linii. | Reguły gałęzi w repozytorium. | produkt §9, decyzje 5 i 8 |
| NFR-51 | Utrzymywalność | Lista budynków szczegółowych jest konfiguracją, nie kodem. | Dodanie budynku: 1 wpis w konfiguracji, 0 zmian w kodzie. | Próbne dodanie budynku C-6. | produkt §6a |

## Odporność

| ID | Kategoria | Wymaganie | Miara / próg | Jak sprawdzamy | Źródło potrzeby |
| --- | --- | --- | --- | --- | --- |
| NFR-52 | Odporność | Budynek bez planu działa w trybie podstawowym zgodnie z BR-G1. | 100 % budynków poziomu 2 otwiera kartę i przyjmuje zgłoszenie bez błędu. | Scenariusz BR-G1, automatyczny przegląd kart wszystkich budynków. | BR-G1, N3 |
| NFR-53 | Odporność | Punkt poza konturem budynku to stan normalny zgodnie z BR-G2. | 0 komunikatów błędu. Zawsze propozycja najbliższego budynku. | Scenariusz BR-G2 z punktem między budynkami. | BR-G2, N3, N5 |
| NFR-54 | Odporność | Nieudany import planu nie psuje innych budynków zgodnie z BR-G3. | Przy uszkodzonych danych 1 z 5 budynków: 4 mają plany, uszkodzony działa na poziomie 2. Import kończy się raportem. | Test importu z celowo uszkodzonymi danymi. | BR-G3 |
| NFR-55 | Odporność | Zgłoszenie daleko od budynków PWr działa zgodnie z BR-G4. | Mapa i zgłoszenie działają, bariera zapisana na poziomie 3. | Scenariusz BR-G4 z punktem na ulicy obok kampusu. | BR-G4, N3 |
| NFR-56 | Odporność | Przy niedostępnym serwerze aplikacja pokazuje dane z telefonu, a zgłoszenia trafiają do kolejki. | 0 awarii aplikacji. Ścieżki US1, US2 i US4 przechodzą. Komunikat bez żargonu technicznego. | Test z wyłączonym serwerem. | N1, N6, NFR-5 |
| NFR-57 | Odporność | Awaria źródeł zewnętrznych (OSM, ArcGIS PWr, USOS) nie wpływa na działającą aplikację. Dane są importowane przy wdrożeniu. | 0 błędów w ścieżkach US1–US4 przy odciętych źródłach. Brak podkładu mapy nie blokuje konturów i list. | Test z zablokowanym dostępem do źródeł zewnętrznych. | decyzja 17, NFR-42 |

## Źródła

1. [ISO/IEC 25010:2023 — model jakości produktu](https://www.iso.org/standard/78176.html)
2. [Zmiany w ISO/IEC 25010:2023 (arc42)](https://quality.arc42.org/articles/iso-25010-update-2023)
3. [WCAG 2.1 (W3C)](https://www.w3.org/TR/WCAG21/)
4. [EN 301 549 V3.2.1 (ETSI)](https://www.etsi.org/deliver/etsi_en/301500_301599/301549/03.02.01_60/en_301549v030201p.pdf)
5. [Ustawa o dostępności cyfrowej stron internetowych i aplikacji mobilnych podmiotów publicznych](https://isap.sejm.gov.pl/isap.nsf/DocDetails.xsp?id=WDU20190000848)
6. [Android: dostępność aplikacji — cele dotyku i kontrast](https://developer.android.com/guide/topics/ui/accessibility/apps)
7. [Android 14: skalowanie czcionki do 200 %](https://developer.android.com/about/versions/14/features)
8. [Android vitals: czas startu aplikacji](https://support.google.com/googleplay/android-developer/answer/9844486)
9. [Android: Location — dokładność jako promień ufności 68 %](https://developer.android.com/reference/android/location/Location)
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
