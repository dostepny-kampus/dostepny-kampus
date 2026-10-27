# language: pl
@demo
Funkcja: Scenariusz demo w budynku A-1
  Jedna bariera przechodzi całą drogę: od pomiaru precyzyjnego przy wejściu głównym A-1,
  przez weryfikację, do karty budynku, postępu inwentaryzacji i eksportu do QGIS.
  Według stanu PWr na 01.01.2025 w A-1 dostosowane jest tylko inne wejście niż wejście główne.

  Założenia:
    Zakładając, że Zbierająca, Weryfikator, Odwiedzający i Koordynator są zalogowani ze swoimi rolami
    Oraz Zbierający oznaczył wcześniej 4 z 5 pięter budynku A-1 jako obejrzane

  @US6.1 @US6.2 @US7.1 @US9.2 @US10.1 @US2.1 @US11.1 @US13.1
  Scenariusz: Schody przy wejściu głównym A-1 od pomiaru do eksportu
    Zakładając, że Zbierająca przy wejściu głównym A-1 po 30 s uśredniania pozycji potwierdza podpowiedziany budynek A-1
    Jeżeli zapisze zgłoszenie TB-1 „schody przy wejściu” z uciążliwością 3 i zdjęciem, gdy telefon w piwnicy A-1 nie ma sieci
    Oraz zgłoszenie wyśle się samo po powrocie sieci
    Oraz Weryfikator przyjmie je, bo podobne zgłoszenie 5 m dalej opisuje inną barierę
    Wtedy Odwiedzający widzi na karcie budynku A-1 wejście główne „niedostępne” ze schodami i inne wejście „dostępne”
    Oraz Koordynator widzi przy budynku A-1 4 z 5 pięter obejrzanych, czyli 80 procent
    Oraz eksport GeoJSON z barierą TB-1 otwiera się w QGIS bez ustawiania układu współrzędnych
