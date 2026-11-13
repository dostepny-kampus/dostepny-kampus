# language: pl
Funkcja: Tryb offline i synchronizacja
  Zbierający pracuje tam, gdzie nie ma sieci, i nie traci zgłoszeń (N6).
  Przy konflikcie wersji nikt nie traci własnej ani cudzej pracy (N7).

  Założenia:
    Zakładając, że Zbierający jest zalogowany z rolą Zbierającego

  @US9.1 @BR-9 @aplikacja
  Scenariusz: Bez sieci widać wskaźnik, a zgłoszenie czeka ze statusem „lokalne”
    Zakładając, że telefon nie ma sieci
    Jeżeli Zbierający zapisze zgłoszenie TB-8 „brak windy” z uciążliwością 3
    Wtedy aplikacja pokazuje wskaźnik braku sieci
    Oraz zgłoszenie czeka w kolejce ze statusem „lokalne”

  @US9.1 @US4.2 @BR-9 @aplikacja
  Scenariusz: Zgłoszenia zapisane bez sieci przetrwają restart telefonu
    Zakładając, że Zbierający zapisał bez sieci 3 zgłoszenia ze zdjęciami
    Jeżeli zamknie aplikację i uruchomi telefon ponownie
    Wtedy wszystkie 3 zgłoszenia ze zdjęciami nadal czekają ze statusem „lokalne”

  @US9.2 @BR-9 @aplikacja
  Scenariusz: Po powrocie sieci synchronizacja rusza sama
    Zakładając, że Zbierający ma 2 zgłoszenia ze statusem „lokalne”
    Oraz aplikacja jest otwarta na ekranie
    Jeżeli sieć wróci
    Wtedy najpóźniej po 60 s oba zgłoszenia mają status „wysłane”
    Ale Zbierający nie musiał niczego wybierać

  @US9.2 @BR-9 @aplikacja
  Scenariusz: Przerwana synchronizacja nie gubi zgłoszenia i nie tworzy drugiego
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli połączenie zerwie się w trakcie synchronizacji
    Oraz sieć wróci ponownie
    Wtedy zgłoszenie dociera na serwer i ma status „wysłane”
    Oraz na serwerze jest dokładnie jedno takie zgłoszenie

  @US9.4 @BR-9 @NFR-50 @aplikacja
  Scenariusz: Awaria serwera działa jak brak sieci
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli serwer odpowiada błędem 5xx
    Wtedy zgłoszenie zostaje w telefonie ze statusem „lokalne”
    Oraz aplikacja ponawia wysyłkę sama
    Ale nie pokazuje statusu „Nie wysłano”

  @US9.4 @US14.4 @BR-26 @aplikacja
  Scenariusz: Wygasłe zalogowanie czeka na ponowne zalogowanie
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli serwer odrzuci wysyłkę błędem 401
    Wtedy zgłoszenie zostaje w telefonie ze statusem „lokalne”
    Oraz aplikacja prosi o ponowne zalogowanie napisem „Logowanie wygasło. Zaloguj się, a wyślemy zgłoszenia same.”
    Jeżeli Zbierający się zaloguje
    Wtedy zgłoszenie ma status „wysłane”
    Ale Zbierający nie musiał niczego wysyłać

  @US9.5 @US14.1 @BR-26 @aplikacja
  Scenariusz: Odebrana rola zostawia zgłoszenie w telefonie ze statusem „Nie wysłano”
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli serwer odrzuci wysyłkę błędem 403
    Wtedy zgłoszenie zostaje w telefonie ze statusem „Nie wysłano”
    Oraz aplikacja nie ponawia wysyłki sama
    Oraz na liście „Moje zgłoszenia” przy zgłoszeniu widać powód „Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie.”
    Oraz przy zgłoszeniu jest „Otwórz”

  @US9.5 @BR-26 @aplikacja
  Szablon scenariusza: Odrzucona treść albo zdjęcie zostawia zgłoszenie ze statusem „Nie wysłano”
    Zakładając, że Zbierający ma zgłoszenie ze statusem „lokalne”
    Jeżeli serwer odrzuci wysyłkę błędem <kod>
    Wtedy zgłoszenie zostaje w telefonie ze statusem „Nie wysłano”
    Oraz aplikacja nie ponawia wysyłki sama
    Oraz na liście „Moje zgłoszenia” przy zgłoszeniu widać powód „Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie.”

    Przykłady:
      | kod |
      | 400 |
      | 404 |
      | 413 |

  @US9.5 @BR-26 @aplikacja
  Scenariusz: Licznik oczekujących nie liczy zgłoszeń „Nie wysłano”
    Zakładając, że Zbierający ma 2 zgłoszenia ze statusem „lokalne” i 1 ze statusem „Nie wysłano”
    Jeżeli otworzy ekran synchronizacji
    Wtedy widzi „Czeka na wysłanie: 2”

  @US9.5 @BR-26 @aplikacja
  Scenariusz: Ponowny zapis wysyła zgłoszenie „Nie wysłano” znów
    Zakładając, że Zbierający ma zgłoszenie ze statusem „Nie wysłano”
    Jeżeli otworzy je, poprawi i zapisze ponownie
    Wtedy zgłoszenie ma status „lokalne”
    Oraz aplikacja wysyła je znów

  @US9.3 @BR-10 @aplikacja
  Scenariusz: Przy konflikcie wersji aplikacja pokazuje obie wersje
    Zakładając, że zgłoszenie Zbierającego z uciążliwością 1 ma status „wysłane” i jest na dwóch jego telefonach
    Oraz na pierwszym telefonie zmienił uciążliwość na 3 i zmiana dotarła na serwer
    Oraz na drugim telefonie bez sieci zmienił uciążliwość na 2
    Jeżeli drugi telefon złapie sieć
    Wtedy aplikacja pokazuje obie wersje: własną z uciążliwością 2 i z serwera z uciążliwością 3
    Oraz nazywa pole, którym wersje się różnią: „uciążliwość”
    Oraz serwer ma nadal wersję z uciążliwością 3

  @US9.3 @BR-10 @aplikacja
  Szablon scenariusza: Przy konflikcie wersji Zbierający wybiera wersję
    Zakładając, że aplikacja pokazuje konflikt wersji: własną z uciążliwością 2 i z serwera z uciążliwością 3
    Jeżeli Zbierający wybierze wersję <wybór>
    Wtedy zgłoszenie na serwerze ma uciążliwość <uciążliwość>

    Przykłady:
      | wybór       | uciążliwość |
      | własną      | 2           |
      | z serwera   | 3           |

  @US9.3 @BR-10 @aplikacja
  Scenariusz: Odłożony wybór wersji zostawia zmianę w telefonie
    Zakładając, że synchronizacja wykryła konflikt wersji zgłoszenia Zbierającego
    Jeżeli Zbierający zamknie ekran z obiema wersjami bez wyboru
    Wtedy jego zmiana nadal czeka w telefonie
    Oraz serwer ma nadal swoją wersję zgłoszenia
