# language: pl
Funkcja: Weryfikacja zgłoszeń
  Weryfikator odsiewa błędy i duplikaty, zanim bariera trafi na mapę (N8).
  Dwie osoby nie sprawdzają tego samego zgłoszenia (N7).

  Założenia:
    Zakładając, że pierwszy i drugi Weryfikator są zalogowani z rolą Weryfikatora

  @BR-11
  Reguła: Zgłoszenie w weryfikacji obsługuje tylko Weryfikator, który je wziął

    @US10.4 @US8.2
    Scenariusz: Wzięte zgłoszenie znika z kolejki pozostałych Weryfikatorów
      Zakładając, że w kolejce jest zgłoszenie TB-13 „próg” z budynku C-13 ze statusem „wysłane”
      Jeżeli pierwszy Weryfikator weźmie to zgłoszenie
      Wtedy zgłoszenie ma status „w weryfikacji”
      Oraz drugi Weryfikator nie widzi go w swojej kolejce

    @US10.4
    Szablon scenariusza: Drugi Weryfikator nie obsłuży zgłoszenia wziętego przez pierwszego
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie TB-13 „próg” z budynku C-13
      Jeżeli drugi Weryfikator spróbuje <czynność> to zgłoszenie
      Wtedy próba jest odrzucona
      Oraz zgłoszenie nadal obsługuje pierwszy Weryfikator

      Przykłady:
        | czynność |
        | wziąć    |
        | przyjąć  |
        | odrzucić |

  @BR-4
  Reguła: Nikt nie weryfikuje własnego zgłoszenia

    @US10.5 @BR-1
    Scenariusz: Weryfikator nie widzi w kolejce własnych zgłoszeń
      Zakładając, że pierwszy Weryfikator ma też rolę Zbierającego
      Oraz jako Zbierający wysłał zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
      Oraz wysłał zmianę „bariera usunięta” zgłoszenia TB-9 „niesprawna winda” w budynku D-2
      Jeżeli pierwszy Weryfikator otworzy kolejkę weryfikacji
      Wtedy nie widzi w niej żadnego z tych zgłoszeń
      Oraz drugi Weryfikator widzi w swojej kolejce oba zgłoszenia

  @BR-5
  Reguła: Weryfikator zna autora tylko z roli i pseudonimu

    Scenariusz: Weryfikator widzi rolę i pseudonim autora, a nie jego dane osobowe
      Zakładając, że zgłoszenie wysłał Zbierający Jan Kowalski z adresem e-mail jan.kowalski@example.org
      Jeżeli pierwszy Weryfikator weźmie to zgłoszenie
      Wtedy widzi rolę autora „Zbierający” i pseudonim autora
      Ale nie widzi imienia, nazwiska ani adresu e-mail autora

  @BR-13
  Reguła: O duplikacie decyduje Weryfikator na podstawie podobnych zgłoszeń w promieniu 10 m

    @US10.1
    Szablon scenariusza: Podobne zgłoszenia w promieniu 10 m
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
      Oraz <odległość> od niego jest zgłoszenie TB-13 „próg” ze statusem „<status>”
      Jeżeli pierwszy Weryfikator przejrzy podobne zgłoszenia
      Wtedy zgłoszenie TB-13 <widoczność> na liście podobnych zgłoszeń

      Przykłady:
        | odległość | status        | widoczność        |
        | 5 m       | przyjęte      | jest widoczne     |
        | 10 m      | przyjęte      | jest widoczne     |
        | 9 m       | wysłane       | jest widoczne     |
        | 9 m       | w weryfikacji | jest widoczne     |
        | 11 m      | przyjęte      | nie jest widoczne |
        | 5 m       | odrzucone     | nie jest widoczne |
        | 5 m       | archiwalne    | nie jest widoczne |

    @US10.1
    Scenariusz: Podobne zgłoszenie pokazuje typ, zdjęcie, odległość i piętro
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
      Oraz 5 m od niego jest przyjęte zgłoszenie TB-13 „próg” z innego piętra
      Jeżeli pierwszy Weryfikator przejrzy podobne zgłoszenia
      Wtedy przy zgłoszeniu TB-13 widzi typ bariery, zdjęcie, odległość 5 m i piętro

    Scenariusz: System sam nie odrzuca ani nie łączy podobnych zgłoszeń
      Zakładając, że 2 m od siebie przy wejściu głównym A-1 wysłano dwa zgłoszenia TB-1 „schody przy wejściu”
      Jeżeli pierwszy Weryfikator otworzy kolejkę weryfikacji
      Wtedy widzi w niej oba zgłoszenia osobno
      Ale żadne z nich nie jest odrzucone ani połączone z drugim

    Scenariusz: Odrzucenie duplikatu wskazuje wcześniejsze zgłoszenie
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie TB-1, a 2 m od niego jest przyjęte zgłoszenie TB-1 tej samej bariery
      Jeżeli odrzuci sprawdzane zgłoszenie jako duplikat
      Wtedy powód odrzucenia wskazuje wcześniejsze zgłoszenie przyjęte
      Oraz sprawdzane zgłoszenie ma status „odrzucone”

  @BR-16
  Reguła: Odrzucenie bez powodu odrzucenia jest niemożliwe

    @US10.2
    Szablon scenariusza: Bez powodu odrzucenia zgłoszenie nie zostaje odrzucone
      Zakładając, że pierwszy Weryfikator wziął <zgłoszenie>
      Jeżeli spróbuje je odrzucić z pustym powodem odrzucenia
      Wtedy zgłoszenie nie zostaje odrzucone
      Oraz zgłoszenie ma nadal status „w weryfikacji”

      Przykłady:
        | zgłoszenie                                          |
        | zgłoszenie TB-13 „próg” ze zdjęciem bez progu       |
        | duplikat zgłoszenia TB-1 „schody przy wejściu”      |

  @BR-14
  Reguła: Typ bariery i uciążliwość zgłoszenia szybkiego ustala Weryfikator

    @US10.3
    Szablon scenariusza: Zgłoszenia szybkiego nie da się przyjąć bez typu i uciążliwości
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie szybkie z grupą barier „komunikacja pionowa” z budynku D-2
      Oraz ustalił tylko <ustalone>
      Jeżeli spróbuje przyjąć zgłoszenie
      Wtedy zgłoszenie nie zostaje przyjęte
      Oraz zgłoszenie ma nadal status „w weryfikacji”

      Przykłady:
        | ustalone                        |
        | typ TB-9 „niesprawna winda”     |
        | uciążliwość 3                   |

    @US10.3
    Scenariusz: Weryfikator ustala typ z innej grupy barier niż wybrana przez Odwiedzającego
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie szybkie z grupą barier „wejście i drzwi” z budynku C-13
      Jeżeli ustali typ TB-13 „próg” z grupy „komunikacja pozioma” i uciążliwość 2
      Oraz przyjmie zgłoszenie
      Wtedy na mapie jest bariera TB-13 „próg” z uciążliwością 2

  @BR-15
  Reguła: Weryfikator nie poprawia zgłoszenia z pomiaru precyzyjnego

    Szablon scenariusza: Weryfikator nie zmienia danych zgłoszenia z pomiaru precyzyjnego
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie z pomiaru precyzyjnego TB-13 „próg” z uciążliwością 2 z budynku C-13
      Jeżeli spróbuje zmienić w zgłoszeniu <dane>
      Wtedy próba jest odrzucona
      Oraz zgłoszenie ma nadal typ TB-13 „próg”, uciążliwość 2 i miejsce podane przez Zbierającego

      Przykłady:
        | dane                                      |
        | typ bariery na TB-1 „schody przy wejściu” |
        | uciążliwość na 3                          |
        | miejsce                                   |

    Scenariusz: Błąd w zgłoszeniu z pomiaru precyzyjnego oznacza odrzucenie z powodem
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie z pomiaru precyzyjnego TB-13 „próg” z budynku C-13
      Jeżeli odrzuci je z powodem „Na zdjęciu są schody, a nie próg”
      Wtedy autor widzi zgłoszenie ze statusem „odrzucone” i powodem odrzucenia „Na zdjęciu są schody, a nie próg”

    Scenariusz: Zasłonięcie twarzy na zdjęciu nie tworzy nowej wersji
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie z pomiaru precyzyjnego, na którego zdjęciu widać twarz przechodnia
      Jeżeli zasłoni twarz na zdjęciu
      Oraz przyjmie zgłoszenie
      Wtedy zgłoszenie przyjęte pokazuje zdjęcie z zasłoniętą twarzą
      Ale autor nie dostaje konfliktu wersji przy następnej synchronizacji

  @BR-3
  Reguła: Weryfikator decyduje o aktualnej wersji zgłoszenia

    @US8.3
    Scenariusz: Edycja autora w trakcie weryfikacji daje Weryfikatorowi nową wersję
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie TB-13 „próg” z uciążliwością 1
      Jeżeli autor zmieni uciążliwość na 2
      Oraz pierwszy Weryfikator przyjmie zgłoszenie
      Wtedy zgłoszenie przyjęte ma uciążliwość 2

  @BR-17
  Reguła: Zgłoszenie przyjęte jest widoczne od chwili decyzji

    @US1.3 @US2.2 @BR-6
    Scenariusz: Przyjęte zgłoszenie od razu pojawia się na mapie i karcie budynku
      Zakładając, że pierwszy Weryfikator wziął zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1
      Jeżeli przyjmie je 12.11.2026
      Wtedy bariera jest na mapie i na karcie budynku A-1 bez osobnego kroku publikacji
      Oraz przy zgłoszeniu widać zdjęcie i datę weryfikacji 12.11.2026

  @BR-18
  Reguła: Zmiana zgłoszenia przyjętego zastępuje je dopiero po przyjęciu

    Scenariusz: Po przyjęciu zmiany poprzednie zgłoszenie staje się archiwalne
      Zakładając, że na mapie jest przyjęte zgłoszenie TB-13 „próg” z uciążliwością 2 w budynku C-13
      Oraz pierwszy Weryfikator wziął zmianę tego zgłoszenia z uciążliwością 3
      Jeżeli przyjmie zmianę
      Wtedy na mapie jest tylko bariera TB-13 z uciążliwością 3
      Oraz poprzednie zgłoszenie ma status „archiwalne”

    Scenariusz: Po odrzuceniu zmiany na mapie zostaje poprzednie zgłoszenie
      Zakładając, że na mapie jest przyjęte zgłoszenie TB-13 „próg” z uciążliwością 2 w budynku C-13
      Oraz pierwszy Weryfikator wziął zmianę tego zgłoszenia z uciążliwością 3
      Jeżeli odrzuci zmianę z powodem „Zdjęcie nie pokazuje progu”
      Wtedy na mapie jest nadal bariera TB-13 z uciążliwością 2
      Oraz to zgłoszenie ma nadal status „przyjęte”

  @BR-19
  Reguła: Usunięcie bariery przechodzi weryfikację jak każda zmiana

    @US10.6 @US8.4
    Scenariusz: Po przyjęciu „bariera usunięta” bariera znika z mapy i karty budynku
      Zakładając, że na mapie jest przyjęte zgłoszenie TB-9 „niesprawna winda” w budynku D-2
      Oraz pierwszy Weryfikator wziął zgłoszenie „bariera usunięta” tej bariery
      Jeżeli przyjmie zgłoszenie „bariera usunięta”
      Wtedy niesprawnej windy nie ma na mapie ani na karcie budynku D-2
      Oraz oba zgłoszenia zostają w systemie ze statusem „archiwalne”
      Oraz zgłoszenie „bariera usunięta” ma to oznaczenie

    @US8.4
    Scenariusz: Po odrzuceniu „bariera usunięta” bariera zostaje na mapie
      Zakładając, że na mapie jest przyjęte zgłoszenie TB-9 „niesprawna winda” w budynku D-2
      Oraz pierwszy Weryfikator wziął zgłoszenie „bariera usunięta” tej bariery
      Jeżeli odrzuci je z powodem „Na zdjęciu winda nadal jest wyłączona”
      Wtedy niesprawna winda jest nadal na mapie i na karcie budynku D-2

  @BR-23
  Reguła: Z dwóch równoległych zmian tego samego zgłoszenia przyjąć można tylko jedną

    @US10.1
    Scenariusz: Druga równoległa zmiana jest widoczna jako podobne zgłoszenie
      Zakładając, że dwóch Zbierających wysłało niezależnie zmiany przyjętego zgłoszenia TB-9 „niesprawna winda” w budynku D-2
      Jeżeli pierwszy Weryfikator weźmie pierwszą zmianę
      Wtedy na liście podobnych zgłoszeń widzi drugą zmianę

    Szablon scenariusza: Zmiany zgłoszenia archiwalnego nie da się przyjąć
      Zakładając, że dwóch Zbierających wysłało niezależnie zmiany przyjętego zgłoszenia TB-9 „niesprawna winda” w budynku D-2
      Oraz drugi Weryfikator przyjął pierwszą zmianę <pierwsza zmiana>
      Oraz pierwszy Weryfikator wziął drugą zmianę
      Jeżeli spróbuje przyjąć drugą zmianę
      Wtedy druga zmiana nie zostaje przyjęta
      Oraz <stan mapy>

      Przykłady:
        | pierwsza zmiana                     | stan mapy                                            |
        | z uciążliwością 2                   | na mapie jest tylko bariera TB-9 z uciążliwością 2   |
        | z oznaczeniem „bariera usunięta”    | niesprawnej windy nie ma na mapie                    |

    Szablon scenariusza: Zmianę zgłoszenia archiwalnego Weryfikator odrzuca z powodem „nieaktualne”
      Zakładając, że dwóch Zbierających wysłało niezależnie zmiany przyjętego zgłoszenia TB-9 „niesprawna winda” w budynku D-2
      Oraz drugi Weryfikator przyjął pierwszą zmianę <pierwsza zmiana>
      Oraz pierwszy Weryfikator wziął drugą zmianę
      Jeżeli odrzuci drugą zmianę z powodem „nieaktualne”
      Wtedy autor widzi zgłoszenie ze statusem „odrzucone” i powodem odrzucenia „nieaktualne”

      Przykłady:
        | pierwsza zmiana                     |
        | z uciążliwością 2                   |
        | z oznaczeniem „bariera usunięta”    |

    Scenariusz: Zmianę odrzuconą jako „nieaktualne” autor wysyła ponownie do bieżącego zgłoszenia
      Zakładając, że zmianę Zbierającego w zgłoszeniu TB-9 „niesprawna winda” w budynku D-2 odrzucono z powodem „nieaktualne”, bo wcześniej przyjęto zmianę z uciążliwością 2
      Jeżeli Zbierający wyśle swoją zmianę ponownie
      Wtedy zmiana jest powiązana z bieżącym zgłoszeniem TB-9 z uciążliwością 2
      Oraz zmiana ma status „wysłane”

    Scenariusz: Po przyjęciu „bariera usunięta” barierę, która nadal istnieje, zgłasza się od nowa
      Zakładając, że zmianę Zbierającego w zgłoszeniu TB-9 „niesprawna winda” w budynku D-2 odrzucono z powodem „nieaktualne”, bo wcześniej przyjęto zmianę z oznaczeniem „bariera usunięta”
      Oraz winda nadal nie działa
      Jeżeli Zbierający złoży nowe zgłoszenie TB-9 „niesprawna winda” w budynku D-2
      Wtedy powstaje nowe zgłoszenie ze statusem „lokalne”
      Ale nowe zgłoszenie nie jest zmianą żadnego zgłoszenia archiwalnego
