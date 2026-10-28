# language: pl
Funkcja: Eksport zgłoszeń przyjętych do QGIS
  Koordynator przekazuje bieżący stan barier do QGIS (N9).
  Eksport nie zawiera danych autora ani historii.

  Założenia:
    Zakładając, że Koordynator jest zalogowany z rolą Koordynatora
    Oraz przy wejściu głównym A-1 jest przyjęte zgłoszenie TB-1 „schody przy wejściu” z uciążliwością 3

  @US13.1
  Scenariusz: Plik GeoJSON otwiera się w QGIS bez ustawiania układu współrzędnych
    Jeżeli Koordynator pobierze eksport GeoJSON
    Oraz otworzy plik w QGIS
    Wtedy QGIS nie pyta o układ współrzędnych
    Oraz plik ma współrzędne w WGS84
    Oraz bariera TB-1 leży przy wejściu głównym A-1 na podkładzie OpenStreetMap

  @US13.2
  Scenariusz: Plik CSV ma współrzędne w PUWG 1992 i otwiera się jako warstwa punktów
    Jeżeli Koordynator pobierze eksport CSV
    Oraz otworzy plik w QGIS ze wskazanym układem EPSG:2180
    Wtedy QGIS pokazuje warstwę punktów
    Oraz bariera TB-1 leży przy wejściu głównym A-1
    Oraz typ „schody przy wejściu” ma polskie znaki bez zniekształceń

  @US13.3 @BR-22 @BR-6
  Szablon scenariusza: Eksport zawiera tylko bieżące zgłoszenia przyjęte
    Zakładając, że w budynku C-13 jest zgłoszenie TB-13 „próg” ze statusem „<status>”
    Jeżeli Koordynator pobierze eksport <format>
    Wtedy zgłoszenie TB-13 <obecność> w pliku

    Przykłady:
      | status        | format  | obecność        |
      | przyjęte      | GeoJSON | jest            |
      | przyjęte      | CSV     | jest            |
      | wysłane       | GeoJSON | nie występuje   |
      | w weryfikacji | CSV     | nie występuje   |
      | odrzucone     | GeoJSON | nie występuje   |
      | archiwalne    | CSV     | nie występuje   |

  @US13.3 @BR-22 @BR-19
  Scenariusz: Bariera usunięta nie trafia do eksportu
    Zakładając, że przyjęto zgłoszenie „bariera usunięta” dla niesprawnej windy TB-9 w budynku D-2
    Jeżeli Koordynator pobierze eksport GeoJSON
    Wtedy w pliku nie ma niesprawnej windy z budynku D-2
    Oraz w pliku jest bariera TB-1 przy wejściu głównym A-1

  @US13.3 @BR-6
  Szablon scenariusza: Eksport nie zawiera danych autora
    Jeżeli Koordynator pobierze eksport <format>
    Wtedy w pliku nie ma imienia, adresu e-mail, identyfikatora konta ani pseudonimu autora
    Oraz zgłoszenie jest powiązane z budynkiem tylko symbolem budynku „A-1”, bez konturu budynku

    Przykłady:
      | format  |
      | GeoJSON |
      | CSV     |
