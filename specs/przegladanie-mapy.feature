# language: pl
Funkcja: Przeglądanie mapy kampusu i karty budynku
  Gość niezalogowany i Odwiedzający sprawdzają przed wizytą, czy wejdą do budynku
  i którym wejściem (N1, N2). Mapa pokazuje tylko zgłoszenia przyjęte.

  Założenia:
    Zakładając, że w aplikacji są budynki szczegółowe A-1, B-1, C-13, D-1 i D-2
    Oraz budynek A-1 ma 6 wejść, z których wejście główne ma schody, a wejściem dostępnym jest inne wejście
    # Dane testowe dla US3.1, a nie stan faktyczny budynku B-4.
    Oraz w danych testowych budynek podstawowy B-4 ma wejścia wyłącznie z oznaczeniem „niedostępne”

  @US1.1
  Szablon scenariusza: Wyszukanie budynku po symbolu w różnym zapisie
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wyszuka „<zapis>”
    Wtedy widzi budynek C-13 w wynikach wyszukiwania

    Przykłady:
      | zapis |
      | C13   |
      | c-13  |
      | C-13  |

  @US1.2
  Scenariusz: Mapa i wyszukiwanie działają bez logowania
    Zakładając, że Gość niezalogowany pierwszy raz otwiera aplikację i pomija instrukcję
    Jeżeli wyszuka „A-1”
    Wtedy widzi mapę kampusu z konturami budynków
    Oraz widzi budynek A-1 w wynikach wyszukiwania
    Ale aplikacja nie prosi go o logowanie

  @US1.3 @BR-6
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

  @US2.1
  Scenariusz: Wejście ma oznaczenie tekstem i ikoną, nie tylko kolorem
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy listę wejść
    Wtedy każde z 6 wejść ma napis „dostępne” albo „niedostępne” i ikonę
    Oraz wejście główne ma napis „niedostępne”
    Oraz oznaczenia wejść da się odróżnić w odcieniach szarości

  @US2.1 @BR-25
  Scenariusz: Wejście zalecane stoi pierwsze na karcie budynku
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy listę wejść
    Wtedy pierwsze jest inne wejście z napisem „Zalecane” i oznaczeniem „dostępne”
    Ale wejście główne nie ma napisu „Zalecane”

  @BR-25
  Scenariusz: Przy kilku wejściach dostępnych zalecane jest wejście główne
    # Dane testowe, a nie stan faktyczny budynku C-13.
    Zakładając, że w danych testowych wejście główne i wejście boczne C-13 mają oznaczenie „dostępne”
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy pierwsze jest wejście główne z napisem „Zalecane”
    Ale wejście boczne nie ma napisu „Zalecane”

  @BR-25
  Scenariusz: Przy kilku wejściach dostępnych bez głównego zalecane jest pierwsze z danych
    # Dane testowe, a nie stan faktyczny budynku C-13.
    Zakładając, że w danych testowych wejście boczne i wejście od dziedzińca C-13 mają oznaczenie „dostępne”
    Oraz wejście główne C-13 ma oznaczenie „niedostępne”
    Oraz wejście boczne jest pierwsze w kolejności z danych
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy pierwsze jest wejście boczne z napisem „Zalecane”
    Ale wejście od dziedzińca nie ma napisu „Zalecane”

  @US3.1 @BR-25
  Scenariusz: Budynek bez wejścia dostępnego nie ma wejścia zalecanego
    Zakładając, że Gość niezalogowany otworzył kartę budynku B-4
    Jeżeli przejrzy listę wejść
    Wtedy żadne wejście nie ma napisu „Zalecane”
    Oraz lista zaczyna się od wejść z oznaczeniem „niedostępne”

  @US2.2 @BR-17
  Scenariusz: Zgłoszenie przyjęte na karcie budynku ma zdjęcie i datę weryfikacji
    Zakładając, że Weryfikator przyjął 12.11.2026 zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
    Jeżeli Odwiedzający otworzy kartę budynku A-1
    Wtedy widzi to zgłoszenie ze zdjęciem
    Oraz przy zgłoszeniu widnieje data weryfikacji 12.11.2026

  @US3.2
  Scenariusz: Wejście dostępne najwyżej 3 dotknięcia od mapy
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli dotknie budynku A-1
    Oraz wybierze „Sprawdź, którędy wejść”
    Wtedy widzi wejście dostępne budynku A-1
    Oraz od mapy wykonał najwyżej 3 dotknięcia

  @US3.1
  Scenariusz: Brak wejścia dostępnego jest powiedziany wprost
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli dotknie budynku B-4
    Oraz wybierze „Sprawdź, którędy wejść”
    Wtedy widzi zdanie, że budynek B-4 nie ma wejścia dostępnego
    Ale ekran nie jest pusty i nie pokazuje samego braku wyniku

  @US1.4
  Scenariusz: Mapa pokazuje wejścia i ścieżki, a legenda objaśnia znaki słowem
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu przy budynku A-1
    Jeżeli przybliży mapę
    Wtedy widzi wejścia budynku A-1 ze znakiem oznaczenia
    Oraz widzi ścieżki dostosowane i pochylnie
    Oraz legenda objaśnia słowem każdy znak, także „brak danych”

  @US1.5 @BR-17
  Scenariusz: Dotknięcie znaku bariery pokazuje jej opis
    Zakładając, że Weryfikator przyjął 12.11.2026 zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
    Jeżeli Gość niezalogowany dotknie znaku tej bariery na mapie
    Wtedy widzi typ, uciążliwość, miejsce „wejście główne”, zdjęcie i datę weryfikacji 12.11.2026
    Oraz może przejść do karty budynku A-1

  @US2.3
  Scenariusz: Karta budynku zaczyna się od skrótu faktów, potem wejścia i bariery
    Zakładając, że dane PWr potwierdzają w budynku A-1 wejście dostępne, windę i toaletę dostosowaną
    Jeżeli Gość niezalogowany otworzy kartę budynku A-1
    Wtedy pod nazwą widzi skrót „Wejście dostępne”, „Winda”, „Toaleta dostosowana”
    Oraz po skrócie są wejścia, zaraz po nich bariery, a dalej windy, toalety i piętra

  @US2.3
  Scenariusz: Skrót nie pokazuje faktu bez potwierdzenia w danych
    # Dane testowe, a nie stan faktyczny budynku B-4.
    Zakładając, że dane budynku B-4 nie mówią nic o windzie
    Jeżeli Gość niezalogowany otworzy kartę budynku B-4
    Wtedy skrót faktów nie ma pozycji „Winda”

  @US2.4 @BR-6
  Scenariusz: Karta budynku bez zgłoszeń przyjętych nie sugeruje braku barier
    Zakładając, że budynek C-13 nie ma zgłoszeń przyjętych
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy widzi zdanie, że nikt jeszcze nie zgłosił tu sprawdzonej bariery
    Ale karta nie mówi, że w budynku nie ma barier

  @US2.5 @BR-14
  Scenariusz: Bariera podaje miejsce na karcie, na liście i na mapie
    Zakładając, że Weryfikator przyjął zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
    Jeżeli Gość niezalogowany zobaczy tę barierę na karcie budynku, na liście budynków i w opisie na mapie
    Wtedy w każdym z tych miejsc bariera ma miejsce „wejście główne”

  @US2.6
  Scenariusz: Wyjścia ewakuacyjne rozwijają się na żądanie
    # Dane testowe, a nie stan faktyczny budynku A-1.
    Zakładając, że dane PWr mają 2 wyjścia ewakuacyjne budynku A-1
    Jeżeli Gość niezalogowany otworzy kartę budynku A-1
    Wtedy widzi liczbę wyjść ewakuacyjnych 2, ale nie ich listę
    Oraz po wybraniu „Pokaż wyjścia ewakuacyjne (2)” widzi listę obu wyjść

  @US3.3 @BR-25
  Scenariusz: Opis wejścia zalecanego podaje dojście, drzwi i najbliższą windę
    Zakładając, że Gość niezalogowany otworzył ekran „Którędy wejść” budynku A-1
    Jeżeli przejrzy opis wejścia zalecanego
    Wtedy widzi dojście do drzwi, szerokość drzwi i najbliższą windę z przybliżoną odległością od wejścia
    Oraz przy każdym wejściu dostępnym jest „Pokaż wejście na mapie”

  @US16.3
  Scenariusz: Karta budynku szczegółowego opisuje piętra tekstem
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy grupy „Winda i toaleta” oraz „Piętra”
    Wtedy widzi tekstem, na których piętrach są winda i toaleta dostosowana
    Oraz każde piętro ma zwinięty opis z funkcji pomieszczeń i „Pokaż na planie”

  @US5.1
  Scenariusz: Instrukcja przy pierwszym uruchomieniu
    Zakładając, że Gość niezalogowany zainstalował aplikację
    Jeżeli pierwszy raz ją uruchomi
    Wtedy widzi krótką instrukcję
    Oraz może ją pominąć i przejść do mapy kampusu

  @US5.1
  Scenariusz: Instrukcję można otworzyć ponownie z pomocy
    Zakładając, że Gość niezalogowany pominął instrukcję przy pierwszym uruchomieniu
    Jeżeli uruchomi aplikację drugi raz
    Wtedy instrukcja nie pojawia się sama
    Ale po otwarciu pomocy może ją obejrzeć ponownie

  @US16.1
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
