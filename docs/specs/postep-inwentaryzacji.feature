# language: pl
Funkcja: Piętro obejrzane i postęp inwentaryzacji
  Zbierający oznacza obejrzane piętra, a Koordynator widzi postęp według budynków
  i wie, gdzie trzeba jeszcze pójść (N9).

  @BR-20
  Reguła: Piętro obejrzane oznacza Zbierający, a cofa ten sam Zbierający albo Koordynator

    @US15.1
    Szablon scenariusza: Oznaczenie piętra albo budynku jako obejrzanego
      Zakładając, że Zbierający jest zalogowany i ma sieć
      Jeżeli w budynku <budynek> oznaczy <co>
      Wtedy w postępie budynku <budynek> widać <wynik>

      Przykłady:
        | budynek | co                                 | wynik                    |
        | A-1     | jedno z 5 pięter jako obejrzane    | 1 z 5 pięter obejrzanych |
        | B-1     | jedno z 7 pięter jako obejrzane    | 1 z 7 pięter obejrzanych |
        | B-4     | cały budynek jako „obejrzany”      | znacznik „obejrzany”     |

    @US15.2
    Scenariusz: Zbierający cofa własny znacznik
      Zakładając, że Zbierająca oznaczyła piętro budynku A-1 jako obejrzane
      Jeżeli cofnie to oznaczenie
      Wtedy piętro nie jest już obejrzane

    @US15.2
    Scenariusz: Inny Zbierający nie cofnie cudzego znacznika
      Zakładając, że Zbierająca oznaczyła piętro budynku A-1 jako obejrzane
      Jeżeli inny Zbierający spróbuje cofnąć to oznaczenie
      Wtedy próba jest odrzucona
      Oraz piętro jest nadal obejrzane

    @US11.3
    Scenariusz: Koordynator cofa znacznik dowolnego Zbierającego
      Zakładając, że Zbierająca oznaczyła piętro budynku A-1 jako obejrzane
      Jeżeli Koordynator cofnie to oznaczenie
      Wtedy piętro nie jest już obejrzane
      Oraz postęp budynku A-1 pokazuje o jedno piętro obejrzane mniej

    @US15.3 @BR-9
    Szablon scenariusza: Znacznik i jego cofnięcie bez sieci czekają w telefonie
      Zakładając, że Zbierający jest w piwnicy budynku A-1, gdzie nie ma sieci
      Jeżeli <czynność>
      Wtedy zmiana czeka w telefonie, a postęp budynku A-1 się nie zmienia
      Oraz po powrocie sieci zmiana wysyła się bez działania Zbierającego
      Oraz postęp budynku A-1 pokazuje nową wartość

      Przykłady:
        | czynność                                               |
        | oznaczy piwnicę jako obejrzaną                         |
        | cofnie swoje wcześniejsze oznaczenie piwnicy           |

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
        | 3     | oznaczenie kolejnego piętra obejrzanego  | 4  | 80      |
        | 4     | cofnięcie oznaczenia jednego piętra      | 3  | 60      |
        | 4     | drugie oznaczenie już obejrzanego piętra | 4  | 80      |

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
