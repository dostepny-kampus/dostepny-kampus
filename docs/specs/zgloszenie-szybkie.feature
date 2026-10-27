# language: pl
@BR-8
Funkcja: Zgłoszenie szybkie
  Odwiedzający zgłasza nową barierę zdjęciem i bieżącą pozycją, bez trudnego formularza (N3, N4).
  Typ bariery i uciążliwość ustala później Weryfikator.

  @US4.1
  Scenariusz: Zgłoszenie szybkie w 3 krokach z pozycją dołączoną automatycznie
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz stoi przy niesprawnej windzie w budynku D-2
    Jeżeli zrobi zdjęcie windy
    Oraz wybierze grupę barier „komunikacja pionowa”
    Oraz wyśle zgłoszenie
    Wtedy zgłoszenie powstaje po 3 krokach
    Oraz ma dołączoną bieżącą pozycję telefonu, o którą aplikacja nie pytała

  @US4.3
  Scenariusz: Wybór jednej z 6 grup barier dużymi przyciskami
    Zakładając, że Odwiedzający zrobił zdjęcie bariery w zgłoszeniu szybkim
    Jeżeli przejdzie do wyboru grupy barier
    Wtedy widzi 6 przycisków: „wejście i drzwi”, „komunikacja pionowa”, „komunikacja pozioma”, „toalety”, „orientacja i informacja”, „inne”
    Oraz każdy przycisk ma co najmniej 48 × 48 dp
    Ale aplikacja nie pyta o typ bariery ani o uciążliwość

  @US4.1 @US4.3
  Szablon scenariusza: Zgłoszenia szybkiego nie da się wysłać bez zdjęcia albo grupy barier
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz zaczął zgłoszenie szybkie w budynku C-13
    Jeżeli spróbuje wysłać zgłoszenie bez <brak>
    Wtedy zgłoszenie nie zostaje wysłane

    Przykłady:
      | brak          |
      | zdjęcia       |
      | grupy barier  |

  Szablon scenariusza: Zgłoszenia szybkiego bez bieżącej pozycji nie da się wysłać
    # Reakcja aplikacji jest ustalana w fazie B (UC7, 2a i 4c); scenariusz sprawdza tylko brak wysyłki.
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz <przyczyna>
    Jeżeli zrobi zdjęcie bariery
    Oraz wybierze grupę barier „wejście i drzwi”
    Oraz spróbuje wysłać zgłoszenie
    Wtedy zgłoszenie nie zostaje wysłane

    Przykłady:
      | przyczyna                             |
      | aplikacja nie ma zgody na lokalizację |
      | telefon nie ustala bieżącej pozycji   |

  @US4.2 @BR-9
  Scenariusz: Zgłoszenie szybkie bez sieci zapisuje się w telefonie
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz telefon nie ma sieci
    Jeżeli wyśle zgłoszenie szybkie z grupą barier „komunikacja pozioma”
    Wtedy zgłoszenie zapisuje się w telefonie

  @US4.2 @BR-9
  Scenariusz: Zgłoszenie szybkie zapisane bez sieci wysyła się samo po powrocie sieci
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz w telefonie czeka zgłoszenie szybkie zapisane bez sieci
    Jeżeli sieć wróci
    Wtedy zgłoszenie dociera na serwer bez działania Odwiedzającego
    Oraz na serwerze jest dokładnie jedno takie zgłoszenie

  @US4.4
  Scenariusz: Potwierdzenie po wysłaniu bez dalszego statusu
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz telefon ma sieć
    Jeżeli wyśle zgłoszenie szybkie z grupą barier „toalety”
    Wtedy widzi potwierdzenie, że zgłoszenie trafi do weryfikacji
    Ale aplikacja nie pokazuje mu listy jego zgłoszeń ani ich statusów

  @US4.4 @US4.2
  Scenariusz: Potwierdzenie bez sieci mówi o późniejszej wysyłce
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz telefon nie ma sieci
    Jeżeli wyśle zgłoszenie szybkie z grupą barier „inne”
    Wtedy widzi potwierdzenie, że zgłoszenie wyśle się po powrocie sieci i trafi do weryfikacji

  @US14.2 @RO-GOS
  Scenariusz: Gość niezalogowany jest proszony o logowanie przed zdjęciem
    Zakładając, że Gość niezalogowany ma otwartą mapę kampusu
    Jeżeli wybierze zgłoszenie szybkie
    Wtedy widzi prośbę o logowanie albo rejestrację
    Ale aparat się nie otwiera

  @US14.2 @RO-GOS
  Scenariusz: Gość po zalogowaniu wraca do zgłoszenia szybkiego
    Zakładając, że Gość niezalogowany wybrał zgłoszenie szybkie i widzi prośbę o logowanie
    Jeżeli zaloguje się jako Odwiedzający
    Wtedy aplikacja otwiera aparat do zdjęcia bariery

  @US14.2 @RO-GOS
  Scenariusz: Gość rezygnuje z logowania
    Zakładając, że Gość niezalogowany wybrał zgłoszenie szybkie i widzi prośbę o logowanie
    Jeżeli zrezygnuje z logowania
    Wtedy zgłoszenie nie powstaje
    Oraz aplikacja wraca do mapy kampusu
