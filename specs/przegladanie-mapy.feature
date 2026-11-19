# language: pl
Funkcja: Przeglądanie mapy kampusu i karty budynku
  Gość niezalogowany i Odwiedzający sprawdzają przed wizytą, czy wejdą do budynku
  i którym wejściem (N1, N2). Mapa pokazuje tylko zgłoszenia przyjęte.

  Założenia:
    Zakładając, że w aplikacji są między innymi budynki szczegółowe A-1, B-1, C-13, D-1 i D-2
    Oraz budynek A-1 ma 6 wejść, z których wejście główne ma schody, a wejściem dostępnym jest inne wejście
    # Dane testowe dla US3.1, a nie stan faktyczny budynku B-8.
    Oraz w danych testowych budynek podstawowy B-8 ma wejścia wyłącznie z oznaczeniem „niedostępne”

  @US1.1 @aplikacja
  Szablon scenariusza: Wyszukanie budynku po symbolu w różnym zapisie
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wyszuka „<zapis>”
    Wtedy widzi budynek C-13 w wynikach wyszukiwania

    Przykłady:
      | zapis |
      | C13   |
      | c-13  |
      | C-13  |

  @US1.1 @aplikacja
  Scenariusz: Podpowiedzi budynków od pierwszego znaku symbolu
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wpisze w pole wyszukiwania „A”
    Wtedy widzi w wynikach wyszukiwania budynki A-1, A-2 i A-10 w tej kolejności
    Ale nie widzi komunikatu „Nie znaleźliśmy budynku”

  @US1.1 @aplikacja
  Szablon scenariusza: Wyszukanie budynku po nazwie
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wpisze w pole wyszukiwania „<tekst>”
    Wtedy widzi budynek A-1 „Budynek główny” w wynikach wyszukiwania

    Przykłady:
      | tekst  |
      | glow   |
      | GŁÓWNY |

  @US1.2 @aplikacja
  Scenariusz: Mapa i wyszukiwanie działają bez logowania
    Zakładając, że Gość niezalogowany pierwszy raz otwiera aplikację i pomija instrukcję
    Jeżeli wyszuka „A-1”
    Wtedy widzi mapę kampusu z konturami budynków
    Oraz widzi budynek A-1 w wynikach wyszukiwania
    Ale aplikacja nie prosi go o logowanie

  @US1.3 @BR-6 @serwer
  Szablon scenariusza: Na mapie są tylko zgłoszenia przyjęte
    Zakładając, że przy wejściu głównym A-1 jest zgłoszenie TB-1 „schody przy wejściu” ze statusem „<status>”
    Jeżeli <osoba> otworzy mapę kampusu przy budynku A-1
    Wtedy to zgłoszenie <widoczność> na mapie

    Przykłady:
      | osoba              | status        | widoczność          |
      | Gość niezalogowany | lokalne       | nie jest widoczne   |
      | Gość niezalogowany | wysłane       | nie jest widoczne   |
      | Gość niezalogowany | w weryfikacji | nie jest widoczne   |
      | Gość niezalogowany | odrzucone     | nie jest widoczne   |
      | Gość niezalogowany | archiwalne    | nie jest widoczne   |
      | Gość niezalogowany | przyjęte      | jest widoczne       |
      | Odwiedzający       | lokalne       | nie jest widoczne   |
      | Odwiedzający       | wysłane       | nie jest widoczne   |
      | Odwiedzający       | w weryfikacji | nie jest widoczne   |
      | Odwiedzający       | odrzucone     | nie jest widoczne   |
      | Odwiedzający       | archiwalne    | nie jest widoczne   |
      | Odwiedzający       | przyjęte      | jest widoczne       |

  @US2.1 @aplikacja
  Scenariusz: Wejście ma oznaczenie znakiem z mapy i słowem, nie tylko kolorem
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy listę wejść
    Wtedy każde z 6 wejść ma słowo „dostępne” albo „niedostępne” i znak tego wejścia z mapy
    Oraz oznaczenie nie ma barwnego tła
    Oraz wejście główne ma napis „niedostępne”
    Oraz oznaczenia wejść da się odróżnić w odcieniach szarości

  @US2.1 @BR-25 @aplikacja
  Scenariusz: Wejście zalecane stoi pierwsze na karcie budynku
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy listę wejść
    Wtedy pierwsze jest inne wejście z napisem „Zalecane” i oznaczeniem „dostępne”
    Ale wejście główne nie ma napisu „Zalecane”

  @BR-25 @aplikacja
  Scenariusz: Przy kilku wejściach dostępnych zalecane jest wejście główne
    # Dane testowe, a nie stan faktyczny budynku C-13.
    Zakładając, że w danych testowych wejście główne i wejście boczne C-13 mają oznaczenie „dostępne”
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy pierwsze jest wejście główne z napisem „Zalecane”
    Ale wejście boczne nie ma napisu „Zalecane”

  @BR-25 @aplikacja
  Scenariusz: Przy kilku wejściach dostępnych bez głównego zalecane jest pierwsze z danych
    # Dane testowe, a nie stan faktyczny budynku C-13.
    Zakładając, że w danych testowych wejście boczne i wejście od dziedzińca C-13 mają oznaczenie „dostępne”
    Oraz wejście główne C-13 ma oznaczenie „niedostępne”
    Oraz oba wejścia dostępne mają nazwę w danych
    Oraz wejście boczne jest pierwsze w kolejności z danych
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy pierwsze jest wejście boczne z napisem „Zalecane”
    Ale wejście od dziedzińca nie ma napisu „Zalecane”

  @US2.1 @BR-25 @aplikacja
  Scenariusz: Wejścia z jednym oznaczeniem stoją na karcie alfabetycznie
    # Dane testowe, a nie stan faktyczny budynku C-13.
    Zakładając, że w danych testowych wejścia C-13 „Wejście "E"”, „Wejście "A"” i „Wejście "C"” mają oznaczenie „niedostępne”
    Oraz jedno wejście C-13 z oznaczeniem „niedostępne” nie ma nazwy w danych
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy wejścia „niedostępne” stoją w kolejności „Wejście "A"”, „Wejście "C"”, „Wejście "E"”
    Oraz wejście bez nazwy w danych stoi po nich

  @US2.1 @US3.3 @BR-27 @aplikacja
  Scenariusz: Wejście bez nazwy w danych ma na karcie opis położenia pod nazwą
    # Dane testowe, a nie stan faktyczny budynku C-19.
    Zakładając, że w danych testowych jedyne wejście C-19 z oznaczeniem „dostępne” nie ma nazwy
    Oraz ma uwagę uczelni „Do wejścia prowadzi rampa.” i drzwi 100 cm
    Oraz serwer podaje dla niego opis położenia „Od strony C-20, od północy.”
    Jeżeli Gość niezalogowany otworzy kartę budynku C-19
    Wtedy pod nagłówkiem stoi zdanie „Zalecane wejście od strony C-20, od północy. Drzwi 100 cm.”
    Oraz w grupie „Wejścia” to wejście ma nazwę „Wejście”
    Oraz pod nazwą stoi najpierw „Od strony C-20, od północy.”, a potem „Do wejścia prowadzi rampa.”
    Jeżeli dotknie wiersza tego wejścia
    Wtedy wejście na mapie ma podpis „Wejście”
    Oraz część „Dojście” arkusza wejścia podaje oba opisy w tej samej kolejności

  @BR-25 @aplikacja
  Scenariusz: Przy kilku wejściach dostępnych bez głównego zalecane jest wejście z nazwą
    # Dane testowe, a nie stan faktyczny budynku C-13.
    Zakładając, że w danych testowych dwa wejścia C-13 z planów PWr mają oznaczenie „dostępne” i żadne nie jest główne
    Oraz pierwsze w kolejności z danych nie ma nazwy w uwagach, więc import nazywa je „Wejście”
    Oraz drugie ma w uwagach nazwę „Wejście dostosowane”
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy pierwsze jest „Wejście dostosowane” z napisem „Zalecane”
    Ale wejście „Wejście” nie ma napisu „Zalecane”

  @US3.1 @BR-25 @aplikacja
  Scenariusz: Budynek bez wejścia dostępnego nie ma wejścia zalecanego
    Zakładając, że Gość niezalogowany otworzył kartę budynku B-8
    Jeżeli przejrzy listę wejść
    Wtedy żadne wejście nie ma napisu „Zalecane”
    Oraz lista zaczyna się od wejść z oznaczeniem „niedostępne”

  @US3.1 @US17.1 @aplikacja
  Scenariusz: Karta budynku bez wejść w danych mówi o braku danych zamiast pustej grupy
    Zakładając, że budynek A-2 nie ma wejść w danych
    Jeżeli Gość niezalogowany otworzy kartę budynku A-2
    Wtedy w grupie „Wejścia” widzi zdanie „Nie mamy jeszcze danych o wejściach do tego budynku.”
    Ale karta nie mówi, że budynek jest niedostępny

  @US2.1 @US17.1 @aplikacja
  Scenariusz: Kilka wejść bez oznaczenia i bez nazwy stoi jako jeden wiersz
    Zakładając, że budynek C-7 ma w danych 3 wejścia bez oznaczenia i bez nazwy
    Jeżeli Gość niezalogowany otworzy kartę budynku C-7 na całym ekranie
    Wtedy w grupie „Wejścia” widzi jeden wiersz „3 wejścia” ze znakiem „Brak danych”
    Oraz pod nim zdanie „Nikt jeszcze nie sprawdził tych wejść.” i jeden odnośnik „Zgłoś barierę przy wejściu”
    Ale nie widzi trzech jednakowych bloków „Wejście”

  @US2.2 @BR-17 @aplikacja
  Scenariusz: Zgłoszenie przyjęte na karcie budynku ma zdjęcie i datę weryfikacji
    Zakładając, że Weryfikator przyjął 12.11.2026 zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
    Jeżeli Odwiedzający otworzy kartę budynku A-1
    Wtedy widzi to zgłoszenie ze zdjęciem
    Oraz przy zgłoszeniu widnieje data weryfikacji 12.11.2026

  @US3.2 @aplikacja
  Scenariusz: Wejście dostępne najwyżej 3 dotknięcia od mapy
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli dotknie budynku A-1
    Wtedy na karcie budynku A-1 widzi „Wejście dostępne” i zdanie z wejściem zalecanym
    Oraz od mapy wykonał 1 dotknięcie, czyli nie więcej niż 3

  @US2.8 @aplikacja
  Szablon scenariusza: Niska karta nad mapą mówi słowami o wejściach
    Zakładając, że w danych testowych <dane>
    Oraz Gość niezalogowany ma otwartą mapę kampusu przy zwykłej czcionce
    Jeżeli dotknie budynku <budynek>
    Wtedy nad mapą widzi kartę budynku <budynek> na niskiej wysokości z napisem „<napis>”
    Oraz <pod napisem>
    Oraz w nagłówku karty jest „Zamknij”, a pod kreską wystaje nagłówek „Wejścia”
    Ale na karcie nie ma „Pokaż budynek na mapie” ani „Sprawdź, którędy wejść”

    Przykłady:
      | budynek | dane                                                 | napis                   | pod napisem                                                                             |
      | A-1     | budynek A-1 ma wejście z oznaczeniem „dostępne”      | Wejście dostępne        | pod napisem stoi zdanie z wejściem zalecanym i przycisk „Pokaż wejście na mapie”        |
      | B-8     | wszystkie wejścia B-8 mają oznaczenie „niedostępne”  | Brak wejścia dostępnego | pod napisem stoi zdanie, że żadne wejście nie jest dostępne, bez przycisku wypełnionego |
      | C-13    | wszystkie wejścia C-13 mają oznaczenie „brak danych” | Brak danych o wejściach | na karcie nie ma zdania ani przycisku wypełnionego                                      |
      | A-2     | budynek A-2 nie ma wejść w danych                    | Brak danych o wejściach | na karcie nie ma zdania ani przycisku wypełnionego                                      |

  @US2.8 @NFR-53 @aplikacja
  Scenariusz: Uchwyt zmienia wysokość karty bez przeciągania
    Zakładając, że Gość niezalogowany dotknął budynku A-1 na mapie przy zwykłej czcionce
    Jeżeli dotknie uchwytu karty
    Wtedy opis budynku A-1 zajmuje cały ekran
    Oraz pod kreską karty stoi pełny opis budynku A-1
    Jeżeli znowu dotknie uchwytu karty
    Wtedy karta wraca na niską wysokość nad mapą

  @US2.8 @NFR-53 @aplikacja
  Szablon scenariusza: Wystający nagłówek „Wejścia” od razu rozwija kartę
    Zakładając, że w danych testowych <dane>
    Oraz Gość niezalogowany dotknął budynku <budynek> na mapie przy zwykłej czcionce
    Jeżeli dotknie wystającego nagłówka „Wejścia”
    Wtedy <po rozwinięciu>

    Przykłady:
      | budynek | dane                                                | po rozwinięciu                                                               |
      | A-1     | budynek A-1 ma wejścia i plany pięter PWr           | opis budynku A-1 zajmuje cały ekran                                          |
      | A-2     | budynek A-2 nie ma wejść ani planów pięter w danych | opis budynku A-2 zajmuje cały ekran, a pod krótką treścią jest puste miejsce |

  @US2.8 @NFR-53 @aplikacja
  Szablon scenariusza: Karta budynku zamyka się na każdej wysokości
    Zakładając, że Gość niezalogowany dotknął budynku A-1 na mapie przy zwykłej czcionce
    Oraz karta stoi na wysokości „<wysokość>”
    Jeżeli <czynność>
    Wtedy widzi mapę bez zaznaczonego budynku
    Oraz położenie i powiększenie mapy są takie jak w chwili zamknięcia karty

    Przykłady:
      | wysokość   | czynność                   |
      | niska      | przeciągnie kartę w dół    |
      | niska      | dotknie pustej mapy        |
      | rozwinięta | wybierze „Zamknij”         |
      | rozwinięta | wybierze „Wstecz” telefonu |

  @US2.8 @NFR-15 @aplikacja
  Scenariusz: Ponowne dotknięcie „Mapa” w pasku dolnym nie zamyka karty
    Zakładając, że Gość niezalogowany dotknął budynku A-1 na mapie przy zwykłej czcionce
    Jeżeli dotknie pozycji „Mapa” w pasku dolnym
    Wtedy nad mapą nadal widzi kartę budynku A-1 na tej samej wysokości

  @US2.8 @US1.4 @NFR-53 @aplikacja
  Scenariusz: Przy karcie budynek jest cały widoczny, a mapą sterują przyciski
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu przy zwykłej czcionce
    Oraz cały kontur budynku A-1 jest widoczny na mapie
    Jeżeli dotknie budynku A-1
    Wtedy cały kontur budynku A-1 mieści się w części mapy nad kartą
    Oraz powiększenie mapy jest takie jak przed dotknięciem
    Oraz nad kartą są „Powiększ”, „Pomniejsz” i strzałki przesuwania mapy
    Oraz po „Pomniejsz” mapa się oddala, a karta budynku A-1 zostaje otwarta
    Ale nad kartą nie ma pola „Szukaj budynku, np. C-13” ani przycisku „Pokaż listę”

  @US2.8 @NFR-53 @aplikacja
  Scenariusz: Przy niskiej mapie nad kartą nie ma przycisków mapy
    Zakładając, że Gość niezalogowany dotknął budynku A-1 na mapie przy zwykłej czcionce
    Oraz nad kartą zostaje mniej niż 300 dp mapy
    Wtedy nad kartą nie ma „Powiększ”, „Pomniejsz” ani strzałek przesuwania mapy
    Oraz po „Zamknij” przyciski mapy wracają

  @US2.9 @aplikacja
  Scenariusz: Z listy przy dużej czcionce opis budynku prowadzi do mapy
    Zakładając, że Gość niezalogowany ma skalę czcionki 2,0 i otwartą listę budynków
    Jeżeli wybierze budynek A-1
    Wtedy widzi opis budynku A-1 na całym ekranie bez mapy
    Oraz po „Pokaż budynek na mapie” widzi mapę z zaznaczonym budynkiem A-1, a nie listę
    Oraz aplikacja pamięta widok mapy, a w wierszu tytułu jest „Pokaż listę”

  @US3.1 @aplikacja
  Scenariusz: Brak wejścia dostępnego jest powiedziany wprost
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli dotknie budynku B-8
    Wtedy na karcie budynku B-8 widzi „Brak wejścia dostępnego” i zdanie, że żadne wejście nie jest dostępne
    Ale karta nie jest pusta i nie pokazuje samego braku wyniku

  @US1.4 @aplikacja
  Scenariusz: Mapa pokazuje wejścia i ścieżki, a legenda objaśnia znaki słowem
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu przy budynku A-1
    Jeżeli przybliży mapę
    Wtedy widzi wejścia budynku A-1 ze znakiem oznaczenia
    Oraz widzi ścieżki dostosowane i pochylnie
    Oraz legenda objaśnia słowem każdy znak, także „brak danych”

  @US1.5 @US2.5 @BR-14 @BR-17 @aplikacja
  Scenariusz: Dotknięcie znaku bariery pokazuje jej opis z miejscem
    Zakładając, że Weryfikator przyjął 12.11.2026 zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
    Jeżeli Gość niezalogowany dotknie znaku tej bariery na mapie
    Wtedy widzi typ, uciążliwość, miejsce „wejście główne”, zdjęcie i datę weryfikacji 12.11.2026
    Oraz może przejść do karty budynku A-1
    Oraz na karcie budynku i na liście budynków ta bariera też ma miejsce „wejście główne”

  @US2.3 @aplikacja
  Scenariusz: Karta budynku zaczyna się od skrótu faktów, potem wejścia i bariery
    Zakładając, że dane PWr potwierdzają w budynku A-1 wejście dostępne, windę i toaletę dostosowaną
    Jeżeli Gość niezalogowany otworzy kartę budynku A-1
    Wtedy pod nazwą widzi skrót „Wejście dostępne”, „Winda”, „Toaleta dostosowana”
    Oraz wejścia i bariery widzi przed opisami pięter

  @US2.3 @aplikacja
  Scenariusz: Skrót nie pokazuje faktu bez potwierdzenia w danych
    # Dane testowe, a nie stan faktyczny budynku B-8.
    Zakładając, że dane budynku B-8 nie mówią nic o windzie
    Jeżeli Gość niezalogowany otworzy kartę budynku B-8
    Wtedy skrót faktów nie ma pozycji „Winda”

  @US2.4 @BR-6 @aplikacja
  Scenariusz: Karta budynku bez zgłoszeń przyjętych nie sugeruje braku barier
    Zakładając, że budynek C-13 nie ma zgłoszeń przyjętych
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy widzi zdanie, że nikt jeszcze nie zgłosił tu sprawdzonej bariery
    Ale karta nie mówi, że w budynku nie ma barier

  @US2.7 @aplikacja
  Scenariusz: Bez danych o barierach lista i karta budynku nic o barierach nie mówią
    Zakładając, że telefon nie ma jeszcze danych o barierach budynku C-13
    Jeżeli Gość niezalogowany otworzy listę budynków
    Wtedy karta budynku C-13 na liście nie mówi nic o barierach
    Oraz po otwarciu karty budynku C-13 karta nie mówi nic o barierach
    Ale nigdzie nie widzi napisu „Brak zgłoszonych barier”

  @US2.6 @aplikacja
  Scenariusz: Wyjścia ewakuacyjne rozwijają się na żądanie
    # Dane testowe, a nie stan faktyczny budynku A-1.
    Zakładając, że dane PWr mają 2 wyjścia ewakuacyjne budynku A-1
    Jeżeli Gość niezalogowany otworzy kartę budynku A-1
    Wtedy widzi liczbę wyjść ewakuacyjnych 2
    Oraz po wybraniu „Pokaż wyjścia ewakuacyjne (2)” widzi listę obu wyjść

  # @do-zrobienia #3118: arkusz nie ma jeszcze windy za drzwiami.
  @US3.3 @BR-25 @aplikacja @do-zrobienia
  Scenariusz: Opis wejścia zalecanego podaje dojście, drzwi i najbliższą windę
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli wybierze „Pokaż wejście na mapie”
    Wtedy widzi mapę z zaznaczonym wejściem zalecanym i arkusz tego wejścia
    Oraz arkusz podaje dojście do drzwi, szerokość drzwi i najbliższą windę z przybliżoną odległością od wejścia
    Ale arkusz nie pokazuje innych wejść budynku

  # @do-zrobienia #3118: arkusz nie ma jeszcze części „Za drzwiami”.
  @US3.3 @US16.3 @BR-25 @aplikacja @do-zrobienia
  Szablon scenariusza: Część „Za drzwiami” podaje najbliższą windę i jej piętra
    Zakładając, że najbliższa winda wejścia zalecanego A-1 na planie PWr: <winda>
    Jeżeli Gość niezalogowany otworzy arkusz tego wejścia
    Wtedy w części „Za drzwiami” widzi „<pierwsze zdanie>”
    Oraz pod spodem widzi „<drugie zdanie>”

    Przykłady:
      | winda                                            | pierwsze zdanie                                 | drugie zdanie                          |
      | przy klatkach C i D, 28 m, z piwnicy na 3 piętro | Winda przy klatkach C i D, ok. 30 m od wejścia. | Jeździ od piwnicy do 3 piętra.         |
      | bez klatek w danych, 31 m, z parteru na 1 piętro | Winda, ok. 30 m od wejścia.                     | Jeździ od parteru do 1 piętra.         |
      | 12 m, staje na piwnicy, parterze i antresoli     | Winda, ok. 10 m od wejścia.                     | Jedzie na: piwnica, parter, antresola. |

  # @do-zrobienia #3118: arkusz nie ma jeszcze części „Za drzwiami”.
  @US3.3 @BR-25 @aplikacja @do-zrobienia
  Scenariusz: Bez windy na planie arkusz wejścia nie ma części „Za drzwiami”
    Zakładając, że plan PWr budynku A-1 nie ma windy
    Jeżeli Gość niezalogowany otworzy arkusz wejścia zalecanego A-1
    Wtedy arkusz nie ma części „Za drzwiami”

  @US2.1 @US3.3 @aplikacja
  Scenariusz: Arkusz każdego wejścia ma te same części
    Zakładając, że niezalecane wejście A-1 ma w danych opis dojścia, szerokość drzwi i zgłoszenie przyjęte
    Oraz Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli dotknie wiersza tego wejścia na liście „Wejścia”
    Wtedy arkusz tego wejścia ma części „Dojście”, „Drzwi” i „Bariery przy tym wejściu” jak arkusz wejścia zalecanego

  @US3.3 @NFR-53 @aplikacja
  Scenariusz: Pinezka wejścia jest widoczna nad arkuszem wejścia
    Zakładając, że wejście zalecane A-1 ma w danych opis dojścia, szerokość drzwi i zgłoszenie przyjęte ze zdjęciem
    Oraz Gość niezalogowany otworzył kartę budynku A-1 przy zwykłej czcionce
    Jeżeli wybierze „Pokaż wejście na mapie”
    Wtedy pinezka wejścia zalecanego stoi na środku części mapy nad arkuszem tego wejścia
    Oraz arkusz zajmuje najwyżej połowę ekranu, a jego treść się przewija

  # @do-zrobienia #3255: „Wstecz” otwiera kartę na niskiej wysokości od góry, bez jej wysokości i przewinięcia.
  @US2.8 @US3.3 @NFR-15 @NFR-53 @aplikacja @do-zrobienia
  Szablon scenariusza: „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz otwarto, a „Zamknij” zostawia mapę
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu przy zwykłej czcionce
    Jeżeli <otwarcie>
    Oraz wybierze <czynność> w arkuszu wejścia
    Wtedy <po zamknięciu>

    Przykłady:
      | otwarcie                                                                   | czynność  | po zamknięciu                                    |
      | rozwinie kartę A-1, przewinie ją i dotknie wiersza wejścia                 | „Wstecz”  | widzi kartę A-1 rozwiniętą, tak samo przewiniętą |
      | otworzy kartę A-1 na niskiej wysokości i wybierze „Pokaż wejście na mapie” | „Wstecz”  | widzi kartę A-1 na niskiej wysokości             |
      | otworzy kartę A-1 na niskiej wysokości i wybierze „Pokaż wejście na mapie” | „Zamknij” | widzi mapę bez karty budynku                     |
      | dotknie znaku wejścia A-1 na mapie bez otwartej karty                      | „Zamknij” | widzi mapę bez karty budynku                     |

  # @do-zrobienia #3242, #1481: arkusze sali i „Który budynek?” nie mają jeszcze wiersza budynku.
  @US2 @US2.8 @NFR-18 @NFR-53 @aplikacja @do-zrobienia
  Szablon scenariusza: Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu przy zwykłej czcionce, bez otwartej karty
    Jeżeli <otwarcie>
    Oraz wybierze wiersz „<budynek>” pod nagłówkiem arkusza
    Wtedy widzi kartę budynku <budynek> na niskiej wysokości
    Ale mapa nie przelatuje do budynku, tylko zachowuje się jak po dotknięciu jego konturu

    Przykłady:
      | otwarcie                                          | budynek     |
      | dotknie znaku wejścia A-1 na mapie                | Budynek A-1 |
      | dotknie znaku toalety dostosowanej na planie A-1  | Budynek A-1 |
      | dotknie schodów na planie A-1                     | Budynek A-1 |
      | dotknie znaku bariery przy budynku A-1            | Budynek A-1 |
      | wyszuka „A-1 329”                                 | Budynek A-1 |
      | dotknie wspólnego konturu C-3/C-4                 | Budynek C-3 |

  # @do-zrobienia #3255: karta wraca na niskiej wysokości od góry, bez jej wysokości i przewinięcia.
  @US2.8 @US3.3 @aplikacja @do-zrobienia
  Scenariusz: Wiersz budynku w arkuszu otwartym z karty wraca do tej karty
    Zakładając, że Gość niezalogowany rozwinął kartę budynku A-1 i ją przewinął
    Jeżeli dotknie wiersza wejścia na liście „Wejścia”
    Oraz wybierze wiersz „Budynek A-1” pod nagłówkiem arkusza wejścia
    Wtedy widzi kartę A-1 rozwiniętą, tak samo przewiniętą

  @US16.3 @aplikacja
  Scenariusz: Karta budynku szczegółowego opisuje piętra tekstem
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy grupy „Winda i toaleta” oraz „Piętra”
    Wtedy widzi tekstem, na których piętrach są winda i toaleta dostosowana
    Oraz przy każdym piętrze może przeczytać opis z funkcji pomieszczeń i wybrać „Pokaż na planie”

  @US5.1 @aplikacja
  Scenariusz: Instrukcja przy pierwszym uruchomieniu
    Zakładając, że Gość niezalogowany zainstalował aplikację
    Jeżeli pierwszy raz ją uruchomi
    Wtedy widzi krótką instrukcję
    Oraz może ją pominąć i przejść do mapy kampusu

  @US5.1 @aplikacja
  Scenariusz: Instrukcję można otworzyć ponownie z pomocy
    Zakładając, że Gość niezalogowany pominął instrukcję przy pierwszym uruchomieniu
    Jeżeli uruchomi aplikację drugi raz
    Wtedy instrukcja nie pojawia się sama
    Ale po otwarciu pomocy może ją obejrzeć ponownie

  @US16.1 @aplikacja
  Szablon scenariusza: Plan piętra w budynku szczegółowym
    Zakładając, że Odwiedzający otworzył kartę budynku <budynek>
    Jeżeli przy piętrze wybierze „Pokaż na planie”
    Wtedy mapa kampusu pokazuje plan tego piętra z windami, schodami, toaletami, wejściami i zgłoszeniami przyjętymi
    Oraz przełącznik pięter nad planem ma <liczba> etykiet pięter z danych PWr
    Oraz przy planie widnieje podpis „Źródło: Politechnika Wrocławska”

    Przykłady:
      | budynek | liczba |
      | A-1     | 5      |
      | B-1     | 7      |
      | C-13    | 5      |
      | D-1     | 5      |
      | D-2     | 5      |

  @US1.6 @BR-G5 @aplikacja
  Scenariusz: Mapa otwiera się na terenie „Kampus główny”
    Zakładając, że Gość niezalogowany pierwszy raz otwiera aplikację i pomija instrukcję
    Jeżeli otworzy mapę kampusu
    Wtedy widzi mapę terenu „Kampus główny” z budynkiem A-1
    Oraz na mapie widzi nazwę terenu „Kampus główny”

  @US1.7 @BR-G5 @aplikacja
  Scenariusz: Lista terenów podaje nazwę, liczbę budynków i odległość
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli otworzy listę terenów
    Wtedy widzi tereny „Kampus główny”, „Wittigowo”, „Bartoszowice”, „Długa” i „T-9 Atol”
    Oraz teren „Kampus główny” jest oznaczony jako wybrany
    Oraz przy każdym innym terenie widzi liczbę budynków i odległość

  @US1.7 @aplikacja
  Scenariusz: Wybór terenu z listy pokazuje jego mapę
    Zakładając, że Gość niezalogowany ma otwartą listę terenów
    Jeżeli wybierze teren „Wittigowo”
    Wtedy widzi mapę terenu „Wittigowo” z budynkiem T-15
    Oraz na mapie widzi nazwę terenu „Wittigowo”

  @US1.7 @BR-G5 @aplikacja
  Scenariusz: Po wyborze terenu widać wszystkie jego budynki
    Zakładając, że Gość niezalogowany ma otwartą listę terenów
    Jeżeli wybierze teren „Wittigowo”
    Wtedy na mapie widzi budynki T-15 i P-23
    Oraz powiększenie mapy jest nie większe niż 17

  @US1.7 @BR-G5 @NFR-39 @aplikacja
  Scenariusz: Mapa nie wychodzi poza wybrany teren
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli przesunie mapę do brzegu terenu w stronę terenu „Wittigowo”
    Wtedy mapa dalej się nie przesuwa
    Oraz brzeg mapy leży na brzegu prostokąta terenu

  @US2.8 @NFR-39 @aplikacja
  Scenariusz: Na małym terenie zamknięcie karty przesuwa mapę tylko do brzegu terenu
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „T-9 Atol”
    Oraz dotknął budynku T-9 na mapie
    Jeżeli wybierze „Wstecz” telefonu
    Wtedy cały widok mapy leży w prostokącie terenu
    Oraz powiększenie mapy jest takie jak w chwili zamknięcia karty

  @US1.7 @BR-G5 @NFR-39 @aplikacja
  Scenariusz: Przy najmniejszym powiększeniu teren wypełnia mapę w krótszym wymiarze
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „Wittigowo”
    Jeżeli pomniejszy mapę do końca
    Wtedy prostokąt terenu wypełnia mapę w krótszym wymiarze
    Oraz wzdłuż dłuższego wymiaru mapę da się przesunąć tylko do brzegu prostokąta
    Oraz mapa nie pokazuje granicy terenu ani wyblakłego podkładu

  @US1.7 @aplikacja
  Scenariusz: Przy granicy terenu TalkBack mówi, jak zmienić teren
    Zakładając, że Gość niezalogowany z włączonym TalkBack ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli przesunie mapę przyciskami do brzegu terenu
    Wtedy TalkBack raz mówi „Tu kończy się teren Kampus główny. Inny teren wybierzesz przyciskiem «Zmień teren» u góry mapy.”

  @US1.8 @BR-G5 @aplikacja
  Scenariusz: Wyszukanie budynku z innego terenu zmienia teren
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli wyszuka „T-15” i wybierze wynik
    Wtedy widzi mapę terenu „Wittigowo” z zaznaczonym budynkiem T-15
    Oraz widzi napis „Pokazuję teren: Wittigowo · 2 km na południowy wschód”

  @US1.8 @NFR-56 @aplikacja
  Scenariusz: TalkBack czyta napis o zmianie terenu raz, po nagłówku karty
    Zakładając, że Gość niezalogowany z włączonym TalkBack ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli wyszuka „T-15” i wybierze wynik
    Wtedy TalkBack czyta nagłówek karty budynku T-15
    Oraz potem raz czyta „Pokazuję teren: Wittigowo · 2 km na południowy wschód”

  @US1.8 @NFR-23 @aplikacja
  Scenariusz: Ruch mapy gasi widoczny napis o zmianie terenu
    Zakładając, że Gość niezalogowany wyszukał „T-15”, wybrał wynik i widzi napis „Pokazuję teren: Wittigowo · 2 km na południowy wschód”
    Jeżeli przesunie mapę gestem
    Wtedy napis znika

  @US1.8 @aplikacja
  Scenariusz: Zamknięcie karty budynku z innego terenu zostawia ten teren
    Zakładając, że Gość niezalogowany wyszukał „T-15”, wybrał wynik i widzi kartę budynku
    Jeżeli zamknie kartę przyciskiem „Wstecz”
    Wtedy mapa dalej pokazuje teren „Wittigowo”
    Oraz zaznaczenie budynku zniknęło

  @US1.8 @aplikacja
  Scenariusz: Teren wybrany przez osobę zostaje po zamknięciu karty
    Zakładając, że Gość niezalogowany wyszukał „T-15” i zamknął kartę budynku, a mapa pokazuje teren „Wittigowo”
    Jeżeli wybierze teren „Kampus główny”
    Wtedy mapa pokazuje teren „Kampus główny”

  @US1.8 @aplikacja
  Scenariusz: Lista budynków jest podzielona na tereny
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli otworzy listę budynków
    Wtedy widzi nagłówki terenów z liczbą budynków
    Oraz budynek T-15 stoi pod nagłówkiem „Wittigowo”
