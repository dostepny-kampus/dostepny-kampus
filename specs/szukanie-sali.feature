# language: pl
Funkcja: Wyszukanie sali
  Gość niezalogowany i Odwiedzający szukają sali po numerze, aby wiedzieć, na którym jest piętrze
  i jak dojść do niej bez schodów (N1). Piętro wejścia zalecanego pochodzi z danych planu PWr.

  # @do-zrobienia #2549: zapisy z numerem na początku i ze słowem „sala” w aplikacji.
  @US18.1 @aplikacja @serwer @do-zrobienia
  Szablon scenariusza: Wyszukanie sali po budynku i numerze w różnym zapisie
    Zakładając, że dane PWr budynku A-1 mają salę 329 z funkcją i piętrem
    Oraz Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wyszuka „<zapis>”
    Wtedy widzi plan piętra sali 329 w budynku A-1 z wyróżnioną salą
    Oraz widzi funkcję sali i jej piętro z danych PWr

    Przykłady:
      | zapis        |
      | A-1 329      |
      | a1 329       |
      | 329 A-1      |
      | sala 329 A-1 |
      | A-1/329      |

  # @do-zrobienia #2549: podpowiedzi sal w panelu wyników.
  @US18.1 @aplikacja @do-zrobienia
  Scenariusz: Symbol budynku i cyfra podpowiadają sale tego budynku
    Zakładając, że dane PWr budynku A-1 mają sale 301, 314 i 329 na 2 piętrze
    Jeżeli Gość niezalogowany wpisze „A-1 3”
    Wtedy wyniki pod polem to sale 301, 314 i 329 budynku A-1 w tej kolejności
    Oraz każdy wynik ma symbol budynku, numer sali, rodzaj pomieszczenia i piętro

  # @do-zrobienia #2549: podpowiedź do samego numeru sali.
  @US18.1 @aplikacja @do-zrobienia
  Scenariusz: Sam numer sali bez budynku prosi o dopisanie budynku
    Jeżeli Gość niezalogowany wpisze „329”
    Wtedy pod polem widzi „Dopisz budynek do numeru sali. Na przykład: A-1 329.”
    Ale aplikacja nie szuka sali

  # @do-zrobienia #1481: lista „Jak dojść” w karcie sali.
  @US18.2 @BR-25 @aplikacja @do-zrobienia
  Szablon scenariusza: Kroki do sali podane tekstem bez rysowania trasy
    # Sala 012 na piętrze wejścia i brak piętra wejścia to dane testowe, a nie stan faktyczny A-1.
    Zakładając, że <dane>
    Jeżeli Gość niezalogowany znajdzie salę <sala> w budynku A-1
    Wtedy widzi listę „Jak dojść” z krokami: <kroki>
    Ale mapa nie pokazuje narysowanej trasy

    Przykłady:
      | dane                                                                 | sala | kroki                                                                                              |
      | wejście zalecane A-1 jest na parterze, a winda łączy parter z piętrem sali 329 | 329  | wejście zalecane na parterze, winda z odległością od wejścia i piętrem sali, sala z odległością od windy |
      | sala 012 jest na piętrze wejścia zalecanego A-1                      | 012  | wejście zalecane, sala z przybliżoną odległością od wejścia                                        |
      | dane A-1 nie podają piętra wejścia zalecanego                        | 329  | wejście zalecane z dopiskiem, że przyjęto parter, winda, sala                                      |

  # @do-zrobienia #1481: karta sali z listą „Jak dojść”.
  @US18.2 @aplikacja @do-zrobienia
  Scenariusz: Znaleziona sala nie zaczyna pokazu drogi
    Jeżeli Gość niezalogowany znajdzie salę 329 w budynku A-1
    Wtedy plan pokazuje 2 piętro z wyróżnioną salą 329
    Oraz karta sali ma całą listę „Jak dojść” bez wybranego kroku i przycisk „Pokaż po kolei”

  # @do-zrobienia #1481: pokaz kroków po kolei.
  @US18.2 @aplikacja @do-zrobienia
  Scenariusz: Pokaż po kolei prowadzi od wejścia do sali
    Zakładając, że Gość niezalogowany znalazł salę 329 w budynku A-1
    Jeżeli dotknie „Pokaż po kolei”
    Wtedy krok 1 jest wybrany, a karta pokazuje „Krok 1 z 3”
    Oraz plan pokazuje parter z wejściem zalecanym
    Jeżeli dotknie „Następny krok”
    Wtedy krok 2 jest wybrany, a mapa pokazuje windę

  # @do-zrobienia #1481: powrót z arkusza wejścia do kroku.
  @US18.2 @aplikacja @do-zrobienia
  Scenariusz: Arkusz wejścia z kroku wraca do tego kroku
    Zakładając, że w karcie sali 329 jest wybrany krok 1
    Jeżeli Gość niezalogowany dotknie „Szczegóły wejścia” i potem „Zamknij”
    Wtedy widzi kartę sali 329 z wybranym krokiem 1

  # @do-zrobienia #3383: dotknięcie sali na planie.
  @US18.1 @aplikacja @do-zrobienia
  Scenariusz: Dotknięcie sali z numerem na planie otwiera kartę sali
    Zakładając, że Gość niezalogowany widzi plan 2 piętra A-1 z numerami sal
    Jeżeli dotknie sali 329
    Wtedy widzi kartę sali 329 z listą „Jak dojść”
    Ale dotknięcie korytarza nie otwiera karty sali

  # @do-zrobienia #1481: arkusz sali ma jeszcze „Pokaż opis budynku” zamiast wiersza budynku.
  @US18.3 @aplikacja @do-zrobienia
  Szablon scenariusza: Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem
    # Dane testowe, a nie stan faktyczny budynku A-1.
    Zakładając, że <sytuacja>
    Jeżeli Gość niezalogowany wyszuka „A-1 <numer>”
    Wtedy widzi zdanie, które mówi, czego brakuje
    Oraz widzi wiersz „Budynek A-1”, który otwiera opis budynku

    Przykłady:
      | sytuacja                                                     | numer |
      | dane PWr budynku A-1 nie mają sali 999                       | 999   |
      | żadna winda budynku A-1 nie łączy piętra wejścia z piętrem sali 329 | 329   |
