# language: pl
@BR-24
Funkcja: Oznaczenie wejścia
  Przy każdym wejściu widać, czy jest dostępne, oraz datę i źródło oznaczenia (N1, N2).
  Zbierający zmienia oznaczenie na obchodzie, a zmianę sprawdza inny Weryfikator (N8).

  Założenia:
    Zakładając, że budynek B-4 jest budynkiem podstawowym z konturem i wejściami z OpenStreetMap
    # Dane testowe, a nie stan faktyczny budynku B-4.
    Oraz w danych testowych wejście boczne B-4 nie ma informacji o dostępności ani w danych PWr, ani w OpenStreetMap

  @US14.1
  Szablon scenariusza: Bez roli Zbierającego zmiany oznaczenia wejścia nie da się złożyć
    Zakładając, że osoba jest zalogowana tylko z rolą <rola>
    Jeżeli przejrzy dostępne ekrany aplikacji
    Wtedy nie widzi czynności „zmiana oznaczenia wejścia”
    Oraz próba złożenia zmiany oznaczenia wejścia z pominięciem aplikacji jest odrzucona

    Przykłady:
      | rola           |
      | Odwiedzającego |
      | Weryfikatora   |
      | Koordynatora   |

  Reguła: Import nadaje oznaczenie z danych, a bez danych „brak danych”

    @US17.1 @US17.3 @US2.1
    Szablon scenariusza: Import nadaje wejściu oznaczenie z danych PWr
      Zakładając, że dane PWr opisują <wejście> A-1 jako <opis>
      Jeżeli 3.11.2026 zakończy się import budynków
      Wtedy na karcie budynku A-1 <wejście> ma napis „<oznaczenie>” i ikonę
      Oraz przy tym wejściu widnieje data oznaczenia 3.11.2026 i źródło „import”

      Przykłady:
        | wejście        | opis                  | oznaczenie  |
        | wejście główne | wejście ze schodami   | niedostępne |
        | inne wejście   | wejście dostosowane   | dostępne    |

    @US17.1 @US17.3 @US2.1
    Scenariusz: Import bez danych daje oznaczenie „brak danych”
      Jeżeli 3.11.2026 zakończy się import budynków
      Wtedy na karcie budynku B-4 wejście boczne ma napis „brak danych” i ikonę
      Ale wejście boczne nie ma napisu „niedostępne”
      Oraz przy tym wejściu widnieje data oznaczenia 3.11.2026 i źródło „import”
      Oraz oznaczenia „dostępne”, „niedostępne” i „brak danych” da się odróżnić w odcieniach szarości

  Reguła: Zmianę oznaczenia składa Zbierający, a sprawdza Weryfikator, który jej nie złożył

    Założenia:
      Zakładając, że Zbierający jest zalogowany z rolą Zbierającego
      Oraz pierwszy i drugi Weryfikator są zalogowani z rolą Weryfikatora

    @US17.2 @BR-9
    Scenariusz: Zmiana oznaczenia wejścia przechodzi te same statusy co zgłoszenie
      Zakładając, że wejście boczne B-4 ma oznaczenie „brak danych”
      Oraz telefon nie ma sieci
      Jeżeli Zbierający wybierze dla wejścia bocznego B-4 oznaczenie „niedostępne” i zrobi zdjęcie wejścia
      Wtedy zgłoszenie zmiany oznaczenia czeka w telefonie ze statusem „lokalne”
      Jeżeli sieć wróci
      Wtedy zgłoszenie zmiany oznaczenia ma status „wysłane” i jest w kolejce weryfikacji
      Oraz wejście boczne B-4 nadal ma oznaczenie „brak danych”

    @US17.2 @BR-7
    Scenariusz: Zmiany oznaczenia wejścia nie da się zapisać bez zdjęcia
      Jeżeli Zbierający wybierze dla wejścia bocznego B-4 oznaczenie „dostępne” i nie zrobi zdjęcia
      Wtedy aplikacja wskazuje brak zdjęcia i nie zapisuje zgłoszenia
      Ale nie pyta o typ bariery ani uciążliwość

    @US17.2 @US17.3
    Scenariusz: Przyjęta zmiana zmienia oznaczenie, datę i źródło na karcie budynku
      Zakładając, że wejście boczne B-4 ma oznaczenie „brak danych” ze źródłem „import”
      Oraz Zbierający wysłał zmianę oznaczenia tego wejścia na „niedostępne” z nowym zdjęciem
      Jeżeli pierwszy Weryfikator 12.11.2026 weźmie i przyjmie tę zmianę
      Wtedy zgłoszenie zmiany oznaczenia ma status „archiwalne”
      Oraz na karcie budynku B-4 wejście boczne ma napis „niedostępne” i ikonę
      Oraz przy tym wejściu widnieje data oznaczenia 12.11.2026 i źródło „obchód”
      Ale zgłoszenie zmiany oznaczenia nie jest widoczne na mapie ani w eksporcie

    @US17.2 @US17.3
    Scenariusz: Zmiana może potwierdzić dotychczasowe oznaczenie
      Zakładając, że wejście główne A-1 ma oznaczenie „niedostępne” z datą 3.11.2026 i źródłem „import”
      Oraz Zbierający wysłał zmianę oznaczenia tego wejścia na „niedostępne” z nowym zdjęciem
      Jeżeli pierwszy Weryfikator 12.11.2026 weźmie i przyjmie tę zmianę
      Wtedy wejście główne A-1 nadal ma oznaczenie „niedostępne”
      Oraz przy tym wejściu widnieje data oznaczenia 12.11.2026 i źródło „obchód”

    @US17.2 @BR-16
    Scenariusz: Odrzucona zmiana nie zmienia oznaczenia wejścia
      Zakładając, że inne wejście A-1 ma oznaczenie „dostępne” z datą 3.11.2026 i źródłem „import”
      Oraz Zbierający wysłał zmianę oznaczenia tego wejścia na „niedostępne” ze zdjęciem innego miejsca
      Jeżeli pierwszy Weryfikator weźmie tę zmianę i odrzuci ją z powodem „zdjęcie nie pokazuje wejścia”
      Wtedy zgłoszenie zmiany oznaczenia ma status „odrzucone” z tym powodem
      Oraz inne wejście A-1 nadal ma oznaczenie „dostępne” z datą 3.11.2026 i źródłem „import”
      Oraz Zbierający widzi powód odrzucenia na liście swoich zgłoszeń

    @US17.2 @US10.5 @BR-4
    Scenariusz: Weryfikator nie sprawdza zmiany oznaczenia, którą sam złożył
      Zakładając, że pierwszy Weryfikator ma też rolę Zbierającego
      Oraz jako Zbierający wysłał zmianę oznaczenia wejścia bocznego B-4 na „dostępne”
      Jeżeli pierwszy Weryfikator otworzy kolejkę weryfikacji
      Wtedy nie widzi w niej tej zmiany
      Oraz próba przyjęcia tej zmiany przez pierwszego Weryfikatora z pominięciem aplikacji jest odrzucona
      Ale drugi Weryfikator widzi tę zmianę w swojej kolejce

    @US17.2 @US17.3
    Scenariusz: Później przyjęta zmiana tego samego wejścia zastępuje wcześniejszą
      Zakładając, że Zbierający wysłał zmianę oznaczenia wejścia bocznego B-4 na „dostępne”
      Oraz inny Zbierający wysłał zmianę oznaczenia tego wejścia na „niedostępne”
      Jeżeli pierwszy Weryfikator 12.11.2026 przyjmie zmianę na „dostępne”
      Oraz drugi Weryfikator 13.11.2026 przyjmie zmianę na „niedostępne”
      Wtedy wejście boczne B-4 ma oznaczenie „niedostępne” z datą 13.11.2026 i źródłem „obchód”
