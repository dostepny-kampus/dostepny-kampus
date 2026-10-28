# Tokeny wizualne

Token to nazwana wartość wyglądu: kolor, rozmiar tekstu, odstęp, wielkość celu dotyku. Ekrany używają nazw tokenów, a nie liczb, więc jedna zmiana w tokenie zmienia wszystkie ekrany naraz. Makiety zostają szare, bez koloru, a tokeny opisują wygląd aplikacji w fazie C.

| Co | Gdzie |
| --- | --- |
| Jedno źródło prawdy | plik `docs/ux/tokeny.yml` |
| Kontrola progów i tabel tej strony | `python scripts/kontrast.py --check-page docs/ux/tokeny.md`, krok „kontrast” w `scripts/verify.py` |
| Odświeżenie tabel po zmianie pliku | `python scripts/kontrast.py --page docs/ux/tokeny.md` |

Tabele w sekcjach „Kolory”, „Kontrast”, „Typografia” i „Wymiary” generuje skrypt z pliku YAML. Ręczna zmiana tabeli bez zmiany pliku nie przejdzie kontroli.

## Skąd styl

Wygląd aplikacji to styl „Karta miejsca”, wybrany w trzeciej rundzie wariantów wyglądu ekranu V2 (decyzja projektu nr 48, sposób wyboru: decyzja nr 42). Wzorem była karta miejsca w Mapach Google: duża nazwa, rząd chipów, białe kontenery grup na szarym arkuszu i czynności przypięte na dole.

Z wyborem stylu zmieniła się jedna zasada: oznaczenie wejścia może stać w spokojnym kontenerze, ale znaczenie niosą kształt ikony i słowo (decyzja 48 pkt 3).

## Zasady

Każda zasada wynika z wymagań albo z person. Grupa docelowa kursu to osoby starsze, więc najpierw liczy się PE-1.

| Zasada | Skutek w tokenach | Źródło |
| --- | --- | --- |
| Kolor nie niesie znaczenia. | Oznaczenie wejścia ma kształt ikony i słowo ze [słownika użytkownika](slownik-uzytkownika.md#metafory-i-ikony): koło ze znakiem ✓ „Dostępne”, ośmiokąt ze znakiem ✕ „Niedostępne”, romb ze znakiem ? „Brak danych”. Może stać w spokojnym kontenerze, a jego kolor tylko wzmacnia. Status zgłoszenia i uciążliwość nie mają kontenera: tekst i ikona w kolorze `onSurface`. | NFR-21, WCAG 1.4.1 [1], decyzja 48 |
| Bez „sygnalizacji świetlnej”. | W palecie interfejsu nie ma zielonego ani żółtego, więc nie ma pary zielony/czerwony. Kontener „Dostępne” jest morski, „Niedostępne” przygaszony szaroróżowy, a „Brak danych” szary. W odcieniach szarości odróżnia je kształt ikony i słowo. | NFR-21, wymagania wspólne fazy B, decyzja 48 |
| Jedna barwa akcentu: ciemny morski. | `primary` wyróżnia symbol budynku, jedyny przycisk wypełniony na ekranie, przyciski tekstowe i fokus. `primaryContainer` oznacza tylko to, co wybrane, np. piętro. Czynność główną wyróżnia też położenie i rozmiar. | [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne), Material 3 [5] |
| Daltonizm nie odbiera czytelności. | Protanopia i deuteranopia zacierają różnicę między czerwonym a zielonym, a oś morska zostaje czytelna. Skrypt liczy kontrast każdej pary także po symulacji obu typów [4]. | NFR-16, NFR-21 |
| Tekst ma kontrast co najmniej 7:1, a nie 4,5:1. | Próg poziomu AAA dla każdego tekstu, także drugorzędnego i błędu. Skrypt odrzuca próg niższy niż w NFR-16. | WCAG 1.4.6 [1], [2]; PE-1, PE-2, PE-3 |
| Ramki, ikony i fokus mają kontrast co najmniej 3:1. | `outline` i `primary` są widoczną granicą pola, ramki i przycisku. Obramowanie ma 1 dp, więc granicę wyznacza kontrast, a nie grubość. | NFR-16, WCAG 1.4.11 [1] |
| Błąd przy polu nie jest statusem. | Ciemna czerwień `error` służy tylko błędowi walidacji: tekstowi przy polu i nad formularzem oraz ramce pola. Pogrubiony tekst mówi, co się stało i co zrobić. Czerwień nigdy nie oznacza statusu, oznaczenia wejścia ani uciążliwości. Błąd serwera, zapisu i brak internetu to zwykły komunikat, bez czerwieni. | NFR-15 (WCAG 3.3.1), NFR-21, wymagania wspólne fazy B |
| Ciemny motyw dobieramy osobno. | Tło jest ciemnoszare zamiast czarnego, tekst jasnoszary zamiast białego, a akcent jaśniejszy i mniej nasycony. Każda para ma w ciemnym motywie własny wynik kontrastu. | Material, motyw ciemny [6] |
| Kolory systemowe z tapety są wyłączone. | Aplikacja nie przejmuje barw z tapety telefonu (Android 12+), bo nie znamy ich kontrastu [8]. | NFR-16 |

PE-1 słabo czyta drobny i blady tekst, zwłaszcza w słońcu, a PE-2 pracuje przed budynkiem w słońcu. Próg 7:1 według objaśnienia WCAG wyrównuje utratę wrażliwości na kontrast typową dla słabego wzroku [2]. Dlatego przyjmujemy go dla każdego tekstu, choć NFR-16 wymaga 4,5:1.

## Kolory

Nazwy ról pochodzą z Material 3 [5]. Plik zawiera tylko role, których potrzebują ekrany. Kontenery oznaczeń wejścia (`status…Container`) to role dodatkowe stylu „Karta miejsca”, nazwane jak role kontenerów Material 3. Kwadrat obok wartości to próbka koloru.

Plik nie ma koloru stanu nieaktywnego. Na makietach każdy przycisk działa zawsze, a brakujące dane wyjaśnia błąd przy polu (C2, C6).

Fokus klawiatury pokazuje ramka `primary` (WCAG 2.4.7), a ramkę fokusu TalkBack rysuje system.

<!-- kolory:start -->
<!-- vale off -->

| Rola                           | Jasny                                                                                                                                                             | Ciemny                                                                                                                                                            | Zastosowanie                                                                                                                         | Źródło                                      |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------- |
| `surface`                      | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#F5FAFC"></span> `#F5FAFC` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#161B1D"></span> `#161B1D` | nagłówek z polem wyszukiwania (V1), przypięty pasek czynności, pasek „Wstecz” po przewinięciu, tło pól tekstowych                    | NFR-16, styl „Karta miejsca”                |
| `surfaceContainerLowest`       | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFFFFF"></span> `#FFFFFF` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#1E2427"></span> `#1E2427` | kontenery grup („Wejścia”, „Winda i toaleta”, „Piętra”, stan pusty barier), pływające kontrolki mapy, legenda, chipy                 | NFR-16, styl „Karta miejsca”                |
| `surfaceContainerLow`          | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#EFF4F6"></span> `#EFF4F6` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#1A1F21"></span> `#1A1F21` | pole wyszukiwania, pasek dolny nawigacji                                                                                             | styl „Karta miejsca”                        |
| `surfaceContainer`             | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#E8EEF0"></span> `#E8EEF0` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#0F1314"></span> `#0F1314` | tło arkusza z kartą budynku (V2) i arkusza na mapie (V1)                                                                             | styl „Karta miejsca”                        |
| `surfaceVariant`               | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#DCE2E4"></span> `#DCE2E4` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#2A3033"></span> `#2A3033` | tło fragmentu mapy przed wczytaniem, ramka komunikatu i wskaźnika braku internetu                                                    | NFR-16, styl „Karta miejsca”                |
| `onSurface`                    | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#161C1E"></span> `#161C1E` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#E8EEF0"></span> `#E8EEF0` | tekst podstawowy, nazwa budynku, nagłówki grup, ikony i napisy uciążliwości oraz statusów zgłoszeń                                   | NFR-16, NFR-21                              |
| `onSurfaceVariant`             | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#3A4549"></span> `#3A4549` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#BCC9CD"></span> `#BCC9CD` | tekst drugorzędny (źródło i data, „Zalecane, parter”, podpisy), napisy i ikony paska dolnego                                         | NFR-16, US17.3                              |
| `outline`                      | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#6A7579"></span> `#6A7579` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#8A969A"></span> `#8A969A` | obramowanie pól (wyszukiwania i formularzy), ramek opcji, chipów, przycisków obrysowanych i segmentów                                | NFR-16 (WCAG 1.4.11)                        |
| `primary`                      | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#004E5E"></span> `#004E5E` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#85D1EA"></span> `#85D1EA` | symbol budynku, jedyny przycisk wypełniony na ekranie, przyciski tekstowe („Zgłoś problem”, „Pokaż na planie”), wskaźnik fokusu      | wymagania wspólne (czynność główna), NFR-16 |
| `onPrimary`                    | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFFFFF"></span> `#FFFFFF` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#002933"></span> `#002933` | napis i ikona na przycisku wypełnionym, np. „Sprawdź, którędy wejść”                                                                 | NFR-16                                      |
| `primaryContainer`             | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#C3E9F6"></span> `#C3E9F6` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#0E4655"></span> `#0E4655` | wybrane piętro, wybrany widok i pozycja paska dolnego, plakietka budynku                                                             | styl „Karta miejsca”                        |
| `onPrimaryContainer`           | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#001F27"></span> `#001F27` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#D3F2FC"></span> `#D3F2FC` | napis na wybranym elemencie i obwódka 2 dp wybranego piętra                                                                          | NFR-16                                      |
| `secondaryContainer`           | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#D5E7ED"></span> `#D5E7ED` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#2A434B"></span> `#2A434B` | przyciski tonalne, np. „Zgłoś barierę”, „Pokaż na mapie”                                                                             | styl „Karta miejsca”                        |
| `onSecondaryContainer`         | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#1C3640"></span> `#1C3640` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#D3EEF7"></span> `#D3EEF7` | napis i ikona przycisku tonalnego                                                                                                    | NFR-16                                      |
| `statusDostepneContainer`      | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#CDE8F0"></span> `#CDE8F0` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#1C4350"></span> `#1C4350` | tło oznaczenia „Dostępne” przy wejściu i chipu „Wejście dostępne”                                                                    | NFR-21, US2.1, decyzja 48                   |
| `onStatusDostepneContainer`    | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#00313C"></span> `#00313C` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#D5F2FB"></span> `#D5F2FB` | słowo „Dostępne” i ikona koła ze znakiem ✓                                                                                           | NFR-16, NFR-21                              |
| `statusNiedostepneContainer`   | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#F2DFE4"></span> `#F2DFE4` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#4A2733"></span> `#4A2733` | tło oznaczenia „Niedostępne” przy wejściu                                                                                            | NFR-21, US2.1, decyzja 48                   |
| `onStatusNiedostepneContainer` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#4A1B2A"></span> `#4A1B2A` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFDDE6"></span> `#FFDDE6` | słowo „Niedostępne” i ikona ośmiokąta ze znakiem ✕                                                                                   | NFR-16, NFR-21                              |
| `statusBrakDanychContainer`    | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#E3E5E6"></span> `#E3E5E6` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#33393C"></span> `#33393C` | tło oznaczenia „Brak danych” przy wejściu i w legendzie mapy                                                                         | NFR-21, US2.1, decyzja 48                   |
| `onStatusBrakDanychContainer`  | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#2A2E30"></span> `#2A2E30` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#E2E6E8"></span> `#E2E6E8` | słowo „Brak danych” i ikona rombu ze znakiem ?                                                                                       | NFR-16, NFR-21                              |
| `error`                        | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#8A0F18"></span> `#8A0F18` | <span style="display:inline-block;width:1.4em;height:1.4em;vertical-align:middle;border:1px solid #808080;border-radius:4px;background:#FFB3AE"></span> `#FFB3AE` | tekst błędu walidacji przy polu i nad formularzem, obramowanie pola z błędem; nigdy dla oznaczenia wejścia, statusu ani uciążliwości | NFR-15 (WCAG 3.3.1), NFR-21                 |

<!-- vale on -->
<!-- kolory:end -->

Podgląd poniżej pokazuje tokeny w obu motywach na fragmencie karty budynku z ekranu V2. Rozmiary w podglądzie są w pikselach równych sp i dp. Skrypt rysuje podgląd jeszcze starym układem, bez kontenerów oznaczeń, więc trzy oznaczenia wejść mają w nim ten sam kolor, a różni je napis i znak.

<!-- podglad:start -->
<!-- vale off -->

<div style="display:inline-block;vertical-align:top;box-sizing:border-box;width:100%;max-width:360px;margin:0 16px 16px 0;padding:16px;background:#F5FAFC;border:1px solid #808080;border-radius:8px"><div style="font-size:18px;line-height:28px;font-weight:500;color:#3A4549">Motyw jasny</div><div style="font-size:22px;line-height:28px;font-weight:600;color:#161C1E">Budynek A-1</div><div style="background:#DCE2E4;border:1px solid #6A7579;border-radius:8px;padding:8px;margin:8px 0"><div style="font-size:18px;line-height:28px;font-weight:400;color:#161C1E">✗ Niedostępne: Wejście główne</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#3A4549">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#161C1E">✓ Dostępne: Wejście bramą od ul. Norwida</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#3A4549">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#161C1E">– Brak danych: [nazwa wejścia]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#3A4549">Z map i opisów, stan z [data]</div></div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;background:#004E5E;color:#FFFFFF;font-weight:700">Sprawdź, którędy wejść</div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;border:1px solid #6A7579;color:#004E5E;font-weight:600">Zmień oznaczenie</div><div style="margin-top:16px"></div><div style="font-size:18px;line-height:28px;font-weight:700;color:#8A0F18">Dodaj zdjęcie wejścia, aby wysłać zmianę.</div><div style="margin-top:8px"></div><div style="font-size:18px;line-height:28px;font-weight:500;color:#3A4549">Źródło: Politechnika Wrocławska</div></div><div style="display:inline-block;vertical-align:top;box-sizing:border-box;width:100%;max-width:360px;margin:0 16px 16px 0;padding:16px;background:#161B1D;border:1px solid #808080;border-radius:8px"><div style="font-size:18px;line-height:28px;font-weight:500;color:#BCC9CD">Motyw ciemny</div><div style="font-size:22px;line-height:28px;font-weight:600;color:#E8EEF0">Budynek A-1</div><div style="background:#2A3033;border:1px solid #8A969A;border-radius:8px;padding:8px;margin:8px 0"><div style="font-size:18px;line-height:28px;font-weight:400;color:#E8EEF0">✗ Niedostępne: Wejście główne</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#BCC9CD">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#E8EEF0">✓ Dostępne: Wejście bramą od ul. Norwida</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#BCC9CD">Sprawdzone na miejscu [data]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#E8EEF0">– Brak danych: [nazwa wejścia]</div><div style="font-size:18px;line-height:28px;font-weight:400;color:#BCC9CD">Z map i opisów, stan z [data]</div></div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;background:#85D1EA;color:#002933;font-weight:700">Sprawdź, którędy wejść</div><div style="display:block;min-height:56px;line-height:56px;text-align:center;border-radius:8px;margin:16px 0 0 0;font-size:18px;border:1px solid #8A969A;color:#85D1EA;font-weight:600">Zmień oznaczenie</div><div style="margin-top:16px"></div><div style="font-size:18px;line-height:28px;font-weight:700;color:#FFB3AE">Dodaj zdjęcie wejścia, aby wysłać zmianę.</div><div style="margin-top:8px"></div><div style="font-size:18px;line-height:28px;font-weight:500;color:#BCC9CD">Źródło: Politechnika Wrocławska</div></div>

<!-- vale on -->
<!-- podglad:end -->

## Kontrast

Wzór pochodzi z WCAG: współczynnik to `(L1 + 0,05) / (L2 + 0,05)`. L1 to luminancja względna jaśniejszego koloru, a L2 ciemniejszego [3]. Kolumny „daltonizm” podają niższy wynik po symulacji protanopii albo deuteranopii [4]. Wyniki są obcięte w dół do dwóch miejsc, więc tabela nie zawyża kontrastu.

<!-- kontrast:start -->
<!-- vale off -->

| Para                                                           | Zastosowanie                                      | Próg  | Jasny   | Jasny, daltonizm | Ciemny  | Ciemny, daltonizm |
| -------------------------------------------------------------- | ------------------------------------------------- | ----- | ------- | ---------------- | ------- | ----------------- |
| `onSurface` na `surface`                                       | nagłówek V1, pasek czynności, pola                | ≥ 7:1 | 16,37:1 | 16,34:1          | 14,82:1 | 14,82:1           |
| `onSurface` na `surfaceContainer`                              | nazwa budynku i nagłówki grup na arkuszu          | ≥ 7:1 | 14,70:1 | 14,69:1          | 15,94:1 | 15,92:1           |
| `onSurface` na `surfaceContainerLowest`                        | treść w kontenerach grup, kontrolki mapy, legenda | ≥ 7:1 | 17,22:1 | 17,11:1          | 13,40:1 | 13,37:1           |
| `onSurface` na `surfaceContainerLow`                           | pole wyszukiwania, pasek dolny                    | ≥ 7:1 | 15,53:1 | 15,51:1          | 14,20:1 | 14,19:1           |
| `onSurface` na `surfaceVariant`                                | komunikat i wskaźnik braku internetu              | ≥ 7:1 | 13,16:1 | 13,15:1          | 11,42:1 | 11,37:1           |
| `onSurfaceVariant` na `surfaceContainer`                       | źródło i data na arkuszu                          | ≥ 7:1 | 8,42:1  | 8,32:1           | 11,01:1 | 10,93:1           |
| `onSurfaceVariant` na `surfaceContainerLowest`                 | „Zalecane, parter”, podpis mapy                   | ≥ 7:1 | 9,87:1  | 9,69:1           | 9,25:1  | 9,21:1            |
| `onSurfaceVariant` na `surfaceContainerLow`                    | podpowiedź w polu, napisy paska dolnego           | ≥ 7:1 | 8,90:1  | 8,78:1           | 9,80:1  | 9,75:1            |
| `onSurfaceVariant` na `surfaceVariant`                         | data przy wskaźniku braku internetu               | ≥ 7:1 | 7,54:1  | 7,45:1           | 7,88:1  | 7,86:1            |
| `primary` na `surfaceContainer`                                | symbol budynku na arkuszu                         | ≥ 7:1 | 7,95:1  | 7,43:1           | 10,95:1 | 10,45:1           |
| `primary` na `surfaceContainerLowest`                          | przyciski tekstowe w kontenerach grup             | ≥ 7:1 | 9,32:1  | 8,66:1           | 9,20:1  | 8,81:1            |
| `primary` na `surface`                                         | „Wstecz”, przycisk obrysowany                     | ≥ 7:1 | 8,86:1  | 8,27:1           | 10,18:1 | 9,73:1            |
| `onPrimary` na `primary`                                       | przycisk wypełniony                               | ≥ 7:1 | 9,32:1  | 8,66:1           | 9,01:1  | 8,79:1            |
| `onPrimaryContainer` na `primaryContainer`                     | wybrane piętro, plakietka budynku                 | ≥ 7:1 | 13,30:1 | 13,20:1          | 8,82:1  | 8,53:1            |
| `onSecondaryContainer` na `secondaryContainer`                 | przycisk tonalny                                  | ≥ 7:1 | 9,98:1  | 9,81:1           | 8,65:1  | 8,56:1            |
| `onStatusDostepneContainer` na `statusDostepneContainer`       | „Dostępne” i koło ze znakiem ✓                    | ≥ 7:1 | 10,89:1 | 10,68:1          | 9,10:1  | 8,88:1            |
| `onStatusNiedostepneContainer` na `statusNiedostepneContainer` | „Niedostępne” i ośmiokąt ze znakiem ✕             | ≥ 7:1 | 11,10:1 | 10,71:1          | 10,27:1 | 10,11:1           |
| `onStatusBrakDanychContainer` na `statusBrakDanychContainer`   | „Brak danych” i romb ze znakiem ?                 | ≥ 7:1 | 10,84:1 | 10,80:1          | 9,33:1  | 9,27:1            |
| `error` na `surface`                                           | błąd przy polu                                    | ≥ 7:1 | 9,23:1  | 8,00:1           | 10,18:1 | 9,22:1            |
| `error` na `surfaceContainerLowest`                            | błąd przy polu w kontenerze grupy                 | ≥ 7:1 | 9,71:1  | 8,45:1           | 9,20:1  | 8,31:1            |
| `outline` na `surface`                                         | obramowanie pola i przycisku obrysowanego         | ≥ 3:1 | 4,50:1  | 4,44:1           | 5,71:1  | 5,67:1            |
| `outline` na `surfaceContainer`                                | obramowanie chipu na arkuszu                      | ≥ 3:1 | 4,04:1  | 3,99:1           | 6,14:1  | 6,09:1            |
| `outline` na `surfaceContainerLowest`                          | obramowanie chipu w kontenerze                    | ≥ 3:1 | 4,73:1  | 4,65:1           | 5,16:1  | 5,14:1            |
| `outline` na `surfaceVariant`                                  | obramowanie ramki komunikatu                      | ≥ 3:1 | 3,61:1  | 3,58:1           | 4,40:1  | 4,38:1            |
| `primary` na `surface`                                         | granica przycisku wypełnionego, fokus             | ≥ 3:1 | 8,86:1  | 8,27:1           | 10,18:1 | 9,73:1            |

<!-- vale on -->
<!-- kontrast:end -->

Paleta mapy jest w sekcji `mapa` pliku: podkład, budynki PWr i plan piętra w obu motywach, z listą par kontrastu w tym samym zapisie. Znacznik wejścia ma ten sam kształt co oznaczenie na karcie (NFR-21).

Te pary mają te same progi (napisy 7:1, kontury i znaczniki 3:1) i spełniają je także po symulacji daltonizmu. Skrypt strony jeszcze ich nie sprawdza ani nie pokazuje w tabelach. Mapę nieba na C1 rysuje aplikacja, więc używa tylko ról z pliku, a system satelity rozróżnia kształt i podpis (US6.5).

## Typografia

Rozmiary podajemy w sp, więc tekst rośnie razem z ustawieniem czcionki w telefonie, do 200% (NFR-20).

Krój to `Atkinson Hyperlegible Next`: litery łatwe do pomylenia, np. I, l i 1, mają wyraźnie różne kształty, a krój powstał dla osób słabowidzących [9]. Licencja `SIL Open Font License` pozwala dołączyć plik czcionki do aplikacji. Jeśli czcionka nie zostanie wczytana, aplikacja pokaże tekst krojem systemowym.

Napisy paska dolnego mają 18 sp i mogą zawinąć się do dwóch wierszy pod ikoną, np. „Zgłoś barierę” i „Konto i praca”. Faza C sprawdza na zrzutach ekranu, czy napisy paska mieszczą się bez ucięcia przy czcionce 200% (NFR-20).

| Reguła | Wartość | Źródło |
| --- | --- | --- |
| Tekst podstawowy | 18 sp; 34 znaki w wierszu na ekranie 360 dp (konwencja makiet) | [konwencje makiet](makiety/index.md#konwencje), NFR-20 |
| Najmniejszy tekst w aplikacji | 16 sp, tylko napisy na mapie; każdy styl interfejsu ma co najmniej 18 sp (próg zespołu) | PE-1, PE-3 |
| Tekst duży według NFR-16 | od 24 sp albo od 19 sp pogrubiony; mimo to wymagamy 7:1 | NFR-16 |
| Interlinia tekstu podstawowego i podpisu | 28 sp przy 18 sp, czyli 1,56 rozmiaru (zalecane ≥ 1,5) | WCAG 1.4.8 [1], decyzja 50 |
| Pogrubienie | waga 700 dla przycisków, oznaczeń wejścia i błędu przy polu | styl „Karta miejsca” |

<!-- typografia:start -->
<!-- vale off -->

| Styl             | Rozmiar | Waga | Interlinia | Zastosowanie                                                                    | Źródło                                                       |
| ---------------- | ------- | ---- | ---------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| `displayLarge`   | 57 sp   | 800  | 57 sp      | symbol budynku na karcie, np. „A-1”                                             | styl „Karta miejsca”                                         |
| `headlineMedium` | 28 sp   | 700  | 36 sp      | nazwa miejsca, np. „Gmach Główny”, „Sala 329”                                   | styl „Karta miejsca”, NFR-16 (duży tekst ≥ 24 sp)            |
| `titleLarge`     | 22 sp   | 600  | 28 sp      | nagłówek grupy, np. „Wejścia”, „Bariery w budynku”                              | styl „Karta miejsca”, NFR-16 (duży tekst ≥ 19 sp pogrubiony) |
| `titleMedium`    | 20 sp   | 700  | 26 sp      | nazwa wejścia, udogodnienia albo piętra w kontenerze grupy                      | styl „Karta miejsca”, NFR-16 (duży tekst ≥ 19 sp pogrubiony) |
| `bodyLarge`      | 18 sp   | 400  | 28 sp      | tekst podstawowy, np. „Drzwi 120 cm”, opis piętra, kroki do sali                | konwencje makiet, NFR-20, PE-1                               |
| `labelLarge`     | 18 sp   | 600  | 24 sp      | napis przycisku i chipu; przyciski i oznaczenia wejścia mają wagę wyróżnienia   | styl „Karta miejsca”, NFR-17                                 |
| `bodyMedium`     | 18 sp   | 500  | 28 sp      | podpis, np. źródło i data oceny wejść, „Zalecane, parter”, podpis OpenStreetMap | styl „Karta miejsca”, US17.3, NFR-37, NFR-40                 |
| `labelMedium`    | 18 sp   | 500  | 20 sp      | napisy pod ikonami paska dolnego, w razie potrzeby w dwóch wierszach            | styl „Karta miejsca”, wymagania wspólne (pasek dolny)        |

<!-- vale on -->
<!-- typografia:end -->

## Wymiary

Odstępy leżą na siatce 4 dp [5], a skrypt sprawdza wielokrotność. Cel dotyku ma co najmniej 48 dp (NFR-17), a przycisk z napisem 56 dp, bo PE-1 ma lekkie drżenie dłoni. Przyciski przypięte nad paskiem dolnym mają 60 dp. Zaokrąglenia idą za skalą kształtów Material 3 [10], a kontrolki 48 dp mają kształt kapsuły.

<!-- wymiary:start -->
<!-- vale off -->

| Grupa        | Token                         | Wartość | Zastosowanie                                                                                                       | Źródło                                                   |
| ------------ | ----------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------- |
| Odstępy      | `odstepy.xs`                  | 4 dp    | odstęp ikony od napisu pod nią w pasku dolnym                                                                      | styl „Karta miejsca”                                     |
| Odstępy      | `odstepy.s`                   | 8 dp    | odstęp między chipami i między ikoną a napisem w chipie                                                            | styl „Karta miejsca”                                     |
| Odstępy      | `odstepy.m`                   | 16 dp   | margines boczny ekranu, wcięcie treści w kontenerze grupy, odstęp pozycji w grupie                                 | konwencje makiet, styl „Karta miejsca”                   |
| Odstępy      | `odstepy.l`                   | 28 dp   | odstęp między grupami arkusza                                                                                      | styl „Karta miejsca”                                     |
| Cele dotyku  | `cele_dotyku.minimum`         | 48 dp   | najmniejszy cel dotyku, np. przycisk tekstowy „Zgłoś problem”, chip, kontrolka mapy, piętro w przełączniku         | NFR-17                                                   |
| Cele dotyku  | `cele_dotyku.przycisk`        | 56 dp   | najmniejsza wysokość przycisku wypełnionego, tonalnego i obrysowanego; przy dużej czcionce rośnie razem z tekstem  | NFR-17, NFR-20, PE-1                                     |
| Cele dotyku  | `cele_dotyku.przypiety`       | 60 dp   | wysokość przycisków przypiętych nad paskiem dolnym, np. „Sprawdź, którędy wejść” i „Pokaż na mapie” (V2)           | styl „Karta miejsca”, NFR-17                             |
| Cele dotyku  | `cele_dotyku.odstep`          | 16 dp   | odstęp między sąsiednimi przyciskami, np. w przypiętym pasie akcji i w potwierdzeniach                             | NFR-17, PE-1, decyzja 50                                 |
| Cele dotyku  | `cele_dotyku.odstep_w_grupie` | 8 dp    | odstęp między celami jednej grupy, np. chipami statusu i segmentami przełącznika pięter                            | styl „Karta miejsca”, wytyczne Material dla celów dotyku |
| Obramowania  | `obramowania.ramka`           | 1 dp    | obramowanie kontenerów i kart, dzielniki, chipy statusu, przyciski obrysowane                                      | styl „Karta miejsca”, NFR-16 (WCAG 1.4.11), decyzja 50   |
| Obramowania  | `obramowania.pole`            | 2 dp    | obramowanie pól tekstowych (wyszukiwanie, formularze C1–C4), ramek opcji do wyboru i segmentów przełącznika pięter | NFR-16 (WCAG 1.4.11), PE-1, PE-2, decyzja 50             |
| Obramowania  | `obramowania.wybrane`         | 2 dp    | obwódka wybranego piętra w przełączniku, w kolorze `onPrimaryContainer`                                            | styl „Karta miejsca”, NFR-21                             |
| Obramowania  | `obramowania.fokus`           | 3 dp    | wskaźnik fokusu klawiatury, w kolorze `primary`, 2 dp od elementu; ramkę fokusu TalkBack rysuje system             | NFR-15 (WCAG 2.4.7), NFR-16                              |
| Zaokrąglenia | `zaokraglenia.standard`       | 8 dp    | oznaczenie wejścia, chipy, podpis OpenStreetMap na mapie                                                           | styl „Karta miejsca” (Material 3, kształt mały)          |
| Zaokrąglenia | `zaokraglenia.srednie`        | 12 dp   | plakietka budynku na liście, tło piktogramu udogodnienia                                                           | styl „Karta miejsca” (Material 3, kształt średni)        |
| Zaokrąglenia | `zaokraglenia.duze`           | 16 dp   | kontenery grup, legenda, kroki do sali, plakietka „A-1” na arkuszu                                                 | styl „Karta miejsca” (Material 3, kształt duży)          |
| Zaokrąglenia | `zaokraglenia.kapsula`        | 24 dp   | kontrolki 48 dp, np. „Wstecz” na mapie, przyciski +/−, piętra w przełączniku, segmenty, małe przyciski             | styl „Karta miejsca”                                     |
| Zaokrąglenia | `zaokraglenia.bardzo_duze`    | 28 dp   | przyciski 56 dp i przypięte, pole wyszukiwania, przełącznik pięter, górne rogi arkusza                             | styl „Karta miejsca” (Material 3, kształt bardzo duży)   |
| Ikony        | `ikony.rozmiar`               | 24 dp   | ikona oznaczenia wejścia, chipu, przycisku i paska dolnego, zawsze z napisem obok                                  | NFR-21, słownik użytkownika (Metafory i ikony)           |

<!-- vale on -->
<!-- wymiary:end -->

Wysokość przycisku to minimum. Gdy czcionka rośnie do 200%, przycisk rośnie razem z napisem i napis się nie ucina (NFR-20).

Między sąsiednimi przyciskami jest 16 dp, bo PE-1 ma lekkie drżenie dłoni. Cele jednej grupy, np. chipy statusu, dzieli 8 dp [7].

Kontenery, karty, dzielniki i przyciski obrysowane mają ramkę 1 dp. Pola tekstowe, ramki opcji do wyboru i segmenty mają ramkę 2 dp, bo dotyka się ich w terenie, w słońcu (PE-1, PE-2).

Wysokości elementów (cień) są w sekcji `wysokosci` pliku, w poziomach Material 3 [11]. Pływające kontrolki mapy mają 3 dp, a przypięty pasek czynności 6 dp. W ciemnym motywie cień słabo widać, więc element pływający ma też obwódkę 1 dp. Skrypt strony jeszcze nie pokazuje tej sekcji w tabelach.

## Jak faza C używa pliku

| Część pliku | Co powstaje w aplikacji |
| --- | --- |
| `kolory.<rola>.jasny` i `kolory.<rola>.ciemny` | dwa schematy kolorów motywu Material 3: jasny i ciemny; pozostałe role schematu (np. `secondary`, `tertiary`, `surfaceContainerHigh`) dostają wartości z ról z pliku, a test porównuje schemat z plikiem, żeby żaden komponent nie użył domyślnego koloru Material |
| `typografia.kroj`, `typografia.style` | krój dołączony do aplikacji i skala typografii motywu: rozmiar i interlinia w sp, waga |
| `odstepy`, `cele_dotyku`, `obramowania`, `zaokraglenia`, `ikony` | stałe wymiarów w dp, używane przez ekrany zamiast liczb |
| `wysokosci` | cień elementów pływających i przypiętych, w dp |
| `mapa` | kolory stylu mapy i planu piętra; nie trafiają do motywu Material 3 |
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
9. [Atkinson Hyperlegible Next: repozytorium kroju, licencja SIL Open Font License 1.1](https://github.com/googlefonts/atkinson-hyperlegible-next)
10. [Material Design 3: skala zaokrągleń](https://m3.material.io/styles/shape/corner-radius-scale)
11. [Material Design 3: poziomy wysokości](https://m3.material.io/styles/elevation/tokens)

Dostęp do źródeł: 28.10.2026.
