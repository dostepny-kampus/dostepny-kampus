# language: pl
Funkcja: Powrót w zgłoszeniu szybkim
  Odwiedzający może cofnąć się w każdym kroku i nie traci pracy, a po wysłaniu nie wraca
  do kroków wysłanego zgłoszenia (N3, N4).

  @US4.7
  Scenariusz: Po wysłaniu „Wstecz” wraca do ekranu, z którego zaczęto zgłoszenie
    Zakładając, że Odwiedzający zaczął zgłoszenie szybkie z karty budynku A-1
    Oraz wysłał je i widzi potwierdzenie
    Jeżeli wybierze „Wstecz”
    Wtedy widzi kartę budynku A-1
    Ale kolejne „Wstecz” nie otwiera kroków wysłanego zgłoszenia
    Oraz na serwer trafia jedno zdjęcie tego zgłoszenia

  @US4.8
  Scenariusz: Powrót między krokami nie usuwa zdjęcia ani grupy barier
    Zakładając, że Odwiedzający zrobił zdjęcie i wybrał grupę barier „toalety”
    Jeżeli wróci do kroku 1, a potem znów przejdzie dalej
    Wtedy w kroku 1 widzi swoje zdjęcie
    Oraz w kroku 2 grupa barier „toalety” jest nadal wybrana

  @US4.8
  Scenariusz: Zdjęcie znika dopiero po potwierdzeniu rezygnacji
    Zakładając, że Odwiedzający zrobił zdjęcie w zgłoszeniu szybkim
    Jeżeli wybierze „Wstecz” w kroku 1
    Wtedy widzi pytanie o rezygnację z dwoma przyciskami tej samej wielkości
    Oraz zdjęcie zostaje, dopóki nie potwierdzi rezygnacji
