# language: pl
Funkcja: Lista i zmiana własnych zgłoszeń Zbierającego
  Zbierający widzi status każdego swojego zgłoszenia i wie, co dotarło,
  a co trzeba poprawić (N6). Zmiana zgłoszenia przyjętego znów przechodzi weryfikację.

  Założenia:
    Zakładając, że Zbierający jest zalogowany z rolą Zbierającego

  @US8.1
  Szablon scenariusza: Każde własne zgłoszenie ma jeden status tekstem i ikoną
    Zakładając, że Zbierający ma zgłoszenie TB-13 „próg” w budynku C-13 ze statusem „<status>”
    Jeżeli otworzy listę swoich zgłoszeń
    Wtedy widzi to zgłoszenie z jednym statusem „<status>”
    Oraz status ma napis i ikonę, czytelne w odcieniach szarości

    Przykłady:
      | status        |
      | lokalne       |
      | wysłane       |
      | w weryfikacji |
      | przyjęte      |
      | odrzucone     |
      | archiwalne    |

  @US8.1 @BR-16
  Scenariusz: Zgłoszenie odrzucone pokazuje powód odrzucenia
    Zakładając, że Weryfikator odrzucił zgłoszenie Zbierającego z powodem „Zdjęcie nie pokazuje progu”
    Jeżeli Zbierający otworzy listę swoich zgłoszeń
    Wtedy zgłoszenie ma status „odrzucone”
    Oraz przy zgłoszeniu widać powód odrzucenia „Zdjęcie nie pokazuje progu”

  @US8.1
  Scenariusz: Zgłoszenie ze zmianą czekającą w telefonie ma napis przy statusie
    Zakładając, że Zbierający ma zgłoszenie TB-13 „próg” w budynku C-13 ze statusem „wysłane”
    Oraz bez sieci zmienił w nim uciążliwość
    Jeżeli otworzy listę swoich zgłoszeń
    Wtedy zgłoszenie ma status „wysłane” i napis „Zmiana czeka na wysłanie”

  @US8.1 @US4.4
  Scenariusz: Lista nie pokazuje własnych zgłoszeń szybkich
    Zakładając, że Zbierający wysłał zgłoszenie szybkie z grupą „Schody i windy”
    Oraz ma zgłoszenie z pomiaru TB-13 „próg” w budynku C-13
    Jeżeli otworzy listę swoich zgłoszeń
    Wtedy widzi zgłoszenie TB-13
    Ale nie widzi zgłoszenia szybkiego

  @US8.1
  Szablon scenariusza: „Otwórz” mają tylko zgłoszenia, które można poprawić
    Zakładając, że Zbierający ma zgłoszenie TB-13 „próg” w budynku C-13 ze statusem „<status>”
    Jeżeli otworzy listę swoich zgłoszeń
    Wtedy przy tym zgłoszeniu <otwórz>

    Przykłady:
      | status        | otwórz                 |
      | lokalne       | jest „Otwórz”          |
      | wysłane       | jest „Otwórz”          |
      | w weryfikacji | jest „Otwórz”          |
      | odrzucone     | jest „Otwórz”          |
      | przyjęte      | nie ma „Otwórz”        |
      | archiwalne    | nie ma „Otwórz”        |

  @US8.1
  Scenariusz: Najnowsze zgłoszenia są na górze listy
    Zakładając, że Zbierający ma zgłoszenia z 12.10.2026, 24.10.2026 i 28.10.2026
    Jeżeli otworzy listę swoich zgłoszeń
    Wtedy pierwsze jest zgłoszenie z 28.10.2026, a ostatnie z 12.10.2026

  @US8.2 @BR-9
  Szablon scenariusza: Status „wysłane” dopiero po potwierdzeniu odbioru przez serwer
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli sieć wróci
    Oraz serwer <odpowiedź>
    Wtedy zgłoszenie ma status „<status>”

    Przykłady:
      | odpowiedź                          | status   |
      | nie potwierdzi odbioru zgłoszenia  | lokalne  |
      | potwierdzi odbiór zgłoszenia       | wysłane  |

  @US8.2
  Scenariusz: Status „w weryfikacji” po wzięciu zgłoszenia przez Weryfikatora
    Zakładając, że zgłoszenie Zbierającego ma status „wysłane”
    Jeżeli Weryfikator weźmie je z kolejki
    Wtedy na liście Zbierającego zgłoszenie ma status „w weryfikacji”

  @US8.3 @BR-3
  Szablon scenariusza: Edycja zgłoszenia przed decyzją Weryfikatora
    Zakładając, że zgłoszenie Zbierającego z uciążliwością 1 ma status „<status>”
    Jeżeli Zbierający zmieni uciążliwość na 2
    Wtedy zgłoszenie ma uciążliwość 2
    Oraz zgłoszenie nadal ma status „<status>”

    Przykłady:
      | status        |
      | lokalne       |
      | wysłane       |
      | w weryfikacji |

  @US8.3 @BR-3 @BR-16
  Scenariusz: Poprawa i ponowne wysłanie zgłoszenia odrzuconego
    Zakładając, że zgłoszenie Zbierającego odrzucono z powodem „Zdjęcie nie pokazuje progu”
    Jeżeli Zbierający zrobi nowe zdjęcie progu
    Oraz wyśle zgłoszenie ponownie
    Wtedy zgłoszenie ma status „wysłane”
    Oraz trafia z powrotem do kolejki weryfikacji

  @BR-3 @BR-18
  Scenariusz: Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie
    Zakładając, że zgłoszenie Zbierającego TB-13 „próg” w C-13 z uciążliwością 2 jest przyjęte
    Jeżeli Zbierający zmieni uciążliwość na 3
    Wtedy powstaje nowe zgłoszenie powiązane z poprzednim, ze statusem „lokalne”
    Oraz poprzednie zgłoszenie nadal jest przyjęte i widoczne na mapie

  @BR-3 @BR-18
  Scenariusz: Zwykłą zmianę przyjętego zgłoszenia z pomiaru precyzyjnego składa tylko autor
    Zakładając, że przyjęte zgłoszenie TB-13 „próg” w C-13 z pomiaru precyzyjnego złożył inny Zbierający
    Jeżeli Zbierający spróbuje zmienić jego uciążliwość z pominięciem aplikacji
    Wtedy próba jest odrzucona
    Oraz nie powstaje nowe zgłoszenie

  @BR-18
  Scenariusz: Przyjęte zgłoszenie szybkie zmienia dowolny Zbierający
    Zakładając, że przyjęte zgłoszenie szybkie Odwiedzającego w budynku C-13 ma typ TB-13 „próg” i uciążliwość 1
    Jeżeli Zbierający, który nie jest autorem tego zgłoszenia, zmieni uciążliwość na 2
    Wtedy powstaje nowe zgłoszenie powiązane z poprzednim, ze statusem „lokalne”
    Oraz autorem nowego zgłoszenia jest ten Zbierający
    Oraz poprzednie zgłoszenie nadal jest przyjęte i widoczne na mapie

  @US8.4 @BR-19
  Scenariusz: Inny Zbierający niż autor zgłasza usunięcie bariery
    Zakładając, że zgłoszenie TB-9 „niesprawna winda” na piętrze budynku C-13 jest przyjęte, a jego autorem jest inna osoba
    Jeżeli Zbierający na karcie budynku C-13 wybierze przy tym zgłoszeniu zmianę z oznaczeniem „bariera usunięta”
    Oraz zrobi nowe zdjęcie sprawnej windy i wyśle zmianę przy dostępnej sieci
    Wtedy powstaje zgłoszenie „bariera usunięta” z typem, miejscem i piętrem poprzedniego zgłoszenia
    Oraz na liście Zbierającego to zgłoszenie ma status „wysłane”
    Oraz niesprawna winda zostaje na mapie do decyzji Weryfikatora

  @US8.4 @BR-19
  Scenariusz: Usunięcia bariery nie da się zgłosić bez nowego zdjęcia
    Zakładając, że Zbierający zaczął zmianę zgłoszenia przyjętego TB-9 „niesprawna winda” z oznaczeniem „bariera usunięta”
    Jeżeli spróbuje wysłać zmianę bez nowego zdjęcia miejsca
    Wtedy zgłoszenie nie zostaje wysłane

  @US8.4 @BR-19 @BR-7
  Scenariusz: Zgłoszenie „bariera usunięta” zapisuje się bez uciążliwości
    Zakładając, że Zbierający zaczął zmianę zgłoszenia przyjętego TB-9 „niesprawna winda” z oznaczeniem „bariera usunięta”
    Oraz zrobił nowe zdjęcie miejsca
    Jeżeli zapisze zmianę bez podania uciążliwości
    Wtedy zgłoszenie „bariera usunięta” zapisuje się ze statusem „lokalne”
    Ale aplikacja nie wymaga uciążliwości
