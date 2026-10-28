# Makiety

Makiety low-fi pokazują układ i treść każdego ekranu z [wymagań do ekranów](../wymagania-ekranow.md). Rysujemy je w PlantUML Salt: szare ramki, tekst i przyciski, bez koloru i stylu. Napisy pochodzą ze [słownika użytkownika](../slownik-uzytkownika.md) i są hipotezą do sprawdzenia w wywiadach.

## Konwencje

| Temat | Zasada |
| --- | --- |
| Element | Na makiecie jest tylko element z tabeli elementów ekranu w wymaganiach. Element bez kryterium, reguły albo NFR usuwamy (decyzja 16). |
| Nazwa pliku | `m-<ekran>-<nazwa>[-<stan>].puml`, małe litery, bez polskich znaków, np. `m-v2-karta-budynku.puml`. Nazwa jest unikalna w całym `docs/`. |
| Stan w nazwie | Stan zwykły nie ma dopisku. Pozostałe: `-brak-sieci`, `-pusty`, `-blad`, `-ladowanie`, `-brak-uprawnien`, a inne stany skrótem nazwy z tabeli stanów. Wariant roli: `-zbierajacy`, `-koordynator`. |
| Stany | Rysujemy tylko stany, które wyglądają inaczej niż już narysowane. Stan, który różni się jednym napisem, opisujemy w tabeli pod makietą. |
| Blok | `@startsalt` bez nazwy, potem `!include _wspolne.iuml`. Treść ekranu stoi między `$ekran_poczatek()` i `$ekran_koniec()`. |
| Fragmenty | Nagłówek, wskaźnik braku internetu, pasek dolny, komunikat stanu, oznaczenia i przyciski tylko z fragmentów. Ręczna kopia rozjeżdża się przy pierwszej zmianie słownika. |
| Szerokość | Ramka ma 266 px, skala 2, więc każdy PNG ma 532 px szerokości. Szerszy PNG znaczy, że jakiś wiersz wychodzi poza ekran telefonu. |
| Tekst | Wiersz ma najwyżej 34 znaki: tyle mieści tekst 18 sp na ekranie 360 dp. Dłuższy tekst zawija `$tekst("…")`. |
| Wysokość | Makieta pokazuje cały przewijany ekran: nagłówek na górze, pasek dolny na dole. |
| Cel dotyku | Salt nie oddaje wysokości 48 dp (NFR-17). Makiety sprawdzają proporcje i obecność celu, a wysokość celu sprawdza faza C: podgląd ekranu i test dostępności. |
| Ikony | Tylko ikony z tabeli „Metafory i ikony” słownika użytkownika, zawsze z tekstem obok, przez funkcje fragmentów. `[X]` w Salt to pole wyboru, a nie ikona statusu. |
| Kolor | Bez koloru: nie używamy `<color>` ani `<back>`. Czynność główna jest pogrubiona i ma pełną szerokość (`$przycisk_glowny`). |
| Zastępniki | Zdjęcie, plan, mapę i dane z bazy rysujemy kursywą w nawiasach, np. „[zdjęcie bariery]”. To nie są napisy aplikacji. |
| Przykłady | Budynek A-1 z wejściami jak w deklaracji dostępności [1]. Dane przykładowe nie mogą przeczyć faktom. Daty, nazwy i etykiety, których nie znamy, są zastępnikami, np. „[data]”. W argumencie `$tekst` zastępnik nie ma spacji w środku. |
| Strona | Obraz `../../_generated/<nazwa>.svg`, a pod nim tabela „Element → AC/BR/NFR” z odwołaniami z wymagań do ekranów. |
| Render | `python scripts/render.py`. Skrypt sprawdza tylko datę plików `.puml`, więc po zmianie `_wspolne.iuml` uruchamiamy go z `--force`. |

## Fragmenty wspólne

Plik `_wspolne.iuml` ma rozszerzenie `.iuml`, więc skrypt `render.py` nie traktuje go jak osobnego diagramu. Procedury PlantUML (`!procedure`, `!function`) działają wewnątrz `@startsalt`, co sprawdziliśmy w wersji 1.2026.9.

| Fragment | Co rysuje | Odwołanie |
| --- | --- | --- |
| `$naglowek("Tytuł", "podtytuł")` | „Wstecz” bez ikony i tytuł ekranu; podtytuł opcjonalny | wymagania wspólne, NFR-15 |
| `$naglowek_startowy("Mapa kampusu")` | sam tytuł, tylko na V1 | wymagania wspólne |
| `$brak_internetu("Dane z <i>[data]</i>")` | „Brak internetu” z ikoną i dopiskiem o wieku danych; bez argumentu sam napis | US9.1, NFR-10, NFR-21 |
| `$pasek_dolny_konto()` | „Mapa”, „Zgłoś barierę”, „Pomoc”, „Konto” | wymagania wspólne, US14 |
| `$pasek_dolny_praca()` | to samo z „Konto i praca” | wymagania wspólne, US14.1 |
| `$komunikat("Treść.", "Przycisk")` | ramka z komunikatem i najwyżej dwoma przyciskami tej samej wielkości | NFR-15, NFR-50 |
| `$wejscie($dostepne(), "Nazwa", "Źródło", "Zmień oznaczenie")` | oznaczenie, nazwa wejścia, data i źródło; przycisk opcjonalny | US2.1, US17.1, US17.3 |
| `$pozycja("Tytuł", $blokada(), "Dopisek", "Przycisk")` | pozycja listy z oznaczeniem tekstem i ikoną | NFR-21 |
| `$przycisk_glowny("…")`, `$przycisk("…", "camera-slr")` | przycisk na pełną szerokość, z ikoną albo bez | NFR-17, NFR-21 |
| `$tekst("…")`, `$sekcja("…")`, `$obraz("…")` | tekst zawinięty, nagłówek sekcji, zastępnik obrazu | NFR-20 |
| `$dostepne()` i pozostałe funkcje oznaczeń | napis z ikoną: oznaczenie wejścia, uciążliwość, status, znacznik | NFR-21, US17.1, US8.1, US15.1 |

![Katalog fragmentów wspólnych: nagłówek, wskaźnik braku internetu, wejścia, pozycja listy, oznaczenia, komunikat, przyciski i dwa warianty paska dolnego](../../_generated/m-wspolne-fragmenty.svg)

Katalog to podgląd fragmentów dla autorów, a nie ekran aplikacji. Trzy pułapki sprawdzone w praktyce:

| Pułapka | Skutek | Jak unikać |
| --- | --- | --- |
| Słowa `@startsalt` albo `@endsalt` w pliku dołączanym, nawet w komentarzu | PlantUML bierze tylko tekst między nimi i zgłasza błąd | nie wpisywać ich w `_wspolne.iuml` |
| Parametr procedury o nazwie innej procedury, np. `$przycisk` | wywołanie trafia w parametr, a nie w procedurę | nazwy parametrów inne niż nazwy procedur |
| Pionowa kreska w tekście | Salt dzieli wiersz na komórki | nie używać jej w napisach |

## Lista ekranów

Każda grupa ma własną stronę z makietami. Strony powstaną w kolejnym kroku zadania.

| Ekran | Nagłówek na ekranie | Grupa i strona |
| --- | --- | --- |
| V1 | „Mapa kampusu” | odwiedzający, `odwiedzajacy.md` |
| V2 | „Budynek C-13” | odwiedzający, `odwiedzajacy.md` |
| V3 | „Którędy wejść do C-13” | odwiedzający, `odwiedzajacy.md` |
| V4 | „Zgłoś barierę” | odwiedzający, `odwiedzajacy.md` |
| V5 | „Jak korzystać z aplikacji” | odwiedzający, `odwiedzajacy.md` |
| L1 | „Konto” | odwiedzający, `odwiedzajacy.md` |
| L2 | „Konto” albo „Konto i praca” | odwiedzający, `odwiedzajacy.md` |
| C1 | „Pomiar pozycji” | zespół w terenie, `zespol-w-terenie.md` |
| C2 | „Formularz zgłoszenia” | zespół w terenie, `zespol-w-terenie.md` |
| C3 | „Moje zgłoszenia” | zespół w terenie, `zespol-w-terenie.md` |
| C4 | „Synchronizacja” | zespół w terenie, `zespol-w-terenie.md` |
| C5 | „Obejrzane piętra” | zespół w terenie, `zespol-w-terenie.md` |
| C6 | „Zmień oznaczenie wejścia” | zespół w terenie, `zespol-w-terenie.md` |
| C7 | „Zgłoś, że bariery nie ma” | zespół w terenie, `zespol-w-terenie.md` |
| R1 | „Zgłoszenia do sprawdzenia” | weryfikacja i koordynacja, `weryfikacja-koordynacja.md` |
| R2 | „Sprawdzenie zgłoszenia” | weryfikacja i koordynacja, `weryfikacja-koordynacja.md` |
| K1 | „Postęp inwentaryzacji” | weryfikacja i koordynacja, `weryfikacja-koordynacja.md` |
| K2 | „Zaproś osobę” | weryfikacja i koordynacja, `weryfikacja-koordynacja.md` |
| K3 | „Pobierz eksport” | weryfikacja i koordynacja, `weryfikacja-koordynacja.md` |

## Wzorcowa makieta: V2 Karta budynku

Stan zwykły, budynek szczegółowy, Gość niezalogowany. Budynek A-1 ma trzy wejścia, po jednym w każdym oznaczeniu.

Wejście główne ma schody, a dostępne jest wejście z tyłu budynku, bramą od ulicy Norwida [1]. Trzecie wejście to przykład oznaczenia „Brak danych”.

![Makieta V2 Karta budynku: nagłówek Budynek A-1, trzy wejścia z oznaczeniami Dostępne, Niedostępne i Brak danych, przycisk Sprawdź, którędy wejść, wybór piętra, plan piętra, dwie bariery i pasek dolny](../../_generated/m-v2-karta-budynku.svg)

| Element | AC/BR/NFR |
| --- | --- |
| „Wstecz” i nagłówek „Budynek A-1” | US2, NFR-15 (wymagania wspólne) |
| Nazwa budynku z danych, zastępnik | US2 |
| Wejście: oznaczenie tekstem i ikoną na początku, potem nazwa wejścia; „Wejście główne” niedostępne, „Wejście bramą od ul. Norwida” dostępne [1]; nazwa odróżnia je od drugiego wejścia przy ul. Norwida, które według deklaracji nie jest dostosowane | US2.1, US17.1, NFR-21 |
| „Brak danych” z ikoną `minus`, inną niż `x` przy „Niedostępne” | US17.1 |
| Pod oznaczeniem data i źródło: „Sprawdzone na miejscu [data]”, „Z map i opisów, stan z [data]” | US17.3, BR-24 |
| „Sprawdź, którędy wejść”: czynność główna, pogrubiona, na pełną szerokość, zaraz pod wejściami | US3, US3.2, NFR-21 |
| „Piętro:” i pięć pól wyboru, jak w przykładzie BR-21; etykiety z danych PWr jako zastępniki; wybrane piętro widać po kształcie pola | US16.1, BR-21, NFR-21 |
| „Plan piętra”, zastępnik planu wybranego piętra | US16.1 |
| „Źródło: Politechnika Wrocławska” pod planem | NFR-40 |
| „Bariery w budynku”: typ, uciążliwość tekstem i ikoną, zdjęcie, „Sprawdzone [data]”; pierwsza bariera to schody przy wejściu głównym (TB-1, „Blokada”) [1] | US2.2, BR-17, NFR-21 |
| Druga bariera: typ i uciążliwość jako zastępniki, bo deklaracja nie podaje innej bariery A-1 | US2.2, BR-7, NFR-21 |
| Pasek dolny z „Konto” | US1, US4, US5.1, US14 (wymagania wspólne) |

Gość nie widzi przycisków Zbierającego, więc karta jest krótka (US14.1). Wariant dla RO-ZBI to osobny plik z dopiskiem `-zbierajacy`. Wskaźnik braku internetu pojawia się dopiero w stanie braku sieci.

## Źródła

1. Deklaracja dostępności budynku A-1 (wejście główne ze schodami, wejście dostępne z tyłu budynku, bramą od strony K.C. Norwida; drugie wejście przy ul. C.K. Norwida niedostosowane dla osób z problemami motorycznymi): <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow/budynki-a/a1>
2. PlantUML Salt: <https://plantuml.com/salt>
3. Preprocesor PlantUML: <https://plantuml.com/preprocessing>

Dostęp do źródeł: 28.10.2026.
