# language: pl
Funkcja: Wyszukanie sali
  Gość niezalogowany i Odwiedzający szukają sali po numerze, aby wiedzieć, na którym jest piętrze
  i jak dojść do niej bez schodów (N1). Wejście zalecane liczy się jako parter, bo dane nie podają jego piętra.

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
    # Sala 012 na parterze to dane testowe, a nie stan faktyczny budynku A-1.
    Zakładając, że budynek A-1 ma wejście zalecane i windę z parteru na piętro sali 329
    Jeżeli Gość niezalogowany znajdzie salę <sala> w budynku A-1
    Wtedy widzi po kolei kroki: <kroki>
    Ale mapa nie pokazuje narysowanej trasy

    Przykłady:
      | sala | kroki                                                                         |
      | 329  | wejście zalecane, winda, piętro sali, sala z przybliżoną odległością od windy |
      | 012  | wejście zalecane, sala z przybliżoną odległością od wejścia                   |

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
      | żadna winda budynku A-1 nie łączy parteru z piętrem sali 329 | 329   |
