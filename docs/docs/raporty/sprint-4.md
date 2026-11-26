# Raport „Sprint 4”: Dostępny Kampus

| Pole | Wartość |
| --- | --- |
| Projekt | Aplikacja Android i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej |
| Kurs | Mobilne systemy informatyczne – Projekt (W04IST-SM0302G) |
| Prowadzący | dr inż. Zbigniew Telec |
| Autorzy raportu | Myroslav Hryniuka, Vladyslav Yevkov |
| Członkowie zespołu | Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko |
| Zakres prac | 05–21.11.2026, od zakończenia prac opisanych w raporcie Sprint 3 do scalenia `sprint/4` z `main` |
| Stan końcowy | 21.11.2026, [commit `4294e4844`][s4] i [PR zamykający sprint][pr-close] |

## 1. Cel i wynik sprintu

Dostępny Kampus ma pomóc odwiedzającemu zaplanować wizytę na uczelni. Przed wyjściem może on sprawdzić, którym wejściem dostać się do budynku i jakie bariery może napotkać. Zespół inwentaryzacyjny potrzebuje z kolei sposobu, by zebrać te informacje w terenie, także bez połączenia z internetem.

W Sprincie 3 aplikacja pobierała już dane o budynkach, a serwer przyjmował zgłoszenia przez API. Osoba korzystająca z aplikacji nie mogła jeszcze wysłać zdjęcia z dostępnego formularza. Dlatego głównym celem Sprintu 4 było przejście od pomiaru i zdjęcia do zapisu w telefonie oraz późniejszej wysyłki. Plan obejmował też przygotowanie danych dla Weryfikatora i przeliczenie współrzędnych potrzebne do eksportu. [Plan projektu](tydzien-2.md) · [Wynik Sprintu 3](sprint-3.md)

Na koniec sprintu można było zademonstrować dwie ścieżki. Gość mógł znaleźć budynek, obejrzeć wejścia i przyjęte bariery, a w budynku z planem sprawdzić, jak dojść do sali. Zbierający po zalogowaniu mógł zmierzyć pozycję, zrobić zdjęcie i zapisać zgłoszenie. Zgłoszenie zapisane bez internetu czekało w telefonie na wysyłkę. [Kopia barier offline][pr-offline-map] · [Droga do sali][pr-room-steps] · [Trasy zgłoszeń w aplikacji][routes]

Na koniec sprintu oba wyniki dało się pokazać osobno. Brak kolejki i ekranu decyzji dla Weryfikatora przerywał jednak przejście od nowego zdjęcia do publikacji na mapie. Serwer obsługiwał już samą decyzję, a mapa potrafiła wyświetlić przyjęte dane. To połączenie zostało zadaniem Sprintu 5. [Operacje weryfikacji][review] · [Brakujące trasy][pending]

## 2. Co zmieniło się dla użytkowników

### Zgłoszenie w terenie

Zbierający uruchamia pomiar pozycji. Aplikacja przez 30 sekund zbiera odczyty, pokazuje dokładność i satelity, a potem podpowiada budynek. Osoba może wybrać inny budynek lub skorygować punkt. [Pomiar][pr-gnss] · [Podpowiedź budynku][pr-building] · [Korekta punktu][pr-point]

W formularzu wybiera piętro, typ bariery i uciążliwość, po czym dodaje zdjęcie. Odwiedzający korzysta z krótszej ścieżki. Robi zdjęcie, wybiera grupę barier i wysyła zgłoszenie. [Formularz C2][pr-form] · [Ekran V4][pr-quick] · [Zapis V4][pr-quick-save]

Pierwsza wersja raportu z 9 listopada opisywała już formularze, do których użytkownik nie mógł dojść bez logowania. Później podłączyliśmy logowanie w przeglądarce systemowej i role. [Logowanie][pr-login] · [Sesja i role][pr-session]

Zbierający zyskał listę własnych zgłoszeń. Może na niej sprawdzić stan wysyłki i wrócić do poprawki. Po wygaśnięciu sesji aplikacja prosi o ponowne zalogowanie, a zmiana konta nie powoduje wysłania cudzych zgłoszeń. [Powrót po 401][pr-relogin] · [Kolejka konta][pr-owner] · [Statusy][pr-history] · [Poprawka][pr-edit]

### Dostępność budynku przed wizytą

Mapa zaczęła pokazywać przyjęte bariery z serwera i zachowywać ich kopię na wypadek braku internetu. Na karcie budynku widać bariery przy wejściach, a po otwarciu szczegółów także ich zdjęcia. [Bariery przy wejściu][pr-card-barrier] · [Zdjęcia][pr-public-photo]

Wskazanie zalecanego wejścia przenieśliśmy na kartę budynku, a szczegóły wejścia do arkusza na mapie. Osobny ekran V3 stał się zbędny. Dane z planów PWr uzupełniły informacje o wejściach i wyjściach ewakuacyjnych. [Zmiana ścieżki][pr-v3-ux] · [Usunięcie V3][pr-v3] · [Dane PWr][pr-entrances]

Odwiedzający może wybrać piętro lub znaleźć salę po numerze. Karta sali podaje kolejne kroki od zalecanego wejścia przez windę, jeśli jest potrzebna, do drzwi sali. Wybór kroku wskazuje dane miejsce na planie. [Plan piętra][pr-plan] · [Wyszukiwanie][pr-room-search] · [Karta sali][pr-room]

### Postęp obchodów i aktualność barier

Zbierający oznacza, które piętra obejrzał. Zmiany czekają w kolejce przy braku internetu. Koordynator może sprawdzić postęp według budynku, obejrzeć znaczniki i cofnąć znacznik. [Kolejka znaczników][pr-marker-queue] · [Oznaczanie pięter][pr-c5] · [Ekran postępu][pr-k1] · [API postępu][pr-progress]

Na karcie budynku można też zgłosić, że opisana wcześniej bariera zniknęła. Taka informacja wymaga ponownej weryfikacji, zanim zmieni się publiczna mapa. [Poprawka bariery][pr-replace] · [Usunięcie bariery][pr-removed]

## 3. Co zmieniliśmy pod spodem i po co

### Wiarygodność danych

Serwer ogranicza edycję zgłoszenia i zdjęć do autora. Weryfikator może wziąć zgłoszenie i rozstrzygnąć je przez API. Dla szybkiego zgłoszenia ustala typ i uciążliwość. [Edycja][pr-author] · [Klasyfikacja][pr-accept-quick]

API podaje też podobne zgłoszenia w promieniu 10 m, co pomaga zauważyć duplikaty. Serwer potrafi zastosować przyjętą zmianę oznaczenia wejścia. [Podobne zgłoszenia][pr-similar] · [Oznaczenie wejścia][pr-marking]

### Dane dla Koordynatora

Do CSV trafiają bieżące, przyjęte bariery ze współrzędnymi w układzie PUWG 1992 (EPSG:2180), które można wczytać do QGIS. Eksport pobiera i zapisuje dane partiami, dzięki czemu nie musi przechowywać całej odpowiedzi w pamięci. Na koniec sprintu CSV był dostępny przez serwer. Przycisk pobrania pozostawał do dodania w aplikacji. [Przeliczenie][pr-proj] · [CSV][pr-csv] · [Eksport strumieniowy][pr-stream] · [Test HTTP][csv-test]

### Utrzymanie aplikacji

Podzieliliśmy odpowiedzialność pomiędzy moduły i uporządkowaliśmy zależności w Androidzie. Ekrany pobierają dane przez `core:data`. [Architektura][adr-modules] · [Zależności Androida][pr-android-modules] · [Usunięcie cykli][pr-cycles]

Jedno miejsce steruje ruchem kamery i granicami widoku. Zmiana piętra aktualizuje filtry mapy, co ogranicza pracę przy przełączaniu planu. [Kamera][pr-camera] · [Pomiary][pr-trace] · [Filtry][pr-style]

Uporządkowaliśmy zasady przeglądu. Testy mutacyjne w narzędziu PIT zostawiliśmy w kontrolach PR i sprintu. Dzięki temu nie obciążają kolejki scalania. [Zasady pracy][pr-agents] · [Rozmiar PR][pr-size] · [PIT][pr-pit]

## 4. Jak sprawdziliśmy wynik

Na końcowej rewizji `4294e4844` przeszły testy Androida, analiza statyczna, kontrola zależności i dokumentacji. W ostatnim pełnym przebiegu przed zamknięciem przeszły też emulatory API 26, 36 i 37 oraz testy mutacyjne serwera. Późniejsze zmiany objęły specyfikacje scenariuszy i kontrolę PR. [Android][ci-final] · [Dokumentacja][ci-docs] · [Przebieg z emulatorami][ci-full] · [Różnica rewizji][ci-delta]

Test systemowy wysyłki uruchamia klienta z prawdziwym serwerem testowym. Tworzy zgłoszenie przez HTTP i sprawdza, że wraca na liście autora. Zakres testu kończy się przed obsługą zdjęcia z aparatu i decyzją Weryfikatora. Na koniec sprintu nie mieliśmy dowodu pełnego przejścia od zdjęcia do publikacji ani pomiaru GNSS w terenie. [Test wysyłki][system-upload] · [Scenariusz pokazu A-1][demo]

Według [generatora pokrycia scenariuszy][coverage] na końcowej rewizji stan był następujący. Liczby opisują powiązania scenariuszy z testami na danej warstwie. Wyniki ich uruchomień są w CI.

| Warstwa | Pełny test | Częściowo | Bez testu |
| --- | ---: | ---: | ---: |
| Aplikacja | 84 | 52 | 41 |
| Serwer | 22 | 0 | 45 |

## 5. Przebieg prac i przekazanie do Sprintu 5

1. **5–9 listopada.** Powstały C1, C2, V4, zapis do kolejki i serwerowe wyszukiwanie podobnych zgłoszeń.
   Część zmian weszła jeszcze do `sprint/3`. Raport z 9 listopada opisał ten etap.
   Jego dopisek wyjaśniał, że ekrany wymagały jeszcze logowania i ról. [Scalenie Sprintu 3][pr-s3], [raport pośredni][interim]
2. **Po 9 listopada.** Doszły logowanie, historia zgłoszeń, eksport CSV, inwentaryzacja i rozwinięcie mapy o plany i sale.
   [Porównanie z raportem pośrednim][delta]
3. **21 listopada.** Zespół scalił `sprint/4` do `main` z historią zmian.
   W opisie scalenia jest wykaz epików, do których przeniesiono otwarte prace. [Zamknięcie sprintu][pr-close]

Na koniec sprintu ustaliliśmy, które prace zostają na Sprint 5. Najważniejsza jest kolejka R1 i decyzja R2. Po ich podłączeniu druga osoba będzie mogła przyjąć nowe zgłoszenie z telefonu. Potrzebne są też porównanie i wybór wersji przy konflikcie, pobieranie zmian od kursora oraz zgłoszenie zmiany oznaczenia wejścia z ekranu C6. [Ekrany ról][epic-roles] · [Kolejka serwera][issue-queue] · [Ciąg dalszy prac serwera][epic-server]

Koordynatorowi pozostają zaproszenia K2 i pobranie eksportu z K3, w tym GeoJSON. Na pokaz potrzebujemy jeszcze sprawdzić pomiar GNSS w warunkach zbliżonych do terenu oraz wykonać scenariusz od zdjęcia do publikacji z osobnymi rolami. [GeoJSON][issue-geo] · [Zadania pokrycia][epic-tests]

Wynik sprintu to działający katalog dostępności i formularze terenowe z kolejką. W kolejnym sprincie priorytetem jest sprawdzenie, czy te części tworzą jedną drogę dla użytkownika, od zgłoszenia problemu do informacji widocznej dla odwiedzających.

## Źródła

- [Karta przedmiotu i programy studiów WIT PWr](https://wit.pwr.edu.pl/studenci/programy-studiow/2024-2025-studia-ii-stopnia)
- [Rewizja końcowa Sprintu 4][s4] i [opis scalenia][pr-close]

[pr-close]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3654
[pr-gnss]: https://github.com/dostepny-kampus/dostepny-kampus/pull/321
[pr-building]: https://github.com/dostepny-kampus/dostepny-kampus/pull/354
[pr-point]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1497
[pr-form]: https://github.com/dostepny-kampus/dostepny-kampus/pull/367
[pr-quick]: https://github.com/dostepny-kampus/dostepny-kampus/pull/375
[pr-quick-save]: https://github.com/dostepny-kampus/dostepny-kampus/pull/374
[pr-relogin]: https://github.com/dostepny-kampus/dostepny-kampus/pull/664
[pr-history]: https://github.com/dostepny-kampus/dostepny-kampus/pull/807
[pr-edit]: https://github.com/dostepny-kampus/dostepny-kampus/pull/772
[pr-proj]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1070
[pr-csv]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1981
[pr-stream]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3461
[pr-similar]: https://github.com/dostepny-kampus/dostepny-kampus/pull/366
[pr-login]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2899
[pr-session]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2813
[pr-owner]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2993
[pr-offline-map]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1136
[pr-card-barrier]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3360
[pr-public-photo]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2238
[pr-v3-ux]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3082
[pr-v3]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3130
[pr-entrances]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3158
[pr-plan]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2596
[pr-room-search]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3357
[pr-room]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3512
[pr-room-steps]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3517
[pr-marker-queue]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1978
[pr-c5]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2820
[pr-k1]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2448
[pr-progress]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3590
[pr-replace]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3616
[pr-removed]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2257
[pr-author]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1149
[pr-accept-quick]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2246
[pr-marking]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3595
[pr-android-modules]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1811
[pr-cycles]: https://github.com/dostepny-kampus/dostepny-kampus/pull/3553
[pr-camera]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2862
[pr-trace]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2849
[pr-style]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2777
[pr-agents]: https://github.com/dostepny-kampus/dostepny-kampus/pull/1348
[pr-size]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2660
[pr-pit]: https://github.com/dostepny-kampus/dostepny-kampus/pull/2415
[pr-s3]: https://github.com/dostepny-kampus/dostepny-kampus/pull/365
[epic-roles]: https://github.com/dostepny-kampus/dostepny-kampus/issues/3756
[issue-queue]: https://github.com/dostepny-kampus/dostepny-kampus/issues/3063
[epic-server]: https://github.com/dostepny-kampus/dostepny-kampus/issues/3636
[issue-geo]: https://github.com/dostepny-kampus/dostepny-kampus/issues/3062
[epic-tests]: https://github.com/dostepny-kampus/dostepny-kampus/issues/3637
[routes]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/android/app/src/main/kotlin/io/github/dostepnykampus/app/navigation/AppScreens.kt#L156
[pending]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/backend/src/test/java/io/github/dostepnykampus/RouteParityIT.java#L38
[review]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/backend/src/main/java/io/github/dostepnykampus/barrier/submission/web/ReviewController.java#L74
[csv-test]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/backend/src/test/java/io/github/dostepnykampus/barrier/ExportCsvHttpIT.java
[adr-modules]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/docs/docs/architektura/decyzje/0011-modularny-monolit.md
[system-upload]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/android/systemtest/src/main/kotlin/io/github/dostepnykampus/systemtest/SubmissionToServerTest.kt
[coverage]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/docs/scripts/check_ids.py
[demo]: https://github.com/dostepny-kampus/dostepny-kampus/blob/4294e4844/specs/scenariusz-demo.feature
[s4]: https://github.com/dostepny-kampus/dostepny-kampus/tree/4294e484455947974775e75ab7b9083e243b467f
[ci-docs]: https://github.com/dostepny-kampus/dostepny-kampus/actions/runs/44936310730
[ci-final]: https://github.com/dostepny-kampus/dostepny-kampus/actions/runs/44936310785
[ci-full]: https://github.com/dostepny-kampus/dostepny-kampus/actions/runs/44922547660
[ci-delta]: https://github.com/dostepny-kampus/dostepny-kampus/compare/d8f561d30...4294e4844
[interim]: https://github.com/dostepny-kampus/dostepny-kampus/blob/0efb42a26/docs/docs/raporty/sprint-4.md
[delta]: https://github.com/dostepny-kampus/dostepny-kampus/compare/0efb42a26...4294e4844
