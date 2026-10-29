# language: pl
Funkcja: Wyszukanie sali
  Gość niezalogowany i Odwiedzający szukają sali po numerze, aby wiedzieć, na którym jest piętrze
  i jak dojść do niej bez schodów (N1). Piętro wejścia zalecanego pochodzi z danych planu PWr.

  @US18.1
  Szablon scenariusza: Wyszukanie sali po budynku i numerze w różnym zapisie
    Zakładając, że dane PWr budynku A-1 mają salę 329 z funkcją i piętrem
    Oraz Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wyszuka „<zapis>”
    Wtedy widzi plan piętra sali 329 w budynku A-1 z wyróżnioną salą
    Oraz widzi funkcję sali i jej piętro z danych PWr

    Przykłady:
      | zapis   |
      | A-1 329 |
      | a1 329  |

  @US18.2 @BR-25
  Szablon scenariusza: Kroki do sali podane tekstem bez rysowania trasy
    # Sala 012 na piętrze wejścia i brak piętra wejścia to dane testowe, a nie stan faktyczny A-1.
    Zakładając, że <dane>
    Jeżeli Gość niezalogowany znajdzie salę <sala> w budynku A-1
    Wtedy widzi po kolei kroki: <kroki>
    Ale mapa nie pokazuje narysowanej trasy

    Przykłady:
      | dane                                                                 | sala | kroki                                                                                   |
      | wejście zalecane A-1 jest na parterze, a winda łączy parter z piętrem sali 329 | 329  | wejście zalecane na parterze, winda, piętro sali, sala z przybliżoną odległością od windy |
      | sala 012 jest na piętrze wejścia zalecanego A-1                      | 012  | wejście zalecane, sala z przybliżoną odległością od wejścia                             |
      | dane A-1 nie podają piętra wejścia zalecanego                        | 329  | wejście zalecane z dopiskiem, że przyjęto parter, winda, piętro sali, sala              |

  @US18.3
  Szablon scenariusza: Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem
    # Dane testowe, a nie stan faktyczny budynku A-1.
    Zakładając, że <sytuacja>
    Jeżeli Gość niezalogowany wyszuka „A-1 <numer>”
    Wtedy widzi zdanie, które mówi, czego brakuje
    Oraz widzi propozycję „Pokaż opis budynku”

    Przykłady:
      | sytuacja                                                     | numer |
      | dane PWr budynku A-1 nie mają sali 999                       | 999   |
      | żadna winda budynku A-1 nie łączy piętra wejścia z piętrem sali 329 | 329   |
