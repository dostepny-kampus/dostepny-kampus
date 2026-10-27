# language: pl
Funkcja: Budynki podstawowe i miejsca poza budynkiem
  Plany pięter ma tylko 5 budynków szczegółowych. W pozostałych budynkach i poza nimi
  aplikacja działa w prostszej formie, ale nic się nie psuje (N1, N3).

  Założenia:
    Zakładając, że budynek B-4 jest budynkiem podstawowym z konturem i wejściami z OpenStreetMap

  @BR-G1
  Reguła: Budynek bez planów pięter nie ma przełącznika pięter, a piętro wpisuje się ręcznie

    @US16.2
    Scenariusz: Karta budynku podstawowego zamiast planu piętra pokazuje napis
      Jeżeli Gość niezalogowany otworzy kartę budynku B-4
      Wtedy nie widzi przełącznika pięter
      Oraz w miejscu planu piętra widzi napis „Plan piętra niedostępny dla tego budynku”
      Oraz widzi wejścia budynku B-4 i jego zgłoszenia przyjęte

    @US7.2
    Szablon scenariusza: Piętro w budynku podstawowym wpisuje się jako liczbę całkowitą
      Zakładając, że Zbierający wypełnia formularz zgłoszenia w budynku B-4
      Jeżeli wpisze piętro <piętro>
      Wtedy zgłoszenie <wynik>

      Przykłady:
        | piętro                      | wynik                                                  |
        | „0”                         | ma piętro 0, czyli parter                              |
        | „2”                         | ma piętro 2                                            |
        | „-1”                        | ma piętro -1                                           |
        | „1” z dopiskiem „półpiętro” | ma piętro 1 z dopiskiem „półpiętro”                    |
        | „1,5”                       | nie zapisuje się, a aplikacja prosi o liczbę całkowitą |

    @US6.4 @BR-G2
    Szablon scenariusza: Korekta punktu na mapie w budynku podstawowym i poza budynkiem
      Zakładając, że Zbierający skończył uśrednianie pozycji <miejsce>
      Jeżeli przesunie punkt bariery na mapie
      Wtedy zgłoszenie ma nowe położenie punktu
      Oraz zgłoszenie dostaje oznaczenie „korekta ręczna”

      Przykłady:
        | miejsce                                         |
        | w budynku B-4                                   |
        | na chodniku przed wejściem głównym A-1          |

    Szablon scenariusza: W budynku podstawowym zgłoszenie można złożyć zawsze
      Zakładając, że <osoba> jest zalogowany i stoi w budynku B-4
      Jeżeli złoży <zgłoszenie> z kompletem wymaganych danych
      Wtedy zgłoszenie zapisuje się bez komunikatu o błędzie
      Oraz zgłoszenie ma budynek B-4

      Przykłady:
        | osoba         | zgłoszenie                                            |
        | Odwiedzający  | zgłoszenie szybkie z grupą barier „wejście i drzwi”   |
        | Zbierający    | zgłoszenie TB-13 „próg” z piętrem 0                   |

  @BR-G2
  Reguła: Punkt poza konturem budynku to stan „Poza budynkiem”, a nie błąd

    @US6.2
    Scenariusz: Poza konturem aplikacja pokazuje stan „Poza budynkiem” i proponuje najbliższy budynek
      Zakładając, że Zbierający stoi na chodniku przed wejściem głównym A-1, poza konturem budynku
      Jeżeli skończy uśrednianie pozycji
      Wtedy widzi stan „Poza budynkiem” bez komunikatu o błędzie
      Oraz aplikacja proponuje budynek A-1

    @US6.2
    Szablon scenariusza: Propozycję najbliższego budynku można wybrać albo odrzucić
      Zakładając, że po pomiarze na chodniku przed wejściem głównym A-1 aplikacja proponuje budynek A-1
      Jeżeli Zbierający <decyzja>
      Wtedy zgłoszenie ma miejsce „<miejsce>”

      Przykłady:
        | decyzja                   | miejsce         |
        | wybierze budynek A-1      | A-1             |
        | odrzuci propozycję        | poza budynkiem  |

    Scenariusz: Zgłoszenie szybkie poza konturem budynku zapisuje się bez błędu
      Zakładając, że Odwiedzający jest zalogowany i stoi na chodniku przed wejściem głównym A-1
      Jeżeli wyśle zgłoszenie szybkie z grupą barier „komunikacja pozioma”
      Wtedy zgłoszenie zapisuje się z bieżącą pozycją
      Ale aplikacja nie pokazuje komunikatu o błędzie

  @BR-G3
  Reguła: Nieudany import planów nie psuje pozostałych budynków

    @US16.2
    Scenariusz: Budynek z uszkodzonymi danymi planów działa jako budynek podstawowy
      Zakładając, że dane planów pięter budynku B-1 są uszkodzone
      Jeżeli import planów pięter się zakończy
      Wtedy budynek B-1 nie ma przełącznika pięter i pokazuje napis „Plan piętra niedostępny dla tego budynku”
      Oraz budynki A-1, C-13, D-1 i D-2 mają plany pięter
      Oraz raport importu wskazuje budynek B-1

    Scenariusz: Zgłoszenie w budynku po nieudanym imporcie planów
      Zakładając, że import planów pięter budynku B-1 się nie udał
      Jeżeli Zbierający złoży zgłoszenie w budynku B-1 i wpisze piętro 3
      Wtedy zgłoszenie zapisuje się z budynkiem B-1 i piętrem 3

  @BR-G4
  Reguła: Daleko od budynków PWr zgłoszenie zapisuje się jako „poza budynkiem”

    @US6.2
    Scenariusz: Pomiar daleko od budynków PWr nie dostaje propozycji budynku
      Zakładając, że Zbierający stoi przy kampusie dalej od budynków PWr niż próg propozycji budynku
      Jeżeli skończy uśrednianie pozycji i zapisze zgłoszenie TB-17 „krawężnik”
      Wtedy aplikacja nie proponuje żadnego budynku
      Oraz zgłoszenie zapisuje się jako „poza budynkiem”, bez piętra
      Ale aplikacja nie pokazuje komunikatu o błędzie

    Scenariusz: Mapa działa daleko od budynków PWr
      Zakładając, że Odwiedzający stoi przy kampusie dalej od budynków PWr niż próg propozycji budynku
      Jeżeli otworzy mapę kampusu
      Wtedy widzi mapę z konturami budynków PWr
      Oraz może złożyć zgłoszenie szybkie
