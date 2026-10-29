# Ekrany — makiety hi-fi

Na tej stronie są makiety hi-fi ekranów aplikacji zatwierdzone przez właściciela produktu. Według nich powstaje kod aplikacji. Wszystkie ekrany mają styl „Karta miejsca” (`k5p-b`) opisany w [tokenach wizualnych](../tokeny.md). Skąd się wziął każdy element, pokazują [wymagania do ekranów](../wymagania-ekranow.md) i [makiety low-fi](../makiety/index.md).

Każdy stan ma trzy warianty: jasny, ciemny i przy czcionce 200% (nieliniowa skala Androida 14). Wariant 200% to cała przewijana strona. Na telefonie przypięty jest tylko pasek dolny, a górny pasek z przyciskiem „Wstecz” może się chować przy przewijaniu w dół.

Na zrzutach mapa ulic i pozostałe budynki pochodzą z OpenStreetMap. Kontury budynków Politechniki, ścieżki dostosowane, pochylnie, symbole i nazwy budynków, wejścia oraz plany pięter pochodzą z danych Politechniki Wrocławskiej. Przytaczamy je do celów dydaktycznych ze wskazaniem źródła „Źródło: Politechnika Wrocławska”.

W aplikacji kontury budynków pochodzą z OpenStreetMap. Przykładowe są bariery, daty, osoba na ekranie konta i wejścia budynku C-7.

Obrazy jasne i ciemne mają 780 px szerokości, czyli ekran 390 dp w skali 2. Obrazy przy czcionce 200% mają 390 px szerokości (skala 1). Kliknięcie obrazu otwiera go w pełnym rozmiarze.

## V1 Mapa kampusu

Wymagania: [V1 Mapa kampusu](../wymagania-ekranow.md#v1-mapa-kampusu).

### V1: mapa ze znakami barier

`m-v1-mapa` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-stan-zwyky)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Mapa kampusu: pole „Wpisz symbol budynku, np. C-13”, przełącznik Mapa i Lista budynków, mapa z budynkami Politechniki i znakami barier, przycisk Legenda, strzałki przesuwania mapy w krzyżu, plus i minus, podpis OpenStreetMap, przycisk informacji i pasek dolny](obrazy/m-v1-mapa-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-jasny.webp) | [![Ta sama mapa w motywie ciemnym: grafitowa mapa nocna, jasne kontury budynków i białe znaki barier](obrazy/m-v1-mapa-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-ciemny.webp) | [![Ta sama mapa przy czcionce 200%: pole symbolu w dwóch wierszach, przełącznik Mapa i Lista, mapa jak w wariancie jasnym z większymi napisami budynków, podpis „© OpenStreetMap”](obrazy/m-v1-mapa-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-jasny-200.webp) |

### V1: lista budynków

`m-v1-mapa-lista` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-lista-budynkow)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Lista budynków wybrana przełącznikiem: każdy budynek to jedna karta z symbolem, nazwą i barierami, na przykład A-1 Gmach Główny z barierami Blokada, Tylko z pomocą i Utrudnienie, albo napis Brak zgłoszonych barier](obrazy/m-v1-mapa-lista-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-lista-jasny.webp) | [![Ta sama lista budynków w motywie ciemnym](obrazy/m-v1-mapa-lista-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-lista-ciemny.webp) | [![Lista budynków przy czcionce 200%, od której zaczyna się V1 przy dużej czcionce: karty na całą szerokość, bariery w kilku wierszach pod nazwą budynku](obrazy/m-v1-mapa-lista-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-lista-jasny-200.webp) |

### V1: wyszukiwanie `c13`

`m-v1-mapa-wyszukiwanie` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-wyszukiwanie)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Wyszukiwanie: w polu wpisano c13, pod polem karta wyniku C-13 Zintegrowane Centrum Studenckie, Budynek C-13, a niżej mapa](obrazy/m-v1-mapa-wyszukiwanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-wyszukiwanie-jasny.webp) | [![To samo wyszukiwanie w motywie ciemnym](obrazy/m-v1-mapa-wyszukiwanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-wyszukiwanie-ciemny.webp) | [![To samo wyszukiwanie przy czcionce 200%: nazwa budynku w wyniku zajmuje trzy wiersze, mapa jest niżej](obrazy/m-v1-mapa-wyszukiwanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-wyszukiwanie-jasny-200.webp) |

### V1: brak wyniku dla „C-31”

`m-v1-mapa-pusty` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-brak-wyniku)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Brak wyniku: komunikat „Nie znaleźliśmy budynku »C-31«.”, podpowiedź, by sprawdzić symbol albo wybrać budynek z listy, i przycisk Lista budynków nad mapą](obrazy/m-v1-mapa-pusty-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pusty-jasny.webp) | [![Ten sam komunikat o braku wyniku w motywie ciemnym](obrazy/m-v1-mapa-pusty-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pusty-ciemny.webp) | [![Ten sam komunikat przy czcionce 200%: tekst w kilku wierszach, przycisk Lista budynków pod nim, mapa niżej](obrazy/m-v1-mapa-pusty-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pusty-jasny-200.webp) |

### V1: opis budynku A-1 na pół ekranu {#v1-arkusz-budynku}

`m-v1-mapa-budynek`

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po dotknięciu budynku: u góry mapa z konturem A-1 i jego wejściami oraz przycisk Wstecz, niżej arkusz A-1 Gmach Główny z chipami Wejście dostępne, Winda i Toaleta dostosowana, nagłówkiem Wejścia i przyciskami Sprawdź, którędy wejść oraz Pokaż na mapie](obrazy/m-v1-mapa-budynek-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-budynek-jasny.webp) | [![Ten sam arkusz budynku w motywie ciemnym nad nocną mapą](obrazy/m-v1-mapa-budynek-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-budynek-ciemny.webp) | [![Opis budynku A-1 przy czcionce 200% od razu na cały ekran, bez mapy: chipy, przyciski Sprawdź, którędy wejść i Pokaż na mapie pod nazwą, potem wejścia, bariery, winda i toaleta oraz piętra](obrazy/m-v1-mapa-budynek-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-budynek-jasny-200.webp) |

### V1: arkusz bariery po dotknięciu znaku

`m-v1-mapa-bariera` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-bariera-na-mapie)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po dotknięciu znaku bariery: na mapie wybrany znak Blokada, pod mapą arkusz z napisem Blokada, przyciskiem Zamknij, tytułem Schody przy wejściu, miejscem A-1 Gmach Główny, Wejście Cesarskie, parter, datą Sprawdzone 14.10.2026, rysunkiem schodów i przyciskiem Pokaż opis budynku](obrazy/m-v1-mapa-bariera-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-bariera-jasny.webp) | [![Ten sam arkusz bariery w motywie ciemnym: rysunek schodów w ciemnych tonach](obrazy/m-v1-mapa-bariera-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-bariera-ciemny.webp) | [![Ten sam arkusz bariery przy czcionce 200%: mapa zajmuje mniej miejsca, tekst bariery w kilku wierszach](obrazy/m-v1-mapa-bariera-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-bariera-jasny-200.webp) |

### V1: znaleziona sala A-1 329

`m-v1-mapa-sala` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-znaleziona-sala)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Znaleziona sala: w polu A-1 329, nad mapą „Piętro:” z wyborem Piwnica, Parter, 1, 2, 3 i zaznaczonym 2, plan piętra z oznaczoną salą 329 i piktogramami z podpisami Toaleta i Winda, a w arkuszu cztery kroki: Wejście dostosowane, Winda przy klatkach C i D, 2 piętro, Sala 329 przy klatkach A i B, ok. 85 m od windy, i przycisk Pokaż opis budynku](obrazy/m-v1-mapa-sala-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-sala-jasny.webp) | [![Ta sama sala w motywie ciemnym z ciemnym planem piętra](obrazy/m-v1-mapa-sala-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-sala-ciemny.webp) | [![Kroki do sali przy czcionce 200% na cały ekran: u góry tylko Wstecz, potem Sala 329, cztery kroki oraz przyciski Pokaż opis budynku i Pokaż na mapie](obrazy/m-v1-mapa-sala-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-sala-jasny-200.webp) |

### V1: legenda

`m-v1-mapa-legenda`

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Otwarta legenda nad mapą z przyciskiem Zamknij: w części Na mapie Budynek Politechniki, Wejście dostępne, Wejście niedostępne, Wejście bez danych, Zalecane wejście, Ścieżka dostosowana i Pochylnia, niżej początek części Bariery, na dole Przewiń w dół](obrazy/m-v1-mapa-legenda-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-legenda-jasny.webp) | [![Ta sama legenda w motywie ciemnym](obrazy/m-v1-mapa-legenda-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-legenda-ciemny.webp) | [![Ta sama legenda przy czcionce 200%: nagłówek z przyciskiem Zamknij, pięć pierwszych pozycji części Na mapie, na dole Przewiń w dół](obrazy/m-v1-mapa-legenda-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-legenda-jasny-200.webp) |

### V1: źródła danych

`m-v1-mapa-info`

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Panel Źródła danych z przyciskiem Zamknij po dotknięciu przycisku informacji: mapa ulic i kontury budynków © OpenStreetMap contributors na otwartej licencji ODbL, wejścia i plany pięter ze źródła Politechnika Wrocławska, stan z 18.12.2023](obrazy/m-v1-mapa-info-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-info-jasny.webp) | [![Ten sam panel źródeł danych w motywie ciemnym](obrazy/m-v1-mapa-info-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-info-ciemny.webp) | [![Ten sam panel źródeł danych przy czcionce 200%: tekst w wielu wierszach nad mapą](obrazy/m-v1-mapa-info-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-info-jasny-200.webp) |

### V1: bez internetu

`m-v1-mapa-brak-sieci` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-brak-sieci)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Mapa bez internetu: pod przełącznikiem pasek „Bez internetu · dane z 27.10.2026”, mapa bez mapy ulic, tylko kontury budynków, ścieżki i znaki barier zapisane w telefonie](obrazy/m-v1-mapa-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-brak-sieci-jasny.webp) | [![Ta sama mapa bez internetu w motywie ciemnym](obrazy/m-v1-mapa-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-brak-sieci-ciemny.webp) | [![Ta sama mapa bez internetu przy czcionce 200%: pasek Bez internetu w dwóch wierszach](obrazy/m-v1-mapa-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-brak-sieci-jasny-200.webp) |

### V1: pierwsze otwarcie bez internetu

`m-v1-mapa-pierwsze-otwarcie` · [makieta low-fi](../makiety/odwiedzajacy.md#v1-pierwsze-otwarcie-bez-internetu)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Pierwsze otwarcie bez internetu: pasek Bez internetu pod tytułem i komunikat „Pierwsze otwarcie aplikacji wymaga internetu.” z dopiskiem, że mapa wczyta się sama po połączeniu](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny.webp) | [![Ten sam komunikat w motywie ciemnym](obrazy/m-v1-mapa-pierwsze-otwarcie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pierwsze-otwarcie-ciemny.webp) | [![Ten sam komunikat przy czcionce 200%](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny-200.webp) |

## V2 Karta budynku

Wymagania: [V2 Karta budynku](../wymagania-ekranow.md#v2-karta-budynku).

### V2: pełny opis budynku A-1

`m-v2-karta-budynku` · [makieta low-fi](../makiety/index.md#wzorcowa-makieta-v2-karta-budynku)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Cały opis budynku A-1 Gmach Główny: fragment mapy, chipy Wejście dostępne, Winda i Toaleta dostosowana, cztery wejścia z wejściem zalecanym na górze, bariery z rysunkami, grupa Winda i toaleta, piętra od piwnicy z rozwiniętym parterem i przypięte przyciski Sprawdź, którędy wejść oraz Pokaż na mapie](obrazy/m-v2-karta-budynku-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-jasny.webp) | [![Ten sam opis budynku A-1 w motywie ciemnym](obrazy/m-v2-karta-budynku-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-ciemny.webp) | [![Ten sam opis budynku A-1 przy czcionce 200%: bez fragmentu mapy, przyciski Sprawdź, którędy wejść i Pokaż na mapie pod nazwą, potem wejścia, bariery, winda i toaleta oraz piętra](obrazy/m-v2-karta-budynku-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-jasny-200.webp) |

### V2: opis na pół ekranu nad mapą

`m-v2-karta-budynku-arkusz-polowa`. Ten sam arkusz co [V1: opis budynku A-1 na pół ekranu](#v1-arkusz-budynku): mapa otwiera opis budynku w tym samym arkuszu.

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku A-1 otwarty z mapy jako arkusz do połowy ekranu: nad nim mapa z konturem A-1, w arkuszu nazwa, chipy Wejście dostępne, Winda i Toaleta dostosowana, początek wejść i przypięte przyciski](obrazy/m-v2-karta-budynku-arkusz-polowa-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-arkusz-polowa-jasny.webp) | [![Ten sam arkusz w motywie ciemnym](obrazy/m-v2-karta-budynku-arkusz-polowa-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-arkusz-polowa-ciemny.webp) | [![Ten sam opis przy czcionce 200% od razu na cały ekran, bez mapy](obrazy/m-v2-karta-budynku-arkusz-polowa-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-arkusz-polowa-jasny-200.webp) |

### V2: budynek bez planu pięter (H-4)

`m-v2-karta-budynku-podstawowy` · [makieta low-fi](../makiety/odwiedzajacy.md#v2-budynek-podstawowy)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku H-4 Studium Języków Obcych: jedno wejście Dostępne z napisem Zalecane i trzy wejścia Niedostępne, bariera Próg z oznaczeniem Tylko z pomocą i napis „Nie mamy planu pięter tego budynku.”](obrazy/m-v2-karta-budynku-podstawowy-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-podstawowy-jasny.webp) | [![Ten sam opis budynku H-4 w motywie ciemnym](obrazy/m-v2-karta-budynku-podstawowy-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-podstawowy-ciemny.webp) | [![Ten sam opis budynku H-4 przy czcionce 200%](obrazy/m-v2-karta-budynku-podstawowy-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-podstawowy-jasny-200.webp) |

### V2: bez zalecanego wejścia (C-4)

`m-v2-karta-budynku-bez-zalecanego`

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku C-4: jedyne wejście z oznaczeniem Niedostępne na górze, z opisem przeszkód z danych i odnośnikiem Zgłoś barierę przy wejściu, bez napisu Zalecane, niżej Bariery w budynku](obrazy/m-v2-karta-budynku-bez-zalecanego-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-bez-zalecanego-jasny.webp) | [![Ten sam opis budynku C-4 w motywie ciemnym](obrazy/m-v2-karta-budynku-bez-zalecanego-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-bez-zalecanego-ciemny.webp) | [![Ten sam opis budynku C-4 przy czcionce 200%: przyciski Sprawdź, którędy wejść i Pokaż na mapie pod nazwą, potem wejście Niedostępne](obrazy/m-v2-karta-budynku-bez-zalecanego-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-bez-zalecanego-jasny-200.webp) |

### V2: wejście bez danych (C-7)

`m-v2-karta-budynku-wejscie-brak-danych`

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku C-7: fragment mapy z trzema przykładowymi znakami wejść, w grupie Wejścia data Z map i opisów, 18.12.2023, Wejście 1 Niedostępne z opisem schodów i drzwiami 90 cm, Wejście 2 i 3 z oznaczeniem Brak danych i rombem z minusem, przy każdym Zgłoś barierę przy wejściu](obrazy/m-v2-karta-budynku-wejscie-brak-danych-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-wejscie-brak-danych-jasny.webp) | [![Ten sam opis budynku C-7 w motywie ciemnym](obrazy/m-v2-karta-budynku-wejscie-brak-danych-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-wejscie-brak-danych-ciemny.webp) | [![Ten sam opis budynku C-7 przy czcionce 200%: bez fragmentu mapy, przyciski Sprawdź, którędy wejść i Pokaż na mapie pod nazwą, potem Wejście 1 Niedostępne oraz Wejście 2 i 3 Brak danych](obrazy/m-v2-karta-budynku-wejscie-brak-danych-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-wejscie-brak-danych-jasny-200.webp) |

### V2: brak zgłoszonych barier

`m-v2-karta-budynku-pusty` · [makieta low-fi](../makiety/odwiedzajacy.md#v2-brak-sprawdzonych-barier)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku A-1 bez barier: w grupie Bariery w budynku napis Brak zgłoszonych barier, wyjaśnienie, kiedy bariera się tu pojawi, i przycisk Zgłoś barierę](obrazy/m-v2-karta-budynku-pusty-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-pusty-jasny.webp) | [![Ten sam opis bez barier w motywie ciemnym](obrazy/m-v2-karta-budynku-pusty-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-pusty-ciemny.webp) | [![Ten sam opis bez barier przy czcionce 200%](obrazy/m-v2-karta-budynku-pusty-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-pusty-jasny-200.webp) |

### V2: bez internetu

`m-v2-karta-budynku-brak-sieci` · [makieta low-fi](../makiety/odwiedzajacy.md#v2-brak-sieci)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku A-1 z telefonu: na górze „Bez internetu · dane z 27.10.2026”, fragment mapy bez mapy ulic, a przy jednej barierze zamiast rysunku napis „Zdjęcie zobaczysz po połączeniu z internetem.”](obrazy/m-v2-karta-budynku-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-brak-sieci-jasny.webp) | [![Ten sam opis bez internetu w motywie ciemnym](obrazy/m-v2-karta-budynku-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-brak-sieci-ciemny.webp) | [![Ten sam opis bez internetu przy czcionce 200%: pasek Bez internetu w dwóch wierszach nad nazwą budynku](obrazy/m-v2-karta-budynku-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-brak-sieci-jasny-200.webp) |

### V2: budynku nie ma w telefonie (C-13)

`m-v2-karta-budynku-brak-w-telefonie` · [makieta low-fi](../makiety/odwiedzajacy.md#v2-budynku-nie-ma-w-telefonie)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Budynek C-13 bez internetu: Wstecz, pasek Bez internetu i komunikat „Tego budynku nie ma jeszcze w telefonie.” z prośbą o połączenie oraz przyciskiem Spróbuj ponownie](obrazy/m-v2-karta-budynku-brak-w-telefonie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-brak-w-telefonie-jasny.webp) | [![Ten sam komunikat w motywie ciemnym](obrazy/m-v2-karta-budynku-brak-w-telefonie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-brak-w-telefonie-ciemny.webp) | [![Ten sam komunikat przy czcionce 200%: tytuł Budynek C-13 w treści pod paskiem Bez internetu](obrazy/m-v2-karta-budynku-brak-w-telefonie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-brak-w-telefonie-jasny-200.webp) |

### V2: widok Zbierającego

`m-v2-karta-budynku-zbierajacy` · [makieta low-fi](../makiety/odwiedzajacy.md#v2-zbierajacy)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Opis budynku A-1 dla Zbierającego: przy każdym wejściu własna data i źródło, przycisk Zmień oznaczenie i odnośniki, pod piętrami Oznacz obejrzane piętra, przy barierze Zgłoś, że bariery nie ma, w pasku dolnym Konto i praca](obrazy/m-v2-karta-budynku-zbierajacy-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-zbierajacy-jasny.webp) | [![Ten sam widok Zbierającego w motywie ciemnym](obrazy/m-v2-karta-budynku-zbierajacy-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-zbierajacy-ciemny.webp) | [![Ten sam widok Zbierającego przy czcionce 200%: przy wejściu Zalecane źródło Sprawdzone na miejscu 23.10.2026](obrazy/m-v2-karta-budynku-zbierajacy-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v2-karta-budynku-zbierajacy-jasny-200.webp) |

## V3 „Którędy wejść”

Wymagania: [V3 „Którędy wejść”](../wymagania-ekranow.md#v3-zaplanuj-wizyte).

### V3: zalecane wejście do A-1

`m-v3-ktoredy-wejsc` · [makieta low-fi](../makiety/odwiedzajacy.md#v3-wejscie-jest)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Którędy wejść do A-1: odpowiedź „Budynek A-1 ma wejście dostępne: »Wejście dostosowane«.”, data i źródło, Zalecane wejście z fragmentem mapy, opisami Dojście, Drzwi i Za drzwiami, odnośnikami Pokaż wejście na mapie i Zgłoś barierę przy wejściu, niżej Inne wejście dostępne](obrazy/m-v3-ktoredy-wejsc-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-jasny.webp) | [![To samo zalecane wejście w motywie ciemnym](obrazy/m-v3-ktoredy-wejsc-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-ciemny.webp) | [![To samo zalecane wejście przy czcionce 200%: bez fragmentu mapy, tytuł Którędy wejść do A-1 w treści, odpowiedź pod znakiem](obrazy/m-v3-ktoredy-wejsc-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-jasny-200.webp) |

### V3: brak wejścia dostępnego (C-4)

`m-v3-ktoredy-wejsc-brak-dostepnego` · [makieta low-fi](../makiety/odwiedzajacy.md#v3-brak-wejscia-dostepnego)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Którędy wejść do C-4: ramka ze znakiem krzyżyka (niedostępne) i zdaniem „Budynek C-4 nie ma wejścia dostępnego.” oraz radą, by wrócić Wstecz do opisu budynku i zobaczyć bariery przy każdym wejściu](obrazy/m-v3-ktoredy-wejsc-brak-dostepnego-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-brak-dostepnego-jasny.webp) | [![Ta sama odpowiedź w motywie ciemnym](obrazy/m-v3-ktoredy-wejsc-brak-dostepnego-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-brak-dostepnego-ciemny.webp) | [![Ta sama odpowiedź przy czcionce 200%](obrazy/m-v3-ktoredy-wejsc-brak-dostepnego-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-brak-dostepnego-jasny-200.webp) |

### V3: brak danych o wejściach (C-7)

`m-v3-ktoredy-wejsc-brak-danych` · [makieta low-fi](../makiety/odwiedzajacy.md#v3-brak-danych-o-wejsciach)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Którędy wejść do C-7: ramka ze znakiem rombu z minusem (brak danych) i zdaniem „Nie wiemy jeszcze, czy budynek C-7 ma wejście dostępne: brak danych o 2 z 3 wejść.” oraz radą, by wrócić do opisu budynku](obrazy/m-v3-ktoredy-wejsc-brak-danych-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-brak-danych-jasny.webp) | [![Ta sama odpowiedź w motywie ciemnym](obrazy/m-v3-ktoredy-wejsc-brak-danych-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-brak-danych-ciemny.webp) | [![Ta sama odpowiedź przy czcionce 200%](obrazy/m-v3-ktoredy-wejsc-brak-danych-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v3-ktoredy-wejsc-brak-danych-jasny-200.webp) |
