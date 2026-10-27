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
| Wyjątki od trybu rozkazującego to utarte zwroty rezygnacji i przejścia, kierunki przesuwania mapy i odpowiedzi na pytanie z ekranu. | „Nie teraz”, „Dalej”, „Zdecyduję później”, „W górę”, „Poza budynkiem”, „Coś innego” | Brzmią naturalnie, a forma rozkazująca byłaby sztuczna. |
| Aplikacja mówi o sobie „my”, do osoby „Ty”, a zaimki pisze wielką literą. | „Wyślemy je, gdy wróci internet.” | Jeden głos na wszystkich ekranach. |
| Komunikat mówi, co się stało i co zrobić, w najwyżej 2 krótkich zdaniach. | „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” | NFR-15 (WCAG 3.3.1, 3.3.3), NFR-50 [1] |
| Komunikat nie obwinia osoby. | „Nie znaleźliśmy budynku” zamiast „Błędny symbol” | PE-1 przypisuje winę sobie i rezygnuje ([persony](persony.md)), a obwinianie siebie to częsta reakcja użytkowników [2]. |
| Dla Gościa i Odwiedzającego nie ma żargonu technicznego. | „Brak internetu” zamiast „tryb offline” | Lista zakazanych słów jest w tabeli niżej. |
| Bez anglicyzmów. | „Plik”, „Pobierz” | Wyjątek: „© OpenStreetMap contributors”, bo podpis wymaga NFR-37. |
| Jedno słowo ma jedno znaczenie w całej aplikacji. | „niedostępne” tylko o wejściu | Osoba nie myli wejścia z brakiem planu albo zdjęcia. |
| Daty jako 12.11.2026, liczby cyframi. | „3 kroki”, „Krok 2 z 3” | Cyfry czyta się szybciej niż słowa. |

Słowa, których Gość i Odwiedzający nie widzą:

| Pojęcie dokumentacji | Zamiast niego |
| --- | --- |
| synchronizacja | „Dane z 12.11.2026” |
| tryb offline, sieć | „Brak internetu” |
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
| przycisk „Wstecz” | „Wstecz” | wszystkie | Słowo bez ikony, zawsze w tym samym miejscu. | obserwacja w części 3 |
| wskaźnik braku sieci | „Brak internetu” | wszystkie | „Internet” zna każdy, a „sieć” i „offline” to żargon. | 3.2 |
| data ostatniej synchronizacji | „Dane z 12.11.2026” | Gość, Odwiedzający | Mówi, jak stare są dane, bez słowa „synchronizacja”. | 2.5, 4.3 |
| data ostatniej synchronizacji | „Ostatnia synchronizacja: 12.11.2026, 10:42” | Zbierający | Krótkie pojęcie dokumentacji. PE-2 ma wysokie umiejętności cyfrowe. | 10.3 |
| „Wróć do mapy” | „Wróć do mapy” | wszystkie | bez zmian | 4.8 |
| „Spróbuj ponownie” | „Spróbuj ponownie” | wszystkie | bez zmian | 3.2 |

## Napisy dla Gościa i Odwiedzającego (hipoteza, do potwierdzenia w wywiadach)

| Pojęcie w dokumentacji | Napis w aplikacji | Dla kogo (rola) | Dlaczego tak | Do sprawdzenia w wywiadzie (nr pytania) |
| --- | --- | --- | --- | --- |
| mapa kampusu (nagłówek V1) | „Mapa kampusu” | wszystkie | bez zmian | 1.3 |
| symbol budynku (pole V1) | „Wpisz symbol budynku, np. C-13” | wszystkie | Symbol wisi na budynku. Przykład pokazuje zapis, a czasownik mówi, co zrobić. | 1.3, część 5 |
| przełącznik „Mapa / Lista budynków” | „Mapa” i „Lista budynków” | wszystkie | Lista pomaga osobie, której mapa sprawia kłopot, i czytnikowi TalkBack. | część 5 |
| przyciski „+” i „−” | „Powiększ” i „Pomniejsz” | wszystkie | Sam znak „+” nie mówi, co powiększy. | obserwacja w części 3 |
| przyciski przesuwania mapy | „W górę”, „W dół”, „W lewo”, „W prawo” | wszystkie | Słowo zamiast samej strzałki. PE-1 nie lubi przeciągania. | obserwacja w części 3 |
| karta budynku (nagłówek V2) | „Budynek C-13” i nazwa z danych | wszystkie | Osoba szuka budynku, a nie „karty”. | 4.4 |
| karta budynku (w zdaniach) | „opis budynku” | Gość, Odwiedzający | Tego słowa używa pytanie 4.4 scenariusza. „Karta” kojarzy się z kartą płatniczą (hipoteza). | 4.4 |
| oznaczenie wejścia „dostępne” | „Dostępne” | wszystkie | Słowo z kryterium US2.1, krótkie. „Bez schodów” byłoby nieprawdą przy wejściu z podnośnikiem (recenzja B3, uwaga 12). | 4.1, 4.2, część 5 |
| oznaczenie wejścia „niedostępne” | „Niedostępne” | wszystkie | Przeciwieństwo „Dostępne”. To słowo nie pada w innym znaczeniu. | 4.1, 4.2 |
| oznaczenie wejścia „brak danych” | „Brak danych” | wszystkie | Mówi uczciwie, że nie wiemy, i różni się od „Niedostępne” (US17.1). | 4.3, 4.4 |
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
| plan piętra | „Plan piętra” | wszystkie | bez zmian | 6.3 |
| podpis pod planem (NFR-40) | „Źródło: Politechnika Wrocławska” | wszystkie | bez zmian; mówi, skąd jest plan | 2.2 |
| budynek podstawowy (napis na V2) | „Nie mamy planu pięter tego budynku.” | wszystkie | Słowo „niedostępny” zostaje dla wejścia. | 6.3 |
| „Zaplanuj wizytę” (US3) | „Sprawdź, którędy wejść” | wszystkie | „Zaplanuj wizytę” brzmi jak rezerwacja terminu. Ekran odpowiada na pytanie o wejście. | 2.1, 2.2, 4.1 |
| nagłówek V3 | „Którędy wejść do C-13” | wszystkie | Nagłówek powtarza napis przycisku. | 4.1, część 5 |
| zgłoszenie szybkie (nagłówek V4) | „Zgłoś barierę” | Odwiedzający | Ten sam napis co na pasku. | 4.6 |
| zgłoszenie (rzeczownik w komunikatach) | „zdjęcie bariery”, „bariera” | Gość, Odwiedzający | Osoba robi zdjęcie i to widzi. Plan fazy B zakazuje tu słowa „zgłoszenie”. | 3.4, 4.6 |
| „Krok 1 z 3” | „Krok 1 z 3” | Odwiedzający | bez zmian | 4.8 |
| przypomnienie o wizerunku | „Nie fotografuj twarzy ani tablic rejestracyjnych.” | Odwiedzający, Zbierający | bez zmian | 3.4 |
| „Zrób zdjęcie” | „Zrób zdjęcie” | Odwiedzający, Zbierający | bez zmian | 3.4 |
| „Zrób ponownie” | „Zrób nowe zdjęcie” | Odwiedzający, Zbierający | Mówi, że nowe zdjęcie zastąpi stare. | 3.4 |
| grupa barier (pytanie w kroku 2) | „Czego dotyczy bariera?” | Odwiedzający | Pytanie zamiast słowa „grupa”. | 4.5 |
| grupa „wejście i drzwi” | „Wejście i drzwi” | Odwiedzający | bez zmian | 2.3, 4.2 |
| grupa „komunikacja pionowa” | „Schody i windy” | Odwiedzający | „Komunikacja” kojarzy się z autobusem. | 2.3, 4.2 |
| grupa „komunikacja pozioma” | „Przejścia i chodniki” | Odwiedzający | Nazywa miejsca, w których są progi i krawężniki. | 2.3, 4.2 |
| grupa „toalety” | „Toalety” | Odwiedzający | bez zmian | 2.3 |
| grupa „orientacja i informacja” | „Napisy i oznaczenia” | Odwiedzający | „Orientacja” to słowo z przepisów. | 2.3, 5.1 |
| grupa „inne” | „Coś innego” | Odwiedzający | Brzmi jak odpowiedź na pytanie z kroku 2. | 4.5 |
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
| rola, lista ról | „Twoje role: Odwiedzający, Zbierający” | wszystkie zalogowane | bez zmian | 10.1 |
| „Twoje czynności” | „Twoja praca” | role pracownicze | „Czynności” brzmi urzędowo. Napis pasuje do „Konto i praca”. | 10.1 |
| klauzula informacyjna | „Informacja o danych osobowych” | wszystkie zalogowane | bez zmian | 6.3 |
| wycofanie zgody | „Wycofaj zgodę”; w potwierdzeniu „Zostaw zgodę” zamiast „Anuluj” | wszystkie zalogowane | Oba przyciski mówią o skutku, więc „Anuluj” nie myli. | 6.3 |
| „Wyloguj” | „Wyloguj się” | wszystkie zalogowane | Ta sama forma co „Zaloguj się”. | 6.3 |

## Napisy dla ról zespołu (hipoteza, do potwierdzenia w wywiadach)

Role zespołu pracują z aplikacją często i mają wysokie umiejętności cyfrowe (PE-2, PE-3, hipoteza). Zostają przy pojęciach dokumentacji, jeśli są krótkie. Zmieniamy tylko napisy niejasne albo bez czasownika.

| Pojęcie w dokumentacji | Napis w aplikacji | Dla kogo (rola) | Dlaczego tak | Do sprawdzenia w wywiadzie (nr pytania) |
| --- | --- | --- | --- | --- |
| pomiar precyzyjny („Nowe zgłoszenie z pomiarem”, „Nowe zgłoszenie”) | „Zmierz i zgłoś barierę” | Zbierający | Czasownik i ten sam napis w L2 i C3. Różni się od „Zgłoś barierę” Odwiedzającego. | 10.2 |
| nagłówek C1 | „Pomiar pozycji” | Zbierający | bez zmian | 10.2 |
| uśrednianie pozycji | „Pomiar: zostało 12 s” | Zbierający | Bez słowa „uśrednianie”. | 10.2 |
| dokładność pozycji | „Dokładność: 0,8 m (szacunek)” | Zbierający | bez zmian | 10.2 |
| liczba satelitów GNSS | „Satelity: GPS 7, Galileo 5” | Zbierający | Nazwy systemów zamiast skrótu „GNSS”. | 10.2 |
| mapa nieba | „Mapa nieba” | Zbierający | bez zmian | 10.2 |
| podpowiedź budynku | „Czy to budynek C-13?” i „Potwierdź”, „Wybierz inny”, „Poza budynkiem” | Zbierający | Pytanie zamiast słowa „podpowiedź”. | 10.2 |
| korekta ręczna | „Korekta ręczna” | Zbierający, Weryfikator | bez zmian | 10.2 |
| „Zmień miejsce”, „Powtórz pomiar”, „Dalej”, „Zapisz” | bez zmian | Zbierający | Czasowniki są już krótkie. | 10.2 |
| typ bariery (C2) | „Typ bariery” | Zbierający, Weryfikator | bez zmian | 10.2 |
| uciążliwość (C2, R2) | „Uciążliwość” i wartości „Utrudnienie”, „Tylko z pomocą”, „Blokada” z opisem | Zbierający, Weryfikator | Te same wartości, które widzi Odwiedzający. | 10.2, 10.5 |
| „półpiętro” | „Półpiętro” | Zbierający | bez zmian | 10.2 |
| status „lokalne” | „W telefonie” | Zbierający | Odpowiada na pytanie PE-2, czy zgłoszenie dotarło. | 10.3 |
| statusy „wysłane”, „w weryfikacji”, „przyjęte”, „odrzucone”, „archiwalne” | „Wysłane”, „W weryfikacji”, „Przyjęte”, „Odrzucone”, „Archiwalne” | Zbierający | Krótkie pojęcia dokumentacji. | 10.3, 10.5 |
| znak „zmiana czeka na wysłanie” | „Zmiana czeka na wysłanie” | Zbierający | bez zmian | 10.3 |
| konflikt wersji („wybierz wersję”) | „Dwie wersje: wybierz jedną” | Zbierający | Mówi, co się stało i co zrobić. | 10.4 |
| pole różniące się w konflikcie | „Inna wartość” | Zbierający | bez zmian; różnicę widać bez koloru (NFR-21) | 10.4 |
| „Wyślij ponownie do bieżącego zgłoszenia” | „Wyślij do aktualnego zgłoszenia” | Zbierający | Krócej i bez słowa „ponownie”, które sugeruje to samo zgłoszenie. | 10.4 |
| synchronizacja (nagłówek C4, „Synchronizuj teraz”) | „Synchronizacja”, „Synchronizuj teraz” | Zbierający | Krótkie pojęcie dokumentacji. | 10.3 |
| „Czeka na wysłanie: 3” | bez zmian | Zbierający | — | 10.3 |
| „Zostaw moją wersję”, „Weź wersję z serwera” | bez zmian; pod nimi skutek według katalogu komunikatów (C4, konflikt) | Zbierający | Skutek widać przed decyzją (recenzja B3, Nielsen 5). | 10.4 |
| „Zdecyduję później” | bez zmian | Zbierający | — | 10.4 |
| piętro obejrzane (przycisk „Piętra obejrzane” na V2) | „Oznacz obejrzane piętra” | Zbierający | Czasownik na przycisku. | 10.7 |
| nagłówek C5 | „Obejrzane piętra” | Zbierający, Koordynator | bez zmian treści | 10.7 |
| znacznik „obejrzane” / „nieobejrzane” | „Obejrzane”, „Nieobejrzane” | Zbierający, Koordynator | bez zmian | 10.7 |
| wiersz budynku podstawowego (C5) | „Cały budynek” | Zbierający, Koordynator | bez zmian | 10.7 |
| znacznik w telefonie (C5) | „Czeka na wysłanie” | Zbierający | Te same słowa co „Czeka na wysłanie: 3” na C3. | 10.3 |
| budynek podstawowy w K1 | „Obejrzany”, „Nieobejrzany” | Koordynator | Forma dla budynku; ikona jak przy znaczniku. | 10.7 |
| „Twój znacznik”, „znacznik innej osoby” | „Twój znacznik”, „Znacznik innej osoby” | Zbierający | bez zmian | 10.4 |
| „Oznacz jako obejrzane” | bez zmian | Zbierający | — | 10.7 |
| „Cofnij” (Zbierający) i „Cofnij znacznik” (Koordynator) | „Cofnij znacznik” dla obu ról | Zbierający, Koordynator | Ta sama czynność ma ten sam napis. | 10.7 |
| zmiana oznaczenia wejścia („Zmień oznaczenie”) | przycisk „Zmień oznaczenie”, nagłówek C6 „Zmień oznaczenie wejścia” | Zbierający | Przycisk stoi przy wejściu, więc słowo „wejścia” jest zbędne. | 10.1 |
| przyciski wyboru w C6 | „Dostępne”, „Niedostępne” | Zbierający | Te same słowa co na karcie budynku. | 10.1 |
| bariera usunięta („Bariery już nie ma”) | przycisk i nagłówek C7 „Zgłoś, że bariery nie ma”; rodzaj na C3 „Bariery już nie ma” | Zbierający | Czasownik na przycisku, opis stanu na liście. | 10.1 |
| „Popraw zgłoszenie”, „Wyślij do sprawdzenia” | bez zmian | Zbierający | „Sprawdzenie” zamiast „weryfikacji”, jak w R1. | 10.5 |
| kolejka weryfikacji (R1) | przycisk i nagłówek „Zgłoszenia do sprawdzenia” | Weryfikator | Nagłówek powtarza napis przycisku z L2. | 10.5 |
| „Wzięte przeze mnie”, „Weź do sprawdzenia” | bez zmian | Weryfikator | — | 10.5 |
| przełącznik „Lista / Mapa” (R1) | „Lista”, „Mapa” | Weryfikator | bez zmian | 10.5 |
| nagłówek R2 | „Sprawdzenie zgłoszenia” | Weryfikator | bez zmian | 10.5 |
| podobne zgłoszenie | „Podobne zgłoszenia do 10 m” | Weryfikator | Krócej niż „w promieniu 10 m”. | 10.4 |
| duplikat, „nieaktualne” | „Duplikat”, „Nieaktualne” | Weryfikator | bez zmian | 10.4 |
| „Przyjmij”, „Odrzuć”, powód odrzucenia | „Przyjmij”, „Odrzuć”, pole „Powód odrzucenia” | Weryfikator | bez zmian | 10.5 |
| zasłonięcie twarzy albo tablicy | „Zasłoń twarz lub tablicę” | Weryfikator | Czasownik na przycisku. | 10.5 |
| pseudonim autora | „Zbierający, Autor 17” | Weryfikator | bez zmian | 10.5 |
| postęp inwentaryzacji | „Postęp inwentaryzacji” | Koordynator | bez zmian | 10.7 |
| postęp budynku | „4 z 5 pięter, 80%” | Koordynator | bez zmian | 10.7 |
| liczby zgłoszeń w K1 | „Czeka na sprawdzenie: 3”, „Przyjęte: 12” | Koordynator | bez zmian | 10.7 |
| źródło „obchód” w K1 | „Wejścia sprawdzone na miejscu: 4 z 6” | Koordynator | Te same słowa co przy wejściu na karcie budynku. | 10.7 |
| „Odśwież” | „Odśwież” i „Stan z 10:42” | Koordynator | Godzina odczytu (recenzja B3, Nielsen 1). | 10.7 |
| zaproszenie | przycisk i nagłówek K2 „Zaproś osobę”, przycisk „Wyślij zaproszenie” | Koordynator | Nagłówek powtarza napis przycisku z L2. | 10.4 |
| pola K2 | „Adres e-mail”, „Rola” | Koordynator | bez zmian | 10.4 |
| eksport | przycisk i nagłówek K3 „Pobierz eksport”, przycisk „Pobierz plik” | Koordynator | Krótkie pojęcie dokumentacji. | 10.6 |
| formaty eksportu | „GeoJSON (WGS84)”, „CSV (PUWG 1992, EPSG:2180)” | Koordynator | Format i kod EPSG pozwalają otworzyć plik w QGIS bez zgadywania układu. | 10.6 |

## Katalog komunikatów (hipoteza, do potwierdzenia w wywiadach)

Każdy komunikat stanu z [wymagań do ekranów](wymagania-ekranow.md) w ostatecznym brzmieniu. Kolumna „Zmiana” odsyła do propozycji PW, gdy treść różni się od wymagań.

Komunikaty sprawdzimy w drugiej rozmowie (pytanie 6.3) i w prototypie papierowym, a przy komunikatach o zaufaniu słuchamy słów z pytań 4.3 i 4.4.

### Wszystkie ekrany

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| każdy ekran, brak sieci | „Brak internetu.” Obok, gdy dane są z telefonu: „Dane z 12.11.2026.” | — | PW-4 |
| ekrany R1, R2, K1–K3, brak sieci | „Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie.” | „Spróbuj ponownie” | — |
| ekrany R1, R2, K1–K3, błąd serwera | „Serwer chwilowo nie odpowiada, a dane w telefonie są bezpieczne. Spróbuj ponownie za kilka minut.” | „Spróbuj ponownie” | PW-10 |
| ekrany R1, R2, K1–K3, brak uprawnień | „Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie.” | „Wróć do mapy” | PW-10 |

### Gość i Odwiedzający

| Ekran i stan | Komunikat | Przyciski | Zmiana |
| --- | --- | --- | --- |
| V1, ładowanie | „Wczytujemy mapę kampusu…” | — | PW-9 |
| V1, brak wyniku | „Nie znaleźliśmy budynku »X«. Sprawdź symbol, np. C-13, albo wybierz budynek z listy.” | „Lista budynków” | PW-9 |
| V1, brak sieci przy pierwszym otwarciu | „Pierwsze otwarcie aplikacji wymaga internetu. Połącz się z internetem, a mapa wczyta się sama.” | — | PW-4 |
| V1, brak mapy ulic | „Mapa ulic chwilowo się nie wczytuje. Budynki i bariery nadal możesz oglądać.” | — | — |
| V2, ładowanie zdjęcia | „Wczytujemy zdjęcie…” | — | PW-9 |
| V2, brak barier | „Nie mamy jeszcze sprawdzonych barier w tym budynku. To nie znaczy, że barier tu nie ma.” | — | PW-6 |
| V2, brak planu (budynek podstawowy) | „Nie mamy planu pięter tego budynku. Bariery są na liście niżej.” | — | PW-6 |
| V2, zdjęcie bez internetu | „Zdjęcie zobaczysz po połączeniu z internetem.” | — | PW-6 |
| V2, budynku nie ma w telefonie | „Tego budynku nie ma jeszcze w telefonie. Połącz się z internetem, aby zobaczyć opis budynku.” | „Spróbuj ponownie” | PW-4 |
| V2, błąd zdjęcia | „Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę.” | „Spróbuj ponownie” | PW-10 |
| V3, jest wejście dostępne | „Budynek A-1 ma wejście dostępne: »nazwa wejścia«.” | — | — |
| V3, brak wejścia dostępnego | „Budynek B-4 nie ma wejścia dostępnego. Wróć do opisu budynku, aby zobaczyć bariery przy każdym wejściu.” | — | PW-10 |
| V3, brak danych o wejściach | „Nie wiemy jeszcze, czy budynek B-4 ma wejście dostępne: brak danych o 2 z 3 wejść. Wróć do opisu budynku, aby zobaczyć każde wejście z datą.” | — | PW-10 |
| V4, krok 1 | „Nie fotografuj twarzy ani tablic rejestracyjnych.” | „Zrób zdjęcie”, „Zrób nowe zdjęcie” | PW-8 |
| V4, krok 3 | „Dołączymy miejsce, w którym teraz stoisz.” | „Wyślij” | — |
| V4, brak zgody na lokalizację | „Aby wysłać zdjęcie bariery, potrzebujemy jej miejsca. Sprawdzimy je tylko teraz i nie będziemy Cię śledzić.” | „Pozwól na lokalizację”, „Wróć do mapy” | PW-5 |
| V4, odmowa zgody | „Bez lokalizacji nie wyślesz zdjęcia bariery. Możesz ją włączyć w ustawieniach telefonu.” | „Otwórz ustawienia”, „Wróć do mapy” | PW-5 |
| V4, zgoda tylko przybliżona | „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. Pozwól na dokładną lokalizację.” | „Pozwól na dokładną lokalizację”, „Wróć do mapy” | — |
| V4, lokalizacja wyłączona | „Lokalizacja w telefonie jest wyłączona. Włącz ją, aby wysłać zdjęcie bariery.” | „Otwórz ustawienia”, „Wróć do mapy” | PW-5 |
| V4, ustalanie miejsca | „Ustalamy, gdzie jesteś…” | „Wyślij” czeka | PW-9 |
| V4, brak pozycji | „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” | „Spróbuj ponownie”, „Zrezygnuj” | PW-5 |
| V4, potwierdzenie | „Dziękujemy. Sprawdzimy zdjęcie, a jeśli bariera się potwierdzi, pokażemy ją na mapie.” | „Wróć do mapy” | PW-5 |
| V4, potwierdzenie bez internetu | „Zapisaliśmy zdjęcie w telefonie. Wyślemy je sami, gdy wróci internet.” | „Wróć do mapy” | PW-5 |
| V4, za mało miejsca | „W telefonie brakuje miejsca, więc nie zapisaliśmy zdjęcia bariery. Usuń niepotrzebne zdjęcia albo filmy i spróbuj ponownie.” | „Spróbuj ponownie” | PW-5 |
| V5, zdanie o koncie | „Aby zgłosić barierę, potrzebne jest konto. Możesz je założyć teraz w »Konto«.” | „Dalej”, „Pomiń instrukcję” | PW-5 |
| V5, zdanie o wejściu | „Napis »Dostępne« przy wejściu znaczy, że wejdziesz bez pokonywania stopni: z poziomu chodnika, pochylnią albo podnośnikiem.” | „Dalej”, „Pomiń instrukcję” | PW-15 |

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
| C1, zgody na lokalizację | jak w V4, z końcówką „aby zapisać miejsce bariery” | jak w V4 | — |
| C2, zmiana zgłoszenia przyjętego | „Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.” | „Zapisz” | — |
| C2, błąd walidacji | „Brakuje typu bariery. Uzupełnij to pole, aby zapisać.” | — | PW-10 |
| C2, C6, C7, za mało miejsca | „Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.” | „Spróbuj ponownie” | — |
| C3, pusty | „Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę.” | „Zmierz i zgłoś barierę” | PW-12 |
| C3, ładowanie | „Sprawdzamy statusy…” | — | PW-9 |
| C3, błąd | „Nie udało się sprawdzić statusów, ale zgłoszenia są bezpieczne w telefonie. Spróbuj ponownie za chwilę.” | „Spróbuj ponownie” | PW-10 |
| C3, nieaktualne po usunięciu bariery | „Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa.” | „Zmierz i zgłoś barierę” | PW-12 |
| C4, pusty | „Wszystko wysłane. Ostatnia synchronizacja: 12.11.2026, 10:42.” | — | — |
| C4, wysyłanie | „Wysyłamy 2 z 5…” | — | PW-9 |
| C4, brak sieci | „Brak internetu. Wyślemy wszystko sami, gdy internet wróci.” | — | — |
| C4, błąd serwera | „Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później.” | — | — |
| C4, mało miejsca | „W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia.” | — | PW-10 |
| C4, konflikt | „Dwie wersje: wybierz jedną. Druga wersja zostanie usunięta.” Przy zgłoszeniu przyjętym drugie zdanie: „Przyjęte zgłoszenie zostanie, a Twoja wersja, jeśli ją wybierzesz, trafi do sprawdzenia jako nowe.” (UC11 3b) | „Zostaw moją wersję”, „Weź wersję z serwera”, „Zdecyduję później” | PW-12 |
| C5, potwierdzenie cofnięcia | „Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający.” | „Cofnij znacznik”, „Zostaw znacznik” | PW-12 |
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
| K2, błąd walidacji | „Adres e-mail jest niepełny, np. brakuje znaku @. Popraw go i wyślij ponownie.” albo „Wybierz rolę.” | — | PW-10 |
| K3, ładowanie | „Przygotowujemy plik…” | — | PW-9 |
| K3, pusty | „Nie ma jeszcze przyjętych barier. Pobrany plik będzie pusty.” | „Pobierz plik” | — |
| K3, sukces | „Plik zapisany w pobranych plikach.” | — | — |
| K3, informacja | „Plik zawiera tylko obecne, sprawdzone bariery, bez danych autorów.” | „Pobierz plik” | — |

## Metafory i ikony

Ikony pochodzą z zestawu OpenIconic dostępnego w PlantUML Salt [3], [4]. Każda ikona stoi obok tekstu, a tekst niesie znaczenie sam. Kolor nie niesie znaczenia (NFR-21) [5].

| Element | Ikona (nazwa z OpenIconic) | Tekst obok | Metafora / dlaczego zrozumiała |
| --- | --- | --- | --- |
| pasek dolny: mapa | `map` | „Mapa” | plan w ramce ze znacznikiem miejsca, jak w aplikacjach map |
| pasek dolny: zgłoszenie | `camera-slr` | „Zgłoś barierę” | zgłoszenie zaczyna się od zdjęcia (US4.1) |
| pasek dolny: pomoc | `question-mark` | „Pomoc” | znak zapytania, czyli „mam pytanie” |
| pasek dolny: konto | `person` | „Konto” albo „Konto i praca” | sylwetka, czyli „ja”; ta sama ikona dla obu napisów |
| wskaźnik braku sieci | `wifi` | „Brak internetu” | znak internetu znany z telefonu i kawiarni (hipoteza) |
| oznaczenie „dostępne” | `check` | „Dostępne” | ptaszek, czyli „tak, da się” |
| oznaczenie „niedostępne” | `x` | „Niedostępne” | krzyżyk, czyli „nie” |
| oznaczenie „brak danych” | `minus` | „Brak danych” | kreska jak w pustej rubryce formularza; inna niż krzyżyk (US17.1) |
| uciążliwość 1 | `warning` | „Utrudnienie” | trójkąt jak drogowy znak ostrzegawczy: uważaj |
| uciążliwość 2 | `people` | „Tylko z pomocą” | dwie osoby: przejdziesz z drugą osobą |
| uciążliwość 3 | `ban` | „Blokada” | koło z kreską jak znak zakazu: tędy nie |
| przycisk zdjęcia (V4, C2, C6, C7) | `camera-slr` | „Zrób zdjęcie” | aparat; ta sama ikona co „Zgłoś barierę” |
| status „lokalne” | `phone` | „W telefonie” | kształt telefonu: zgłoszenie jest tylko w nim |
| status „wysłane” | `data-transfer-upload` | „Wysłane” | strzałka w górę: poszło dalej |
| status „w weryfikacji” | `eye` | „W weryfikacji” | oko: ktoś teraz ogląda |
| status „przyjęte” | `circle-check` | „Przyjęte” | ptaszek w kole: decyzja na tak |
| status „odrzucone” | `circle-x` | „Odrzucone” | krzyżyk w kole: decyzja na nie |
| status „archiwalne” | `box` | „Archiwalne” | pudło na stare dokumenty |
| znacznik „obejrzane” | `task` | „Obejrzane” | odhaczona pozycja na liście kontrolnej |

Zasady ikon:

| Zasada | Skutek |
| --- | --- |
| Jedna ikona ma jedno znaczenie. | `question-mark` oznacza tylko pomoc, dlatego „Brak danych” ma `minus`. Lupa nie występuje, bo kojarzy się z wyszukiwaniem. |
| Ikona tylko tam, gdzie wymaga jej NFR-21 albo wymagania wspólne, oraz na pasku dolnym. | Status, oznaczenie wejścia, uciążliwość, znacznik i wskaźnik sieci mają ikonę. Pasek dolny ma ikony, bo widać go na 5 ekranach i osoba uczy się ich raz. „Zrób zdjęcie” powtarza ikonę paska. |
| Reszta elementów jest bez ikony. | „Wstecz”, przyciski mapy, 6 grup barier, wszystkie przyciski czynności, „Nieobejrzane”. Słowo jest dla PE-1 jaśniejsze niż strzałka albo symbol [6]. |
| Grupy barier w V4 są bez ikon. | OpenIconic nie ma ikon toalety, drzwi ani chodnika, a ikona na siłę myli. US4.3 wymaga tylko dużych przycisków (PW-7). |

### Udział ikon

Element to jedno miejsce na ekranie, które osoba czyta osobno: napis, przycisk, pole albo oznaczenie. Trzy wartości oznaczenia wejścia to jeden element. Pasek dolny to 4 elementy, a nagłówek, „Wstecz” i wskaźnik sieci liczymy na każdym ekranie, na którym są.

| Ekran | Elementy | Z ikoną | Udział | Elementy z ikoną |
| --- | --- | --- | --- | --- |
| V1 Mapa kampusu | 17 | 6 | 35% | wskaźnik sieci, uciążliwość, pasek (4) |
| V2 Budynek | 18 | 7 | 39% | wskaźnik sieci, oznaczenie, uciążliwość, pasek (4) |
| V3 Którędy wejść (stan zwykły) | 15 | 7 | 47% | wskaźnik sieci, oznaczenie, uciążliwość, pasek (4) |
| V4 Zgłoś barierę | 20 | 2 | 10% | wskaźnik sieci, „Zrób zdjęcie” |
| V5 Jak korzystać z aplikacji | 12 | 5 | 42% | wskaźnik sieci, pasek (4) |
| L1 Konto (Gość) | 8 | 1 | 13% | wskaźnik sieci |
| L2 Konto albo Konto i praca (wszystkie role) | 18 | 5 | 28% | wskaźnik sieci, pasek (4) |
| C1 Pomiar pozycji | 19 | 1 | 5% | wskaźnik sieci |
| C2 Formularz zgłoszenia | 13 | 3 | 23% | wskaźnik sieci, uciążliwość, „Zrób zdjęcie” |
| C3 Moje zgłoszenia | 13 | 2 | 15% | wskaźnik sieci, status |
| C4 Synchronizacja | 12 | 1 | 8% | wskaźnik sieci |
| C5 Obejrzane piętra | 10 | 2 | 20% | wskaźnik sieci, znacznik |
| C6 Zmień oznaczenie wejścia | 12 | 5 | 42% | wskaźnik sieci, obecne oznaczenie, „Dostępne”, „Niedostępne”, „Zrób zdjęcie” |
| C7 Zgłoś, że bariery nie ma | 8 | 2 | 25% | wskaźnik sieci, „Zrób zdjęcie” |
| R1 Zgłoszenia do sprawdzenia | 8 | 1 | 13% | wskaźnik sieci |
| R2 Sprawdzenie zgłoszenia | 16 | 4 | 25% | wskaźnik sieci, uciążliwość, status podobnych, oznaczenie |
| K1 Postęp inwentaryzacji | 11 | 2 | 18% | wskaźnik sieci, znacznik |
| K2 Zaproś osobę | 6 | 1 | 17% | wskaźnik sieci |
| K3 Pobierz eksport | 6 | 1 | 17% | wskaźnik sieci |
| **Razem** | **242** | **58** | **24%** | — |

Tabela liczy każdy ekran w stanie typowym: bez sieci, z jednym wejściem i jedną barierą. V3 ma mało treści, więc jego udział zależy od stanu:

| Stan V3 | Elementy | Z ikoną | Udział | Elementy z ikoną |
| --- | --- | --- | --- | --- |
| zwykły: 1 wejście dostępne, 1 bariera, bez sieci | 15 | 7 | 47% | wskaźnik sieci, oznaczenie, uciążliwość, pasek (4) |
| 1 wejście dostępne bez barier, bez sieci | 11 | 6 | 55% | wskaźnik sieci, oznaczenie, pasek (4) |
| brak wejścia dostępnego albo brak danych, z siecią | 7 | 4 | 57% | pasek (4) |
| brak wejścia dostępnego albo brak danych, bez sieci | 8 | 5 | 63% | wskaźnik sieci, pasek (4) |

Limit kursu, najwyżej 50% ikon, mierzymy dla całego interfejsu i dla typowego stanu każdego ekranu. Cały interfejs ma 24%, a najwięcej w stanie typowym ma V3, 47%. Stany V3 powyżej 50% to świadomy wyjątek.

Oznaczenie wejścia i wskaźnik sieci zawsze mają tekst i ikonę (NFR-21, kryterium WCAG „Use of Color” [5]). Pasek dolny wygląda tak samo na każdym ekranie. Nie usuwamy tych ikon, żeby zmieścić się w limicie, bo każda stoi obok tekstu i tekst niesie znaczenie sam.

## Nazwa wejścia do pracy zespołu {#nazwa-wejscia}

Recenzja wymagań do ekranów (uwaga 24, spór) wskazała, że cała praca Zbierającego, Weryfikatora i Koordynatora zaczyna się w „Konto”. Osoba szukająca „Moich zgłoszeń” nie skojarzy ich z kontem. Strukturę nawigacji ocenia osobno mapa nawigacji (zadanie B4), a ta sekcja wybiera tylko nazwę.

| Wariant | Za | Przeciw |
| --- | --- | --- |
| „Konto” (bez zmian) | Jedna nazwa dla wszystkich ról. | Praca ról zespołu jest ukryta pod nazwą, która jej nie zapowiada (Nielsen 2, 6). |
| „Moja praca” | Nazywa główną czynność ról zespołu na L2. | Ukrywa „Wyloguj się” i „Informację o danych osobowych”. Zmienia drogę do klauzuli „Konto, Informacja o danych osobowych” (NFR-30). |
| **„Konto i praca” (wybrany)** | Zaczyna się od „Konto”, więc instrukcja V5 i droga do klauzuli zostają prawdziwe. Słowo „praca” zapowiada czynności ról. | Dłuższy napis, ale tej samej długości co „Zgłoś barierę” (13 znaków), więc pasek się nie zmienia. |

Decyzja: „Konto i praca” widzi każda osoba z co najmniej jedną rolą pracowniczą, na pasku dolnym i w nagłówku L2. Gość i Odwiedzający widzą „Konto”. Ikona `person` zostaje ta sama, więc pasek wygląda tak samo dla wszystkich ról. Sekcja na L2 nazywa się „Twoja praca”.

## Propozycje do wymagań ekranów {#propozycje}

Strona [wymagań do ekranów](wymagania-ekranow.md) zostaje bez zmian. Poniższe zmiany wprowadzi jej właściciel.

| Nr | Ekran | Teraz w wymaganiach | Propozycja | Dlaczego |
| --- | --- | --- | --- | --- |
| PW-1 | pasek dolny, L2 | „Konto” dla wszystkich ról | „Konto i praca” dla ról pracowniczych; sekcja L2 „Twoja praca” zamiast „Twoje czynności” | spór z recenzji, uwaga 24 |
| PW-2 | V2, V3 | „Zaplanuj wizytę”; nazwa robocza V3 „Zaplanuj wizytę” | przycisk „Sprawdź, którędy wejść”, nagłówek V3 „Którędy wejść do C-13” | „Zaplanuj wizytę” brzmi jak rezerwacja terminu |
| PW-3 | V2, V3, C6, K1 | źródło „import” albo „obchód”; data weryfikacji bez słowa; K1 „Wejścia potwierdzone na obchodzie” | „Z map i opisów, stan z …”, „Sprawdzone na miejscu …”, „Sprawdzone …”; K1 „Wejścia sprawdzone na miejscu: x z y” | recenzja B3, uwaga 23; te same słowa dla Odwiedzającego i Koordynatora |
| PW-4 | wymagania wspólne, V1, V2, L1 | wskaźnik braku sieci bez napisu; „Do pierwszego uruchomienia”; „karta budynku” w komunikatach | „Brak internetu”; „Pierwsze otwarcie aplikacji”; „opis budynku” | żargon dla osoby starszej |
| PW-5 | V4, V5, L1, L2 | rzeczownik „zgłoszenie” w komunikatach; „Anuluj zgłoszenie”; „Anuluj” w potwierdzeniu wycofania zgody | „zdjęcie bariery”, „bariera”; „Zrezygnuj”; „Zostaw zgodę”; treść według katalogu komunikatów | plan fazy B: bez słowa „zgłoszenie” dla Odwiedzającego |
| PW-6 | V2 | „Plan piętra niedostępny dla tego budynku”, „Zdjęcie niedostępne bez internetu”, zdanie o barierach „którą sprawdziliśmy” | „Nie mamy planu pięter tego budynku. Bariery są na liście niżej.”, „Zdjęcie zobaczysz po połączeniu z internetem.”, „Nie mamy jeszcze sprawdzonych barier w tym budynku…” | słowo „niedostępne” tylko o wejściu; prostsze zdanie |
| PW-7 | V4 | 6 przycisków grup barier z tekstem i ikoną; nazwy grup z klasyfikacji | pytanie „Czego dotyczy bariera?”, nazwy „Wejście i drzwi”, „Schody i windy”, „Przejścia i chodniki”, „Toalety”, „Napisy i oznaczenia”, „Coś innego”, bez ikon | NFR-21 nie obejmuje grup; brak trafnych ikon; próg 50% |
| PW-8 | V4, C2, C6, C7, V1, V5 | „Zrób ponownie”; „+” i „−”; pole „Symbol budynku”; „Pomiń” | „Zrób nowe zdjęcie”; „Powiększ”, „Pomniejsz”, „W górę”, „W dół”, „W lewo”, „W prawo”; „Wpisz symbol budynku, np. C-13”; „Pomiń instrukcję” | napis mówi, co się stanie |
| PW-9 | V1, V2, V4, C1, C3, C4, R1, R2, K1, K3 | ładowanie w 1. osobie liczby pojedynczej, np. „Wczytuję”, „Ustalam”; „Nie znaleziono” | 1. osoba liczby mnogiej: „Wczytujemy”, „Ustalamy, gdzie jesteś”, „Nie znaleźliśmy” | jeden głos aplikacji, jak w „Wyślemy”, „Dziękujemy” |
| PW-10 | stany wspólne, V2, V3, C2, C3, C4, R1, R2, K2 | komunikaty z 3 zdaniami albo bez rady, co zrobić | treść według katalogu komunikatów; „Spróbuj ponownie” także przy błędzie serwera | zasada „co się stało i co zrobić” w 2 zdaniach |
| PW-11 | wymagania wspólne, C5, K1 | ikona przy „Wstecz” nieopisana; „nieobejrzane” tekstem i ikoną | „Wstecz” bez ikony; „Nieobejrzane” bez ikony, „Obejrzane” z ikoną `task` | próg 50% na V3; NFR-21 nie obejmuje znacznika |
| PW-12 | V2, L2, C3, C4, C5, C7 | „Nowe zgłoszenie z pomiarem”, „Nowe zgłoszenie”, „Bariery już nie ma”, „Piętra obejrzane”, „Cofnij”, „lokalne”. Konflikt bez napisu o skutku, „Anuluj” w C5. | „Zmierz i zgłoś barierę”, „Zgłoś, że bariery nie ma”, „Oznacz obejrzane piętra”, „Cofnij znacznik”, „W telefonie”. Napis „Druga wersja zostanie usunięta.”, „Zostaw znacznik”. | czasownik na przycisku; skutek przed decyzją |
| PW-13 | V1, V2, V3, C2, R2 | uciążliwość 1–3 bez napisów | „Utrudnienie”, „Tylko z pomocą”, „Blokada” dla wszystkich ról | słowa z kryteriów skali uciążliwości |
| PW-14 | C3, L2, R1, K2, K3 | „Wyślij ponownie do bieżącego zgłoszenia”, „Wyloguj”. Nagłówki „Kolejka weryfikacji”, „Zaproszenie”, „Eksport”. | „Wyślij do aktualnego zgłoszenia”, „Wyloguj się”. Nagłówki „Zgłoszenia do sprawdzenia”, „Zaproś osobę”, „Pobierz eksport”. | nagłówek równy napisowi przycisku |
| PW-15 | V5 | instrukcja bez objaśnienia oznaczenia wejścia | zdanie z katalogu komunikatów: „Napis »Dostępne« przy wejściu znaczy, że wejdziesz bez pokonywania stopni…” | „Dostępne” bez objaśnienia można odczytać jako „otwarte”; definicja wejścia dostępnego ze słownika pojęć |

## Źródła

1. Treść komunikatów o błędach: co się stało i jak to naprawić (Nielsen Norman Group): <https://www.nngroup.com/articles/error-message-guidelines/>
2. L. Kane, „Usability for Older Adults: Challenges and Changes”, NN/g: <https://www.nngroup.com/articles/usability-for-senior-citizens/>
3. Open Iconic, zestaw ikon: <https://useiconic.com/open>
4. PlantUML Salt, ikony OpenIconic w makietach: <https://plantuml.com/salt>
5. WCAG 2.1 (W3C), m.in. 1.4.1 „Use of Color”, 3.2.4 „Consistent Identification”: <https://www.w3.org/TR/WCAG21/>
6. A. Harley, „Icon Usability”, NN/g (ikona z etykietą tekstową): <https://www.nngroup.com/articles/icon-usability/>

Wewnętrzne źródła: [Wymagania do ekranów](wymagania-ekranow.md), [Słownik pojęć](../biznes/slownik.md), [Persony](persony.md), [Scenariusz wywiadu](scenariusz-wywiadu.md).

Dostęp do źródeł: 27.10.2026.
