# Słownik użytkownika

!!! warning "Napisy to hipoteza, do potwierdzenia w wywiadach"
    Wszystkie napisy i komunikaty na tej stronie to propozycje zespołu. Sprawdzimy je w wywiadach ze [scenariusza](scenariusz-wywiadu.md) i w prototypie papierowym, a potem poprawimy.

Strona zamienia pojęcia dokumentacji na napisy w aplikacji. Gość i Odwiedzający dostają słowa zrozumiałe dla osoby starszej, bo dla nich projektujemy najpierw ([PE-1](persony.md)). Role zespołu mogą widzieć krótkie pojęcia z [słownika pojęć](../biznes/slownik.md).

Słownik pojęć zostaje źródłem nazw w dokumentach i w kodzie. Ta strona ustala tylko to, co osoba czyta na ekranie. Robocze etykiety pochodzą z [wymagań do ekranów](wymagania-ekranow.md), a zmiany w nich zbiera sekcja [Propozycje do wymagań ekranów](#propozycje).

## Zasady napisów

| Zasada | Przykład | Dlaczego |
| --- | --- | --- |
| Przycisk czynności zaczyna się od czasownika w trybie rozkazującym. | „Zrób zdjęcie”, „Sprawdź, którędy wejść” | Osoba wie, co się stanie po dotknięciu. |
| Przycisk do listy albo ekranu nosi nazwę tego miejsca, taką jak nagłówek ekranu docelowego. | „Moje zgłoszenia” prowadzi do ekranu „Moje zgłoszenia” | Osoba sprawdza, czy trafiła tam, gdzie chciała (NFR-15, WCAG 3.2.4). |
| Wyjątki od trybu rozkazującego to utarte zwroty rezygnacji i przejścia. | „Nie teraz”, „Dalej”, „Zdecyduję później” | Brzmią naturalnie, a forma rozkazująca byłaby sztuczna. |
| Aplikacja mówi o sobie „my”, do osoby „Ty”, z wielkiej litery w zaimkach. | „Wyślemy je, gdy wróci internet.” | Jeden głos na wszystkich ekranach. |
| Komunikat mówi, co się stało i co zrobić, w najwyżej 2 krótkich zdaniach. | „Telefon nie ustalił, gdzie jesteś. Podejdź do okna albo wyjdź na zewnątrz i spróbuj ponownie.” | NFR-15 (WCAG 3.3.1, 3.3.3), NFR-50 [1] |
| Komunikat nie obwinia osoby. | „Nie znaleźliśmy budynku” zamiast „Błędny symbol” | PE-1 przypisuje winę sobie i rezygnuje [2]. |
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
| oznaczenie wejścia „niedostępne” | „Niedostępne” | wszystkie | Para do „Dostępne”. To słowo nie pada w innym znaczeniu. | 4.1, 4.2 |
| oznaczenie wejścia „brak danych” | „Brak danych” | wszystkie | Mówi uczciwie, że nie wiemy, i różni się od „Niedostępne” (US17.1). | 4.3, 4.4 |
| wejście dostępne (zdanie na V3) | „Budynek A-1 ma wejście dostępne: »nazwa wejścia«.” | wszystkie | bez zmian; zdanie powtarza słowo z oznaczenia | 4.1 |
| źródło oznaczenia „obchód” z datą | „Sprawdzone na miejscu 12.11.2026” | wszystkie | Mówi, że ktoś tam był. To buduje zaufanie (N2). | 2.5, 4.3, 4.4 |
| źródło oznaczenia „import” z datą | „Z map i opisów, stan z 1.10.2026” | wszystkie | Import bierze dane z planów PWr i z OpenStreetMap. „Dane uczelni” byłyby nieprawdą przy OpenStreetMap (recenzja B3, uwaga 23). | 2.2, 4.3 |
| data weryfikacji | „Sprawdzone 12.11.2026” | wszystkie | „Weryfikacja” to żargon, a data budzi zaufanie (N2). | 2.5, 4.4, 5.1 |
| zgłoszenie przyjęte (listy na V1–V3) | nagłówek „Bariery w budynku”, w wierszu typ bariery | Gość, Odwiedzający | Osoba widzi bariery, a nie zgłoszenia. | 2.3, 4.5 |
| typ bariery | nazwa z klasyfikacji, np. „niesprawna winda” | wszystkie | Nazwy są potoczne. Wyjątek TB-22 opisuje opis PR (propozycja dla słownika pojęć). | 2.3, 4.2 |
| uciążliwość 1 „utrudnienie” | „Utrudnienie” | wszystkie | bez zmian | 2.3, 4.2 |
| uciążliwość 2 „poważne utrudnienie” | „Potrzebna pomoc” | wszystkie | Kryterium stopnia 2 to „tylko z pomocą”. Słowo „poważne” każdy rozumie inaczej. | 2.4, 4.2 |
| uciążliwość 3 „blokada” | „Blokada” | wszystkie | bez zmian; krótkie i jednoznaczne | 4.2 |
| przełącznik pięter | „Piętro:” i etykiety z danych PWr | wszystkie | Bez słowa „przełącznik”. | 6.3 |
| plan piętra | „Plan piętra” | wszystkie | bez zmian | 6.3 |
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
| „Twoje czynności” | „Twoja praca” | role pracownicze | „Czynności” brzmi urzędowo. To para do „Konto i praca”. | 10.1 |
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
| uciążliwość (C2, R2) | „Uciążliwość” i wartości „Utrudnienie”, „Potrzebna pomoc”, „Blokada” z opisem | Zbierający, Weryfikator | Te same wartości, które widzi Odwiedzający. | 10.2, 10.5 |
| „półpiętro” | „Półpiętro” | Zbierający | bez zmian | 10.2 |
| status „lokalne” | „W telefonie” | Zbierający | Odpowiada na pytanie PE-2, czy zgłoszenie dotarło. | 10.3 |
| statusy „wysłane”, „w weryfikacji”, „przyjęte”, „odrzucone”, „archiwalne” | „Wysłane”, „W weryfikacji”, „Przyjęte”, „Odrzucone”, „Archiwalne” | Zbierający | Krótkie pojęcia dokumentacji. | 10.3, 10.5 |
| znak „zmiana czeka na wysłanie” | „Zmiana czeka na wysłanie” | Zbierający | bez zmian | 10.3 |
| konflikt wersji („wybierz wersję”) | „Dwie wersje: wybierz jedną” | Zbierający | Mówi, co się stało i co zrobić. | 10.4 |
| „Wyślij ponownie do bieżącego zgłoszenia” | „Wyślij do aktualnego zgłoszenia” | Zbierający | Krócej i bez słowa „ponownie”, które sugeruje to samo zgłoszenie. | 10.4 |
| synchronizacja (nagłówek C4, „Synchronizuj teraz”) | „Synchronizacja”, „Synchronizuj teraz” | Zbierający | Krótkie pojęcie dokumentacji. | 10.3 |
| „Czeka na wysłanie: 3” | bez zmian | Zbierający | — | 10.3 |
| „Zostaw moją wersję”, „Weź wersję z serwera” | bez zmian; pod nimi „Druga wersja zostanie usunięta.” | Zbierający | Skutek widać przed decyzją (recenzja B3, Nielsen 5). | 10.4 |
| „Zdecyduję później” | bez zmian | Zbierający | — | 10.4 |
| piętro obejrzane (przycisk „Piętra obejrzane” na V2) | „Oznacz obejrzane piętra” | Zbierający | Czasownik na przycisku. | 10.7 |
| nagłówek C5 | „Obejrzane piętra” | Zbierający, Koordynator | bez zmian treści | 10.7 |
| znacznik „obejrzane” / „nieobejrzane” | „Obejrzane”, „Nieobejrzane” | Zbierający, Koordynator | bez zmian | 10.7 |
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
| formaty eksportu | „GeoJSON (WGS84)”, „CSV (PUWG 1992, EPSG:2180)” | Koordynator | Tych nazw używa QGIS. | 10.6 |

## Źródła

1. Treść komunikatów o błędach: co się stało i jak to naprawić (Nielsen Norman Group): <https://www.nngroup.com/articles/error-message-guidelines/>
2. L. Kane, „Usability for Older Adults: Challenges and Changes”, NN/g: <https://www.nngroup.com/articles/usability-for-senior-citizens/>
3. Open Iconic, zestaw ikon: <https://useiconic.com/open>
4. PlantUML Salt, ikony OpenIconic w makietach: <https://plantuml.com/salt>
5. WCAG 2.1 (W3C), m.in. 1.4.1 „Use of Color”, 3.2.4 „Consistent Identification”: <https://www.w3.org/TR/WCAG21/>
6. A. Harley, „Icon Usability”, NN/g (ikona z etykietą tekstową): <https://www.nngroup.com/articles/icon-usability/>

Wewnętrzne źródła: [Wymagania do ekranów](wymagania-ekranow.md), [Słownik pojęć](../biznes/slownik.md), [Persony](persony.md), [Scenariusz wywiadu](scenariusz-wywiadu.md).

Dostęp do źródeł: 27.10.2026.
