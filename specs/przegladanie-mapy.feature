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
  Scenariusz: Wejście ma oznaczenie tekstem i ikoną, nie tylko kolorem
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy listę wejść
    Wtedy każde z 6 wejść ma napis „dostępne” albo „niedostępne” i ikonę
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
    Oraz wejście boczne jest pierwsze w kolejności z danych
    Jeżeli Gość niezalogowany otworzy kartę budynku C-13
    Wtedy pierwsze jest wejście boczne z napisem „Zalecane”
    Ale wejście od dziedzińca nie ma napisu „Zalecane”

  @US3.1 @BR-25 @aplikacja
  Scenariusz: Budynek bez wejścia dostępnego nie ma wejścia zalecanego
    Zakładając, że Gość niezalogowany otworzył kartę budynku B-8
    Jeżeli przejrzy listę wejść
    Wtedy żadne wejście nie ma napisu „Zalecane”
    Oraz lista zaczyna się od wejść z oznaczeniem „niedostępne”

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
    Oraz wybierze „Sprawdź, którędy wejść”
    Wtedy widzi wejście dostępne budynku A-1
    Oraz od mapy wykonał najwyżej 3 dotknięcia

  @US2.8 @aplikacja
  Szablon scenariusza: Mała karta nad mapą mówi słowami o wejściach
    Zakładając, że w danych testowych <dane>
    Oraz Gość niezalogowany ma otwartą mapę kampusu przy zwykłej czcionce
    Jeżeli dotknie budynku <budynek>
    Wtedy nad mapą widzi małą kartę budynku <budynek> z napisem „<napis>”
    Oraz na karcie są „Sprawdź, którędy wejść” i „Zamknij”
    Ale na karcie nie ma „Pokaż budynek na mapie”

    Przykłady:
      | budynek | dane                                                 | napis                   |
      | A-1     | budynek A-1 ma wejście z oznaczeniem „dostępne”      | Wejście dostępne        |
      | B-8     | wszystkie wejścia B-8 mają oznaczenie „niedostępne”  | Brak wejścia dostępnego |
      | C-13    | wszystkie wejścia C-13 mają oznaczenie „brak danych” | Brak danych o wejściach |
      | A-2     | budynek A-2 nie ma wejść w danych                    | Brak danych o wejściach |

  @US2.8 @aplikacja
  Scenariusz: Z małej karty osoba rozwija opis albo wraca do mapy
    Zakładając, że Gość niezalogowany dotknął budynku A-1 na mapie przy zwykłej czcionce
    Jeżeli wybierze „Rozwiń opis budynku”
    Wtedy widzi opis budynku A-1 na całym ekranie bez mapy
    Oraz po „Wstecz” widzi znowu małą kartę nad mapą
    Oraz po „Zamknij” widzi mapę bez zaznaczonego budynku

  @US2.8 @US1.4 @NFR-53 @aplikacja
  Scenariusz: Przy małej karcie budynek jest cały widoczny, a mapą sterują przyciski
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu przy zwykłej czcionce
    Jeżeli dotknie budynku A-1
    Wtedy cały kontur budynku A-1 i jego wejścia mieszczą się w części mapy nad małą kartą
    Oraz nad małą kartą są „Powiększ”, „Pomniejsz” i strzałki przesuwania mapy
    Oraz po „Pomniejsz” mapa się oddala, a mała karta budynku A-1 zostaje otwarta
    Ale nad małą kartą nie ma pola „Wpisz symbol budynku, np. C-13” ani przełącznika „Mapa” i „Lista budynków”

  @US2.8 @NFR-53 @aplikacja
  Scenariusz: Przy niskiej mapie nad małą kartą nie ma przycisków mapy
    Zakładając, że Gość niezalogowany dotknął budynku A-1 na mapie przy zwykłej czcionce
    Oraz nad małą kartą zostaje mniej niż 300 dp mapy
    Wtedy nad małą kartą nie ma „Powiększ”, „Pomniejsz” ani strzałek przesuwania mapy
    Oraz po „Zamknij” przyciski mapy wracają

  @US2.9 @aplikacja
  Scenariusz: Z listy przy dużej czcionce opis budynku prowadzi do mapy
    Zakładając, że Gość niezalogowany ma skalę czcionki 2,0 i otwartą listę budynków
    Jeżeli wybierze budynek A-1
    Wtedy widzi opis budynku A-1 na całym ekranie bez mapy
    Oraz po „Pokaż budynek na mapie” widzi mapę z zaznaczonym budynkiem A-1, a nie listę
    Oraz przełącznik „Mapa” i „Lista budynków” pamięta wybór „Mapa”

  @US3.1 @aplikacja
  Scenariusz: Brak wejścia dostępnego jest powiedziany wprost
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli dotknie budynku B-8
    Oraz wybierze „Sprawdź, którędy wejść”
    Wtedy widzi zdanie, że budynek B-8 nie ma wejścia dostępnego
    Ale ekran nie jest pusty i nie pokazuje samego braku wyniku

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

  @US3.3 @BR-25 @aplikacja
  Scenariusz: Opis wejścia zalecanego podaje dojście, drzwi i najbliższą windę
    Zakładając, że Gość niezalogowany otworzył ekran „Którędy wejść” budynku A-1
    Jeżeli przejrzy opis wejścia zalecanego
    Wtedy widzi dojście do drzwi, szerokość drzwi i najbliższą windę z przybliżoną odległością od wejścia
    Oraz przy każdym wejściu dostępnym jest „Pokaż wejście na mapie”

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

  @US1.7 @BR-G5 @NFR-39 @aplikacja
  Scenariusz: Mapa nie wychodzi poza wybrany teren
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli przesunie mapę do granicy terenu w stronę terenu „Wittigowo”
    Wtedy mapa dalej się nie przesuwa
    Oraz obszar poza terenem jest przyciemniony
    Oraz przy krawędzi mapy widzi przycisk terenu „Wittigowo” z odległością

  @US1.7 @aplikacja
  Scenariusz: Przycisk przy krawędzi mapy przechodzi do sąsiedniego terenu
    Zakładając, że Gość niezalogowany widzi przy krawędzi mapy przycisk terenu „Wittigowo”
    Jeżeli wybierze ten przycisk
    Wtedy widzi mapę terenu „Wittigowo”

  @US1.8 @BR-G5 @aplikacja
  Scenariusz: Wyszukanie budynku z innego terenu zmienia teren
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu na terenie „Kampus główny”
    Jeżeli wyszuka „T-15” i wybierze wynik
    Wtedy widzi mapę terenu „Wittigowo” z zaznaczonym budynkiem T-15
    Oraz widzi napis „Pokazuję teren: Wittigowo”

  @US1.8 @aplikacja
  Scenariusz: Lista budynków jest podzielona na tereny
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli otworzy listę budynków
    Wtedy widzi nagłówki terenów z liczbą budynków
    Oraz budynek T-15 stoi pod nagłówkiem „Wittigowo”
