# Tokeny wizualne

Token to nazwana wartość wyglądu: kolor, rozmiar tekstu, odstęp, wielkość celu dotyku. Ekrany używają nazw tokenów, a nie liczb, więc jedna zmiana w tokenie zmienia wszystkie ekrany naraz. Makiety zostają szare, bez koloru, a tokeny opisują wygląd aplikacji w fazie C.

| Co | Gdzie |
| --- | --- |
| Jedno źródło prawdy | plik `docs/ux/tokeny.yml` |
| Kontrola progów i tabel tej strony | `python scripts/kontrast.py --check-page docs/ux/tokeny.md`, krok „kontrast” w `scripts/verify.py` |
| Odświeżenie tabel po zmianie pliku | `python scripts/kontrast.py --page docs/ux/tokeny.md` |

Tabele w sekcjach „Kolory”, „Kontrast”, „Typografia” i „Wymiary” generuje skrypt z pliku YAML. Ręczna zmiana tabeli bez zmiany pliku nie przejdzie kontroli.

## Zasady

Każda zasada wynika z wymagań albo z person. Grupa docelowa kursu to osoby starsze, więc najpierw liczy się PE-1.

| Zasada | Skutek w tokenach | Źródło |
| --- | --- | --- |
| Kolor nie niesie znaczenia. | Status zgłoszenia, oznaczenie wejścia i uciążliwość mają tekst i ikonę ze [słownika użytkownika](slownik-uzytkownika.md#metafory-i-ikony), w kolorze zwykłego tekstu `onSurface`. Kolor najwyżej wzmacnia, np. wypełnienie czynności głównej. | NFR-21, WCAG 1.4.1 [1] |
| Bez „sygnalizacji świetlnej”. | W palecie nie ma zielonego ani żółtego. „Dostępne”, „Niedostępne” i „Brak danych” wyglądają tak samo poza napisem i ikoną; tak samo „Utrudnienie”, „Tylko z pomocą” i „Blokada” oraz wszystkie statusy zgłoszeń. | NFR-21, wymagania wspólne fazy B |
| Jedna barwa akcentu: niebieski. | `primary` wyróżnia czynność główną, fokus i wybrane pole wyboru. Czynność główną wyróżnia przede wszystkim położenie i rozmiar. | [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne), Material 3 [5] |
| Daltonizm nie odbiera czytelności. | Protanopia i deuteranopia zacierają różnicę między czerwonym a zielonym, a niebieski zostaje czytelny. Skrypt liczy kontrast każdej pary także po symulacji obu typów [4]. | NFR-16, NFR-21 |
| Tekst ma kontrast co najmniej 7:1, a nie 4,5:1. | Próg poziomu AAA dla każdego tekstu, także drugorzędnego i błędu. Skrypt odrzuca próg niższy niż w NFR-16. | WCAG 1.4.6 [1], [2]; PE-1, PE-2, PE-3 |
| Ramki, ikony i fokus mają kontrast co najmniej 3:1. | `outline` i `primary` są widoczną granicą pola, ramki i przycisku. Obramowanie ma 2 dp. | NFR-16, WCAG 1.4.11 [1] |
| Błąd przy polu nie jest statusem. | Ciemna czerwień `error` służy tylko błędowi walidacji: tekstowi przy polu i nad formularzem oraz ramce pola. Pogrubiony tekst mówi, co się stało i co zrobić. Czerwień nigdy nie oznacza statusu, oznaczenia wejścia ani uciążliwości. Błąd serwera, zapisu i brak internetu to zwykły komunikat, bez czerwieni. | NFR-15 (WCAG 3.3.1), NFR-21, wymagania wspólne fazy B |
| Ciemny motyw dobieramy osobno. | Tło jest ciemnoszare zamiast czarnego, tekst jasnoszary zamiast białego, a akcent jaśniejszy i mniej nasycony. Każda para ma w ciemnym motywie własny wynik kontrastu. | Material, motyw ciemny [6] |
| Kolory systemowe z tapety są wyłączone. | Aplikacja nie przejmuje barw z tapety telefonu (Android 12+), bo nie znamy ich kontrastu [8]. | NFR-16 |

PE-1 słabo czyta drobny i blady tekst, zwłaszcza w słońcu, a PE-2 pracuje przed budynkiem w słońcu. Próg 7:1 według objaśnienia WCAG wyrównuje utratę wrażliwości na kontrast typową dla słabego wzroku [2]. Dlatego przyjmujemy go dla każdego tekstu, choć NFR-16 wymaga 4,5:1.

## Kolory

Nazwy ról pochodzą z Material 3 [5]. Plik zawiera tylko role, których potrzebują ekrany z makiet. Kwadrat obok wartości to próbka koloru.

Plik nie ma koloru stanu nieaktywnego. Na makietach każdy przycisk działa zawsze, a brakujące dane wyjaśnia błąd przy polu (C2, C6).

Fokus klawiatury pokazuje ramka `primary` (WCAG 2.4.7), a ramkę fokusu TalkBack rysuje system.

<!-- kolory:start -->
<!-- vale off -->

| Rola               | Jasny                                                                                                                                                             | Ciemny                                                                                                                                                            | Zastosowanie                                                                                                                         | Źródło                                      |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------- |
| `surface`          | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFFFFF"></span> `#FFFFFF` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#16181B"></span> `#16181B` | tło ekranu, pól tekstowych i paska dolnego                                                                                           | NFR-16, PE-1, PE-2                          |
| `onSurface`        | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#1A1C1E"></span> `#1A1C1E` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#E4E5E7"></span> `#E4E5E7` | tekst podstawowy, nagłówki, ikony i napisy statusów, oznaczeń wejść i uciążliwości                                                   | NFR-16, NFR-21                              |
| `surfaceVariant`   | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#EDF0F4"></span> `#EDF0F4` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#262A2F"></span> `#262A2F` | tło ramek pozycji listy, ramki komunikatu i wskaźnika braku internetu                                                                | NFR-16                                      |
| `onSurfaceVariant` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#3D4248"></span> `#3D4248` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#C4C8CE"></span> `#C4C8CE` | tekst drugorzędny (data i źródło przy wejściu, podpisy), ikony w pasku dolnym, zawsze z napisem                                      | NFR-16, US17.3                              |
| `outline`          | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#6B7178"></span> `#6B7178` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#8C929A"></span> `#8C929A` | obramowanie pól, ramek pozycji listy, przycisków drugorzędnych i pól wyboru                                                          | NFR-16 (WCAG 1.4.11)                        |
| `primary`          | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#0A4680"></span> `#0A4680` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#A9C9F5"></span> `#A9C9F5` | wypełnienie czynności głównej, napis przycisku drugorzędnego, wskaźnik fokusu, wybrane pole wyboru                                   | wymagania wspólne (czynność główna), NFR-16 |
| `onPrimary`        | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFFFFF"></span> `#FFFFFF` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#0A2B4D"></span> `#0A2B4D` | napis i ikona na czynności głównej                                                                                                   | NFR-16                                      |
| `error`            | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#8A0F18"></span> `#8A0F18` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFB3AE"></span> `#FFB3AE` | tekst błędu walidacji przy polu i nad formularzem, obramowanie pola z błędem; nigdy dla statusu, oznaczenia wejścia ani uciążliwości | NFR-15 (WCAG 3.3.1), NFR-21                 |

<!-- vale on -->
<!-- kolory:end -->

Podgląd poniżej pokazuje tokeny w obu motywach na fragmencie karty budynku z ekranu V2. Rozmiary w podglądzie są w pikselach równych sp i dp. Trzy oznaczenia wejść mają ten sam kolor, a różni je napis i znak.

<!-- podglad:start -->
<!-- vale off -->

<div style="display:inline-block;vertical-align:top;box-sizing:border-box;width:100%;max-width:360px;margin:0 16px 16px 0;padding:16px;background:#FFFFFF;border:1px solid #808080;border-radius:8px"><div style="font-size:16px;line-height:24px;font-weight:400;color:#3D4248">Motyw jasny</div><div style="font-size:24px;line-height:32px;font-weight:700;color:#1A1C1E">Budynek A-1</div><div style="background:#EDF0F4;border:2px solid #6B7178;border-radius:8px;padding:8px;margin:8px 0"><div style="font-size:18px;line-height:28px;font-weight:400;color:#1A1C1E">✗ Niedostępne: Wejście główne</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#3D4248">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#1A1C1E">✓ Dostępne: Wejście bramą od ul. Norwida</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#3D4248">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#1A1C1E">– Brak danych: [nazwa wejścia]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#3D4248">Z map i opisów, stan z [data]</div></div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;background:#0A4680;color:#FFFFFF;font-weight:700">Sprawdź, którędy wejść</div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;border:2px solid #6B7178;color:#0A4680;font-weight:500">Zmień oznaczenie</div><div style="margin-top:16px"></div><div style="font-size:18px;line-height:28px;font-weight:700;color:#8A0F18">Dodaj zdjęcie wejścia, aby wysłać zmianę.</div><div style="margin-top:8px"></div><div style="font-size:16px;line-height:24px;font-weight:400;color:#3D4248">Źródło: Politechnika Wrocławska</div></div><div style="display:inline-block;vertical-align:top;box-sizing:border-box;width:100%;max-width:360px;margin:0 16px 16px 0;padding:16px;background:#16181B;border:1px solid #808080;border-radius:8px"><div style="font-size:16px;line-height:24px;font-weight:400;color:#C4C8CE">Motyw ciemny</div><div style="font-size:24px;line-height:32px;font-weight:700;color:#E4E5E7">Budynek A-1</div><div style="background:#262A2F;border:2px solid #8C929A;border-radius:8px;padding:8px;margin:8px 0"><div style="font-size:18px;line-height:28px;font-weight:400;color:#E4E5E7">✗ Niedostępne: Wejście główne</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#C4C8CE">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#E4E5E7">✓ Dostępne: Wejście bramą od ul. Norwida</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#C4C8CE">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#E4E5E7">– Brak danych: [nazwa wejścia]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#C4C8CE">Z map i opisów, stan z [data]</div></div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;background:#A9C9F5;color:#0A2B4D;font-weight:700">Sprawdź, którędy wejść</div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;border:2px solid #8C929A;color:#A9C9F5;font-weight:500">Zmień oznaczenie</div><div style="margin-top:16px"></div><div style="font-size:18px;line-height:28px;font-weight:700;color:#FFB3AE">Dodaj zdjęcie wejścia, aby wysłać zmianę.</div><div style="margin-top:8px"></div><div style="font-size:16px;line-height:24px;font-weight:400;color:#C4C8CE">Źródło: Politechnika Wrocławska</div></div>

<!-- vale on -->
<!-- podglad:end -->

## Kontrast

Wzór pochodzi z WCAG: współczynnik to `(L1 + 0,05) / (L2 + 0,05)`. L1 to luminancja względna jaśniejszego koloru, a L2 ciemniejszego [3]. Kolumny „daltonizm” podają niższy wynik po symulacji protanopii albo deuteranopii [4]. Wyniki są obcięte w dół do dwóch miejsc, więc tabela nie zawyża kontrastu.

<!-- kontrast:start -->
<!-- vale off -->

| Para                                   | Zastosowanie                       | Próg  | Jasny   | Jasny, daltonizm | Ciemny  | Ciemny, daltonizm |
| -------------------------------------- | ---------------------------------- | ----- | ------- | ---------------- | ------- | ----------------- |
| `onSurface` na `surface`               | tekst podstawowy i nagłówki        | ≥ 7:1 | 17,09:1 | 17,04:1          | 14,11:1 | 14,09:1           |
| `onSurface` na `surfaceVariant`        | tekst w ramce pozycji i komunikatu | ≥ 7:1 | 14,95:1 | 14,94:1          | 11,45:1 | 11,38:1           |
| `onSurfaceVariant` na `surface`        | data i źródło                      | ≥ 7:1 | 10,13:1 | 10,03:1          | 10,58:1 | 10,56:1           |
| `onSurfaceVariant` na `surfaceVariant` | data i źródło w ramce              | ≥ 7:1 | 8,86:1  | 8,80:1           | 8,59:1  | 8,58:1            |
| `onPrimary` na `primary`               | napis czynności głównej            | ≥ 7:1 | 9,54:1  | 8,70:1           | 8,43:1  | 8,40:1            |
| `primary` na `surface`                 | napis przycisku drugorzędnego      | ≥ 7:1 | 9,54:1  | 8,70:1           | 10,46:1 | 10,18:1           |
| `primary` na `surfaceVariant`          | przycisk drugorzędny w ramce       | ≥ 7:1 | 8,34:1  | 7,64:1           | 8,49:1  | 8,29:1            |
| `error` na `surface`                   | błąd przy polu                     | ≥ 7:1 | 9,71:1  | 8,45:1           | 10,42:1 | 9,46:1            |
| `error` na `surfaceVariant`            | błąd przy polu w ramce             | ≥ 7:1 | 8,49:1  | 7,37:1           | 8,46:1  | 7,65:1            |
| `outline` na `surface`                 | obramowanie pola i ramki           | ≥ 3:1 | 4,93:1  | 4,87:1           | 5,66:1  | 5,64:1            |
| `outline` na `surfaceVariant`          | obramowanie pola wewnątrz ramki    | ≥ 3:1 | 4,31:1  | 4,28:1           | 4,60:1  | 4,59:1            |
| `primary` na `surface`                 | granica czynności głównej          | ≥ 3:1 | 9,54:1  | 8,70:1           | 10,46:1 | 10,18:1           |

<!-- vale on -->
<!-- kontrast:end -->

Kontroli skryptem nie podlega mapa ani plan piętra: znaczniki barier leżą na obrazie podkładu OpenStreetMap albo planu PWr. Znacznik ma kształt i napis (NFR-21), a jego kontrast z podkładem sprawdza faza C na zrzutach ekranu. Mapę nieba na C1 rysuje aplikacja, więc używa tylko ról z pliku, a system satelity rozróżnia kształt i podpis (US6.5).

## Typografia

Rozmiary podajemy w sp, więc tekst rośnie razem z ustawieniem czcionki w telefonie, do 200% (NFR-20). Krój jest systemowy: PE-1 zachowuje czcionkę ustawioną w telefonie, a aplikacja nie dołącza pliku czcionki.

Napisy paska dolnego mają 16 sp i mogą zawinąć się do dwóch wierszy pod ikoną, np. „Zgłoś barierę” i „Konto i praca”. Faza C sprawdza na zrzutach ekranu, czy napisy paska mieszczą się bez ucięcia przy czcionce 200% (NFR-20).

| Reguła | Wartość | Źródło |
| --- | --- | --- |
| Tekst podstawowy | 18 sp; 34 znaki w wierszu na ekranie 360 dp (konwencja makiet) | [konwencje makiet](makiety/index.md#konwencje), NFR-20 |
| Najmniejszy tekst w aplikacji | 16 sp, żaden styl nie jest mniejszy (próg zespołu) | PE-1, PE-3 |
| Tekst duży według NFR-16 | od 24 sp albo od 19 sp pogrubiony; mimo to wymagamy 7:1 | NFR-16 |
| Interlinia tekstu podstawowego | około 1,5 rozmiaru (28 sp przy 18 sp) | WCAG 1.4.8 [1] |
| Pogrubienie | waga 700 dla czynności głównej i błędu przy polu | konwencje makiet |

<!-- typografia:start -->
<!-- vale off -->

| Styl          | Rozmiar | Waga | Interlinia | Zastosowanie                                                         | Źródło                                           |
| ------------- | ------- | ---- | ---------- | -------------------------------------------------------------------- | ------------------------------------------------ |
| `titleLarge`  | 24 sp   | 700  | 32 sp      | nagłówek ekranu, np. „Budynek A-1”                                   | NFR-16 (duży tekst ≥ 24 sp)                      |
| `titleMedium` | 20 sp   | 700  | 28 sp      | nagłówek sekcji, np. „Bariery w budynku”                             | NFR-16 (duży tekst ≥ 19 sp pogrubiony)           |
| `bodyLarge`   | 18 sp   | 400  | 28 sp      | tekst podstawowy, oznaczenia, statusy, data i źródło przy wejściu    | konwencje makiet, NFR-20, PE-1                   |
| `labelLarge`  | 18 sp   | 500  | 24 sp      | napis przycisku; czynność główna ma wagę wyróżnienia                 | konwencje makiet, NFR-17                         |
| `bodyMedium`  | 16 sp   | 400  | 24 sp      | podpis źródła pod planem piętra, podpis OpenStreetMap na mapie       | NFR-37, NFR-40, minimum zespołu                  |
| `labelMedium` | 16 sp   | 500  | 22 sp      | napisy pod ikonami paska dolnego, w razie potrzeby w dwóch wierszach | wymagania wspólne (pasek dolny), minimum zespołu |

<!-- vale on -->
<!-- typografia:end -->

## Wymiary

Odstępy leżą na siatce 4 dp [5], a skrypt sprawdza wielokrotność. Cel dotyku ma co najmniej 48 dp (NFR-17), a przycisk z napisem 56 dp, bo PE-1 ma lekkie drżenie dłoni.

<!-- wymiary:start -->
<!-- vale off -->

| Grupa        | Token                   | Wartość | Zastosowanie                                                                                           | Źródło                                         |
| ------------ | ----------------------- | ------- | ------------------------------------------------------------------------------------------------------ | ---------------------------------------------- |
| Odstępy      | `odstepy.xs`            | 4 dp    | odstęp ikony od napisu                                                                                 | siatka 4 dp                                    |
| Odstępy      | `odstepy.s`             | 8 dp    | odstęp tekstu od ramki wewnątrz pozycji listy                                                          | siatka 4 dp                                    |
| Odstępy      | `odstepy.m`             | 16 dp   | margines boczny ekranu, odstęp między przyciskami i pozycjami listy                                    | konwencje makiet, PE-1 (drżenie dłoni)         |
| Odstępy      | `odstepy.l`             | 24 dp   | odstęp między sekcjami ekranu                                                                          | siatka 4 dp                                    |
| Cele dotyku  | `cele_dotyku.minimum`   | 48 dp   | najmniejszy cel dotyku, także ikona paska dolnego i pole wyboru z etykietą                             | NFR-17                                         |
| Cele dotyku  | `cele_dotyku.przycisk`  | 56 dp   | najmniejsza wysokość przycisku z napisem; przy dużej czcionce rośnie razem z tekstem                   | NFR-17, NFR-20, PE-1                           |
| Cele dotyku  | `cele_dotyku.odstep`    | 8 dp    | najmniejszy odstęp między dwoma osobnymi celami dotyku; między przyciskami stosujemy odstęp m          | wytyczne Material dla celów dotyku             |
| Obramowania  | `obramowania.ramka`     | 2 dp    | obramowanie pól, ramek pozycji listy i przycisków drugorzędnych                                        | NFR-16 (WCAG 1.4.11)                           |
| Obramowania  | `obramowania.fokus`     | 3 dp    | wskaźnik fokusu klawiatury, w kolorze `primary`, 2 dp od elementu; ramkę fokusu TalkBack rysuje system | NFR-15 (WCAG 2.4.7), NFR-16                    |
| Zaokrąglenia | `zaokraglenia.standard` | 8 dp    | przyciski, pola, ramki pozycji i komunikatów                                                           | heurystyka Nielsena 4 (spójność)               |
| Ikony        | `ikony.rozmiar`         | 24 dp   | ikona statusu, oznaczenia i paska dolnego, zawsze z napisem obok                                       | NFR-21, słownik użytkownika (Metafory i ikony) |

<!-- vale on -->
<!-- wymiary:end -->

Wysokość przycisku to minimum. Gdy czcionka rośnie do 200%, przycisk rośnie razem z napisem i napis się nie ucina (NFR-20). Odstęp między dwoma osobnymi celami to co najmniej 8 dp [7]. Między przyciskami stosujemy 16 dp.

## Jak faza C używa pliku

| Część pliku | Co powstaje w aplikacji |
| --- | --- |
| `kolory.<rola>.jasny` i `kolory.<rola>.ciemny` | dwa schematy kolorów motywu Material 3: jasny i ciemny; pozostałe role schematu (np. `secondaryContainer` w pasku dolnym) dostają wartości z ról z pliku, a test porównuje schemat z plikiem, żeby żaden komponent nie użył domyślnego koloru Material |
| `typografia.style` | skala typografii motywu: rozmiar i interlinia w sp, waga |
| `odstepy`, `cele_dotyku`, `obramowania`, `zaokraglenia`, `ikony` | stałe wymiarów w dp, używane przez ekrany zamiast liczb |
| `kontrast.pary` | lista par do sprawdzenia w podglądach ekranów i w teście dostępności |

Faza C generuje motyw z pliku, a nie przepisuje wartości ręcznie. Zmiana koloru zaczyna się od pliku YAML i kontroli skryptem, a dopiero potem trafia do aplikacji.

## Źródła

1. [WCAG 2.1 (W3C): 1.4.1, 1.4.3, 1.4.6, 1.4.8, 1.4.11](https://www.w3.org/TR/WCAG21/)
2. [Understanding WCAG 2.1: Contrast (Enhanced) 1.4.6](https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html)
3. [WCAG 2.1: definicja luminancji względnej i współczynnika kontrastu](https://www.w3.org/TR/WCAG21/#dfn-relative-luminance)
4. [Machado, Oliveira, Fernandes (2009): A Physiologically-based Model for Simulation of Color Vision Deficiency](https://www.inf.ufrgs.br/~oliveira/pubs_files/CVD_Simulation/CVD_Simulation.html)
5. [Material Design 3: role kolorów](https://m3.material.io/styles/color/roles)
6. [Material Design: motyw ciemny](https://m2.material.io/design/color/dark-theme.html)
7. [Material Design: dostępność, rozmiar i odstęp celów dotyku](https://m2.material.io/design/usability/accessibility.html)
8. [Android: kolory dynamiczne (Android 12+)](https://developer.android.com/develop/ui/views/theming/dynamic-colors)

Dostęp do źródeł: 28.10.2026.
