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

  @US8.1 @BR-16
  Scenariusz: Zgłoszenie odrzucone pokazuje powód odrzucenia
    Zakładając, że Weryfikator odrzucił zgłoszenie Zbierającego z powodem „Zdjęcie nie pokazuje progu”
    Jeżeli Zbierający otworzy listę swoich zgłoszeń
    Wtedy zgłoszenie ma status „odrzucone”
    Oraz przy zgłoszeniu widać powód odrzucenia „Zdjęcie nie pokazuje progu”

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

  @US8.4 @BR-19
  Scenariusz: Inny Zbierający niż autor zgłasza usunięcie bariery z karty budynku
    Zakładając, że zgłoszenie TB-9 „niesprawna winda” na piętrze budynku C-13 jest przyjęte, a jego autorem jest inna osoba
    Jeżeli Zbierający otworzy kartę budynku C-13 i wybierze zmianę tego zgłoszenia z oznaczeniem „bariera usunięta”
    Oraz zrobi nowe zdjęcie sprawnej windy i wyśle zmianę przy dostępnej sieci
    Wtedy powstaje zgłoszenie „bariera usunięta” z typem, miejscem i piętrem poprzedniego zgłoszenia
    Oraz na liście Zbierającego to zgłoszenie ma status „wysłane”
    Oraz niesprawna winda zostaje na mapie do decyzji Weryfikatora

  @US8.4 @BR-19
  Scenariusz: Usunięcia bariery nie da się zgłosić bez nowego zdjęcia
    Zakładając, że Zbierający zaczął zmianę zgłoszenia przyjętego TB-9 „niesprawna winda” z oznaczeniem „bariera usunięta”
    Jeżeli spróbuje wysłać zmianę bez nowego zdjęcia miejsca
    Wtedy zgłoszenie nie zostaje wysłane
