# language: pl
Funkcja: Budynki podstawowe i miejsca poza budynkiem
  Plany pięter PWr ma 24 budynki szczegółowe. W pozostałych budynkach i poza nimi
  aplikacja działa w prostszej formie, ale nic się nie psuje (N1, N3).

  Założenia:
    Zakładając, że budynek B-8 jest budynkiem podstawowym z konturem i wejściami z OpenStreetMap

  @BR-G1
  Reguła: Budynek bez planów pięter nie ma przełącznika pięter, a piętro wpisuje się ręcznie

    # brak pełnego testu (przełącznik pięter na mapie): #3666
    @US16.2 @aplikacja @do-zrobienia
    Scenariusz: Karta budynku podstawowego zamiast planu piętra pokazuje napis
      Jeżeli Gość niezalogowany otworzy kartę budynku B-8
      Wtedy nie widzi przełącznika pięter
      Oraz w miejscu planu piętra widzi napis „Nie mamy planu pięter tego budynku.”
      Oraz widzi wejścia budynku B-8 i jego zgłoszenia przyjęte

    @US16.2 @aplikacja
    Scenariusz: Budynek podstawowy blisko budynku szczegółowego nie pokazuje planu sąsiada
      Zakładając, że budynek podstawowy B-8 leży bliżej niż 60 m od budynku szczegółowego A-1
      Jeżeli Odwiedzający otworzy kartę budynku B-8 przy powiększeniu 18 albo przesunie środek mapy do B-8
      Wtedy nie widzi planu piętra ani kolumny pięter budynku A-1
      Oraz po wyjściu środka poza kontur B-8 o ponad 5 m bez karty widzi plan budynku A-1

    @US16.2 @NFR-10 @aplikacja
    Scenariusz: Bez danych z serwera karta nie twierdzi, że budynek nie ma planu
      Zakładając, że Gość niezalogowany pierwszy raz otwiera aplikację bez internetu
      Jeżeli otworzy kartę budynku A-1
      Wtedy w grupie „Piętra” widzi napis „Plan pięter sprawdzimy, gdy telefon połączy się z internetem.”
      Ale nie widzi napisu „Nie mamy planu pięter tego budynku.”

    @US7.2 @aplikacja
    Szablon scenariusza: Piętro w budynku podstawowym wpisuje się jako liczbę całkowitą
      Zakładając, że Zbierający wypełnia formularz zgłoszenia w budynku B-8
      Jeżeli wpisze piętro <piętro>
      Wtedy zgłoszenie <wynik>

      Przykłady:
        | piętro                      | wynik                                                  |
        | „0”                         | ma piętro 0, czyli parter                              |
        | „2”                         | ma piętro 2                                            |
        | „-1”                        | ma piętro -1                                           |
        | „1” z dopiskiem „półpiętro” | ma piętro 1 z dopiskiem „półpiętro”                    |
        | „1,5”                       | nie zapisuje się z piętrem „1,5”                       |

    @US6.4 @BR-G2 @aplikacja
    Szablon scenariusza: Korekta punktu na mapie w każdym budynku i poza budynkiem
      Zakładając, że Zbierający skończył uśrednianie pozycji <miejsce>
      Jeżeli przesunie punkt bariery na mapie
      Wtedy zgłoszenie ma nowe położenie punktu
      Oraz zgłoszenie dostaje oznaczenie „korekta ręczna”

      Przykłady:
        | miejsce                                         |
        | w budynku B-8                                   |
        | w budynku szczegółowym A-1                      |
        | na chodniku przed wejściem głównym A-1          |

    @serwer
    Szablon scenariusza: W budynku podstawowym zgłoszenie można złożyć zawsze
      Zakładając, że <osoba> jest zalogowany z rolą <rola>
      Oraz stoi w budynku B-8
      Jeżeli złoży <zgłoszenie> z kompletem wymaganych danych
      Wtedy zgłoszenie zapisuje się bez komunikatu o błędzie
      Oraz zgłoszenie ma budynek B-8

      Przykłady:
        | osoba         | rola            | zgłoszenie                                            |
        | Odwiedzający  | Odwiedzającego  | zgłoszenie szybkie z grupą barier „wejście i drzwi”   |
        | Zbierający    | Zbierającego    | zgłoszenie TB-13 „próg” z piętrem 0                   |

  @BR-G2
  Reguła: Punkt poza konturem budynku to stan „Poza budynkiem”, a nie błąd

    @US6.2 @aplikacja
    Scenariusz: Poza konturem aplikacja pokazuje podpowiedź najbliższego budynku i przycisk „Poza budynkiem”
      Zakładając, że Zbierający stoi na chodniku przed wejściem głównym A-1, poza konturem budynku
      Jeżeli skończy uśrednianie pozycji
      Wtedy aplikacja podpowiada budynek A-1 z nazwą i odległością od punktu bez komunikatu o błędzie
      Oraz widzi przycisk „Poza budynkiem”

    @US6.2 @aplikacja
    Szablon scenariusza: Propozycję najbliższego budynku można wybrać albo odrzucić
      Zakładając, że Zbierający stoi na chodniku przed wejściem głównym A-1, poza konturem budynku
      Oraz po uśrednianiu pozycji aplikacja podpowiada budynek A-1
      Jeżeli Zbierający <decyzja>
      Wtedy zgłoszenie <wynik>

      Przykłady:
        | decyzja                       | wynik                   |
        | wybierze budynek A-1          | ma budynek A-1          |
        | odrzuci podpowiedź budynku    | jest „poza budynkiem”   |

    @US6.2 @aplikacja
    Szablon scenariusza: Punkt dokładnie na krawędzi konturu jest poza budynkiem z propozycją
      Zakładając, że Zbierający stoi dokładnie <miejsce> budynku A-1
      Jeżeli skończy uśrednianie pozycji
      Wtedy widzi stan „Poza budynkiem” bez komunikatu o błędzie
      Oraz aplikacja podpowiada budynek A-1 do potwierdzenia
      Ale aplikacja nie przypisuje budynku A-1 bez potwierdzenia

      Przykłady:
        | miejsce                      |
        | na krawędzi konturu          |
        | na krawędzi dziedzińca       |

    @aplikacja
    Scenariusz: Zgłoszenie szybkie poza konturem budynku zapisuje się bez błędu
      Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
      Oraz stoi na chodniku przed wejściem głównym A-1, poza konturem budynku
      Jeżeli wyśle zgłoszenie szybkie z grupą barier „komunikacja pozioma”
      Wtedy zgłoszenie zapisuje się z bieżącą pozycją
      Ale aplikacja nie pokazuje komunikatu o błędzie

  @BR-G3
  Reguła: Nieudany import planów nie psuje pozostałych budynków

    # brak kodu: import nie toleruje uszkodzonych planów i nie ma raportu importu: #3665
    @US16.2 @aplikacja @do-zrobienia
    Scenariusz: Budynek z uszkodzonymi danymi planów działa jako budynek podstawowy
      Zakładając, że dane planów pięter budynku B-1 są uszkodzone
      Jeżeli import planów pięter się zakończy
      Wtedy budynek B-1 nie ma przełącznika pięter i pokazuje napis „Nie mamy planu pięter tego budynku.”
      Oraz budynki A-1, C-13, D-1 i D-2 mają plany pięter
      Oraz raport importu wskazuje budynek B-1

    @aplikacja
    Scenariusz: Zgłoszenie w budynku po nieudanym imporcie planów
      Zakładając, że import planów pięter budynku B-1 się nie udał
      Jeżeli Zbierający złoży zgłoszenie w budynku B-1 i wpisze piętro 3
      Wtedy zgłoszenie ma budynek B-1
      Oraz zgłoszenie ma piętro 3

  @BR-G4
  Reguła: Dalej niż 50 m od budynków PWr zgłoszenie zapisuje się jako „poza budynkiem”

    # brak testu wiersza 50 m, bo kod dla 50 m podaje „poza budynkiem”, a tabela podpowiada A-1: #3664
    @US6.2 @aplikacja @do-zrobienia
    Szablon scenariusza: Propozycja budynku po pomiarze zależy od odległości od konturu
      Zakładając, że Zbierający stoi na terenie PWr poza konturem, <odległość> od konturu budynku A-1, a dalej od innych budynków PWr
      Jeżeli skończy uśrednianie pozycji
      Wtedy <propozycja>
      Ale aplikacja nie pokazuje komunikatu o błędzie

      Przykłady:
        | odległość | propozycja                                                                                            |
        | 30 m      | aplikacja podpowiada budynek A-1 do potwierdzenia                                                     |
        | 50 m      | aplikacja podpowiada budynek A-1 do potwierdzenia                                                     |
        | 60 m      | aplikacja nie podpowiada żadnego budynku, a po zapisaniu zgłoszenie jest „poza budynkiem”, bez piętra |

    # brak pełnego testu: #3666
    @aplikacja @do-zrobienia
    Scenariusz: Mapa działa daleko od budynków PWr
      Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
      Oraz stoi na terenie „Kampus główny” 60 m od konturu najbliższego budynku PWr
      Jeżeli otworzy mapę kampusu
      Wtedy widzi mapę z konturami budynków PWr
      Oraz może złożyć zgłoszenie szybkie
