# language: pl
Funkcja: Wyszukanie sali
  Gość niezalogowany i Odwiedzający szukają sali po numerze, aby wiedzieć, na którym jest piętrze
  i jak dojść do niej bez schodów (N1). Aplikacja podaje kroki tekstem i nie rysuje trasy.

  Założenia:
    Zakładając, że dane PWr budynku A-1 mają salę 329 z funkcją i piętrem
    Oraz budynek A-1 ma wejście zalecane i windę z piętra tego wejścia na piętro sali 329

  @US18.1
  Szablon scenariusza: Wyszukanie sali po budynku i numerze w różnym zapisie
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wyszuka „<zapis>”
    Wtedy widzi plan piętra sali 329 w budynku A-1 z wyróżnioną salą
    Oraz widzi funkcję sali i jej piętro z danych PWr

    Przykłady:
      | zapis   |
      | A-1 329 |
      | a1 329  |

  @US18.2 @BR-25
  Scenariusz: Kroki do sali podane tekstem bez rysowania trasy
    Zakładając, że Gość niezalogowany znalazł salę 329 w budynku A-1
    Jeżeli przejrzy kroki do sali
    Wtedy widzi po kolei: wejście zalecane, windę, piętro sali i salę z przybliżoną odległością od windy
    Ale mapa nie pokazuje narysowanej trasy

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
      | budynek A-1 nie ma windy z piętra wejścia na piętro sali 329 | 329   |
