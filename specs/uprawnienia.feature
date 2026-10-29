# language: pl
Funkcja: Logowanie, role i zaproszenia
  Każda osoba korzysta tylko z czynności swojej roli. Role pracownicze nadaje Koordynator,
  więc o publikacji decydują osoby mu znane (N2, N8, N9).

  @US14.1
  Szablon scenariusza: Bez wymaganej roli czynność jest niedostępna w aplikacji i na serwerze
    Zakładając, że osoba jest zalogowana tylko z rolą <rola>
    Jeżeli przejrzy dostępne ekrany aplikacji
    Wtedy nie widzi ekranu „<czynność>”
    Oraz próba wykonania czynności „<czynność>” z pominięciem aplikacji jest odrzucona

    Przykłady:
      | rola           | czynność                  |
      | Odwiedzającego | pomiar precyzyjny         |
      | Odwiedzającego | lista zgłoszeń            |
      | Zbierającego   | kolejka weryfikacji       |
      | Zbierającego   | postęp inwentaryzacji     |
      | Weryfikatora   | eksport                   |
      | Weryfikatora   | zaproszenie               |
      | Koordynatora   | pomiar precyzyjny         |
      | Koordynatora   | kolejka weryfikacji       |

  @US14.1 @BR-19 @BR-20
  Szablon scenariusza: Znacznik piętra i zmianę „bariera usunięta” składa tylko Zbierający
    Zakładając, że osoba jest zalogowana tylko z rolą <rola>
    Jeżeli przejrzy dostępne ekrany aplikacji
    Wtedy nie widzi czynności „<czynność>”
    Oraz próba wykonania czynności „<czynność>” z pominięciem aplikacji jest odrzucona

    Przykłady:
      | rola           | czynność                       |
      | Koordynatora   | znacznik piętra obejrzanego    |
      | Weryfikatora   | zmiana »bariera usunięta«      |
      | Odwiedzającego | zmiana »bariera usunięta«      |

  @US14.1 @BR-3
  Scenariusz: Zbierający nie edytuje cudzego zgłoszenia
    Zakładając, że Zbierający wysłał zgłoszenie TB-13 „próg” w budynku C-13
    Jeżeli inny Zbierający spróbuje zmienić uciążliwość w tym zgłoszeniu z pominięciem aplikacji
    Wtedy próba jest odrzucona
    Oraz zgłoszenie ma nadal uciążliwość nadaną przez autora

  @US14.3 @BR-1 @BR-2
  Scenariusz: Samodzielna rejestracja daje tylko uprawnienia Odwiedzającego
    Zakładając, że osoba bez konta korzysta z aplikacji jako Gość niezalogowany
    Jeżeli założy konto przez samodzielną rejestrację i zaloguje się
    Wtedy w profilu widzi rolę „Odwiedzający”
    Oraz może złożyć zgłoszenie szybkie
    Ale nie widzi pomiaru precyzyjnego, kolejki weryfikacji ani postępu inwentaryzacji

  @US12.1 @BR-2
  Szablon scenariusza: Osoba z zaproszenia loguje się z wybraną rolą
    Zakładając, że Koordynator wysłał zaproszenie na adres <adres> z rolą „<rola>”
    Jeżeli osoba z tym adresem zaloguje się pierwszy raz
    Wtedy w profilu widzi rolę „<rola>”
    Oraz ma dostęp do ekranu „<ekran>”

    Przykłady:
      | adres                      | rola        | ekran                  |
      | zbierajacy@example.org     | Zbierający  | pomiar precyzyjny      |
      | weryfikator@example.org    | Weryfikator | kolejka weryfikacji    |
      | koordynator@example.org    | Koordynator | postęp inwentaryzacji  |

  @US12.2 @BR-1 @BR-2
  Scenariusz: Zaproszenie na adres istniejącego konta dodaje rolę
    Zakładając, że konto z adresem anna.nowak@example.org ma role Odwiedzający i Zbierający
    Jeżeli Koordynator zaprosi ten adres z rolą „Weryfikator”
    Oraz Anna Nowak zaloguje się ponownie
    Wtedy w profilu widzi role Odwiedzający, Zbierający i Weryfikator
    Oraz może otworzyć pomiar precyzyjny i kolejkę weryfikacji

  @BR-2
  Scenariusz: Rolę pracowniczą nadaje tylko Koordynator
    Zakładając, że Weryfikator jest zalogowany z rolą Weryfikatora
    Jeżeli spróbuje zaprosić adres nowy@example.org z rolą „Zbierający” z pominięciem aplikacji
    Wtedy próba jest odrzucona
    Oraz konto z adresem nowy@example.org nie dostaje roli Zbierającego

  @BR-4 @BR-1
  Scenariusz: Osoba z rolami Zbierającego i Weryfikatora nie przyjmie własnego zgłoszenia
    Zakładając, że osoba ma role Zbierającego i Weryfikatora
    Oraz jako Zbierający wysłała zgłoszenie TB-1 „schody przy wejściu” przy A-1
    Jeżeli jako Weryfikator spróbuje przyjąć to zgłoszenie z pominięciem aplikacji
    Wtedy próba jest odrzucona
    Oraz zgłoszenie ma nadal status „wysłane”

  @US14.4
  Scenariusz: Zalogowany Odwiedzający nie jest proszony o logowanie ponownie
    Zakładając, że Odwiedzający zalogował się przy pierwszym zgłoszeniu szybkim
    Oraz zamknął i ponownie uruchomił aplikację
    Jeżeli wybierze zgłoszenie szybkie
    Wtedy aplikacja od razu otwiera aparat
    Ale po wylogowaniu albo wycofaniu zgody znów prosi o logowanie
