# Słownik użytkownika

!!! warning "Napisy to hipoteza, do potwierdzenia w wywiadach"
    Wszystkie napisy i komunikaty na tej stronie to propozycje zespołu. Sprawdzimy je w wywiadach ze [scenariusza](scenariusz-wywiadu.md) i w prototypie papierowym, a potem poprawimy.

Strona zamienia pojęcia dokumentacji na napisy w aplikacji. Gość i Odwiedzający dostają słowa zrozumiałe dla osoby starszej, bo dla nich projektujemy najpierw ([PE-1](persony.md)). Role zespołu mogą widzieć krótkie pojęcia ze [słownika pojęć](../biznes/slownik.md).

Słownik pojęć zostaje źródłem nazw w dokumentach i w kodzie. Ta strona ustala tylko to, co osoba czyta na ekranie. Robocze etykiety pochodzą z [wymagań do ekranów](wymagania-ekranow.md), a zmiany w nich zbiera sekcja [Propozycje do wymagań ekranów](#propozycje).

## Zasady napisów

| Zasada | Przykład | Dlaczego |
| --- | --- | --- |
| Przycisk czynności zaczyna się od czasownika w trybie rozkazującym. | „Zrób zdjęcie”, „Sprawdź, którędy wejść” | Osoba wie, co się stanie po dotknięciu. |
| Przycisk do listy albo ekranu nosi nazwę tego miejsca, taką jak nagłówek ekranu docelowego. | „Moje zgłoszenia” prowadzi do ekranu „Moje zgłoszenia” | Osoba sprawdza, czy trafiła tam, gdzie chciała (NFR-15, WCAG 3.2.4). |
| Wyjątki od trybu rozkazującego to utarte zwroty rezygnacji i przejścia, kierunki przesuwania mapy i odpowiedzi na pytanie z ekranu. | „Nie teraz”, „Dalej”, „Zdecyduję później”, „Poza budynkiem”, „Coś innego” | Brzmią naturalnie, a forma rozkazująca byłaby sztuczna. |
| Aplikacja mówi o sobie „my”, do osoby „Ty”, a zaimki pisze wielką literą. | „Wyślemy je, gdy wróci internet.” | Jeden głos na wszystkich ekranach. |
| Komunikat mówi, co się stało i co zrobić, w najwyżej 2 krótkich zdaniach. | „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” | NFR-15 (WCAG 3.3.1, 3.3.3), NFR-50 [1] |
| Komunikat nie obwinia osoby. | „Nie znaleźliśmy budynku” zamiast „Błędny symbol” | PE-1 przypisuje winę sobie i rezygnuje ([persony](persony.md)), a obwinianie siebie to częsta reakcja użytkowników [2]. |
| Dla Gościa i Odwiedzającego nie ma żargonu technicznego. | „Bez internetu” zamiast „tryb offline” | Lista zakazanych słów jest w tabeli niżej. |
| Bez anglicyzmów. | „Plik”, „Pobierz” | Wyjątek: „© OpenStreetMap contributors”, bo podpis wymaga NFR-37. |
| Jedno słowo ma jedno znaczenie w całej aplikacji. | „niedostępne” tylko o wejściu | Osoba nie myli wejścia z brakiem planu albo zdjęcia. |
| Daty jako 12.11.2026, liczby cyframi. | „3 kroki”, „Krok 2 z 3” | Cyfry czyta się szybciej niż słowa. |
| Jednoliterowe słowo, np. „i”, „w” albo „z”, nie zostaje na końcu wiersza: po nim stoi twarda spacja. | „Konto i praca” łamie się jako „Konto / i praca” | Zasada polskiego składu. Przy dużej czcionce wiersz zaczyna się od słowa, które niesie znaczenie. |

Słowa, których Gość i Odwiedzający nie widzą:

| Pojęcie dokumentacji | Zamiast niego |
| --- | --- |
| synchronizacja | „dane z 12.11.2026” |
| tryb offline, sieć | „Bez internetu” |
| GNSS, GPS, pozycja | „miejsce”, „gdzie jesteś” |
| status zgłoszenia | brak; Odwiedzający widzi tylko potwierdzenie (US4.4) |
| eksport | brak; ekran tylko dla Koordynatora |
| serwer | brak; komunikaty mówią o internecie |
| zgłoszenie (rzeczownik) | „zdjęcie bariery”, „bariera” |
| weryfikacja | „sprawdzenie”, „Sprawdzone 12.11.2026” |
| karta budynku | „opis budynku” |
| import, obchód | „Z map i opisów”, „Sprawdzone na miejscu” |
| uruchomienie | „otwarcie” |

## Napisy wspólne (hipoteza, do potwierdzenia w wywiadach)

Kolumna „Do sprawdzenia w wywiadzie” podaje pytanie, przy którym słuchamy słów uczestnika. Scenariusz zabrania pokazywania szkiców. Dlatego same napisy sprawdzimy dopiero w drugiej rozmowie (pytanie 6.3) i w prototypie papierowym.

| Pojęcie w dokumentacji | Napis w aplikacji | Dla kogo (rola) | Dlaczego tak | Do sprawdzenia w wywiadzie (nr pytania) |
| --- | --- | --- | --- | --- |
| pasek dolny: mapa kampusu | „Mapa” | wszystkie | Jedno słowo, znane z każdej aplikacji map. | 1.3, część 5 |
| pasek dolny: zgłoszenie szybkie | „Zgłoś barierę” | wszystkie | Czasownik mówi, co się stanie. Rzeczownik „zgłoszenie” znika. | 4.5, 4.6 |
| pasek dolny: „Pomoc” | „Pomoc” | wszystkie | bez zmian | 4.7 |
| pasek dolny: „Konto” | „Konto” | Gość, Odwiedzający | bez zmian | 4.7, 4.8 |
| pasek dolny: „Konto” z pracą ról | „Konto i praca” | Zbierający, Weryfikator, Koordynator | Rozstrzygnięcie sporu z recenzji, sekcja [Nazwa wejścia do pracy zespołu](#nazwa-wejscia). | 10.1, 6.3 |
| przycisk „Wstecz” | „Wstecz” ze strzałką w lewo | wszystkie | Strzałka i słowo, zawsze w tym samym miejscu. Strzałkę „wstecz” osoba zna z telefonu, a słowo mówi, co się stanie (PW-11). | obserwacja w części 3 |
| wskaźnik braku sieci | jedna linia „Bez internetu · dane z 12.11.2026”; Zbierający zamiast „dane z …” widzi „Ostatnia synchronizacja: 12.11.2026, 10:42” | wszystkie | „Internet” zna każdy, a „sieć” i „offline” to żargon. Data mówi, że dane w telefonie mogą być nieaktualne (decyzja 54). | 3.2 |
| data ostatniej synchronizacji | „dane z 12.11.2026”, po „Bez internetu ·” | Gość, Odwiedzający | Mówi, jak stare są dane, bez słowa „synchronizacja”. | 2.5, 4.3 |
| data ostatniej synchronizacji | „Ostatnia synchronizacja: 12.11.2026, 10:42” | Zbierający | Krótkie pojęcie dokumentacji. PE-2 ma wysokie umiejętności cyfrowe. | 10.3 |
| „Wróć do mapy” | „Wróć do mapy” | wszystkie | bez zmian | 4.8 |
| „Spróbuj ponownie” | „Spróbuj ponownie” | wszystkie | bez zmian | 3.2 |

## Napisy dla Gościa i Odwiedzającego (hipoteza, do potwierdzenia w wywiadach)

| Pojęcie w dokumentacji | Napis w aplikacji | Dla kogo (rola) | Dlaczego tak | Do sprawdzenia w wywiadzie (nr pytania) |
| --- | --- | --- | --- | --- |
| mapa kampusu (nagłówek V1) | „Mapa kampusu” | wszystkie | bez zmian | 1.3 |
| symbol budynku (pole V1) | „Wpisz symbol budynku, np. C-13” | wszystkie | Symbol wisi na budynku. Przykład pokazuje zapis, a czasownik mówi, co zrobić. | 1.3, część 5 |
| przełącznik „Mapa / Lista budynków” | „Mapa” i „Lista budynków”; przy skali czcionki co najmniej 1,5 widoczny napis „Lista”, a TalkBack czyta „Lista budynków” | wszystkie | Lista pomaga osobie, której mapa sprawia kłopot, i czytnikowi TalkBack. | część 5 |
| przyciski „+” i „−” | „Powiększ” i „Pomniejsz” | wszystkie | Sam znak „+” nie mówi, co powiększy. | obserwacja w części 3 |
| przyciski przesuwania mapy | strzałki na mapie; nazwa dla TalkBack „Przesuń mapę w górę”, „…w dół”, „…w lewo”, „…w prawo” | wszystkie | Cztery strzałki w krzyżu na mapie, każda co najmniej 48 dp. Kierunek widać po położeniu i strzałce, a pełną nazwę czyta TalkBack (PW-11). PE-1 nie lubi przeciągania. | obserwacja w części 3 |
| karta budynku (nagłówek V2) | symbol dużym napisem, np. „A-1”, a pod nim nazwa z danych; bez nazwy „Budynek C-4”; TalkBack czyta „Budynek A-1, Gmach Główny” | wszystkie | Osoba szuka budynku, a nie „karty”. | 4.4 |
| karta budynku (w zdaniach) | „opis budynku” | Gość, Odwiedzający | Tego słowa używa pytanie 4.4 scenariusza. „Karta” kojarzy się z kartą płatniczą (hipoteza). | 4.4 |
| oznaczenie wejścia „dostępne” | „Dostępne” | wszystkie | Słowo z kryterium US2.1, krótkie. „Bez schodów” byłoby nieprawdą przy wejściu z podnośnikiem (recenzja B3, uwaga 12). | 4.1, 4.2, część 5 |
| oznaczenie wejścia „niedostępne” | „Niedostępne” | wszystkie | Przeciwieństwo „Dostępne”. To słowo nie pada w innym znaczeniu. | 4.1, 4.2 |
| oznaczenie wejścia „brak danych” | „Brak danych” | wszystkie | Mówi uczciwie, że nie wiemy, i różni się od „Niedostępne” (US17.1). | 4.3, 4.4 |
| wejście zalecane (V2, V3, BR-25) | „Zalecane” | wszystkie | Jedno słowo obok oznaczenia mówi, którym wejściem wejść (decyzja 48). Napis nie podaje piętra, żeby etykieta była krótka; piętro podają kroki do sali i opis wejścia (decyzje 53 i 64). | 4.1 |
| wejście bez nazwy w danych | „Wejście bez nazwy w danych” | wszystkie | Mówi, skąd brak nazwy, zamiast zmyślać nazwę. | 4.4 |
| chipy na V2 | „Wejście dostępne”, „Winda”, „Toaleta dostosowana” | wszystkie | Chip stoi tylko przy fakcie z danych, nigdy przy braku (decyzja 48). | 4.1, 4.2 |
| zgłoszenie szybkie przy wejściu (V2, V3) | „Zgłoś barierę przy wejściu”; nazwa dla TalkBack „Zgłoś barierę przy wejściu: »nazwa wejścia«”; na kroku 1 V4 „Wejście: »nazwa wejścia«” | wszystkie; Gość przechodzi do L1 | Zaczyna się nazwą ekranu V4, a koniec odróżnia ten przycisk od pozycji „Zgłoś barierę” w pasku dolnym: wejście będzie już wybrane (WCAG 2.4.6, 2.5.3). | 4.6 |
| wybór budynku przy konturze wspólnym (V1) | nagłówek „Który budynek?”; przycisk na każdy symbol, np. „Budynek C-3” i „Budynek C-4”, z nazwą z danych, gdy jest; „Zamknij” | wszystkie | Pytanie mówi, co wybrać. Przycisk ma ten sam wzór co wynik wyszukiwania (decyzja 85). | 1.3 |
| przejścia z V1 i V2 | „Pokaż na mapie”, „Pokaż na planie”, „Pokaż opis budynku”, „Pokaż wyjścia ewakuacyjne (2)”, „Legenda” | wszystkie | Czasownik i to, co się pokaże. | 4.4, 6.3 |
| wejście na mapie (V2, V3) | „Pokaż wejście na mapie”; nazwa dla TalkBack „Pokaż wejście na mapie: »nazwa wejścia«” | wszystkie | Inny cel niż „Pokaż na mapie” przy budynku, więc inne słowa (WCAG 2.4.6, decyzja 53). | 4.4 |
| wejście dostępne (zdanie na V3) | „Budynek A-1 ma wejście dostępne: »nazwa wejścia«.” | wszystkie | bez zmian; zdanie powtarza słowo z oznaczenia | 4.1 |
| źródło oznaczenia „obchód” z datą | „Sprawdzone na miejscu 12.11.2026” | wszystkie | Mówi, że ktoś tam był. To buduje zaufanie (N2). | 2.5, 4.3, 4.4 |
| źródło oznaczenia „import” z datą | „Z map i opisów, stan z 1.10.2026” | wszystkie | Import bierze dane z planów PWr i z OpenStreetMap. „Dane uczelni” byłyby nieprawdą przy OpenStreetMap (recenzja B3, uwaga 23). | 2.2, 4.3 |
| data weryfikacji | „Sprawdzone 12.11.2026” | wszystkie | „Weryfikacja” to żargon, a data budzi zaufanie (N2). | 2.5, 4.4, 5.1 |
| zgłoszenie przyjęte (listy na V1–V3) | nagłówek „Bariery w budynku”, w wierszu typ bariery | Gość, Odwiedzający | Osoba widzi bariery, a nie zgłoszenia. | 2.3, 4.5 |
| typ bariery | nazwa z klasyfikacji, np. „niesprawna winda” | wszystkie | Nazwy są potoczne. Wyjątki są w 3 wierszach niżej. | 2.3, 4.2 |
| typ „toaleta niedostępna w praktyce” (TB-20) | „toaleta zamknięta, zastawiona lub trudna do dojścia” | Gość, Odwiedzający | Słowo „niedostępne” zostaje dla wejścia. | 2.3 |
| typ „brak planu tyflograficznego” (TB-22) | „brak planu dotykowego lub głosowego” | Gość, Odwiedzający | „Tyflograficzny” to słowo specjalistów. | 2.3, 5.1 |
| typ „niedostępne miejsce obsługi” (TB-28) | „brak obniżonej lady lub miejsca dla wózka” | Gość, Odwiedzający | Słowo „niedostępne” zostaje dla wejścia. | 2.3 |
| uciążliwość 1 „utrudnienie” | „Utrudnienie” | wszystkie | bez zmian | 2.3, 4.2 |
| uciążliwość 2 „poważne utrudnienie” | „Tylko z pomocą” | wszystkie | Dosłownie z kryterium stopnia 2. Brzmi jak warunek, a nie jak wezwanie pomocy albo przycisk „Pomoc”. Słowo „poważne” każdy rozumie inaczej. | 2.4, 4.2 |
| uciążliwość 3 „blokada” | „Blokada” | wszystkie | bez zmian; krótkie i jednoznaczne | 4.2 |
| przełącznik pięter | „Piętro:” i etykiety z danych PWr | wszystkie | Bez słowa „przełącznik”. | 6.3 |
| plan piętra (V1) i opis pięter (V2) | grupa „Piętra”, przy piętrze „Pokaż na planie” | wszystkie | Plan jest na mapie, a karta opisuje piętra słowami (decyzja 44). | 6.3 |
| podpis pod planem (NFR-40) | „Źródło: Politechnika Wrocławska” | wszystkie | bez zmian; mówi, skąd jest plan | 2.2 |
| budynek podstawowy (napis na V2, C1 i C2) | „Nie mamy planu pięter tego budynku.” | wszystkie | Słowo „niedostępny” zostaje dla wejścia. Ten sam napis na każdym ekranie, na którym brakuje planu (BR-G1). | 6.3 |
| przycisk do ekranu V3 (US3) | „Sprawdź, którędy wejść” | wszystkie | Dawna robocza nazwa brzmiała jak rezerwacja terminu. Ekran odpowiada na pytanie o wejście. | 2.1, 2.2, 4.1 |
| nagłówek V3 | „Którędy wejść do C-13” | wszystkie | Nagłówek powtarza napis przycisku. | 4.1, część 5 |
| zgłoszenie szybkie (nagłówek V4) | „Zgłoś barierę” | Odwiedzający | Ten sam napis co na pasku. | 4.6 |
| zgłoszenie (rzeczownik w komunikatach) | „zdjęcie bariery”, „bariera” | Gość, Odwiedzający | Osoba robi zdjęcie i to widzi. Plan fazy B zakazuje tu słowa „zgłoszenie”. | 3.4, 4.6 |
| „Krok 1 z 3” | „Krok 1 z 3” | Odwiedzający | bez zmian | 4.8 |
| przypomnienie o wizerunku | „Nie fotografuj twarzy ani tablic rejestracyjnych.” | Odwiedzający, Zbierający | bez zmian | 3.4 |
| komunikat ostrzegawczy (V4, krok 1) | „Ważne” nad przypomnieniem o wizerunku, pod nim „Zdjęcie zobaczą inni użytkownicy.” | Odwiedzający, Zbierający | Słowo i koło ze znakiem „!” mówią, że to ważne, bez samego koloru, a drugie zdanie mówi, dlaczego (decyzja 54). | 3.4 |
| „Zrób zdjęcie” | „Zrób zdjęcie” | Odwiedzający, Zbierający | bez zmian | 3.4 |
| „Zrób ponownie” | „Zrób nowe zdjęcie” | Odwiedzający, Zbierający | Mówi, że nowe zdjęcie zastąpi stare. | 3.4 |
| grupa barier (pytanie w kroku 2) | „Czego dotyczy bariera?” | Odwiedzający | Pytanie zamiast słowa „grupa”. | 4.5 |
| grupa „wejście i drzwi” | „Wejście i drzwi” | Odwiedzający | bez zmian | 2.3, 4.2 |
| grupa „komunikacja pionowa” | „Schody i windy” | Odwiedzający | „Komunikacja” kojarzy się z autobusem. | 2.3, 4.2 |
| grupa „komunikacja pozioma” | „Przejścia i chodniki” | Odwiedzający | Nazywa miejsca, w których są progi i krawężniki. | 2.3, 4.2 |
| grupa „toalety” | „Toalety” | Odwiedzający | bez zmian | 2.3 |
| grupa „orientacja i informacja” | „Napisy i oznaczenia” | Odwiedzający | „Orientacja” to słowo z przepisów. | 2.3, 5.1 |
| grupa „inne” | „Coś innego” | Odwiedzający | Brzmi jak odpowiedź na pytanie z kroku 2. | 4.5 |
| piętro (pytanie w kroku 2, US4.5) | „Na którym piętrze?” | Odwiedzający | Pytanie zamiast samego słowa „piętro”. | 4.5 |
| puste piętro (odpowiedź w kroku 2) | „Nie wiem” | Odwiedzający | Odpowiedź na pytanie z ekranu; nie zmusza do zgadywania. | 4.5 |
| piętro 0 w budynku podstawowym (krok 2) | „Parter” | Odwiedzający | Osoba starsza nie liczy pięter od zera. W budynku szczegółowym są etykiety z danych PWr. | 4.5 |
| bieżąca pozycja (krok 3) | „Dołączymy miejsce, w którym teraz stoisz.” | Odwiedzający | bez zmian | 3.4 |
| „Wyślij” | „Wyślij” | Odwiedzający | bez zmian | 3.4 |
| „Anuluj zgłoszenie” | „Zrezygnuj” | Odwiedzający | Bez rzeczownika „zgłoszenie”. | 4.8 |
| zgoda na lokalizację | „Pozwól na lokalizację”, „Pozwól na dokładną lokalizację” | Odwiedzający, Zbierający | Słowo „lokalizacja” widać w prośbie systemu i w ustawieniach telefonu (hipoteza, do sprawdzenia na telefonie testowym). | 3.2, 3.3 |
| „Otwórz ustawienia” | „Otwórz ustawienia” | Odwiedzający, Zbierający | bez zmian | 3.3 |
| instrukcja V5 (nagłówek) | „Jak korzystać z aplikacji” | wszystkie | Mówi, czego dotyczy pomoc. | 4.7 |
| objaśnienie „Dostępne” (V5) | zdanie z katalogu komunikatów (V5, zdanie o wejściu) | wszystkie | PE-1 szuka „wejścia bez schodów”, a samo „Dostępne” można odczytać jako „otwarte”. Zdanie objaśnia słowo raz, bez zmiany napisu (PW-15). | 4.1, 4.2, część 5 |
| „Dalej”, „Zacznij” | „Dalej”, „Zacznij” | wszystkie | bez zmian | 4.7 |
| „Pomiń” | „Pomiń instrukcję” | wszystkie | Samo „Pomiń” nie mówi, co się pominie. | 4.7 |

## Napisy logowania i konta (hipoteza, do potwierdzenia w wywiadach)

| Pojęcie w dokumentacji | Napis w aplikacji | Dla kogo (rola) | Dlaczego tak | Do sprawdzenia w wywiadzie (nr pytania) |
| --- | --- | --- | --- | --- |
| prośba o logowanie (nagłówek L1) | „Konto” | Gość | Ta sama nazwa co na pasku dolnym. | 4.7 |
| logowanie przez Usługę logowania | „Zaloguj się” | Gość | bez zmian | 4.7, 4.8 |
| rejestracja | „Załóż konto” | Gość | bez zmian | 4.8 |
| rezygnacja z logowania | „Nie teraz” | Gość | bez zmian; nie brzmi jak błąd | 4.8 |
| nagłówek L2 | „Konto” albo „Konto i praca” | Odwiedzający; role pracownicze | Nagłówek powtarza napis z paska. | 6.3 |
| rola, lista ról | „Twoje role: Odwiedzający, Zbierający, Koordynator” | wszystkie zalogowane | bez zmian | 10.1 |
| „Twoje czynności” | „Twoja praca” | role pracownicze | „Czynności” brzmi urzędowo. Napis pasuje do „Konto i praca”. | 10.1 |
| klauzula informacyjna | „Informacja o danych osobowych” | wszystkie zalogowane | bez zmian | 6.3 |
| administrator danych w klauzuli | „Politechnika Wrocławska nie jest administratorem Twoich danych.” | wszystkie zalogowane | Nazwę PWr osoba widzi w aplikacji, więc zdanie usuwa wątpliwość, kto odpowiada za dane. Tekst niesprawdzony prawnie (decyzja 53). | 6.3 |
| kontakt w klauzuli | „Kontakt z zespołem:”, a po nim odnośnik „repozytorium projektu na GitHub” | wszystkie zalogowane | Bez słowa „zgłoszenia”, którego Gość i Odwiedzający nie widzą (PW-5, decyzja 53). | 6.3 |
| wycofanie zgody | „Wycofaj zgodę”; w potwierdzeniu „Zostaw zgodę” zamiast „Anuluj” | wszystkie zalogowane | Oba przyciski mówią o skutku, więc „Anuluj” nie myli. | 6.3 |
| „Wyloguj” | „Wyloguj się” | wszystkie zalogowane | Ta sama forma co „Zaloguj się”. | 6.3 |

## Napisy dla ról zespołu (hipoteza, do potwierdzenia w wywiadach)

Role zespołu pracują z aplikacją często i mają wysokie umiejętności cyfrowe (PE-2, PE-3, hipoteza). Zostają przy pojęciach dokumentacji, jeśli są krótkie. Zmieniamy tylko napisy niejasne albo bez czasownika.

| Pojęcie w dokumentacji | Napis w aplikacji | Dla kogo (rola) | Dlaczego tak | Do sprawdzenia w wywiadzie (nr pytania) |
| --- | --- | --- | --- | --- |
| pomiar precyzyjny („Nowe zgłoszenie z pomiarem”, „Nowe zgłoszenie”) | „Zmierz i zgłoś barierę” | Zbierający | Czasownik i ten sam napis w L2 i C3. Różni się od „Zgłoś barierę” Odwiedzającego. | 10.2 |
| nagłówek C1 | „Zmierz i zgłoś barierę” | Zbierający | Ten sam napis co przycisk w L2 i C3, który otwiera C1 (decyzja 93). | 10.2 |
| nagłówek C2 | „Opisz barierę”; przy poprawianiu i zmianie zgłoszenia przyjętego „Popraw zgłoszenie” | Zbierający | Nagłówek mówi, co się robi; przy poprawianiu powtarza napis przycisku z C3 i V2 (decyzja 93). | 10.2 |
| uśrednianie pozycji | „Pomiar: zostało 12 s” | Zbierający | Bez słowa „uśrednianie”. | 10.2 |
| dokładność pozycji | „Dokładność: 0,8 m (szacunek)” | Zbierający | bez zmian | 10.2 |
| liczba satelitów GNSS | „Satelity: GPS 7, Galileo 5, GLONASS 4” | Zbierający | Nazwy systemów zamiast skrótu „GNSS”, każdy system osobno (US6.1). | 10.2 |
| mapa nieba | „Mapa nieba” | Zbierający | bez zmian | 10.2 |
| koniec uśredniania (C1) | „Pomiar zakończony” | Zbierający | Mówi, że można przejść dalej (US6.1). | 10.2 |
| podpowiedź budynku | „Czy to budynek C-13?” i „Potwierdź”, „Wybierz inny”, „Poza budynkiem” | Zbierający | Pytanie zamiast słowa „podpowiedź”. | 10.2 |
| lista budynków po „Wybierz inny” (C1) | „Wybierz budynek”, „Najbliżej punktu, od najbliższego:”, przyciski „Budynek A-1” | Zbierający | Ten sam zapis budynku co na liście V1. | 10.2 |
| korekta ręczna | „Korekta ręczna” | Zbierający, Weryfikator | bez zmian | 10.2 |
| przełącznik „Plan piętra / Mapa” (C1, budynek szczegółowy) | „Plan piętra”, „Mapa” | Zbierający | Ten sam przełącznik co „Mapa / Lista” na V1 i R1. Jedna powierzchnia i jeden zestaw przycisków skracają ekran. | 10.2 |
| ustawienie punktu (C1) | „Przesuń mapę, aby punkt stał na miejscu bariery.” | Zbierający | Punkt stoi na środku, a przesuwa się mapę tymi samymi strzałkami co na V1 i R1 (decyzja 93). Odpowiednik przeciągania punktu (NFR-53). | 10.2 |
| nagłówki sekcji C1 i C2 | „Miejsce bariery”, „Piętro”, „Zdjęcie”, „Opis” | Zbierający | Krótki rzeczownik nad grupą pól. | 10.2 |
| „Zmień miejsce”, „Powtórz pomiar”, „Dalej”, „Zapisz” | bez zmian | Zbierający | Czasowniki są już krótkie. | 10.2 |
| typ bariery (C2) | „Typ bariery” | Zbierający, Weryfikator | bez zmian | 10.2 |
| uciążliwość (C2, R2) | „Uciążliwość” i wartości „Utrudnienie”, „Tylko z pomocą”, „Blokada” z opisem | Zbierający, Weryfikator | Te same wartości, które widzi Odwiedzający. | 10.2, 10.5 |
| wskazówka nad uciążliwością (C2) | „Oceń dla osoby, dla której bariera jest najtrudniejsza.” | Zbierający | Opisy wartości mówią „Osoba”, a skala dotyczy osoby, dla której bariera jest najtrudniejsza, a nie samego Zbierającego. | 10.2, 10.5 |
| opis „Utrudnienie” (C2) | „Osoba pokona ją sama, ale z dodatkowym wysiłkiem lub ryzykiem.” | Zbierający | Skrócone kryterium stopnia 1 ze skali uciążliwości. | 10.2, 10.5 |
| opis „Tylko z pomocą” (C2) | „Osoba pokona ją tylko z pomocą innej osoby albo urządzenia obsługiwanego przez pracownika.” | Zbierający | Skrócone kryterium stopnia 2. | 10.2, 10.5 |
| opis „Blokada” (C2) | „Osoba nie pokona jej wcale albo tylko wtedy, gdy ktoś ją przeniesie.” | Zbierający | Skrócone kryterium stopnia 3. | 10.2, 10.5 |
| opis przy TB-29 (C2) | „Obowiązkowy przy typie »inna bariera«.” | Zbierający | Mówi przed zapisem, kiedy pole jest wymagane (US7.3). | 10.2 |
| „półpiętro” | „Półpiętro” | Zbierający | bez zmian | 10.2 |
| budynek, piętro i data w pozycji listy (C3, C4, C7, R2) | „C-13, piętro 2, 12.11.2026”; w R2 „Budynek C-13, piętro 2”; przy zmianie oznaczenia wejścia „A-1, Wejście główne” | Zbierający, Weryfikator | Ten sam wzór co „Piętro:” na karcie budynku. | 10.3 |
| piętro wpisywane ręcznie (C2, budynek podstawowy) | „Wpisz numer piętra, 0 to parter.” | Zbierający | Mówi, jak liczyć piętra, bo bez planu nie ma etykiet (BR-G1). | 10.2 |
| status „lokalne” | „W telefonie” | Zbierający | Odpowiada na pytanie PE-2, czy zgłoszenie dotarło. | 10.3 |
| statusy „wysłane”, „w weryfikacji”, „przyjęte”, „odrzucone”, „archiwalne” | „Wysłane”, „W weryfikacji”, „Przyjęte”, „Odrzucone”, „Archiwalne” | Zbierający | Krótkie pojęcia dokumentacji. | 10.3, 10.5 |
| znak „zmiana czeka na wysłanie” | „Zmiana czeka na wysłanie” | Zbierający | bez zmian | 10.3 |
| powód odrzucenia i rodzaj zmiany (C3) | „Powód odrzucenia:”, „Zmiana oznaczenia wejścia” | Zbierający | Te same słowa co pole „Powód odrzucenia” w R2 i nagłówek C6. | 10.3 |
| konflikt wersji („wybierz wersję”) | „Dwie wersje: wybierz jedną” | Zbierający | Mówi, co się stało i co zrobić. | 10.4 |
| pole różniące się w konflikcie | „Różni się: [pole]”, np. „Różni się: uciążliwość”, nad obiema wersjami; przy kilku polach „Różnią się: [pole], [pole]” | Zbierający | Mówi, które pole się różni, tym samym wzorem co „Zmienione: uciążliwość” na R2; różnicę widać bez koloru (NFR-21). | 10.4 |
| wersje w konflikcie (C4) | nagłówki „Moja wersja”, „Wersja z serwera” | Zbierający | Te same słowa co w przyciskach wyboru wersji. | 10.4 |
| podział kolejki (C4) | „2 zgłoszenia i 1 znacznik piętra” | Zbierający | Mówi, co dokładnie czeka na wysłanie. | 10.3 |
| „Wyślij ponownie do bieżącego zgłoszenia” | „Wyślij ponownie”, a nad przyciskiem zdanie z katalogu komunikatów (C3, nieaktualne) | Zbierający | Krótki napis mieści się na przycisku w jednym wierszu. Zdanie nad nim mówi, że zmiana trafi do aktualnej wersji, a nie do tego samego zgłoszenia. | 10.4 |
| synchronizacja (nagłówek C4, „Synchronizuj teraz”) | „Synchronizacja”, „Synchronizuj teraz” | Zbierający | Krótkie pojęcie dokumentacji. | 10.3 |
| „Czeka na wysłanie: 3” | bez zmian | Zbierający | — | 10.3 |
| „Zostaw moją wersję”, „Weź wersję z serwera” | bez zmian; pod nimi skutek według katalogu komunikatów (C4, konflikt) | Zbierający | Skutek widać przed decyzją (recenzja B3, Nielsen 5). | 10.4 |
| „Zdecyduję później” | bez zmian | Zbierający | — | 10.4 |
| piętro obejrzane (przycisk „Piętra obejrzane” na V2) | „Oznacz obejrzane piętra” | Zbierający | Czasownik na przycisku. | 10.7 |
| nagłówek C5 | „Obejrzane piętra” | Zbierający, Koordynator | bez zmian treści | 10.7 |
| znacznik „obejrzane” / „nieobejrzane” | „Obejrzane”, „Nieobejrzane” | Zbierający, Koordynator | bez zmian | 10.7 |
| wiersz budynku podstawowego (C5) | „Cały budynek” | Zbierający, Koordynator | bez zmian | 10.7 |
| wiersz piętra (C5) | „Piętro: [etykieta]” z etykietą z danych PWr | Zbierający, Koordynator | Ten sam wzór co „Piętro:” na karcie budynku. | 10.7 |
| znacznik w telefonie (C5) | „Czeka na wysłanie” | Zbierający | Te same słowa co „Czeka na wysłanie: 3” na C3. | 10.3 |
| budynek podstawowy w K1 | „Obejrzany”, „Nieobejrzany” | Koordynator | Forma dla budynku; ikona jak przy znaczniku. | 10.7 |
| „Twój znacznik”, „znacznik innej osoby” | „Twój znacznik”, „Znacznik innej osoby”; u Koordynatora „Znacznik: Zbierający, Autor 14” | Zbierający, Koordynator | Koordynator nie stawia znaczników, więc widzi rolę i pseudonim autora jak w R2 (BR-5). | 10.4 |
| „Oznacz jako obejrzane” | bez zmian | Zbierający | — | 10.7 |
| „Cofnij” (Zbierający) i „Cofnij znacznik” (Koordynator) | „Cofnij znacznik” dla obu ról | Zbierający, Koordynator | Ta sama czynność ma ten sam napis. | 10.7 |
| zmiana oznaczenia wejścia („Zmień oznaczenie”) | przycisk „Zmień oznaczenie”, nagłówek C6 „Zmień oznaczenie wejścia” | Zbierający | Przycisk stoi przy wejściu, więc słowo „wejścia” jest zbędne. | 10.1 |
| przyciski wyboru w C6 | „Dostępne”, „Niedostępne” | Zbierający | Te same słowa co na karcie budynku. | 10.1 |
| nagłówki sekcji C6 i C7 | „Nowe oznaczenie”, „Zdjęcie wejścia”, „Nowe zdjęcie miejsca” | Zbierający | Słowo „nowe” odróżnia to, co Zbierający dodaje, od obecnych danych nad sekcją. | 10.1 |
| bariera usunięta („Bariery już nie ma”) | przycisk i nagłówek C7 „Zgłoś, że bariery nie ma”; rodzaj na C3 „Bariery już nie ma” | Zbierający | Czasownik na przycisku, opis stanu na liście. | 10.1 |
| „Popraw zgłoszenie”, „Wyślij do sprawdzenia” | bez zmian | Zbierający | „Sprawdzenie” zamiast „weryfikacji”, jak w R1. | 10.5 |
| kolejka weryfikacji (R1) | przycisk i nagłówek „Zgłoszenia do sprawdzenia” | Weryfikator | Nagłówek powtarza napis przycisku z L2. | 10.5 |
| „Wzięte przeze mnie”, „Weź do sprawdzenia” | bez zmian | Weryfikator | — | 10.5 |
| otwarcie pozycji listy (R1 „Wzięte przeze mnie”, K1 budynek, C3 zgłoszenie do poprawy) | „Otwórz” z ikoną `chevron-right` | Weryfikator, Koordynator, Zbierający | Ramka bez znaku nie wygląda na coś, co da się dotknąć (recenzja B5, Nielsen 6). | 10.5, 10.7 |
| przełącznik „Mapa / Lista” (R1) | „Mapa”, „Lista” | Weryfikator | Ta sama kolejność co na V1; zaznaczony jest widok domyślny, na R1 lista. | 10.5 |
| nagłówek R2 | „Sprawdzenie zgłoszenia” | Weryfikator | bez zmian | 10.5 |
| podobne zgłoszenie | „Podobne zgłoszenia do 10 m” | Weryfikator | Krócej niż „w promieniu 10 m”. | 10.4 |
| duplikat, „nieaktualne” | „Duplikat”, „Nieaktualne”; przy zmianie oznaczenia wejścia bez „Duplikat” | Weryfikator | bez zmian; duplikat to zgłoszenie tej samej bariery, a zmiana oznaczenia wejścia nie opisuje bariery (BR-24) | 10.4 |
| własny powód odrzucenia (R2) | opcja „Inny powód”, pole „Opisz powód dla autora” | Weryfikator | Etykieta mówi, co wpisać i dla kogo (recenzja B5). | 10.5 |
| status poprzedniego zgłoszenia (R2, „bariera usunięta”) | „Poprzednie zgłoszenie:” i status, np. „Archiwalne” | Weryfikator | Bez statusu nie widać, że zmiana jest nieaktualna (BR-23). | 10.5 |
| oznaczenie wejścia od wysłania zmiany (R2) | „Bez zmian od wysłania” z ikoną `link-intact`, „Zmienione po wysłaniu” z ikoną `link-broken` | Weryfikator | Mówi, czy zmiana dotyczy obecnego oznaczenia (BR-24). | 10.5 |
| zmienione pola nowej wersji (R2) | „Zmienione: uciążliwość” | Weryfikator | Weryfikator nie szuka różnicy sam (BR-3). | 10.5 |
| „Przyjmij”, „Odrzuć”, powód odrzucenia | „Przyjmij”, „Odrzuć”, pole „Powód odrzucenia” | Weryfikator | bez zmian | 10.5 |
| zasłonięcie twarzy albo tablicy | „Zasłoń twarz lub tablicę” | Weryfikator | Czasownik na przycisku. | 10.5 |
| pseudonim autora | „Autor: Zbierający, Autor [nr]”; przy zgłoszeniu szybkim „Autor: Odwiedzający, Autor [nr]” | Weryfikator | Rola i pseudonim bez danych osobowych (BR-5). Numer pseudonimu jest zastępnikiem, bo nadaje go system. | 10.5 |
| rodzaj zgłoszenia (R1, R2) | „Zgłoszenie z pomiaru”, „Zgłoszenie szybkie”, „Bariery już nie ma”, „Zmiana oznaczenia wejścia” | Weryfikator | Rodzaj mówi, czy typ i uciążliwość trzeba ustalić (BR-14, BR-15). Dwa ostatnie napisy są te same co na C3. | 10.5 |
| data wysłania w kolejce (R1) | „Wysłane 12.11.2026” | Weryfikator | To samo słowo co status „Wysłane” u Zbierającego. | 10.5 |
| etykiety danych zgłoszenia (R2) | „Czego dotyczy:”, „Miejsce:”, „Obecne oznaczenie:”, „Wybrane oznaczenie:”, „Wcześniejsze zgłoszenie:” | Weryfikator | Krótka etykieta przed wartością z danych. „Czego dotyczy” powtarza pytanie z kroku 2 V4. | 10.5 |
| podobne zgłoszenie: odległość i piętro (R2) | „5 m, piętro 2”; przy wyborze duplikatu „schody przy wejściu, 2 m”, „próg, 5 m” | Weryfikator | Odległość tekstem, a nie tylko punktem na mapie (NFR-19). | 10.4 |
| postęp inwentaryzacji | „Postęp inwentaryzacji” | Koordynator | bez zmian | 10.7 |
| postęp budynku | „4 z 5 pięter, 80%” | Koordynator | bez zmian | 10.7 |
| liczby zgłoszeń w K1 | „Czeka na sprawdzenie: 3”, „Przyjęte: 4” | Koordynator | bez zmian; liczby jak w scenariuszu `@US11.1` | 10.7 |
| źródło „obchód” w K1 | „Wejścia sprawdzone na miejscu: 1 z 6” | Koordynator | Te same słowa co przy wejściu na karcie budynku. | 10.7 |
| „Odśwież” | „Odśwież” i „Stan z 12.11.2026, 10:42” | Koordynator | Data i godzina odczytu (recenzja B3, Nielsen 1; US11.2). | 10.7 |
| zaproszenie | przycisk i nagłówek K2 „Zaproś osobę”, przycisk „Wyślij zaproszenie” | Koordynator | Nagłówek powtarza napis przycisku z L2. | 10.4 |
| pola K2 | „Adres e-mail”, „Rola” | Koordynator | bez zmian | 10.4 |
| eksport | przycisk i nagłówek K3 „Pobierz eksport”, przycisk „Pobierz plik” | Koordynator | Krótkie pojęcie dokumentacji. | 10.6 |
| wybór formatu eksportu (K3) | „Format pliku” | Koordynator | bez zmian; nagłówek nad dwoma formatami | 10.6 |
| formaty eksportu | „GeoJSON (WGS84)”, „CSV (PUWG 1992, EPSG:2180)” | Koordynator | Format i kod EPSG pozwalają otworzyć plik w QGIS bez zgadywania układu. | 10.6 |

## Katalog komunikatów (hipoteza, do potwierdzenia w wywiadach)

Każdy komunikat stanu z [wymagań do ekranów](wymagania-ekranow.md) w ostatecznym brzmieniu. Kolumna „Zmiana” odsyła do propozycji PW, gdy treść różni się od wymagań.

Komunikaty sprawdzimy w drugiej rozmowie (pytanie 6.3) i w prototypie papierowym, a przy komunikatach o zaufaniu słuchamy słów z pytań 4.3 i 4.4.

### Wszystkie ekrany

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| każdy ekran, brak sieci | jedna linia „Bez internetu · dane z 12.11.2026” | — | PW-4; decyzja 54 |
| ekrany R1, R2, K1–K3, brak sieci | „Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie.” | „Spróbuj ponownie” | — |
| ekrany R1, R2, K1–K3, błąd serwera | „Serwer chwilowo nie odpowiada. Spróbuj ponownie za kilka minut.” | „Spróbuj ponownie” | PW-10 |
| ekrany R1, R2, K1–K3, brak uprawnień | „Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie.” | „Wróć do mapy” | PW-10 |

### Gość i Odwiedzający

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| V1, ładowanie | „Wczytujemy mapę kampusu…” | — | PW-9 |
| V1, brak wyniku | „Nie znaleźliśmy budynku »X«. Sprawdź symbol, np. C-13, albo wybierz budynek z listy.” | „Lista budynków” | PW-9 |
| V1, brak sieci przy pierwszym otwarciu | bez komunikatu: jedna linia „Bez internetu” bez daty nad mapą z konturami i wejściami | — | PW-4; decyzja 84 |
| V1, brak mapy ulic | „Mapa ulic chwilowo się nie wczytuje. Budynki i bariery nadal możesz oglądać.” | — | — |
| V1, brak sali | „Nie znaleźliśmy sali »X« w budynku A-1. Sprawdź numer albo otwórz opis budynku.” | „Pokaż opis budynku” | decyzja 44 |
| V1, sala bez drogi | „Nie znaleźliśmy w danych wejścia i windy, którymi dotrzesz do tej sali bez schodów.” | „Pokaż opis budynku” | decyzja 48 |
| V2, ładowanie zdjęcia | „Wczytujemy zdjęcie…” | — | PW-9 |
| V2, brak barier | nagłówek „Brak zgłoszonych barier”, zdanie „Bariera pojawi się tutaj, gdy ktoś ją zgłosi i zostanie sprawdzona.” | „Zgłoś barierę” | PW-6; decyzja 48 |
| V2, brak planu (budynek podstawowy) | „Nie mamy planu pięter tego budynku.” | — | PW-6; lista barier stoi nad napisem (decyzja 54) |
| V2, zdjęcie bez internetu | „Zdjęcie zobaczysz po połączeniu z internetem.” | — | PW-6 |
| V2, budynku nie ma w telefonie | „Tego budynku nie ma jeszcze w telefonie. Połącz się z internetem, aby zobaczyć opis budynku.” | „Spróbuj ponownie” | PW-4 |
| V2, błąd zdjęcia | „Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę.” | „Spróbuj ponownie” | PW-10 |
| V3, jest wejście dostępne | „Budynek A-1 ma wejście dostępne: »nazwa wejścia«.” | — | — |
| V3, brak wejścia dostępnego | „Budynek B-4 nie ma wejścia dostępnego. Wybierz »Wstecz«, aby w opisie budynku zobaczyć bariery przy każdym wejściu.” | — | PW-10; rada wskazuje przycisk „Wstecz” (recenzja B5) |
| V3, brak danych o wejściach | „Nie wiemy jeszcze, czy budynek B-4 ma wejście dostępne: brak danych o 2 z 3 wejść. Wybierz »Wstecz«, aby w opisie budynku zobaczyć każde wejście z datą.” | — | PW-10; rada wskazuje przycisk „Wstecz” (recenzja B5) |
| V4, krok 1 | „Ważne”, „Nie fotografuj twarzy ani tablic rejestracyjnych.”, „Zdjęcie zobaczą inni użytkownicy.” | „Zrób zdjęcie”, „Zrób nowe zdjęcie” | PW-8; decyzja 54 |
| V4, krok 3 | „Dołączymy miejsce, w którym teraz stoisz.” | „Wyślij” | — |
| V4, brak zgody na lokalizację | „Aby wysłać zdjęcie bariery, potrzebujemy jej miejsca. Sprawdzimy je tylko teraz i nie będziemy Cię śledzić.” | „Pozwól na lokalizację”, „Wróć do mapy” | PW-5 |
| V4, odmowa zgody | „Bez lokalizacji nie wyślesz zdjęcia bariery. Możesz ją włączyć w ustawieniach telefonu.” | „Otwórz ustawienia”, „Wróć do mapy” | PW-5 |
| V4, zgoda tylko przybliżona | „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. Pozwól na dokładną lokalizację.” | „Pozwól na dokładną lokalizację”, „Wróć do mapy” | — |
| V4, lokalizacja wyłączona | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby wysłać zdjęcie bariery.” | „Otwórz ustawienia”, „Wróć do mapy” | PW-5 |
| V4, ustalanie miejsca | „Ustalamy, gdzie jesteś…” | „Wyślij” czeka | PW-9 |
| V4, brak pozycji | „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” | „Spróbuj ponownie”, „Zrezygnuj” | PW-5 |
| V4, potwierdzenie rezygnacji (po „Zrezygnuj” albo po „Wstecz” w kroku 1 ze zdjęciem) | „Jeśli zrezygnujesz, usuniemy zrobione zdjęcie i niczego nie wyślemy. Aby je zachować, wybierz »Wróć do zdjęcia«.” | „Tak, zrezygnuj”, „Wróć do zdjęcia” | nowy; recenzja B4, uwaga 17 |
| V4, potwierdzenie | „Dziękujemy. Sprawdzimy zdjęcie, a jeśli bariera się potwierdzi, pokażemy ją na mapie.” | „Wróć do mapy” | PW-5 |
| V4, potwierdzenie bez internetu | „Zapisaliśmy zdjęcie w telefonie. Wyślemy je sami, gdy wróci internet.” | „Wróć do mapy” | PW-5 |
| V4, za mało miejsca | „W telefonie brakuje miejsca, więc nie zapisaliśmy zdjęcia bariery. Usuń niepotrzebne zdjęcia albo filmy i spróbuj ponownie.” | „Spróbuj ponownie” | PW-5 |
| V5, zdanie o koncie | „Aby zgłosić barierę, potrzebne jest konto. Możesz je założyć teraz w »Konto«.” | „Dalej”, „Pomiń instrukcję” | PW-5 |
| V5, zdanie o wejściu | „Napis »Dostępne« przy wejściu znaczy, że wejdziesz bez pokonywania stopni: z poziomu chodnika, pochylnią albo podnośnikiem.” | „Dalej”, „Pomiń instrukcję” | PW-15 |
| V5, tytuły kroków | „Znajdź budynek”, „Co znaczy »Dostępne«”, „Zgłoś barierę” | „Dalej”, „Pomiń instrukcję” | decyzja 89 |

### Logowanie i konto

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| L1, zwykły | „Aby zgłosić barierę, potrzebne jest konto. Konto chroni mapę przed fałszywymi informacjami.” | „Zaloguj się”, „Załóż konto”, „Nie teraz” | PW-5 |
| L1, wejście z V4 | „Po zalogowaniu od razu zrobisz zdjęcie. Całość to 3 krótkie kroki.” | jak wyżej | PW-5 |
| L1, brak sieci | „Logowanie wymaga internetu. Mapa i opisy budynków działają bez niego.” | „Wróć do mapy” | PW-4 |
| L1, błąd | „Logowanie się nie udało albo zostało przerwane. Sprawdź internet i spróbuj ponownie.” | „Spróbuj ponownie”, „Nie teraz” | — |
| strona logowania, blokada konta | „Konto jest chwilowo zablokowane po kilku błędnych hasłach. Spróbuj ponownie później.” | — | — |
| L2, potwierdzenie wycofania zgody | „Wylogujemy Cię i w ciągu miesiąca usuniemy Twoje dane osobowe. Wysłane zdjęcia barier zostaną, ale bez Twoich danych.” | „Wycofaj zgodę”, „Zostaw zgodę” | PW-5 |

### Zbierający

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| C1, szukanie satelitów | „Szukamy satelitów. Na otwartej przestrzeni pomiar będzie dokładniejszy.” | „Powtórz pomiar” | PW-9 |
| C1, brak zgody na lokalizację | „Aby zapisać miejsce bariery, potrzebujemy lokalizacji. Sprawdzimy ją tylko teraz i nie będziemy Cię śledzić.” | „Pozwól na lokalizację” | — |
| C1, odmowa zgody | „Bez lokalizacji nie zapiszesz miejsca bariery. Możesz ją włączyć w ustawieniach telefonu.” | „Otwórz ustawienia” | — |
| C1, zgoda tylko przybliżona | „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. Pozwól na dokładną lokalizację.” | „Pozwól na dokładną lokalizację” | — |
| C1, lokalizacja wyłączona | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby zapisać miejsce bariery.” | „Otwórz ustawienia” | — |
| C2, wyjście bez zapisu | „Wyjść bez zapisu? Wpisane pola nie zostaną zapisane.” | „Wyjdź bez zapisu”, „Wróć do formularza” | — |
| C2, zmiana zgłoszenia przyjętego | „Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.” | „Zapisz” | — |
| C2, błąd walidacji | „Brakuje typu bariery. Uzupełnij to pole, aby zapisać.” Ten sam wzór dla innych pól, np. „Brakuje opisu. Uzupełnij to pole, aby zapisać.” | — | PW-10 |
| C2, C6, C7, za mało miejsca | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” | „Spróbuj ponownie” | — |
| C3, pusty | „Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę.” | „Zmierz i zgłoś barierę” | PW-12 |
| C3, ładowanie | „Sprawdzamy statusy…” | — | PW-9 |
| C3, błąd | „Nie udało się sprawdzić statusów, ale zgłoszenia są bezpieczne w telefonie. Spróbuj ponownie za chwilę.” | „Spróbuj ponownie” | PW-10 |
| C3, nieaktualne | „Ktoś już zmienił to zgłoszenie. Twoja zmiana trafi do jego aktualnej wersji.” Przy zmianie oznaczenia wejścia: „Oznaczenie tego wejścia już się zmieniło. Sprawdź je i wyślij swoją zmianę ponownie.” | „Wyślij ponownie” | PW-14 |
| C3, nieaktualne po usunięciu bariery | „Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa.” | „Zmierz i zgłoś barierę” | PW-12 |
| C4, pusty | „Wszystko wysłane. Ostatnia synchronizacja: 12.11.2026, 10:42.” | — | — |
| C4, wysyłanie | „Wysyłamy 2 z 5…” | — | PW-9 |
| C4, brak sieci | „Bez internetu. Wyślemy wszystko sami, gdy internet wróci.” | — | decyzja 54 |
| C4, błąd serwera | „Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później.” | — | — |
| C4, mało miejsca | „W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia.” | — | PW-10 |
| C4, konflikt | „Dwie wersje: wybierz jedną. Druga wersja zostanie usunięta.” Przy zgłoszeniu przyjętym drugie zdanie: „Przyjęte zgłoszenie zostanie, a Twoja wersja, jeśli ją wybierzesz, trafi do sprawdzenia jako nowe.” (UC11 3b) | „Zostaw moją wersję”, „Weź wersję z serwera”, „Zdecyduję później” | PW-12 |
| C5, potwierdzenie cofnięcia (Zbierający) | „Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy.” | „Cofnij znacznik”, „Zostaw znacznik” | PW-12 |
| C5, potwierdzenie cofnięcia (Koordynator) | „Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający.” | „Cofnij znacznik”, „Zostaw znacznik” | PW-12 |
| C5, brak sieci | „Znacznik zapisany w telefonie. Wyślemy go, gdy wróci internet.” | — | — |
| C5, błąd zapisu | „Nie udało się zapisać znacznika w telefonie. Spróbuj ponownie, a jeśli to nie pomoże, zwolnij miejsce.” | „Spróbuj ponownie” | — |
| C6, wybór obecnej wartości | „Potwierdzasz obecne oznaczenie. Zmieni się data i źródło.” | — | — |
| C6, informacja | „Zmianę sprawdzi inna osoba. Do tego czasu karta budynku pokazuje obecne oznaczenie.” | „Wyślij do sprawdzenia” | — |
| C6, błąd walidacji | „Dodaj zdjęcie wejścia, aby wysłać zmianę.” albo „Wybierz: dostępne czy niedostępne.” | — | — |
| C6, brak sieci | „Zmiana zapisana w telefonie. Wyślemy ją, gdy wróci internet.” | — | — |
| C7, informacja | „Bariera zostanie na mapie, dopóki Weryfikator nie sprawdzi zgłoszenia.” | „Wyślij do sprawdzenia” | — |
| C7, błąd walidacji | „Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.” | — | — |
| C7, brak sieci | „Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet.” | — | — |

### Weryfikator i Koordynator

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| R1, pusty | „Nie ma zgłoszeń do sprawdzenia. Zajrzyj tu później.” | — | PW-10 |
| R1, ładowanie | „Wczytujemy zgłoszenia…” | — | PW-9 |
| R1, zgłoszenie zajęte | „Ktoś inny wziął już to zgłoszenie. Wybierz inne.” | — | — |
| R2, brak podobnych | „W promieniu 10 m nie ma podobnych zgłoszeń.” | — | — |
| R2, ładowanie zdjęcia | „Wczytujemy zdjęcie…” | — | PW-9 |
| R2, błąd przy przyjęciu | „Ustal typ bariery i uciążliwość, aby przyjąć zgłoszenie.” | — | — |
| R2, błąd przy odrzuceniu | „Podaj powód odrzucenia, aby autor wiedział, co poprawić.” | — | — |
| R2, nowa wersja | „Autor zmienił zgłoszenie. Sprawdź nową wersję przed decyzją.” | — | PW-10 |
| R2, brak sieci | „Decyzja wymaga internetu, a zgłoszenie czeka w »Wziętych przeze mnie«. Połącz się z internetem i podejmij decyzję ponownie.” | — | PW-10 |
| K1, ładowanie | „Wczytujemy postęp…” | — | PW-9 |
| K2, sukces | „Zaproszenie wysłane na podany adres.” | — | — |
| K2, konto istnieje | „To konto już istnieje. Dodaliśmy mu rolę Weryfikatora, dotychczasowe role zostają.” | — | — |
| K2, rola już jest | „Ta osoba ma już rolę Weryfikatora. Nic nie zmieniliśmy.” | — | — |
| K2, błąd walidacji | „Adres e-mail jest niepełny, np. brakuje znaku @. Popraw go i wyślij ponownie.” albo „Wybierz rolę.” | — | PW-10 |
| K3, ładowanie | „Przygotowujemy plik…” | — | PW-9 |
| K3, pusty | „Nie ma jeszcze przyjętych barier. Pobrany plik będzie pusty.” | „Pobierz plik” | — |
| K3, sukces | „Plik zapisany w pobranych plikach.” | — | — |
| K3, informacja | „Plik zawiera tylko obecne, sprawdzone bariery, bez danych autorów.” | „Pobierz plik” | — |

## Metafory i ikony

Ikony pochodzą z zestawu OpenIconic dostępnego w PlantUML Salt [3], [4]. Każda ikona stoi obok tekstu, a tekst niesie znaczenie sam. Kolor nie niesie znaczenia (NFR-21) [5].

Zatwierdzone makiety hi-fi rysują znaki oznaczenia wejścia i uciążliwości własnymi kształtami, tymi samymi na mapie i w treści. Tabela podaje je po słowie „hi-fi”.

| Element | Ikona (nazwa z OpenIconic) | Tekst obok | Metafora / dlaczego zrozumiała |
| --- | --- | --- | --- |
| pasek dolny: mapa | `map` | „Mapa” | plan w ramce ze znacznikiem miejsca, jak w aplikacjach map |
| pasek dolny: zgłoszenie | `camera-slr` | „Zgłoś barierę”; ta sama ikona przy „Zgłoś barierę przy wejściu” (V2, V3) | zgłoszenie zaczyna się od zdjęcia (US4.1) |
| pasek dolny: pomoc | `question-mark` | „Pomoc” | znak zapytania, czyli „mam pytanie” |
| pasek dolny: konto | `person` | „Konto” albo „Konto i praca” | sylwetka, czyli „ja”; ta sama ikona dla obu napisów |
| wskaźnik braku sieci | `wifi` | „Bez internetu” | znak internetu znany z telefonu i kawiarni (hipoteza) |
| oznaczenie „dostępne” | `check`; hi-fi: koło z ptaszkiem | „Dostępne” | ptaszek, czyli „tak, da się” |
| oznaczenie „niedostępne” | `x`; hi-fi: ośmiokąt z krzyżykiem | „Niedostępne” | krzyżyk, czyli „nie”; ośmiokąt jak znak „stop” |
| oznaczenie „brak danych” | `minus`; hi-fi: romb z poziomą kreską | „Brak danych” | kreska jak w pustej rubryce formularza; inna niż krzyżyk (US17.1) |
| uciążliwość 1 | `warning`; hi-fi: trójkąt z „!” | „Utrudnienie” | trójkąt jak drogowy znak ostrzegawczy: uważaj |
| uciążliwość 2 | `people`; hi-fi: tarcza z dwiema osobami | „Tylko z pomocą” | dwie osoby: przejdziesz z drugą osobą |
| uciążliwość 3 | `ban`; hi-fi: zapora z ukośnymi pasami | „Blokada” | znak zakazu albo zapora drogowa: tędy nie |
| przycisk zdjęcia (C2, C6, C7); w V4 bez ikony | `camera-slr` | „Zrób zdjęcie” | aparat; ta sama ikona co „Zgłoś barierę” |
| status „lokalne” | `phone` | „W telefonie” | kształt telefonu: zgłoszenie jest tylko w nim |
| status „wysłane” | `data-transfer-upload` | „Wysłane” | strzałka w górę: poszło dalej |
| status „w weryfikacji” | `eye` | „W weryfikacji” | oko: ktoś teraz ogląda |
| status „przyjęte” | `circle-check` | „Przyjęte” | ptaszek w kole: decyzja na tak |
| status „odrzucone” | `circle-x` | „Odrzucone” | krzyżyk w kole: decyzja na nie |
| status „archiwalne” | `box` | „Archiwalne” | pudło na stare dokumenty |
| znacznik „obejrzane” | `task` | „Obejrzane” | odhaczona pozycja na liście kontrolnej |
| otwarcie pozycji listy (R1, K1, C3) | `chevron-right` | „Otwórz” | strzałka w prawo: dalej, do szczegółów |
| przycisk „Wstecz” | `arrow-left` | „Wstecz” | strzałka w lewo: powrót do poprzedniego miejsca |
| przesuwanie mapy (V1, C1, R1) | `caret-top`, `caret-bottom`, `caret-left`, `caret-right` | brak; nazwa dla TalkBack „Przesuń mapę w górę” i dalsze kierunki | grot pokazuje, w którą stronę przesunie się mapa; inny znak niż strzałka „Wstecz” |
| informacje o mapie (V1, C1, R1) | `info` | brak; nazwa dla TalkBack „Informacje o mapie” | litera „i” jak w punkcie informacji |
| oznaczenie wejścia bez zmian od wysłania zmiany (R2) | `link-intact` | „Bez zmian od wysłania” | całe ogniwo: zmiana dotyczy obecnego oznaczenia |
| oznaczenie wejścia zmienione po wysłaniu zmiany (R2) | `link-broken` | „Zmienione po wysłaniu” | przerwane ogniwo: zmiana dotyczy oznaczenia, którego już nie ma |

Zasady ikon. Ikona stoi tylko w przypadkach z tabel w tej sekcji, a pozostałe elementy są bez ikony:

| Zasada | Skutek |
| --- | --- |
| Jedna ikona ma jedno znaczenie. | `question-mark` oznacza tylko pomoc, dlatego „Brak danych” ma `minus`. Lupa stoi tylko w polu wyszukiwania. |
| Ikonę mają elementy, dla których wymaga jej NFR-21 albo wymagania wspólne, oraz pasek dolny. | Status, oznaczenie wejścia, uciążliwość, znacznik i wskaźnik sieci mają ikonę. Pasek dolny ma ikony, bo widać go na 5 ekranach i osoba uczy się ich raz. |
| Przy zwykłej czcionce ikona może stać obok słowa w odnośnikach, chipach, wierszach L2 i blokach V3. | Ikona pomaga rozpoznać czynność, ale znaczenie niesie słowo. „Zgłoś barierę przy wejściu” powtarza ikonę paska, bo zaczyna to samo zgłoszenie. Przy skali czcionki co najmniej 1,5 te ikony znikają ([reguła 11](wymagania-ekranow.md#duza-czcionka)). |
| Strzałki zostają przy każdej czcionce. | Strzałka „Wstecz” i strzałka w dół przy rozwijanych piętrach i wyjściach ewakuacyjnych. Strzałka w prawo przy „Otwórz” w R1, K1 i C3 odróżnia otwarcie pozycji od czynności na niej (recenzja B5); ta sama stoi w wierszach L2. Ołówek przy „Popraw zgłoszenie” (V2, Zbierający) odróżnia poprawę od nowego zgłoszenia. |
| Na V1, C1 i R1 sterowanie mapą, a na V1 też lupa i wybrana pozycja przełącznika mają ikony przy każdej czcionce. | Strzałki przesuwania, „+”, „−”, „ⓘ” i „Legenda” stoją na mapie jako jeden zestaw. Lupa oznacza pole wyszukiwania. Sam ptaszek bez koła pokazuje wybraną pozycję „Mapa” albo „Lista budynków” i nie myli się ze znakiem „Dostępne”. |
| Nawigacja wstecz i przesuwanie mapy mają strzałkę. | „Wstecz” to strzałka w lewo i słowo. Przyciski przesuwania mapy to same strzałki na mapie z nazwą dla TalkBack, bo słowa pod mapą zabierały jej miejsce. „Powiększ” i „Pomniejsz” to w hi-fi znaki „+” i „−” na mapie, też z nazwą dla TalkBack. Wzorzec znają aplikacje map, a przesuwanie nadal nie wymaga przeciągania (WCAG 2.5.7). |
| Pozostałe elementy są bez ikony. | 6 grup barier, przyciski czynności, „Zrób zdjęcie” w V4, „Nieobejrzane”. Słowo jest dla PE-1 jaśniejsze niż strzałka albo symbol [6]. |
| Grupy barier w V4 są bez ikon. | OpenIconic nie ma ikon toalety, drzwi ani chodnika, a ikona na siłę myli. US4.3 wymaga tylko dużych przycisków (PW-7). |

### Udział ikon

Kurs „Projektowanie doświadczeń użytkownika” w etapie 4 dopuszcza najwyżej 50% ikon w całym interfejsie [7]. Udział liczymy tylko na zatwierdzonych makietach hi-fi (decyzja 60). Teraz to 42 stany ekranów partii 1, a po partii 2 cały interfejs.

Element to miejsce, które osoba czyta albo dotyka osobno: nagłówek, akapit, pozycja listy lub legendy, przycisk, pole albo chip. Element z ikoną ma ikonę w sobie albo tuż obok, a karta budynku z kilkoma znakami to jeden element. Zdjęcie ani rysunek nie są ikoną.

Każdy stan liczymy osobno i w całości, także treść pod krawędzią okna, przy zwykłej czcionce. Elementy powtarzane liczymy w każdym stanie, więc pasek dolny daje 4 elementy z ikoną wszędzie, gdzie jest. Znaki i napisy na samej mapie (symbole budynków, znaki wejść i barier) to treść mapy i ich nie liczymy, a przyciski na mapie liczymy.

| Ekran | Stany hi-fi | Elementy | Z ikoną | Udział |
| --- | --- | --- | --- | --- |
| V1 Mapa kampusu | 11 | 301 | 143 | 48% |
| V2 Budynek | 9 | 231 | 147 | 64% |
| V3 Którędy wejść | 3 | 40 | 26 | 65% |
| V4 Zgłoś barierę | 11 | 86 | 14 | 16% |
| V5 Jak korzystać z aplikacji | 2 | 27 | 12 | 44% |
| L1 Konto (Gość) | 2 | 14 | 3 | 21% |
| L2 Konto albo Konto i praca | 4 | 64 | 32 | 50% |
| **Partia 1** | **42** | **763** | **377** | **49%** |

Partia 1 mieści się w limicie z małym zapasem. Lista budynków V1 to 72 elementy, z czego 10 z ikoną, a bez niej udział wynosi 53%. Gdy partia 2 w hi-fi przekroczy limit, najpierw znikną ikony obok słów, bo słowo niesie znaczenie samo.

Oznaczenie wejścia i wskaźnik sieci zawsze mają tekst i ikonę (NFR-21 i kryterium WCAG o użyciu koloru [5]). Tych ikon ani ikon paska dolnego nie usuwamy, żeby zmieścić się w limicie: wymaga ich NFR-21, a pasek wygląda tak samo na każdym ekranie.

## Nazwa wejścia do pracy zespołu {#nazwa-wejscia}

Recenzja wymagań do ekranów (uwaga 24, spór) wskazała, że cała praca Zbierającego, Weryfikatora i Koordynatora zaczyna się w „Konto”. Osoba szukająca „Moich zgłoszeń” nie skojarzy ich z kontem. Strukturę nawigacji ocenia osobno mapa nawigacji (zadanie B4), a ta sekcja wybiera tylko nazwę.

| Wariant | Za | Przeciw |
| --- | --- | --- |
| „Konto” (bez zmian) | Jedna nazwa dla wszystkich ról. | Praca ról zespołu jest ukryta pod nazwą, która jej nie zapowiada (Nielsen 2, 6). |
| „Moja praca” | Nazywa główną czynność ról zespołu na L2. | Ukrywa „Wyloguj się” i „Informację o danych osobowych”. Zmienia drogę do klauzuli „Konto, Informacja o danych osobowych” (NFR-30). |
| **„Konto i praca” (wybrany)** | Zaczyna się od „Konto”, więc instrukcja V5 i droga do klauzuli zostają prawdziwe. Słowo „praca” zapowiada czynności ról. | Dłuższy napis, ale tej samej długości co „Zgłoś barierę” (13 znaków), więc pasek się nie zmienia. |

Decyzja: „Konto i praca” widzi każda osoba z co najmniej jedną rolą pracowniczą, na pasku dolnym i w nagłówku L2. Gość i Odwiedzający widzą „Konto”. Ikona `person` zostaje ta sama, więc pasek wygląda tak samo dla wszystkich ról. Sekcja na L2 nazywa się „Twoja praca”.

## Propozycje do wymagań ekranów {#propozycje}

Propozycje PW-1…PW-15 są wprowadzone w [wymaganiach do ekranów](wymagania-ekranow.md) w brzmieniu po recenzji. Kolumna „Przed zmianą” pokazuje stan wymagań sprzed fali B6.

| Nr | Ekran | Przed zmianą | Propozycja | Dlaczego |
| --- | --- | --- | --- | --- |
| PW-1 | pasek dolny, L2 | „Konto” dla wszystkich ról | „Konto i praca” dla ról pracowniczych; sekcja L2 „Twoja praca” zamiast „Twoje czynności” | spór z recenzji, uwaga 24 |
| PW-2 | V2, V3 | dawna nazwa przycisku i ekranu V3 | przycisk „Sprawdź, którędy wejść”, nagłówek V3 „Którędy wejść do C-13” | dawna nazwa brzmiała jak rezerwacja terminu |
| PW-3 | V2, V3, C6, K1 | źródło „import” albo „obchód”; data weryfikacji bez słowa; K1 „Wejścia potwierdzone na obchodzie” | „Z map i opisów, stan z …”, „Sprawdzone na miejscu …”, „Sprawdzone …”; K1 „Wejścia sprawdzone na miejscu: x z y” | recenzja B3, uwaga 23; te same słowa dla Odwiedzającego i Koordynatora |
| PW-4 | wymagania wspólne, V1, V2, L1 | wskaźnik braku sieci bez napisu; „Do pierwszego uruchomienia”; „karta budynku” w komunikatach | „Bez internetu · dane z …”, a przy pierwszym otwarciu samo „Bez internetu” (decyzja 84); „opis budynku” | żargon dla osoby starszej |
| PW-5 | V4, V5, L1, L2 | rzeczownik „zgłoszenie” w komunikatach; „Anuluj zgłoszenie”; „Anuluj” w potwierdzeniu wycofania zgody | „zdjęcie bariery”, „bariera”; „Zrezygnuj”; „Zostaw zgodę”; treść według katalogu komunikatów | plan fazy B: bez słowa „zgłoszenie” dla Odwiedzającego |
| PW-6 | V2 | „Plan piętra niedostępny dla tego budynku”, „Zdjęcie niedostępne bez internetu”, zdanie o barierach „którą sprawdziliśmy” | „Nie mamy planu pięter tego budynku.”, „Zdjęcie zobaczysz po połączeniu z internetem.”, „Nie mamy jeszcze sprawdzonych barier w tym budynku…” | słowo „niedostępne” tylko o wejściu; prostsze zdanie |
| PW-7 | V4 | 6 przycisków grup barier z tekstem i ikoną; nazwy grup z klasyfikacji | pytanie „Czego dotyczy bariera?”, nazwy „Wejście i drzwi”, „Schody i windy”, „Przejścia i chodniki”, „Toalety”, „Napisy i oznaczenia”, „Coś innego”, bez ikon | NFR-21 nie obejmuje grup; brak trafnych ikon; limit 50% ikon |
| PW-8 | V4, C2, C6, C7, V1, V5 | „Zrób ponownie”; „+” i „−”; pole „Symbol budynku”; „Pomiń” | „Zrób nowe zdjęcie”; „Powiększ”, „Pomniejsz”; strzałki przesuwania mapy z nazwą „Przesuń mapę w górę” itd.; „Wpisz symbol budynku, np. C-13”; „Pomiń instrukcję” | napis mówi, co się stanie |
| PW-9 | V1, V2, V4, C1, C3, C4, R1, R2, K1, K3 | ładowanie w 1. osobie liczby pojedynczej, np. „Wczytuję”, „Ustalam”; „Nie znaleziono” | 1. osoba liczby mnogiej: „Wczytujemy”, „Ustalamy, gdzie jesteś”, „Nie znaleźliśmy” | jeden głos aplikacji, jak w „Wyślemy”, „Dziękujemy” |
| PW-10 | stany wspólne, V2, V3, C2, C3, C4, R1, R2, K2 | komunikaty z 3 zdaniami albo bez rady, co zrobić | treść według katalogu komunikatów; „Spróbuj ponownie” także przy błędzie serwera | zasada „co się stało i co zrobić” w 2 zdaniach |
| PW-11 | wymagania wspólne, V1, C5, K1 | ikona przy „Wstecz” nieopisana; „nieobejrzane” tekstem i ikoną | „Wstecz” ze strzałką i słowem: wyjątek dla nawigacji wstecz. Przyciski przesuwania mapy to strzałki z nazwą dla TalkBack. „Nieobejrzane” bez ikony, „Obejrzane” z ikoną `task` | limit 50% ikon; NFR-21 nie obejmuje znacznika |
| PW-12 | V2, L2, C3, C4, C5, C7 | „Nowe zgłoszenie z pomiarem”, „Nowe zgłoszenie”, „Bariery już nie ma”, „Piętra obejrzane”, „Cofnij”, „lokalne”. Konflikt bez napisu o skutku, „Anuluj” w C5. | „Zmierz i zgłoś barierę”, „Zgłoś, że bariery nie ma”, „Oznacz obejrzane piętra”, „Cofnij znacznik”, „W telefonie”. Napis „Druga wersja zostanie usunięta.”, „Zostaw znacznik”. | czasownik na przycisku; skutek przed decyzją |
| PW-13 | V1, V2, V3, C2, R2 | uciążliwość 1–3 bez napisów | „Utrudnienie”, „Tylko z pomocą”, „Blokada” dla wszystkich ról | słowa z kryteriów skali uciążliwości |
| PW-14 | C3, L2, R1, K2, K3 | „Wyślij ponownie do bieżącego zgłoszenia”, „Wyloguj”. Nagłówki „Kolejka weryfikacji”, „Zaproszenie”, „Eksport”. | „Wyślij ponownie” ze zdaniem o aktualnej wersji, „Wyloguj się”. Nagłówki „Zgłoszenia do sprawdzenia”, „Zaproś osobę”, „Pobierz eksport”. | nagłówek równy napisowi przycisku |
| PW-15 | V5 | instrukcja bez objaśnienia oznaczenia wejścia | zdanie z katalogu komunikatów: „Napis »Dostępne« przy wejściu znaczy, że wejdziesz bez pokonywania stopni…” | „Dostępne” bez objaśnienia można odczytać jako „otwarte”; definicja wejścia dostępnego ze słownika pojęć |

## Źródła

1. Treść komunikatów o błędach: co się stało i jak to naprawić (Nielsen Norman Group): <https://www.nngroup.com/articles/error-message-guidelines/>
2. L. Kane, „Usability for Older Adults: Challenges and Changes”, NN/g: <https://www.nngroup.com/articles/usability-for-senior-citizens/>
3. Open Iconic, zestaw ikon: <https://useiconic.com/open>
4. PlantUML Salt, ikony OpenIconic w makietach: <https://plantuml.com/salt>
5. WCAG 2.1 (W3C), m.in. 1.4.1 „Use of Color”, 3.2.4 „Consistent Identification”: <https://www.w3.org/TR/WCAG21/>
6. A. Harley, „Icon Usability”, NN/g (ikona z etykietą tekstową): <https://www.nngroup.com/articles/icon-usability/>
7. Kurs „Projektowanie doświadczeń użytkownika” (PWr), wymagania etapu 4: metafory i ikony, najwyżej 50% ikon w interfejsie.

Wewnętrzne źródła: [Wymagania do ekranów](wymagania-ekranow.md), [Słownik pojęć](../biznes/slownik.md), [Persony](persony.md), [Scenariusz wywiadu](scenariusz-wywiadu.md).

Dostęp do źródeł: 27.10.2026.
