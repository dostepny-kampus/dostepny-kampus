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
    Oraz wybierze grupę barier „komunikacja pionowa” i piętro, na którym stoi
    Oraz wyśle zgłoszenie
    Wtedy zgłoszenie powstaje po 3 krokach
    Oraz ma dołączoną bieżącą pozycję telefonu, o którą aplikacja nie pytała

  @US4.1
  Scenariusz: Zgłoszenie przy wejściu podpowiada wejście, a miejsce bierze z bieżącej pozycji
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz otworzył kartę budynku A-1
    Jeżeli przy wejściu głównym wybierze „Zgłoś barierę przy wejściu”
    Wtedy krok 1 zgłoszenia szybkiego ma napis „Wejście: »Wejście główne«”
    Oraz wysłane zgłoszenie ma wejście główne A-1 jako wskazówkę dla Weryfikatora
    Ale miejsce zgłoszenia pochodzi z bieżącej pozycji telefonu, a nie z wybranego wejścia

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

  @US4.1
  Scenariusz: Bez zgody na lokalizację aparat się nie otwiera
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz aplikacja nie ma zgody na lokalizację
    Jeżeli wybierze zgłoszenie szybkie
    Oraz odmówi zgody na lokalizację
    Wtedy aparat się nie otwiera
    Oraz widzi komunikat, że bez lokalizacji zgłoszenia nie da się wysłać
    Oraz widzi przycisk otwierający ustawienia telefonu
    Ale zgłoszenie nie powstaje

  @US4.1
  Scenariusz: Bez bieżącej pozycji zgłoszenie czeka na ponowną próbę
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz telefon nie ustala bieżącej pozycji
    Jeżeli zrobi zdjęcie bariery
    Oraz wybierze grupę barier „inne”
    Oraz spróbuje wysłać zgłoszenie
    Wtedy zgłoszenie nie zostaje wysłane
    Oraz zgłoszenie nie zapisuje się w telefonie do późniejszej wysyłki
    Oraz widzi komunikat, że telefon nie ustalił miejsca, z radą, co zrobić
    Oraz zdjęcie i grupa barier zostają do ponownej próby

  @US4.1
  Scenariusz: Pozycja ustalona przy ponownej próbie pozwala wysłać zgłoszenie
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz zrobił zdjęcie bariery i wybrał grupę barier „inne”
    Oraz widzi komunikat, że telefon nie ustalił miejsca
    Oraz telefon ma sieć
    Jeżeli telefon ustali bieżącą pozycję
    Oraz Odwiedzający wybierze ponowną próbę
    Wtedy zgłoszenie zostaje wysłane z tym samym zdjęciem, grupą barier i bieżącą pozycją
    Oraz Odwiedzający widzi potwierdzenie, że zgłoszenie trafi do weryfikacji

  @BR-14
  Szablon scenariusza: Budynek zgłoszenia szybkiego przypisuje system, a przy grupie „inne” piętro zostaje puste
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz stoi <miejsce>
    Jeżeli wyśle zgłoszenie szybkie z grupą barier „inne”
    Wtedy zgłoszenie <budynek>
    Oraz zgłoszenie nie ma piętra
    Ale aplikacja nie pyta o budynek ani o piętro

    Przykłady:
      | miejsce                                                        | budynek               |
      | w budynku C-13                                                 | ma budynek C-13       |
      | na chodniku przed wejściem głównym A-1, poza konturem budynku  | jest „poza budynkiem” |

  @US4.5 @BR-14
  Szablon scenariusza: Pytanie o piętro tylko w budynku i przy grupie, która go wymaga
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz zrobił zdjęcie bariery, stojąc <miejsce>
    Jeżeli wybierze grupę barier <grupa>
    Wtedy <pytanie>
    Oraz to nadal krok 2 z 3

    Przykłady:
      | miejsce                              | grupa                 | pytanie                                                                   |
      | w budynku D-2                        | „komunikacja pionowa” | widzi pytanie o piętro z „Nie wiem” i etykietami pięter z danych PWr      |
      | w budynku B-4                        | „toalety”             | widzi pytanie o piętro z „Nie wiem” i przyciskami „Parter”, „1”, „2”      |
      | w budynku C-13                       | „wejście i drzwi”     | widzi pytanie o piętro z „Nie wiem” i etykietami pięter z danych PWr      |
      | w budynku C-13                       | „inne”                | przechodzi do kroku 3 bez pytania o piętro                                |
      | na chodniku przed A-1, poza konturem | „komunikacja pozioma” | przechodzi do kroku 3 bez pytania o piętro                                |

  @US4.5
  Scenariusz: Odpowiedź „Nie wiem” zostawia piętro puste i pozwala wysłać zgłoszenie
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz w budynku D-2 wybrał grupę barier „komunikacja pionowa” i widzi pytanie o piętro
    Jeżeli wybierze „Nie wiem”
    Oraz wyśle zgłoszenie
    Wtedy zgłoszenie ma budynek D-2
    Oraz zgłoszenie nie ma piętra
    Oraz zgłoszenie powstało po 3 krokach

  @US4.5
  Scenariusz: Wybrane piętro trafia do zgłoszenia
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz w budynku B-4 wybrał grupę barier „toalety” i widzi pytanie o piętro
    Jeżeli wybierze piętro 2
    Oraz wyśle zgłoszenie
    Wtedy zgłoszenie ma budynek B-4 i piętro 2

  @US4.5 @BR-14
  Scenariusz: Pozycja poza konturem przy wysłaniu zostawia piętro puste
    Zakładając, że Odwiedzający jest zalogowany z rolą Odwiedzającego
    Oraz wybrał grupę barier „komunikacja pozioma”, zanim telefon ustalił bieżącą pozycję
    Oraz na pytanie o piętro wybrał „1”
    Jeżeli telefon ustali pozycję na chodniku przed A-1, poza konturem budynku
    Oraz Odwiedzający wyśle zgłoszenie
    Wtedy zgłoszenie jest „poza budynkiem”
    Oraz zgłoszenie nie ma piętra

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
