# language: pl
Funkcja: Pomiar precyzyjny i formularz zgłoszenia
  Zbierający szybko i dokładnie zapisuje miejsce bariery, budynek, piętro i zdjęcie (N5).
  Pomiar działa także bez sieci.

  Założenia:
    Zakładając, że Zbierający jest zalogowany z rolą Zbierającego
    Oraz stoi przy wejściu głównym budynku A-1

  @US6.1
  Scenariusz: Uśrednianie pozycji trwa 30 s i pokazuje dane o satelitach
    Zakładając, że telefon odbiera satelity GPS, Galileo i GLONASS
    Jeżeli Zbierający zacznie nowe zgłoszenie
    Wtedy aplikacja uśrednia pozycję przez 30 s
    Oraz pokazuje dokładność pozycji w metrach z informacją, że to szacunek
    Oraz pokazuje liczbę satelitów osobno dla GPS, Galileo i GLONASS

  @US6.1
  Scenariusz: Uśrednianie można przerwać i zacząć od nowa
    Zakładając, że uśrednianie pozycji trwa od 10 s
    Jeżeli Zbierający przerwie uśrednianie
    Oraz zacznie je ponownie
    Wtedy uśrednianie trwa znowu pełne 30 s

  @US6.2
  Szablon scenariusza: Zbierający decyduje o podpowiedzianym budynku
    Zakładając, że po uśrednianiu pozycji aplikacja podpowiada budynek A-1
    Jeżeli Zbierający <decyzja>
    Wtedy zgłoszenie <wynik>

    Przykłady:
      | decyzja                       | wynik                   |
      | potwierdzi budynek A-1        | ma budynek A-1          |
      | wybierze budynek B-1          | ma budynek B-1          |
      | zostawi „poza budynkiem”      | jest „poza budynkiem”   |

  @US6.2
  Scenariusz: Podpowiedź budynku działa bez sieci
    Zakładając, że telefon nie ma sieci
    Jeżeli uśrednianie pozycji przy wejściu głównym A-1 się skończy
    Wtedy najpóźniej po 2 s aplikacja podpowiada budynek A-1

  @US6.3
  Scenariusz: Korekta punktu na planie piętra w budynku szczegółowym
    Zakładając, że Zbierający potwierdził budynek A-1 i wybrał piętro z etykiet z danych PWr
    Jeżeli przesunie punkt bariery na planie tego piętra
    Wtedy zgłoszenie ma nowe położenie punktu
    Oraz zgłoszenie dostaje oznaczenie „korekta ręczna”

  @US6.5
  Scenariusz: Mapa nieba rozróżnia systemy GNSS nie tylko kolorem
    Zakładając, że trwa uśrednianie pozycji
    Jeżeli Zbierający spojrzy na mapę nieba
    Wtedy widzi położenie każdego widocznego satelity
    Oraz satelity GPS, Galileo i GLONASS da się odróżnić w odcieniach szarości

  @US7.2 @BR-7
  Scenariusz: Piętro w budynku szczegółowym wybiera się z etykiet z danych PWr
    Zakładając, że Zbierający potwierdził budynek A-1
    Jeżeli otworzy wybór piętra w formularzu
    Wtedy widzi 5 etykiet pięter budynku A-1 z danych PWr
    Ale nie może wpisać piętra ręcznie

  @US7.1 @BR-7
  Szablon scenariusza: Bez typu bariery, uciążliwości albo zdjęcia zgłoszenia nie da się zapisać
    Zakładając, że Zbierający wypełnia formularz zgłoszenia dla budynku A-1, piętro wybrane
    Jeżeli spróbuje zapisać zgłoszenie bez <brak>
    Wtedy zgłoszenie nie zostaje zapisane
    Oraz aplikacja wskazuje brakujące pole „<pole>”

    Przykłady:
      | brak          | pole          |
      | typu bariery  | typ bariery   |
      | uciążliwości  | uciążliwość   |
      | zdjęcia       | zdjęcie       |

  @BR-7
  Scenariusz: W budynku trzeba podać piętro
    Zakładając, że Zbierający wybrał TB-1 „schody przy wejściu”, uciążliwość 3 i zrobił zdjęcie
    Jeżeli spróbuje zapisać zgłoszenie dla budynku A-1 bez piętra
    Wtedy zgłoszenie nie zostaje zapisane
    Oraz aplikacja wskazuje brakujące pole „piętro”

  @US7.1 @BR-7
  Scenariusz: Kompletne zgłoszenie zapisuje się ze statusem „lokalne”
    Zakładając, że Zbierający potwierdził budynek A-1 i wybrał piętro
    Jeżeli wybierze TB-1 „schody przy wejściu” i uciążliwość 3
    Oraz zrobi zdjęcie i zapisze zgłoszenie
    Wtedy zgłoszenie zapisuje się w telefonie ze statusem „lokalne”

  @US7.3 @BR-7
  Scenariusz: Typ TB-29 „inna bariera” wymaga opisu
    Zakładając, że Zbierający wybrał TB-29 „inna bariera”, uciążliwość 2, piętro i zrobił zdjęcie
    Jeżeli spróbuje zapisać zgłoszenie z pustym opisem
    Wtedy zgłoszenie nie zostaje zapisane
    Oraz aplikacja wskazuje brakujące pole „opis”

  @US7.3 @BR-7
  Scenariusz: Typ TB-29 „inna bariera” z opisem zapisuje się
    Zakładając, że Zbierający wybrał TB-29 „inna bariera”, uciążliwość 2, piętro i zrobił zdjęcie
    Jeżeli wpisze opis „Na długim korytarzu nie ma gdzie usiąść i odpocząć”
    Oraz zapisze zgłoszenie
    Wtedy zgłoszenie zapisuje się w telefonie ze statusem „lokalne”
