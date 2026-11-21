# language: pl
Funkcja: Piętro obejrzane i postęp inwentaryzacji
  Zbierający oznacza obejrzane piętra, a Koordynator widzi postęp według budynków
  i wie, gdzie trzeba jeszcze pójść (N9).

  @BR-20
  Reguła: Piętro obejrzane oznacza Zbierający, a cofa ten sam Zbierający albo Koordynator

    @US15.1 @serwer @postep-http
    Szablon scenariusza: Oznaczenie piętra albo budynku jako obejrzanego
      Zakładając, że Zbierający jest zalogowany z rolą Zbierającego
      Oraz budynek "<budynek>" ma <piętra> pięter z danych testowych i nie ma znaczników
      Jeżeli <czynność>
      Oraz Koordynator otworzy postęp inwentaryzacji
      Wtedy <wynik>

      Przykłady:
        | budynek | piętra | czynność                                      | wynik                               |
        | A-1     | 5      | Zbierający oznaczy budynek "A-1" na poziomie 1  | postęp "A-1" pokazuje 1 z 5 pięter   |
        | B-1     | 7      | Zbierający oznaczy budynek "B-1" na poziomie 1  | postęp "B-1" pokazuje 1 z 7 pięter   |
        | B-8     | 0      | Zbierający oznaczy budynek "B-8" w całości      | budynek "B-8" jest obejrzany w całości |

    @US15.2 @aplikacja
    Scenariusz: Zbierający cofa własny znacznik po potwierdzeniu
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli wybierze „Cofnij znacznik”
      Wtedy aplikacja pyta o potwierdzenie i mówi, że postęp budynku się zmniejszy
      Jeżeli potwierdzi cofnięcie
      Wtedy piętro nie jest już obejrzane

    @US15.2 @serwer
    Scenariusz: Inny Zbierający nie cofnie cudzego znacznika
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli inny Zbierający spróbuje cofnąć ten znacznik
      Wtedy próba jest odrzucona
      Oraz piętro jest nadal obejrzane

    @US11.3 @aplikacja
    Scenariusz: Koordynator cofa znacznik dowolnego Zbierającego po potwierdzeniu
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli Koordynator wybierze „Cofnij znacznik”
      Wtedy aplikacja pyta o potwierdzenie i mówi, że postęp budynku się zmniejszy
      Jeżeli Koordynator potwierdzi cofnięcie
      Wtedy piętro nie jest już obejrzane
      Oraz postęp budynku A-1 pokazuje o jedno piętro obejrzane mniej

    @US11.3 @US15.2 @aplikacja
    Szablon scenariusza: Bez potwierdzenia znacznik zostaje
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli <osoba> wybierze „Cofnij znacznik”
      Oraz wybierze „Zostaw znacznik”
      Wtedy piętro jest nadal obejrzane

      Przykłady:
        | osoba       |
        | Zbierający  |
        | Koordynator |

    @US15.3 @BR-9 @aplikacja
    Szablon scenariusza: Znacznik i jego cofnięcie bez sieci czekają w telefonie
      Zakładając, że Zbierający oznaczył 3 z 5 pięter budynku A-1 jako obejrzane
      Oraz telefon nie ma sieci
      Jeżeli Zbierający <czynność>
      Wtedy zmiana znacznika czeka w telefonie na synchronizację
      Oraz postęp budynku A-1 liczony na serwerze to nadal 3 z 5 pięter

      Przykłady:
        | czynność                                             |
        | oznaczy kolejne piętro jako obejrzane                |
        | cofnie swój znacznik jednego z obejrzanych pięter    |

    @US15.3 @BR-9 @aplikacja
    Szablon scenariusza: Po powrocie sieci znacznik i jego cofnięcie wysyłają się same
      Zakładając, że Zbierający oznaczył 3 z 5 pięter budynku A-1 jako obejrzane
      Oraz bez sieci <czynność>
      Jeżeli sieć wróci
      Wtedy zmiana znacznika dociera na serwer bez działania Zbierającego
      Oraz postęp budynku A-1 liczony na serwerze to <po> z 5 pięter

      Przykłady:
        | czynność                                              | po |
        | oznaczył kolejne piętro jako obejrzane                | 4  |
        | cofnął swój znacznik jednego z obejrzanych pięter     | 2  |

  @BR-21
  Reguła: Postęp budynku liczy się tym samym miernikiem w każdym budynku

    Założenia:
      Zakładając, że Koordynator jest zalogowany z rolą Koordynatora

    @US11.1 @serwer @postep-http
    Scenariusz: Postęp budynku szczegółowego i podstawowego z liczbami zgłoszeń
      Zakładając, że w budynku A-1 obejrzano 3 z 5 pięter
      Oraz budynek B-8 ma znacznik całego budynku
      Oraz w budynku A-1 są 2 wysłane, 1 w weryfikacji i 4 przyjęte zgłoszenia
      Jeżeli Koordynator otworzy postęp inwentaryzacji
      Wtedy postęp "A-1" to 3 z 5 pięter, czyli 60 procent
      Oraz postęp A-1 ma 3 w liczniku czekających i 4 w liczniku przyjętych
      Oraz budynek "B-8" jest obejrzany w całości

    @US11.2 @serwer @postep-http
    Szablon scenariusza: Postęp pokazuje nową wartość po dotarciu znacznika na serwer
      Zakładając, że w budynku A-1 obejrzano <przed> z 5 pięter
      Jeżeli <zmiana>
      Oraz Koordynator otworzy postęp inwentaryzacji
      Wtedy postęp "A-1" to <po> z 5 pięter, czyli <procent> procent

      Przykłady:
        | przed | zmiana                                                      | po | procent |
        | 3     | Zbierający oznaczy budynek "A-1" na poziomie 4               | 4  | 80      |
        | 4     | do serwera dotrze cofnięcie znacznika z piętra 4 budynku A-1  | 3  | 60      |
        | 4     | Zbierający oznaczy budynek "A-1" na poziomie 1               | 4  | 80      |

    @US11.2 @aplikacja
    Scenariusz: Postęp podaje datę i godzinę odczytu i daje się odświeżyć
      Zakładając, że Koordynator otworzył postęp inwentaryzacji 28.10.2026 o 10:15
      Oraz potem na serwer dotarł znacznik kolejnego piętra budynku A-1
      Jeżeli Koordynator odświeży postęp
      Wtedy widzi nową wartość postępu budynku A-1
      Oraz datę i godzinę nowego odczytu

    # @do-zrobienia #703: wymaga operacji GET /progress.
    @serwer @do-zrobienia
    Szablon scenariusza: Do postępu nie wchodzą zgłoszenia nieznane serwerowi, archiwalne ani poza budynkiem
      Zakładając, że przy budynku A-1 są 4 zgłoszenia przyjęte
      Oraz jest też <zgłoszenie>
      Jeżeli Koordynator otworzy postęp inwentaryzacji
      Wtedy przy budynku A-1 widzi nadal 4 zgłoszenia przyjęte i 0 czekających

      Przykłady:
        | zgłoszenie                                                      |
        | zgłoszenie „lokalne” z budynku A-1 w telefonie Zbierającego     |
        | zgłoszenie „archiwalne” w budynku A-1                           |
        | zgłoszenie przyjęte „poza budynkiem” 5 m od wejścia głównego A-1 |

  @BR-24
  Reguła: Postęp pokazuje wejścia budynku szczegółowego sprawdzone na miejscu

    Założenia:
      Zakładając, że Koordynator jest zalogowany z rolą Koordynatora
      # Dane testowe, a nie stan faktyczny budynku A-1.
      Oraz budynek A-1 ma w danych testowych 6 wejść

    # @do-zrobienia #703: wymaga operacji GET /progress.
    @US11.4 @serwer @do-zrobienia
    Szablon scenariusza: Liczba wejść ze źródłem „obchód” w budynku szczegółowym
      Zakładając, że liczba wejść A-1 ze źródłem oznaczenia „obchód” to <obchód>, a pozostałe mają źródło „import”
      Oraz liczba wejść A-1 ze zmianą oznaczenia czekającą na decyzję Weryfikatora to <czeka>
      Jeżeli Koordynator otworzy postęp inwentaryzacji
      Wtedy przy budynku A-1 widzi „Wejścia sprawdzone na miejscu: <obchód> z 6”

      Przykłady:
        | obchód | czeka |
        | 0      | 0     |
        | 4      | 0     |
        | 4      | 2     |
        | 6      | 0     |

    # @do-zrobienia #703: wymaga operacji GET /progress.
    @US11.4 @serwer @do-zrobienia
    Scenariusz: Przyjęta zmiana oznaczenia wejścia zwiększa liczbę wejść sprawdzonych na miejscu
      Zakładając, że 4 wejścia A-1 mają oznaczenie ze źródłem „obchód”
      Jeżeli Weryfikator przyjmie zmianę oznaczenia jednego z pozostałych wejść A-1
      Wtedy przy budynku A-1 Koordynator widzi „Wejścia sprawdzone na miejscu: 5 z 6”
