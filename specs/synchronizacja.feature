# language: pl
Funkcja: Tryb offline i synchronizacja
  Zbierający pracuje tam, gdzie nie ma sieci, i nie traci zgłoszeń (N6).
  Przy konflikcie wersji nikt nie traci własnej ani cudzej pracy (N7).

  Założenia:
    Zakładając, że Zbierający jest zalogowany z rolą Zbierającego

  @US9.1 @BR-9
  Scenariusz: Bez sieci widać wskaźnik, a zgłoszenie czeka ze statusem „lokalne”
    Zakładając, że telefon nie ma sieci
    Jeżeli Zbierający zapisze zgłoszenie TB-8 „brak windy” z uciążliwością 3
    Wtedy aplikacja pokazuje wskaźnik braku sieci
    Oraz zgłoszenie czeka w kolejce ze statusem „lokalne”

  @US9.1 @US4.2 @BR-9
  Scenariusz: Zgłoszenia zapisane bez sieci przetrwają restart telefonu
    Zakładając, że Zbierający zapisał bez sieci 3 zgłoszenia ze zdjęciami
    Jeżeli zamknie aplikację i uruchomi telefon ponownie
    Wtedy wszystkie 3 zgłoszenia ze zdjęciami nadal czekają ze statusem „lokalne”

  @US9.2 @BR-9
  Scenariusz: Po powrocie sieci synchronizacja rusza sama
    Zakładając, że Zbierający ma 2 zgłoszenia ze statusem „lokalne”
    Oraz aplikacja jest otwarta na ekranie
    Jeżeli sieć wróci
    Wtedy najpóźniej po 60 s oba zgłoszenia mają status „wysłane”
    Ale Zbierający nie musiał niczego wybierać

  @US9.2 @BR-9
  Scenariusz: Przerwana synchronizacja nie gubi zgłoszenia i nie tworzy drugiego
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli połączenie zerwie się w trakcie synchronizacji
    Oraz sieć wróci ponownie
    Wtedy zgłoszenie dociera na serwer i ma status „wysłane”
    Oraz na serwerze jest dokładnie jedno takie zgłoszenie

  @US9.3 @BR-10
  Scenariusz: Przy konflikcie wersji aplikacja pokazuje obie wersje
    Zakładając, że zgłoszenie Zbierającego z uciążliwością 1 ma status „wysłane” i jest na dwóch jego telefonach
    Oraz na pierwszym telefonie zmienił uciążliwość na 3 i zmiana dotarła na serwer
    Oraz na drugim telefonie bez sieci zmienił uciążliwość na 2
    Jeżeli drugi telefon złapie sieć
    Wtedy aplikacja pokazuje obie wersje: własną z uciążliwością 2 i z serwera z uciążliwością 3
    Oraz nazywa pole, którym wersje się różnią: „uciążliwość”
    Oraz serwer ma nadal wersję z uciążliwością 3

  @US9.3 @BR-10
  Szablon scenariusza: Przy konflikcie wersji Zbierający wybiera wersję
    Zakładając, że aplikacja pokazuje konflikt wersji: własną z uciążliwością 2 i z serwera z uciążliwością 3
    Jeżeli Zbierający wybierze wersję <wybór>
    Wtedy zgłoszenie na serwerze ma uciążliwość <uciążliwość>

    Przykłady:
      | wybór       | uciążliwość |
      | własną      | 2           |
      | z serwera   | 3           |

  @US9.3 @BR-10
  Scenariusz: Odłożony wybór wersji zostawia zmianę w telefonie
    Zakładając, że synchronizacja wykryła konflikt wersji zgłoszenia Zbierającego
    Jeżeli Zbierający zamknie ekran z obiema wersjami bez wyboru
    Wtedy jego zmiana nadal czeka w telefonie
    Oraz serwer ma nadal swoją wersję zgłoszenia
