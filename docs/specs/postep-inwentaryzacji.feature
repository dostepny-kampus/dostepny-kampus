# language: pl
Funkcja: Piętro obejrzane i postęp inwentaryzacji
  Zbierający oznacza obejrzane piętra, a Koordynator widzi postęp według budynków
  i wie, gdzie trzeba jeszcze pójść (N9).

  @BR-20
  Reguła: Piętro obejrzane oznacza Zbierający, a cofa ten sam Zbierający albo Koordynator

    @US15.1
    Szablon scenariusza: Oznaczenie piętra albo budynku jako obejrzanego
      Zakładając, że Zbierający jest zalogowany z rolą Zbierającego
      Oraz telefon ma sieć
      Oraz w budynku <budynek> nie ma jeszcze żadnego znacznika
      Jeżeli w budynku <budynek> oznaczy <co>
      Wtedy postęp budynku <budynek> liczony na serwerze to <wynik>

      Przykłady:
        | budynek | co                                 | wynik                    |
        | A-1     | jedno z 5 pięter jako obejrzane    | 1 z 5 pięter             |
        | B-1     | jedno z 7 pięter jako obejrzane    | 1 z 7 pięter             |
        | B-4     | cały budynek jako „obejrzany”      | znacznik „obejrzany”     |

    @US15.2
    Scenariusz: Zbierający cofa własny znacznik
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli cofnie ten znacznik
      Wtedy piętro nie jest już obejrzane

    @US15.2
    Scenariusz: Inny Zbierający nie cofnie cudzego znacznika
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli inny Zbierający spróbuje cofnąć ten znacznik
      Wtedy próba jest odrzucona
      Oraz piętro jest nadal obejrzane

    @US11.3
    Scenariusz: Koordynator cofa znacznik dowolnego Zbierającego
      Zakładając, że Zbierający oznaczył piętro budynku A-1 jako obejrzane
      Jeżeli Koordynator cofnie ten znacznik
      Wtedy piętro nie jest już obejrzane
      Oraz postęp budynku A-1 pokazuje o jedno piętro obejrzane mniej

    @US15.3 @BR-9
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

    @US15.3 @BR-9
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

    @US11.1
    Scenariusz: Postęp budynku szczegółowego i podstawowego z liczbami zgłoszeń
      Zakładając, że w budynku A-1 obejrzano 3 z 5 pięter, a budynek B-4 ma znacznik „obejrzany”
      Oraz w budynku A-1 są 2 zgłoszenia „wysłane”, 1 „w weryfikacji” i 4 „przyjęte”
      Jeżeli Koordynator otworzy postęp inwentaryzacji
      Wtedy przy budynku A-1 widzi 60 procent pięter obejrzanych, 3 zgłoszenia czekające i 4 przyjęte
      Oraz przy budynku B-4 widzi znacznik „obejrzany”

    @US11.2
    Szablon scenariusza: Postęp pokazuje nową wartość po dotarciu znacznika na serwer
      Zakładając, że w budynku A-1 obejrzano <przed> z 5 pięter
      Jeżeli na serwer dotrze <zmiana>
      Wtedy postęp budynku A-1 pokazuje <po> z 5 pięter, czyli <procent> procent

      Przykłady:
        | przed | zmiana                                   | po | procent |
        | 3     | znacznik kolejnego piętra obejrzanego    | 4  | 80      |
        | 4     | cofnięcie znacznika jednego piętra       | 3  | 60      |
        | 4     | drugi znacznik już obejrzanego piętra    | 4  | 80      |

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

    @US11.4
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

    @US11.4
    Scenariusz: Przyjęta zmiana oznaczenia wejścia zwiększa liczbę wejść sprawdzonych na miejscu
      Zakładając, że 4 wejścia A-1 mają oznaczenie ze źródłem „obchód”
      Jeżeli Weryfikator przyjmie zmianę oznaczenia jednego z pozostałych wejść A-1
      Wtedy przy budynku A-1 Koordynator widzi „Wejścia sprawdzone na miejscu: 5 z 6”
