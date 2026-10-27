# language: pl
Funkcja: Przeglądanie mapy kampusu i karty budynku
  Gość niezalogowany i Odwiedzający sprawdzają przed wizytą, czy wejdą do budynku
  i którym wejściem (N1, N2). Mapa pokazuje tylko zgłoszenia przyjęte.

  Założenia:
    Zakładając, że w aplikacji są budynki szczegółowe A-1, B-1, C-13, D-1 i D-2
    Oraz budynek A-1 ma 6 wejść, z których wejście główne ma schody, a wejściem dostępnym jest inne wejście

  @US1.1
  Szablon scenariusza: Wyszukanie budynku po symbolu w różnym zapisie
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wyszuka „<zapis>”
    Wtedy pierwszym wynikiem jest budynek C-13
    Oraz po wybraniu wyniku mapa pokazuje kontur budynku C-13

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
    Jeżeli Odwiedzający otworzy mapę kampusu przy budynku A-1
    Wtedy to zgłoszenie <widoczność> na mapie

    Przykłady:
      | status        | widoczność          |
      | lokalne       | nie jest widoczne   |
      | wysłane       | nie jest widoczne   |
      | w weryfikacji | nie jest widoczne   |
      | odrzucone     | nie jest widoczne   |
      | archiwalne    | nie jest widoczne   |
      | przyjęte      | jest widoczne       |

  @US2.1
  Scenariusz: Wejście ma oznaczenie tekstem i ikoną, nie tylko kolorem
    Zakładając, że Gość niezalogowany otworzył kartę budynku A-1
    Jeżeli przejrzy listę wejść
    Wtedy każde z 6 wejść ma napis „dostępne” albo „niedostępne” i ikonę
    Oraz wejście główne ma napis „niedostępne”
    Oraz oznaczenia wejść da się odróżnić w odcieniach szarości

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
    Oraz wybierze „Zaplanuj wizytę”
    Wtedy widzi wejście dostępne budynku A-1
    Oraz od mapy wykonał najwyżej 3 dotknięcia

  @US3.1
  Scenariusz: Brak wejścia dostępnego jest powiedziany wprost
    Zakładając, że w danych budynku B-4 żadne wejście nie jest wejściem dostępnym
    Jeżeli Gość niezalogowany otworzy „Zaplanuj wizytę” dla budynku B-4
    Wtedy widzi zdanie, że budynek B-4 nie ma wejścia dostępnego
    Ale ekran nie jest pusty i nie pokazuje samego braku wyniku

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
    Jeżeli otworzy przełącznik pięter
    Wtedy widzi <liczba> etykiet pięter z danych PWr
    Oraz po wybraniu piętra widzi plan piętra z windami, schodami, toaletami, wejściami i zgłoszeniami przyjętymi
    Oraz pod planem piętra widnieje podpis „Źródło: Politechnika Wrocławska”

    Przykłady:
      | budynek | liczba |
      | A-1     | 5      |
      | B-1     | 7      |
      | C-13    | 5      |
      | D-1     | 5      |
      | D-2     | 5      |
