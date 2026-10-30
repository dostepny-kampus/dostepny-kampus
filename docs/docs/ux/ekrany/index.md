# Ekrany — makiety hi-fi

Na tej stronie są makiety hi-fi ekranów aplikacji zatwierdzone przez właściciela produktu. Według nich powstaje kod aplikacji. Wszystkie ekrany mają styl „Karta miejsca” (`k5p-b`) opisany w [tokenach wizualnych](../tokeny.md). Skąd się wziął każdy element, pokazują [wymagania do ekranów](../wymagania-ekranow.md) i [makiety low-fi](../makiety/index.md).

Każdy stan ma trzy warianty: jasny, ciemny i przy czcionce 200% (nieliniowa skala Androida 14). Wariant 200% to cała przewijana strona. Na telefonie przypięty jest tylko pasek dolny, a górny pasek z przyciskiem „Wstecz” może się chować przy przewijaniu w dół.

Ekrany zespołu (C1–C7, R1, R2, K1–K3) nie mają paska dolnego. Na C1, C2, C6, C7, K2 i K3 przypięta jest czynność główna (reguła 10 z sekcji [Duża czcionka](../wymagania-ekranow.md#duza-czcionka)). Gdy czcionka ma 200%, nazwa ekranu stoi w treści, a na pasku zostaje „Wstecz”. Przy każdym stanie są odwołania do kryteriów, reguł i wymagań, z których wynikają jego elementy.

Na zrzutach mapa ulic i pozostałe budynki pochodzą z OpenStreetMap. Kontury budynków Politechniki, ścieżki dostosowane, pochylnie, symbole i nazwy budynków, wejścia oraz plany pięter pochodzą z danych Politechniki Wrocławskiej. Przytaczamy je do celów dydaktycznych ze wskazaniem źródła „Źródło: Politechnika Wrocławska”.

W aplikacji kontury budynków pochodzą z OpenStreetMap. Przykładowe są bariery, daty, osoba na ekranie konta i wejścia budynku C-7. Na ekranach zespołu przykładowe są też zgłoszenia, pseudonimy autorów, odległości i dokładności pomiaru, liczby w postępie inwentaryzacji i adres e-mail w zaproszeniu.

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
| [![Znaleziona sala: w polu A-1 329, nad mapą „Piętro:” z wyborem Piwnica, Parter, 1, 2, 3 i zaznaczonym 2, plan piętra z oznaczoną salą 329 i piktogramami z podpisami Toaleta i Winda, a w arkuszu cztery kroki: Wejście dostosowane, parter, Winda przy klatkach C i D, 2 piętro, Sala 329 przy klatkach A i B, ok. 85 m od windy, i przycisk Pokaż opis budynku](obrazy/m-v1-mapa-sala-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-sala-jasny.webp) | [![Ta sama sala w motywie ciemnym z ciemnym planem piętra](obrazy/m-v1-mapa-sala-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-sala-ciemny.webp) | [![Kroki do sali przy czcionce 200% na cały ekran: u góry tylko Wstecz, potem Sala 329, cztery kroki oraz przyciski Pokaż opis budynku i Pokaż na mapie](obrazy/m-v1-mapa-sala-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-sala-jasny-200.webp) |

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
| [![Pierwsze otwarcie bez internetu: pole symbolu, przełącznik Mapa i Lista budynków, pod nim pasek „Bez internetu” bez daty, mapa bez mapy ulic i bez znaków barier, tylko kontury budynków z symbolami](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny.webp) | [![Ta sama mapa przy pierwszym otwarciu w motywie ciemnym](obrazy/m-v1-mapa-pierwsze-otwarcie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pierwsze-otwarcie-ciemny.webp) | [![Ta sama mapa przy pierwszym otwarciu przy czcionce 200%: pole symbolu w dwóch wierszach, przełącznik Mapa i Lista, pasek Bez internetu](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v1-mapa-pierwsze-otwarcie-jasny-200.webp) |

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

## V4 Zgłoszenie szybkie

Wymagania: [V4 Zgłoszenie szybkie](../wymagania-ekranow.md#v4-zgoszenie-szybkie).

### V4: prośba o lokalizację

`m-v4-zgoda-lokalizacja` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-prosba-o-zgode-na-lokalizacje)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zgłoś barierę przed aparatem: „Aby wysłać zdjęcie bariery, potrzebujemy jej miejsca.”, zapewnienie, że nie będziemy śledzić, przyciski Pozwól na lokalizację i Wróć do mapy](obrazy/m-v4-zgoda-lokalizacja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-zgoda-lokalizacja-jasny.webp) | [![Ta sama prośba w motywie ciemnym](obrazy/m-v4-zgoda-lokalizacja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-zgoda-lokalizacja-ciemny.webp) | [![Ta sama prośba przy czcionce 200%: oba przyciski w treści pod tekstem](obrazy/m-v4-zgoda-lokalizacja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-zgoda-lokalizacja-jasny-200.webp) |

### V4: tylko przybliżona lokalizacja

`m-v4-zgoda-przyblizona` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-zgoda-tylko-przyblizona)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zgłoś barierę: „Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę.”, przyciski Pozwól na dokładną lokalizację i Wróć do mapy](obrazy/m-v4-zgoda-przyblizona-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-zgoda-przyblizona-jasny.webp) | [![Ten sam komunikat w motywie ciemnym](obrazy/m-v4-zgoda-przyblizona-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-zgoda-przyblizona-ciemny.webp) | [![Ten sam komunikat przy czcionce 200%](obrazy/m-v4-zgoda-przyblizona-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-zgoda-przyblizona-jasny-200.webp) |

### V4: krok 1 z 3, zdjęcie

`m-v4-krok-1` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-krok-1-z-3)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Krok 1 z 3 z paskiem postępu: ostrzeżenie Ważne „Nie fotografuj twarzy ani tablic rejestracyjnych.” z dopiskiem, że zdjęcie zobaczą inni, i przypięty na dole przycisk Zrób zdjęcie](obrazy/m-v4-krok-1-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-1-jasny.webp) | [![Ten sam krok 1 w motywie ciemnym](obrazy/m-v4-krok-1-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-1-ciemny.webp) | [![Ten sam krok 1 przy czcionce 200%](obrazy/m-v4-krok-1-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-1-jasny-200.webp) |

### V4: krok 1 po zrobieniu zdjęcia

`m-v4-krok-1-zdjecie` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-krok-1-ze-zdjeciem)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Krok 1 z 3 po powrocie z kroku 2: zrobione zdjęcie wejścia ze schodami, to samo ostrzeżenie Ważne, przyciski Dalej i Zrób nowe zdjęcie](obrazy/m-v4-krok-1-zdjecie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-1-zdjecie-jasny.webp) | [![Ten sam krok 1 ze zdjęciem w motywie ciemnym](obrazy/m-v4-krok-1-zdjecie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-1-zdjecie-ciemny.webp) | [![Ten sam krok 1 ze zdjęciem przy czcionce 200%: Zrób nowe zdjęcie w treści pod ostrzeżeniem, przypięty tylko Dalej](obrazy/m-v4-krok-1-zdjecie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-1-zdjecie-jasny-200.webp) |

### V4: krok 2, czego dotyczy bariera

`m-v4-krok-2` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-krok-2-z-3)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Krok 2 z 3: pytanie „Czego dotyczy bariera?” i sześć dużych przycisków: Wejście i drzwi, Schody i windy, Przejścia i chodniki, Toalety, Napisy i oznaczenia, Coś innego](obrazy/m-v4-krok-2-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-2-jasny.webp) | [![Ten sam krok 2 w motywie ciemnym](obrazy/m-v4-krok-2-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-2-ciemny.webp) | [![Ten sam krok 2 przy czcionce 200%](obrazy/m-v4-krok-2-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-2-jasny-200.webp) |

### V4: krok 2, na którym piętrze

`m-v4-krok-2-pietro` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-krok-2-pytanie-o-pietro)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Krok 2 z 3 po wyborze Schody i windy: pytanie „Na którym piętrze?”, przycisk Nie wiem i piętra A-1 od najniższego: Piwnica, Parter, 1, 2 i 3 piętro](obrazy/m-v4-krok-2-pietro-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-2-pietro-jasny.webp) | [![Ten sam wybór piętra w motywie ciemnym](obrazy/m-v4-krok-2-pietro-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-2-pietro-ciemny.webp) | [![Ten sam wybór piętra przy czcionce 200%](obrazy/m-v4-krok-2-pietro-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-2-pietro-jasny-200.webp) |

### V4: krok 3, sprawdzenie i wysłanie

`m-v4-krok-3` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-krok-3-z-3)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Krok 3 z 3: miniatura zdjęcia, podsumowanie Schody i windy oraz 2 piętro, zdanie „Dołączymy miejsce, w którym teraz stoisz.” i przypięty przycisk Wyślij](obrazy/m-v4-krok-3-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-3-jasny.webp) | [![Ten sam krok 3 w motywie ciemnym](obrazy/m-v4-krok-3-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-3-ciemny.webp) | [![Ten sam krok 3 przy czcionce 200%](obrazy/m-v4-krok-3-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-krok-3-jasny-200.webp) |

### V4: telefon nie ustalił miejsca

`m-v4-brak-pozycji` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-brak-pozycji)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Krok 3 z 3 z podsumowaniem i komunikatem „Telefon nie ustalił, gdzie jesteś.”, radą, by podejść do okna albo wyjść na zewnątrz, oraz przyciskami Spróbuj ponownie i Zrezygnuj](obrazy/m-v4-brak-pozycji-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-brak-pozycji-jasny.webp) | [![Ten sam komunikat w motywie ciemnym](obrazy/m-v4-brak-pozycji-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-brak-pozycji-ciemny.webp) | [![Ten sam komunikat przy czcionce 200%](obrazy/m-v4-brak-pozycji-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-brak-pozycji-jasny-200.webp) |

### V4: potwierdzenie rezygnacji

`m-v4-rezygnacja` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-potwierdzenie-rezygnacji)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Potwierdzenie rezygnacji: miniatura zdjęcia, które zostanie usunięte, zdanie „Jeśli zrezygnujesz, usuniemy zrobione zdjęcie i niczego nie wyślemy.” i dwa jednakowe przyciski Tak, zrezygnuj oraz Wróć do zdjęcia](obrazy/m-v4-rezygnacja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-rezygnacja-jasny.webp) | [![To samo potwierdzenie w motywie ciemnym](obrazy/m-v4-rezygnacja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-rezygnacja-ciemny.webp) | [![To samo potwierdzenie przy czcionce 200%](obrazy/m-v4-rezygnacja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-rezygnacja-jasny-200.webp) |

### V4: wysłane

`m-v4-wyslane` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-potwierdzenie-wysania)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po wysłaniu: „Dziękujemy.” i zdanie, że sprawdzimy zdjęcie i pokażemy barierę na mapie, jeśli się potwierdzi, oraz przypięty przycisk Wróć do mapy](obrazy/m-v4-wyslane-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-wyslane-jasny.webp) | [![To samo potwierdzenie w motywie ciemnym](obrazy/m-v4-wyslane-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-wyslane-ciemny.webp) | [![To samo potwierdzenie przy czcionce 200%](obrazy/m-v4-wyslane-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-wyslane-jasny-200.webp) |

### V4: zapisane bez internetu

`m-v4-wyslane-brak-sieci` · [makieta low-fi](../makiety/odwiedzajacy.md#v4-potwierdzenie-bez-internetu)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po wysłaniu bez internetu: pasek „Bez internetu · dane z 27.10.2026”, „Zapisaliśmy zdjęcie w telefonie.”, „Wyślemy je sami, gdy wróci internet.” i przycisk Wróć do mapy](obrazy/m-v4-wyslane-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v4-wyslane-brak-sieci-jasny.webp) | [![To samo potwierdzenie w motywie ciemnym](obrazy/m-v4-wyslane-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v4-wyslane-brak-sieci-ciemny.webp) | [![To samo potwierdzenie przy czcionce 200%](obrazy/m-v4-wyslane-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v4-wyslane-brak-sieci-jasny-200.webp) |

## V5 Pomoc

Wymagania: [V5 Pomoc](../wymagania-ekranow.md#v5-pomoc).

### V5: pierwsze uruchomienie, krok 1 z 3

`m-v5-pomoc-pierwsze-uruchomienie` · [makieta low-fi](../makiety/odwiedzajacy.md#v5-pomoc). Przy pierwszym uruchomieniu „Wstecz” na tym kroku pomija instrukcję i otwiera mapę.

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Instrukcja otwarta sama przy pierwszym uruchomieniu, Jak korzystać z aplikacji, krok 1 z 3: Znajdź budynek, wyjaśnienie, że po wpisaniu symbolu w polu na górze mapy pokażemy opis budynku z wejściami i barierami, przykład pola wyszukiwania bez lupy, przyciski Dalej i Pomiń instrukcję oraz pasek dolny z zaznaczoną Pomocą](obrazy/m-v5-pomoc-pierwsze-uruchomienie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v5-pomoc-pierwsze-uruchomienie-jasny.webp) | [![Ten sam pierwszy krok instrukcji w motywie ciemnym](obrazy/m-v5-pomoc-pierwsze-uruchomienie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v5-pomoc-pierwsze-uruchomienie-ciemny.webp) | [![Ten sam pierwszy krok instrukcji przy czcionce 200%: Dalej i Pomiń instrukcję na końcu treści, pod przykładem pola](obrazy/m-v5-pomoc-pierwsze-uruchomienie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v5-pomoc-pierwsze-uruchomienie-jasny-200.webp) |

### V5: krok 2 z 3, co znaczy „Dostępne”

`m-v5-pomoc` · [makieta low-fi](../makiety/odwiedzajacy.md#v5-pomoc). Instrukcja ma 3 kroki, trzeci to „Zgłoś barierę” ze zdaniem o koncie, bez osobnej makiety.

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Jak korzystać z aplikacji, krok 2 z 3: wyjaśnienie napisu Dostępne przy wejściu, przykład wejścia z opisu budynku z oznaczeniem Dostępne, Wejście dostosowane, Drzwi 120 cm, przyciski Dalej i Pomiń instrukcję oraz pasek dolny z zaznaczoną Pomocą](obrazy/m-v5-pomoc-jasny.webp){: width="240" loading="lazy" }](obrazy/m-v5-pomoc-jasny.webp) | [![Ten sam krok instrukcji w motywie ciemnym](obrazy/m-v5-pomoc-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-v5-pomoc-ciemny.webp) | [![Ten sam krok instrukcji przy czcionce 200%: Dalej i Pomiń instrukcję na końcu treści, pod przykładem](obrazy/m-v5-pomoc-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-v5-pomoc-jasny-200.webp) |

## L1 Prośba o logowanie

Wymagania: [L1 Prośba o logowanie](../wymagania-ekranow.md#l1-prosba-o-logowanie).

### L1: prośba o logowanie przy zgłoszeniu

`m-l1-konto` · [makieta low-fi](../makiety/odwiedzajacy.md#l1-stan-zwyky)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Konto: „Aby zgłosić barierę, potrzebne jest konto.”, wyjaśnienie, że konto chroni mapę przed fałszywymi informacjami, informacja o trzech krótkich krokach i trzy przyciski jeden pod drugim: Zaloguj się, Załóż konto, Nie teraz](obrazy/m-l1-konto-jasny.webp){: width="240" loading="lazy" }](obrazy/m-l1-konto-jasny.webp) | [![Ta sama prośba w motywie ciemnym](obrazy/m-l1-konto-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-l1-konto-ciemny.webp) | [![Ta sama prośba przy czcionce 200%: trzy przyciski na końcu treści](obrazy/m-l1-konto-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-l1-konto-jasny-200.webp) |

### L1: logowanie bez internetu

`m-l1-konto-brak-sieci` · [makieta low-fi](../makiety/odwiedzajacy.md#l1-brak-sieci)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Konto bez internetu: pasek „Bez internetu · dane z 27.10.2026”, komunikat „Logowanie wymaga internetu.”, dopisek, że mapa i opisy budynków działają bez niego, i przycisk Wróć do mapy](obrazy/m-l1-konto-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-l1-konto-brak-sieci-jasny.webp) | [![Ten sam komunikat w motywie ciemnym](obrazy/m-l1-konto-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-l1-konto-brak-sieci-ciemny.webp) | [![Ten sam komunikat przy czcionce 200%](obrazy/m-l1-konto-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-l1-konto-brak-sieci-jasny-200.webp) |

## L2 Konto

Wymagania: [L2 Konto](../wymagania-ekranow.md#l2-konto).

### L2: konto Odwiedzającego

`m-l2-konto` · [makieta low-fi](../makiety/odwiedzajacy.md#l2-odwiedzajacy)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Konto: karta osoby Halina Nowak z rolą Odwiedzający, a pod nią lista Konto z wierszami Informacja o danych osobowych, Wycofaj zgodę i Wyloguj się, w pasku dolnym zaznaczone Konto](obrazy/m-l2-konto-jasny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-jasny.webp) | [![To samo konto w motywie ciemnym](obrazy/m-l2-konto-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-ciemny.webp) | [![To samo konto przy czcionce 200%](obrazy/m-l2-konto-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-jasny-200.webp) |

### L2: Konto i praca

`m-l2-konto-praca` · [makieta low-fi](../makiety/odwiedzajacy.md#l2-rola-pracownicza)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Konto i praca: karta osoby z rolami Odwiedzający, Zbierający, Koordynator, lista Twoja praca z wierszami Zmierz i zgłoś barierę, Moje zgłoszenia, Postęp inwentaryzacji, Zaproś osobę, Pobierz eksport, niżej lista Konto](obrazy/m-l2-konto-praca-jasny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-praca-jasny.webp) | [![Ten sam ekran Konto i praca w motywie ciemnym](obrazy/m-l2-konto-praca-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-praca-ciemny.webp) | [![Ten sam ekran Konto i praca przy czcionce 200%: wiersze list bez ikon, dłuższe nazwy w dwóch wierszach tekstu](obrazy/m-l2-konto-praca-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-praca-jasny-200.webp) |

### L2: informacja o danych osobowych

`m-l2-konto-klauzula` · [makieta low-fi](../makiety/odwiedzajacy.md#l2-klauzula-informacyjna). Tekst klauzuli nie był jeszcze sprawdzony prawnie.

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Informacja o danych osobowych: administratorem jest Zespół projektu Dostępny Kampus, Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko, ze zdaniem „Politechnika Wrocławska nie jest administratorem Twoich danych.”, dalej kontakt przez repozytorium projektu na GitHub, jakie dane, po co, na jakiej podstawie, jak długo i jakie masz prawa](obrazy/m-l2-konto-klauzula-jasny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-klauzula-jasny.webp) | [![Ta sama informacja w motywie ciemnym](obrazy/m-l2-konto-klauzula-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-klauzula-ciemny.webp) | [![Ta sama informacja przy czcionce 200%](obrazy/m-l2-konto-klauzula-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-klauzula-jasny-200.webp) |

### L2: wycofanie zgody

`m-l2-konto-wycofanie` · [makieta low-fi](../makiety/odwiedzajacy.md#l2-potwierdzenie-wycofania-zgody)

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Wycofanie zgody: „Wylogujemy Cię i w ciągu miesiąca usuniemy Twoje dane osobowe.”, dopisek, że wysłane zdjęcia barier zostaną bez danych osoby, i dwa jednakowe przyciski Wycofaj zgodę oraz Zostaw zgodę](obrazy/m-l2-konto-wycofanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-wycofanie-jasny.webp) | [![To samo potwierdzenie w motywie ciemnym](obrazy/m-l2-konto-wycofanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-wycofanie-ciemny.webp) | [![To samo potwierdzenie przy czcionce 200%](obrazy/m-l2-konto-wycofanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-l2-konto-wycofanie-jasny-200.webp) |

## C1 Pomiar pozycji

Wymagania: [C1 Pomiar pozycji](../wymagania-ekranow.md#c1-pomiar-pozycji). Nagłówek w aplikacji: „Zmierz i zgłoś barierę”.

### C1: pomiar trwa

`m-c1-pomiar-pozycji` · [makieta low-fi](../makiety/zespol-w-terenie.md#pomiar-trwa) · odwołania: US6.1, US6.5, NFR-2, NFR-21, NFR-23

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Pomiar trwa: karta „Pomiar: zostało 12 s” z dokładnością 0,8 m (szacunek), satelitami GPS 7, Galileo 5, GLONASS 4 i przyciskiem Powtórz pomiar, niżej Mapa nieba z kierunkami Pn, Pd, Wsch, Zach, satelitami w trzech kształtach i legendą kształtów GPS, Galileo, GLONASS](obrazy/m-c1-pomiar-pozycji-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-jasny.webp) | [![Ten sam pomiar w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, liczby dokładności i satelitów w kilku wierszach, mapa nieba niżej](obrazy/m-c1-pomiar-pozycji-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-jasny-200.webp) |

### C1: po pomiarze, podpowiedź budynku

`m-c1-pomiar-pozycji-podpowiedz` · [makieta low-fi](../makiety/zespol-w-terenie.md#po-pomiarze-podpowiedz-budynku) · odwołania: US6.1, US6.2, US6.3, US6.4, BR-G2, NFR-37, NFR-40, NFR-53

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po pomiarze: karta Pomiar zakończony, pytanie Czy to budynek A-1? z napisem Gmach Główny, ok. 2 m od punktu i przyciskami Potwierdź, Wybierz inny, Poza budynkiem, niżej Miejsce bariery z podpisem Przesuń mapę, aby punkt stał na miejscu bariery, mapa z punktem na środku, strzałkami w krzyżu, plusem i minusem, podpisem OpenStreetMap i przyciskiem informacji, na dole przycisk Dalej](obrazy/m-c1-pomiar-pozycji-podpowiedz-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-podpowiedz-jasny.webp) | [![Ta sama podpowiedź budynku w motywie ciemnym z nocną mapą](obrazy/m-c1-pomiar-pozycji-podpowiedz-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-podpowiedz-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, trzy przyciski podpowiedzi jeden pod drugim, mapa i Dalej niżej](obrazy/m-c1-pomiar-pozycji-podpowiedz-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-podpowiedz-jasny-200.webp) |

### C1: wybór innego budynku

`m-c1-pomiar-pozycji-wybor-budynku` · [makieta low-fi](../makiety/zespol-w-terenie.md#wybor-innego-budynku) · odwołania: US6.2, BR-G2, NFR-4

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po dotknięciu Wybierz inny: sekcja Wybierz budynek z napisem Najbliżej punktu, od najbliższego i wyborem Budynek A-1 Gmach Główny ok. 2 m, Budynek A-6 Karlicówka ok. 22 m, Budynek A-7 ok. 31 m oraz Poza budynkiem, niżej mapa z punktem i przycisk Dalej](obrazy/m-c1-pomiar-pozycji-wybor-budynku-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-wybor-budynku-jasny.webp) | [![Ten sam wybór budynku w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-wybor-budynku-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-wybor-budynku-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, każdy budynek na liście w dwóch wierszach](obrazy/m-c1-pomiar-pozycji-wybor-budynku-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-wybor-budynku-jasny-200.webp) |

### C1: korekta na planie piętra

`m-c1-pomiar-pozycji-korekta` · [makieta low-fi](../makiety/zespol-w-terenie.md#korekta-w-budynku-szczegoowym) · odwołania: US6.3, US6.4, NFR-3, NFR-21, NFR-37, NFR-40, NFR-53

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Korekta w budynku A-1: Budynek A-1 Gmach Główny z Wybierz inny, przełącznik z wybranym Plan piętra obok Mapa, wybór piętra z zaznaczonym 2 piętro, podpis Przesuń mapę, aby punkt stał na miejscu bariery, napis Korekta ręczna, plan piętra z punktem na środku, strzałkami i plusem oraz minusem, pod planem Źródło: Politechnika Wrocławska i przycisk Dalej](obrazy/m-c1-pomiar-pozycji-korekta-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-korekta-jasny.webp) | [![Ta sama korekta na planie piętra w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-korekta-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-korekta-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, piętra w trzech wierszach, plan niżej](obrazy/m-c1-pomiar-pozycji-korekta-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-korekta-jasny-200.webp) |

### C1: budynek bez planu pięter (A-6)

`m-c1-pomiar-pozycji-podstawowy` · odwołania: US6.4, BR-G1, PW-6, NFR-53

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Budynek podstawowy A-6 Karlicówka: zamiast przełącznika i pięter napis Nie mamy planu pięter tego budynku, podpis Przesuń mapę, aby punkt stał na miejscu bariery, Korekta ręczna, mapa z punktem i przyciskami mapy, na dole Dalej](obrazy/m-c1-pomiar-pozycji-podstawowy-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-podstawowy-jasny.webp) | [![Ten sam budynek bez planu pięter w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-podstawowy-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-podstawowy-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, napis o braku planu w dwóch wierszach](obrazy/m-c1-pomiar-pozycji-podstawowy-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-podstawowy-jasny-200.webp) |

### C1: poza budynkiem, bez internetu

`m-c1-pomiar-pozycji-poza-budynkiem` · [makieta low-fi](../makiety/zespol-w-terenie.md#daleko-od-budynkow-bez-internetu) · odwołania: BR-G4, NFR-4, NFR-10, NFR-49

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Daleko od budynków i bez internetu: linia Bez internetu · Ostatnia synchronizacja: 29.10.2026, 16:20, karta Pomiar zakończony, karta Poza budynkiem z Wybierz inny, mapa z punktem między budynkami A-11 i A-13, przycisk Dalej](obrazy/m-c1-pomiar-pozycji-poza-budynkiem-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-poza-budynkiem-jasny.webp) | [![Ten sam stan poza budynkiem w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-poza-budynkiem-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-poza-budynkiem-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, linia braku internetu w dwóch wierszach, data z godziną razem](obrazy/m-c1-pomiar-pozycji-poza-budynkiem-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-poza-budynkiem-jasny-200.webp) |

### C1: szukanie satelitów

`m-c1-pomiar-pozycji-szukanie` · [makieta low-fi](../makiety/zespol-w-terenie.md#szukanie-satelitow) · odwołania: NFR-1, PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Szukanie satelitów: wskaźnik ładowania, napis Szukamy satelitów, Na otwartej przestrzeni pomiar będzie dokładniejszy, i przycisk Powtórz pomiar](obrazy/m-c1-pomiar-pozycji-szukanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-szukanie-jasny.webp) | [![To samo szukanie satelitów w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-szukanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-szukanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, napis w kilku wierszach](obrazy/m-c1-pomiar-pozycji-szukanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-szukanie-jasny-200.webp) |

### C1: brak zgody na lokalizację

`m-c1-pomiar-pozycji-brak-zgody` · [makieta low-fi](../makiety/zespol-w-terenie.md#brak-zgody-na-lokalizacje) · odwołania: NFR-15, NFR-27

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Brak zgody na lokalizację: Aby zapisać miejsce bariery, potrzebujemy lokalizacji. Sprawdzimy ją tylko teraz i nie będziemy Cię śledzić, przycisk Pozwól na lokalizację, bez przycisku powrotu do mapy](obrazy/m-c1-pomiar-pozycji-brak-zgody-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-brak-zgody-jasny.webp) | [![Ta sama prośba o lokalizację w motywie ciemnym](obrazy/m-c1-pomiar-pozycji-brak-zgody-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-brak-zgody-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, przycisk Pozwól na lokalizację w dwóch wierszach](obrazy/m-c1-pomiar-pozycji-brak-zgody-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c1-pomiar-pozycji-brak-zgody-jasny-200.webp) |

## C2 Formularz zgłoszenia

Wymagania: [C2 Formularz zgłoszenia](../wymagania-ekranow.md#c2-formularz-zgoszenia). Nagłówek w aplikacji: „Opisz barierę”, przy edycji „Popraw zgłoszenie”.

### C2: nowe zgłoszenie

`m-c2-formularz-zgloszenia` · [makieta low-fi](../makiety/zespol-w-terenie.md#nowe-zgoszenie-w-budynku-szczegoowym) · odwołania: US7.1, US7.2, US7.3, US7.4, NFR-19, NFR-21, NFR-25, PW-8, PW-13

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Nowe zgłoszenie: Budynek A-1 Gmach Główny, ok. 3 m od Wejścia Cesarskiego, dokładność 0,8 m, przycisk Zmień miejsce, wybór piętra od Piwnica do 3 piętro, pole Typ bariery z listą, Uciążliwość ze zdaniem Oceń dla osoby, dla której bariera jest najtrudniejsza, i trzema wyborami Utrudnienie, Tylko z pomocą, Blokada z ikoną i opisem, komunikat Ważne o twarzach i tablicach, Zrób zdjęcie bez ikony, pole Opis i przycisk Zapisz](obrazy/m-c2-formularz-zgloszenia-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-jasny.webp) | [![Ten sam formularz w motywie ciemnym](obrazy/m-c2-formularz-zgloszenia-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, opisy uciążliwości w kilku wierszach, Zapisz na końcu formularza](obrazy/m-c2-formularz-zgloszenia-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-jasny-200.webp) |

### C2: edycja w budynku bez planu (C-13)

`m-c2-formularz-zgloszenia-edycja` · [makieta low-fi](../makiety/zespol-w-terenie.md#edycja-w-budynku-podstawowym) · odwołania: US8.3, US7.2, US7.3, BR-G1, PW-6

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Popraw zgłoszenie: Budynek C-13 Zintegrowane Centrum Studenckie, ok. 6 m od wejścia głównego, dokładność 1,2 m, napis Nie mamy planu pięter tego budynku. Wpisz numer piętra, 0 to parter, pole Numer piętra z wartością 2 i pole Półpiętro, Typ bariery Inna bariera, wybrana uciążliwość Tylko z pomocą, zdjęcie progu, Zrób nowe zdjęcie, wypełniony Opis i Zapisz](obrazy/m-c2-formularz-zgloszenia-edycja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-edycja-jasny.webp) | [![Ta sama edycja w motywie ciemnym](obrazy/m-c2-formularz-zgloszenia-edycja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-edycja-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, pole numeru piętra i Półpiętro jedno pod drugim](obrazy/m-c2-formularz-zgloszenia-edycja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-edycja-jasny-200.webp) |

### C2: błąd walidacji

`m-c2-formularz-zgloszenia-blad` · [makieta low-fi](../makiety/zespol-w-terenie.md#bad-walidacji) · odwołania: US7.1, NFR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd walidacji: nad formularzem Brakuje typu bariery. Uzupełnij to pole, aby zapisać; przy polu Typ bariery ten sam pogrubiony tekst z kołem ze znakiem „!” i ramka pola w kolorze błędu, reszta formularza wypełniona](obrazy/m-c2-formularz-zgloszenia-blad-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-blad-jasny.webp) | [![Ten sam błąd w motywie ciemnym: tekst błędu i ramka pola jasnołososiowe](obrazy/m-c2-formularz-zgloszenia-blad-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-blad-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, tekst błędu przy polu w trzech wierszach](obrazy/m-c2-formularz-zgloszenia-blad-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-blad-jasny-200.webp) |

### C2: zmiana zgłoszenia przyjętego

`m-c2-formularz-zgloszenia-przyjete` · odwołania: BR-18, US8.3

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Popraw zgłoszenie przyjęte: na górze Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator, niżej formularz z wybraną uciążliwością Utrudnienie, zdjęciem i przyciskiem Zapisz](obrazy/m-c2-formularz-zgloszenia-przyjete-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-przyjete-jasny.webp) | [![Ten sam formularz zgłoszenia przyjętego w motywie ciemnym](obrazy/m-c2-formularz-zgloszenia-przyjete-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-przyjete-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, zdanie o nowym zgłoszeniu w kilku wierszach](obrazy/m-c2-formularz-zgloszenia-przyjete-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-przyjete-jasny-200.webp) |

### C2: wyjście bez zapisu

`m-c2-formularz-zgloszenia-wyjscie` · odwołania: US7.4, NFR-23

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Wyjście bez zapisu po Wstecz: Wyjść bez zapisu? Wpisane pola nie zostaną zapisane, dwa przyciski Wyjdź bez zapisu i Wróć do formularza](obrazy/m-c2-formularz-zgloszenia-wyjscie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-wyjscie-jasny.webp) | [![To samo pytanie w motywie ciemnym](obrazy/m-c2-formularz-zgloszenia-wyjscie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-wyjscie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, przyciski jeden pod drugim](obrazy/m-c2-formularz-zgloszenia-wyjscie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-wyjscie-jasny-200.webp) |

### C2: błąd zapisu

`m-c2-formularz-zgloszenia-blad-zapisu` · odwołania: NFR-7, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd zapisu: Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie, przycisk Spróbuj ponownie, niżej wypełniony formularz](obrazy/m-c2-formularz-zgloszenia-blad-zapisu-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-blad-zapisu-jasny.webp) | [![Ten sam błąd zapisu w motywie ciemnym](obrazy/m-c2-formularz-zgloszenia-blad-zapisu-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-blad-zapisu-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach nad formularzem](obrazy/m-c2-formularz-zgloszenia-blad-zapisu-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c2-formularz-zgloszenia-blad-zapisu-jasny-200.webp) |

## C3 Moje zgłoszenia

Wymagania: [C3 Moje zgłoszenia](../wymagania-ekranow.md#c3-moje-zgoszenia). Nagłówek w aplikacji: „Moje zgłoszenia”.

### C3: lista zgłoszeń

`m-c3-moje-zgloszenia` · [makieta low-fi](../makiety/zespol-w-terenie.md#lista-zgoszen) · odwołania: US8.1, US8.2, US8.4, US9.1, US17.2, BR-16, NFR-21, PW-12

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Lista zgłoszeń: przyciski Czeka na wysłanie: 2 i Zmierz i zgłoś barierę, potem karty ze statusem słowem i ikoną: W telefonie, Bariery już nie ma, Niesprawna winda z Otwórz; Odrzucone, Próg z powodem odrzucenia Zdjęcie nie pokazuje progu i Otwórz; W weryfikacji, Ciężkie drzwi ze Zmiana czeka na wysłanie; Wysłane, Próg z Dwie wersje: wybierz jedną; Przyjęte, Schody przy wejściu bez przycisku; Archiwalne, zmiana oznaczenia Wejścia Cesarskiego z Nowe oznaczenie: Niedostępne](obrazy/m-c3-moje-zgloszenia-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-jasny.webp) | [![Ta sama lista zgłoszeń w motywie ciemnym](obrazy/m-c3-moje-zgloszenia-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, karty na całą szerokość, statusy i daty w kilku wierszach](obrazy/m-c3-moje-zgloszenia-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-jasny-200.webp) |

### C3: odrzucone jako „Nieaktualne”

`m-c3-moje-zgloszenia-nieaktualne` · [makieta low-fi](../makiety/zespol-w-terenie.md#odrzucone-jako-nieaktualne) · odwołania: BR-23, BR-24, PW-12, PW-14

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Trzy zgłoszenia odrzucone z powodem Nieaktualne: Próg z Ktoś już zmienił to zgłoszenie. Twoja zmiana trafi do jego aktualnej wersji i Wyślij ponownie; Bariery już nie ma, Niesprawna winda z Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa i Zmierz i zgłoś barierę; zmiana oznaczenia Wejścia Cesarskiego z Nowe oznaczenie: Dostępne, zdaniem Oznaczenie tego wejścia już się zmieniło i Wyślij ponownie](obrazy/m-c3-moje-zgloszenia-nieaktualne-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-nieaktualne-jasny.webp) | [![Te same odrzucone zgłoszenia w motywie ciemnym](obrazy/m-c3-moje-zgloszenia-nieaktualne-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-nieaktualne-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, wyjaśnienia w kilku wierszach](obrazy/m-c3-moje-zgloszenia-nieaktualne-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-nieaktualne-jasny-200.webp) |

### C3: pusta lista

`m-c3-moje-zgloszenia-pusty` · [makieta low-fi](../makiety/zespol-w-terenie.md#pusta-lista) · odwołania: PW-12

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Pusta lista: Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę, przycisk Zmierz i zgłoś barierę](obrazy/m-c3-moje-zgloszenia-pusty-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-pusty-jasny.webp) | [![Ta sama pusta lista w motywie ciemnym](obrazy/m-c3-moje-zgloszenia-pusty-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-pusty-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, przycisk w dwóch wierszach](obrazy/m-c3-moje-zgloszenia-pusty-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-pusty-jasny-200.webp) |

### C3: sprawdzanie statusów

`m-c3-moje-zgloszenia-ladowanie` · odwołania: US8.1, PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Ładowanie: nad listą z telefonu wskaźnik i napis Sprawdzamy statusy…, karty zgłoszeń widoczne](obrazy/m-c3-moje-zgloszenia-ladowanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-ladowanie-jasny.webp) | [![To samo ładowanie w motywie ciemnym](obrazy/m-c3-moje-zgloszenia-ladowanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-ladowanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, napis ładowania nad kartami](obrazy/m-c3-moje-zgloszenia-ladowanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-ladowanie-jasny-200.webp) |

### C3: bez internetu

`m-c3-moje-zgloszenia-brak-sieci` · odwołania: US9.1, NFR-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Bez internetu: linia Bez internetu · Ostatnia synchronizacja: 29.10.2026, 16:20, pod nią lista zgłoszeń z telefonu ze statusami](obrazy/m-c3-moje-zgloszenia-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-brak-sieci-jasny.webp) | [![Ta sama lista bez internetu w motywie ciemnym](obrazy/m-c3-moje-zgloszenia-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, linia braku internetu w dwóch wierszach, data z godziną razem](obrazy/m-c3-moje-zgloszenia-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-brak-sieci-jasny-200.webp) |

### C3: błąd sprawdzania statusów

`m-c3-moje-zgloszenia-blad` · odwołania: NFR-5, NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd: Nie udało się sprawdzić statusów, ale zgłoszenia są bezpieczne w telefonie. Spróbuj ponownie za chwilę, przycisk Spróbuj ponownie, pod nim lista z telefonu](obrazy/m-c3-moje-zgloszenia-blad-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-blad-jasny.webp) | [![Ten sam błąd w motywie ciemnym](obrazy/m-c3-moje-zgloszenia-blad-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-blad-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach nad listą](obrazy/m-c3-moje-zgloszenia-blad-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c3-moje-zgloszenia-blad-jasny-200.webp) |

## C4 Synchronizacja i wybór wersji

Wymagania: [C4 Synchronizacja i wybór wersji](../wymagania-ekranow.md#c4-synchronizacja-i-wybor-wersji). Nagłówek w aplikacji: „Synchronizacja”.

### C4: kolejka w telefonie

`m-c4-synchronizacja` · [makieta low-fi](../makiety/zespol-w-terenie.md#kolejka-w-telefonie) · odwołania: US9.1, US9.2, BR-9, NFR-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Kolejka: karta Czeka na wysłanie: 3, 2 zgłoszenia i 1 znacznik piętra, Ostatnia synchronizacja: 29.10.2026, 16:20, przycisk Synchronizuj teraz](obrazy/m-c4-synchronizacja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-jasny.webp) | [![Ta sama kolejka w motywie ciemnym](obrazy/m-c4-synchronizacja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, data z godziną w osobnym wierszu](obrazy/m-c4-synchronizacja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-jasny-200.webp) |

### C4: konflikt wersji

`m-c4-synchronizacja-konflikt` · [makieta low-fi](../makiety/zespol-w-terenie.md#konflikt-wersji) · odwołania: US9.3, BR-10, NFR-21, NFR-52, NFR-54, PW-12

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Konflikt: Dwie wersje: wybierz jedną. Druga wersja zostanie usunięta, napis Różni się: uciążliwość, karta Moja wersja, Próg, C-13, piętro 1, Uciążliwość Tylko z pomocą, karta Wersja z serwera z Uciążliwość Blokada, pod nimi Zostaw moją wersję, Weź wersję z serwera i Zdecyduję później](obrazy/m-c4-synchronizacja-konflikt-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-konflikt-jasny.webp) | [![Ten sam konflikt w motywie ciemnym](obrazy/m-c4-synchronizacja-konflikt-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-konflikt-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, wersje jedna pod drugą, trzy przyciski na końcu](obrazy/m-c4-synchronizacja-konflikt-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-konflikt-jasny-200.webp) |

### C4: wszystko wysłane

`m-c4-synchronizacja-pusty` · odwołania: US9.1, NFR-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Wszystko wysłane: Wszystko wysłane. Ostatnia synchronizacja: 29.10.2026, 16:20](obrazy/m-c4-synchronizacja-pusty-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-pusty-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-c4-synchronizacja-pusty-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-pusty-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, data z godziną razem](obrazy/m-c4-synchronizacja-pusty-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-pusty-jasny-200.webp) |

### C4: wysyłanie

`m-c4-synchronizacja-ladowanie` · odwołania: US9.2, PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Wysyłanie: wskaźnik i napis Wysyłamy 2 z 5…](obrazy/m-c4-synchronizacja-ladowanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-ladowanie-jasny.webp) | [![To samo wysyłanie w motywie ciemnym](obrazy/m-c4-synchronizacja-ladowanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-ladowanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści](obrazy/m-c4-synchronizacja-ladowanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-ladowanie-jasny-200.webp) |

### C4: bez internetu

`m-c4-synchronizacja-brak-sieci` · odwołania: US9.1, US9.2, NFR-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Bez internetu: linia Bez internetu · Ostatnia synchronizacja: 29.10.2026, 16:20, komunikat Wyślemy wszystko sami, gdy internet wróci, karta Czeka na wysłanie: 3](obrazy/m-c4-synchronizacja-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-brak-sieci-jasny.webp) | [![Ten sam stan bez internetu w motywie ciemnym](obrazy/m-c4-synchronizacja-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, linia braku internetu w dwóch wierszach, data z godziną razem](obrazy/m-c4-synchronizacja-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-brak-sieci-jasny-200.webp) |

### C4: błąd serwera

`m-c4-synchronizacja-blad` · odwołania: NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd serwera: Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później, pod nim karta Czeka na wysłanie: 3](obrazy/m-c4-synchronizacja-blad-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-blad-jasny.webp) | [![Ten sam błąd serwera w motywie ciemnym](obrazy/m-c4-synchronizacja-blad-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-blad-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-c4-synchronizacja-blad-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-blad-jasny-200.webp) |

### C4: mało miejsca w telefonie

`m-c4-synchronizacja-malo-miejsca` · odwołania: NFR-7, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Mało miejsca: komunikat Ważne z kołem ze znakiem „!”, W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia, pod nim kolejka i Synchronizuj teraz](obrazy/m-c4-synchronizacja-malo-miejsca-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-malo-miejsca-jasny.webp) | [![Ten sam komunikat w motywie ciemnym](obrazy/m-c4-synchronizacja-malo-miejsca-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-malo-miejsca-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat Ważne w kilku wierszach](obrazy/m-c4-synchronizacja-malo-miejsca-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c4-synchronizacja-malo-miejsca-jasny-200.webp) |

## C5 Piętra obejrzane

Wymagania: [C5 Piętra obejrzane](../wymagania-ekranow.md#c5-pietra-obejrzane). Nagłówek w aplikacji: „Obejrzane piętra”.

### C5: Zbierający, budynek A-1

`m-c5-obejrzane-pietra` · [makieta low-fi](../makiety/zespol-w-terenie.md#zbierajacy-budynek-szczegoowy) · odwołania: US15.1, US15.2, US15.3, US16.1, BR-5, PW-11

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Budynek A-1, Gmach Główny: piętra od Piwnica do 3 piętro, przy obejrzanych Obejrzane z ikoną i Twój znacznik albo Znacznik innej osoby, przy własnych Cofnij znacznik, przy 1 piętrze Czeka na wysłanie, 3 piętro Nieobejrzane z Oznacz jako obejrzane](obrazy/m-c5-obejrzane-pietra-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-jasny.webp) | [![Ta sama lista pięter w motywie ciemnym](obrazy/m-c5-obejrzane-pietra-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, karty pięter na całą szerokość](obrazy/m-c5-obejrzane-pietra-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-jasny-200.webp) |

### C5: Zbierający, potwierdzenie cofnięcia

`m-c5-obejrzane-pietra-potwierdzenie` · [makieta low-fi](../makiety/zespol-w-terenie.md#zbierajacy-potwierdzenie-cofniecia) · odwołania: US15.2, BR-20, PW-12

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Potwierdzenie przy 2 piętrze: Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy, przyciski Cofnij znacznik i Zostaw znacznik](obrazy/m-c5-obejrzane-pietra-potwierdzenie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-potwierdzenie-jasny.webp) | [![To samo potwierdzenie w motywie ciemnym](obrazy/m-c5-obejrzane-pietra-potwierdzenie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-potwierdzenie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, pytanie w kilku wierszach](obrazy/m-c5-obejrzane-pietra-potwierdzenie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-potwierdzenie-jasny-200.webp) |

### C5: Koordynator, autorzy znaczników

`m-c5-obejrzane-pietra-koordynator` · [makieta low-fi](../makiety/zespol-w-terenie.md#koordynator-potwierdzenie-cofniecia) · odwołania: US11.3, BR-5, BR-20

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Widok Koordynatora: przy każdym piętrze Znacznik: Zbierający, Autor 14 albo Autor 9 i Cofnij znacznik, przy 2 piętrze pytanie Cofnąć znacznik piętra 2? Postęp budynku się zmniejszy, a znacznik postawi ponownie tylko Zbierający, z przyciskami Cofnij znacznik i Zostaw znacznik](obrazy/m-c5-obejrzane-pietra-koordynator-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-koordynator-jasny.webp) | [![Ten sam widok Koordynatora w motywie ciemnym](obrazy/m-c5-obejrzane-pietra-koordynator-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-koordynator-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, pytanie i przyciski pod znacznikiem](obrazy/m-c5-obejrzane-pietra-koordynator-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-koordynator-jasny-200.webp) |

### C5: budynek bez planu (A-6), bez internetu

`m-c5-obejrzane-pietra-podstawowy-brak-sieci` · [makieta low-fi](../makiety/zespol-w-terenie.md#budynek-podstawowy-bez-internetu) · odwołania: US15.1, US15.3, BR-20, NFR-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Budynek A-6, Karlicówka bez internetu: linia Bez internetu · Ostatnia synchronizacja, komunikat Znacznik zapisany w telefonie. Wyślemy go, gdy wróci internet, jeden wiersz Cały budynek z Obejrzane, Twój znacznik, Czeka na wysłanie i Cofnij znacznik](obrazy/m-c5-obejrzane-pietra-podstawowy-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-podstawowy-brak-sieci-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-c5-obejrzane-pietra-podstawowy-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-podstawowy-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, linia braku internetu w dwóch wierszach](obrazy/m-c5-obejrzane-pietra-podstawowy-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-podstawowy-brak-sieci-jasny-200.webp) |

### C5: błąd zapisu znacznika

`m-c5-obejrzane-pietra-blad-zapisu` · odwołania: NFR-7, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd zapisu: Nie udało się zapisać znacznika w telefonie. Spróbuj ponownie, a jeśli to nie pomoże, zwolnij miejsce, przycisk Spróbuj ponownie, pod nim lista pięter](obrazy/m-c5-obejrzane-pietra-blad-zapisu-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-blad-zapisu-jasny.webp) | [![Ten sam błąd w motywie ciemnym](obrazy/m-c5-obejrzane-pietra-blad-zapisu-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-blad-zapisu-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-c5-obejrzane-pietra-blad-zapisu-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c5-obejrzane-pietra-blad-zapisu-jasny-200.webp) |

## C6 Zmiana oznaczenia wejścia

Wymagania: [C6 Zmiana oznaczenia wejścia](../wymagania-ekranow.md#c6-zmiana-oznaczenia-wejscia). Nagłówek w aplikacji: „Zmień oznaczenie wejścia”.

### C6: przed wyborem

`m-c6-zmien-oznaczenie-wejscia` · [makieta low-fi](../makiety/zespol-w-terenie.md#przed-wyborem) · odwołania: US17.2, US17.3, BR-4, BR-24, NFR-17, NFR-21, NFR-25, PW-3, PW-8

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Przed wyborem: karta Obecne oznaczenie z Niedostępne, Z map i opisów, 18.12.2023, Wejście Cesarskie, Budynek A-1, Gmach Główny, niżej Nowe oznaczenie z dwoma dużymi wyborami Dostępne i Niedostępne, Zdjęcie wejścia z komunikatem Ważne i Zrób zdjęcie, napis Zmianę sprawdzi inna osoba. Do tego czasu karta budynku pokazuje obecne oznaczenie, przycisk Wyślij do sprawdzenia](obrazy/m-c6-zmien-oznaczenie-wejscia-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-jasny.webp) | [![Ten sam ekran w motywie ciemnym](obrazy/m-c6-zmien-oznaczenie-wejscia-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, nazwa ekranu w dwóch wierszach, wybory na całą szerokość](obrazy/m-c6-zmien-oznaczenie-wejscia-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-jasny-200.webp) |

### C6: potwierdzenie obecnego oznaczenia

`m-c6-zmien-oznaczenie-wejscia-obecna` · [makieta low-fi](../makiety/zespol-w-terenie.md#potwierdzenie-obecnego-oznaczenia) · odwołania: BR-24, US17.2

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Wybrane Niedostępne, czyli obecne oznaczenie: pod wyborem Potwierdzasz obecne oznaczenie. Zmieni się data i źródło, zdjęcie wejścia ze schodami, Zrób nowe zdjęcie i Wyślij do sprawdzenia](obrazy/m-c6-zmien-oznaczenie-wejscia-obecna-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-obecna-jasny.webp) | [![To samo potwierdzenie w motywie ciemnym](obrazy/m-c6-zmien-oznaczenie-wejscia-obecna-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-obecna-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, zdanie pod wyborem w kilku wierszach](obrazy/m-c6-zmien-oznaczenie-wejscia-obecna-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-obecna-jasny-200.webp) |

### C6: błąd walidacji

`m-c6-zmien-oznaczenie-wejscia-walidacja` · odwołania: BR-24, NFR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd walidacji: pod Nowe oznaczenie Wybierz: dostępne czy niedostępne, nad Zrób zdjęcie Dodaj zdjęcie wejścia, aby wysłać zmianę; oba z kołem ze znakiem „!”, pogrubione, w kolorze błędu, ramki wyborów też](obrazy/m-c6-zmien-oznaczenie-wejscia-walidacja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-walidacja-jasny.webp) | [![Ten sam błąd w motywie ciemnym: teksty błędów i ramki jasnołososiowe](obrazy/m-c6-zmien-oznaczenie-wejscia-walidacja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-walidacja-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, teksty błędów w dwóch wierszach](obrazy/m-c6-zmien-oznaczenie-wejscia-walidacja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-walidacja-jasny-200.webp) |

### C6: błąd zapisu

`m-c6-zmien-oznaczenie-wejscia-blad-zapisu` · odwołania: NFR-7, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd zapisu: Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie z przyciskiem Spróbuj ponownie, niżej obecne oznaczenie, wybrane Niedostępne i zdjęcie wejścia](obrazy/m-c6-zmien-oznaczenie-wejscia-blad-zapisu-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-blad-zapisu-jasny.webp) | [![Ten sam błąd zapisu w motywie ciemnym](obrazy/m-c6-zmien-oznaczenie-wejscia-blad-zapisu-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-blad-zapisu-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-c6-zmien-oznaczenie-wejscia-blad-zapisu-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-blad-zapisu-jasny-200.webp) |

### C6: po wysłaniu bez internetu (C3)

`m-c6-zmien-oznaczenie-wejscia-brak-sieci` · odwołania: US9.1, NFR-54

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po wysłaniu bez internetu otwiera się Moje zgłoszenia: linia Bez internetu · Ostatnia synchronizacja, komunikat Zmiana zapisana w telefonie. Wyślemy ją, gdy wróci internet, karta W telefonie, Zmiana oznaczenia wejścia, A-1, Wejście Cesarskie](obrazy/m-c6-zmien-oznaczenie-wejscia-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-brak-sieci-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-c6-zmien-oznaczenie-wejscia-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-c6-zmien-oznaczenie-wejscia-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c6-zmien-oznaczenie-wejscia-brak-sieci-jasny-200.webp) |

## C7 Usunięcie bariery

Wymagania: [C7 Usunięcie bariery](../wymagania-ekranow.md#c7-usuniecie-bariery). Nagłówek w aplikacji: „Zgłoś, że bariery nie ma”.

### C7: przyjęte zgłoszenie i nowe zdjęcie

`m-c7-bariery-nie-ma` · [makieta low-fi](../makiety/zespol-w-terenie.md#c7-zgos-ze-bariery-nie-ma) · odwołania: US8.4, BR-19, NFR-25, NFR-54, PW-8

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Przyjęte zgłoszenie Niesprawna winda, A-1, piętro 1, ok. 25 m od Wejścia dostosowanego, dokładność 1,1 m, zdjęcie zamkniętej windy, Sprawdzone 7.10.2026, bez pól do edycji; niżej Nowe zdjęcie miejsca z komunikatem Ważne i Zrób zdjęcie, napis Bariera zostanie na mapie, dopóki Weryfikator nie sprawdzi zgłoszenia, przycisk Wyślij do sprawdzenia](obrazy/m-c7-bariery-nie-ma-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-jasny.webp) | [![Ten sam ekran w motywie ciemnym](obrazy/m-c7-bariery-nie-ma-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, nazwa ekranu w dwóch wierszach](obrazy/m-c7-bariery-nie-ma-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-jasny-200.webp) |

### C7: błąd walidacji

`m-c7-bariery-nie-ma-walidacja` · odwołania: BR-19, NFR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd walidacji: nad Zrób zdjęcie Dodaj zdjęcie miejsca, aby wysłać zgłoszenie, z kołem ze znakiem „!”, pogrubione, w kolorze błędu](obrazy/m-c7-bariery-nie-ma-walidacja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-walidacja-jasny.webp) | [![Ten sam błąd w motywie ciemnym](obrazy/m-c7-bariery-nie-ma-walidacja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-walidacja-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, tekst błędu w dwóch wierszach](obrazy/m-c7-bariery-nie-ma-walidacja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-walidacja-jasny-200.webp) |

### C7: błąd zapisu

`m-c7-bariery-nie-ma-blad-zapisu` · odwołania: NFR-7, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd zapisu: Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie z przyciskiem Spróbuj ponownie, niżej przyjęte zgłoszenie i nowe zdjęcie windy z Zrób nowe zdjęcie](obrazy/m-c7-bariery-nie-ma-blad-zapisu-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-blad-zapisu-jasny.webp) | [![Ten sam błąd zapisu w motywie ciemnym](obrazy/m-c7-bariery-nie-ma-blad-zapisu-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-blad-zapisu-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-c7-bariery-nie-ma-blad-zapisu-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-blad-zapisu-jasny-200.webp) |

### C7: po wysłaniu bez internetu (C3)

`m-c7-bariery-nie-ma-brak-sieci` · odwołania: US8.4, US9.1, NFR-54

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Po wysłaniu bez internetu otwiera się Moje zgłoszenia: linia Bez internetu · Ostatnia synchronizacja, komunikat Zgłoszenie zapisane w telefonie. Wyślemy je, gdy wróci internet, karta W telefonie, Bariery już nie ma, A-1, piętro 1](obrazy/m-c7-bariery-nie-ma-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-brak-sieci-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-c7-bariery-nie-ma-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-c7-bariery-nie-ma-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-c7-bariery-nie-ma-brak-sieci-jasny-200.webp) |

## R1 Kolejka weryfikacji

Wymagania: [R1 Kolejka weryfikacji](../wymagania-ekranow.md#r1-kolejka-weryfikacji). Nagłówek w aplikacji: „Zgłoszenia do sprawdzenia”.

### R1: lista do sprawdzenia

`m-r1-kolejka-weryfikacji` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r1-kolejka-weryfikacji) · odwołania: US10.4, US10.5, US10.7, BR-4, BR-11, BR-14, BR-24, NFR-21

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Lista: sekcja Wzięte przeze mnie z kartą Schody przy wejściu, Budynek A-1, Zgłoszenie z pomiaru, Wysłane 26.10.2026 i Otwórz, nagłówek Do sprawdzenia, przełącznik Mapa i Lista z wybraną listą, cztery zgłoszenia od najdawniej wysłanego: Próg w C-13, Schody i windy w C-6 jako zgłoszenie szybkie, Niesprawna winda w A-1 z Bariery już nie ma, Wejście dostosowane w A-1 ze Zmiana oznaczenia wejścia, każde z przyciskiem Weź do sprawdzenia](obrazy/m-r1-kolejka-weryfikacji-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-jasny.webp) | [![Ta sama lista w motywie ciemnym](obrazy/m-r1-kolejka-weryfikacji-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, przełącznik i karty na całą szerokość, Weź do sprawdzenia przy każdej karcie](obrazy/m-r1-kolejka-weryfikacji-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-jasny-200.webp) |

### R1: widok mapy

`m-r1-kolejka-weryfikacji-mapa` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r1-kolejka-weryfikacji) · odwołania: UC14, NFR-18, NFR-19, NFR-21, NFR-37, NFR-40, NFR-53

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Widok mapy: ta sama mapa kampusu co na V1, na niej cztery znaki zgłoszenia do sprawdzenia w kształcie kartki z zagiętym rogiem, wybrany znak Próg przy C-13 większy i podpisany, strzałki w krzyżu, plus i minus, podpis OpenStreetMap i przycisk informacji; pod mapą karta wybranego zgłoszenia Próg, Budynek C-13 z Weź do sprawdzenia](obrazy/m-r1-kolejka-weryfikacji-mapa-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-mapa-jasny.webp) | [![Ten sam widok mapy w motywie ciemnym: nocna mapa, znaki zgłoszeń jasne na ciemnym](obrazy/m-r1-kolejka-weryfikacji-mapa-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-mapa-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, mapa tej samej wysokości, napisy na mapie bez powiększenia](obrazy/m-r1-kolejka-weryfikacji-mapa-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-mapa-jasny-200.webp) |

### R1: brak zgłoszeń

`m-r1-kolejka-weryfikacji-pusty` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r1-kolejka-weryfikacji) · odwołania: US10.4, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Pusty: Nie ma zgłoszeń do sprawdzenia. Zajrzyj tu później, bez przełącznika i bez przycisku](obrazy/m-r1-kolejka-weryfikacji-pusty-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-pusty-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-r1-kolejka-weryfikacji-pusty-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-pusty-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-r1-kolejka-weryfikacji-pusty-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-pusty-jasny-200.webp) |

### R1: zgłoszenie zajęte

`m-r1-kolejka-weryfikacji-zajete` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r1-kolejka-weryfikacji) · odwołania: BR-11, US10.4

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zgłoszenie zajęte: na górze Ktoś inny wziął już to zgłoszenie. Wybierz inne, niżej Wzięte przeze mnie i lista bez zgłoszenia Próg z C-13](obrazy/m-r1-kolejka-weryfikacji-zajete-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-zajete-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-r1-kolejka-weryfikacji-zajete-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-zajete-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach nad listą](obrazy/m-r1-kolejka-weryfikacji-zajete-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-zajete-jasny-200.webp) |

### R1: bez internetu

`m-r1-kolejka-weryfikacji-brak-sieci` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#stany-wspolne) · odwołania: NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Bez internetu: linia Bez internetu, komunikat Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie, przycisk Spróbuj ponownie; ten sam układ mają R2, K1, K2 i K3](obrazy/m-r1-kolejka-weryfikacji-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-brak-sieci-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-r1-kolejka-weryfikacji-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-r1-kolejka-weryfikacji-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-brak-sieci-jasny-200.webp) |

### R1: wczytywanie

`m-r1-kolejka-weryfikacji-ladowanie` · odwołania: PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Ładowanie: wskaźnik i napis Wczytujemy zgłoszenia…](obrazy/m-r1-kolejka-weryfikacji-ladowanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-ladowanie-jasny.webp) | [![To samo ładowanie w motywie ciemnym](obrazy/m-r1-kolejka-weryfikacji-ladowanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-ladowanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści](obrazy/m-r1-kolejka-weryfikacji-ladowanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r1-kolejka-weryfikacji-ladowanie-jasny-200.webp) |

## R2 Sprawdzenie zgłoszenia

Wymagania: [R2 Sprawdzenie zgłoszenia](../wymagania-ekranow.md#r2-sprawdzenie-zgoszenia). Nagłówek w aplikacji: „Sprawdzenie zgłoszenia”.

### R2: zgłoszenie z pomiaru

`m-r2-sprawdzenie-zgloszenia` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r2-sprawdzenie-zgoszenia) · odwołania: US10, US10.1, US10.2, BR-5, BR-13, BR-15, BR-16, NFR-21, NFR-25

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zgłoszenie z pomiaru Schody przy wejściu z uciążliwością Blokada, Budynek A-1, parter, Miejsce: ok. 2 m od Wejścia Cesarskiego, dokładność 0,8 m, zdjęcie schodów, Zasłoń twarz lub tablicę, Autor: Zbierający, Autor 14; Podobne zgłoszenia do 10 m: Próg, 5 m, piętro 1, Przyjęte; pod kreską Przyjmij, Powód odrzucenia z wyborem Duplikat, Nieaktualne, Inny powód i polem Opisz powód dla autora, przycisk Odrzuć](obrazy/m-r2-sprawdzenie-zgloszenia-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-jasny.webp) | [![To samo zgłoszenie w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, dane zgłoszenia w kilku wierszach, decyzja na końcu strony](obrazy/m-r2-sprawdzenie-zgloszenia-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-jasny-200.webp) |

### R2: duplikat

`m-r2-sprawdzenie-zgloszenia-duplikat` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r2-sprawdzenie-zgoszenia) · odwołania: US10.1, US10.2, BR-13, BR-16

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Duplikat: podobne zgłoszenia Schody przy wejściu, 2 m, parter i Próg, 5 m, piętro 1, oba Przyjęte; wybrany powód Duplikat, pod nim Wcześniejsze zgłoszenie: z wybranym Schody przy wejściu, 2 m](obrazy/m-r2-sprawdzenie-zgloszenia-duplikat-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-duplikat-jasny.webp) | [![Ten sam duplikat w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-duplikat-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-duplikat-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, zdjęcia podobnych na całą szerokość](obrazy/m-r2-sprawdzenie-zgloszenia-duplikat-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-duplikat-jasny-200.webp) |

### R2: zgłoszenie szybkie

`m-r2-sprawdzenie-zgloszenia-szybkie` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r2-sprawdzenie-zgoszenia) · odwołania: US10.3, US7.1, BR-14, BR-5, NFR-21, PW-13

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zgłoszenie szybkie: Czego dotyczy: Schody i windy, Budynek C-6, piętro 1, ok. 6 m od wejścia do C-6, dokładność 15 m, zdjęcie schodów, Autor: Odwiedzający, Autor 31; pole Typ bariery z listą, Uciążliwość ze zdaniem Oceń dla osoby, dla której bariera jest najtrudniejsza, i trzema wyborami z opisem jak na C2; W promieniu 10 m nie ma podobnych zgłoszeń; decyzja](obrazy/m-r2-sprawdzenie-zgloszenia-szybkie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-szybkie-jasny.webp) | [![To samo zgłoszenie szybkie w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-szybkie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-szybkie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, opisy uciążliwości w kilku wierszach](obrazy/m-r2-sprawdzenie-zgloszenia-szybkie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-szybkie-jasny-200.webp) |

### R2: bariery już nie ma

`m-r2-sprawdzenie-zgloszenia-usunieta` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r2-sprawdzenie-zgoszenia) · odwołania: US10.6, US10.8, BR-19, BR-23, NFR-20, NFR-21

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Bariery już nie ma: Niesprawna winda, Budynek A-1, piętro 1, Miejsce: winda przy klatkach schodowych C i D, Poprzednie zgłoszenie: Archiwalne, obok siebie Zdjęcie z poprzedniego zgłoszenia i Nowe zdjęcie miejsca, Autor: Zbierający, Autor 22, podobne Niesprawna winda 0 m Przyjęte, wybrany powód Nieaktualne](obrazy/m-r2-sprawdzenie-zgloszenia-usunieta-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-usunieta-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-usunieta-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-usunieta-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, dwa zdjęcia jedno pod drugim z podpisami](obrazy/m-r2-sprawdzenie-zgloszenia-usunieta-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-usunieta-jasny-200.webp) |

### R2: zmiana oznaczenia wejścia

`m-r2-sprawdzenie-zgloszenia-wejscie` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r2-sprawdzenie-zgoszenia) · odwołania: US17.2, US17.3, US10.8, BR-23, BR-24, NFR-21, PW-3

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zmiana oznaczenia wejścia: Wejście dostosowane, Budynek A-1, Obecne oznaczenie: Dostępne, Z map i opisów, 18.12.2023, Bez zmian od wysłania z ikoną ogniwa, Wybrane oznaczenie: Dostępne, zdjęcie wejścia, Autor: Zbierający, Autor 14; W promieniu 10 m nie ma podobnych zgłoszeń; Przyjmij i powody Nieaktualne oraz Inny powód, bez Duplikat](obrazy/m-r2-sprawdzenie-zgloszenia-wejscie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-wejscie-jasny.webp) | [![Ta sama zmiana oznaczenia w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-wejscie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-wejscie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, oznaczenia i daty w kilku wierszach](obrazy/m-r2-sprawdzenie-zgloszenia-wejscie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-wejscie-jasny-200.webp) |

### R2: nowa wersja od autora

`m-r2-sprawdzenie-zgloszenia-nowa-wersja` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#r2-sprawdzenie-zgoszenia) · odwołania: US10.8, BR-3, BR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Nowa wersja: na górze Autor zmienił zgłoszenie. Sprawdź nową wersję przed decyzją, Zmienione: uciążliwość; niżej zgłoszenie Próg z Tylko z pomocą, Budynek C-13, piętro 1, ok. 3 m od Wejścia „C”, dokładność 0,9 m, Autor: Zbierający, Autor 9](obrazy/m-r2-sprawdzenie-zgloszenia-nowa-wersja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-nowa-wersja-jasny.webp) | [![Ta sama nowa wersja w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-nowa-wersja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-nowa-wersja-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-r2-sprawdzenie-zgloszenia-nowa-wersja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-nowa-wersja-jasny-200.webp) |

### R2: wczytywanie zdjęć

`m-r2-sprawdzenie-zgloszenia-ladowanie` · odwołania: US10, PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Ładowanie: dane zgłoszenia widoczne od razu, w ramkach zdjęć wskaźnik i napis Wczytujemy zdjęcie…](obrazy/m-r2-sprawdzenie-zgloszenia-ladowanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-ladowanie-jasny.webp) | [![To samo ładowanie w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-ladowanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-ladowanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, ramki zdjęć z napisem ładowania](obrazy/m-r2-sprawdzenie-zgloszenia-ladowanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-ladowanie-jasny-200.webp) |

### R2: błąd przy przyjęciu

`m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie` · odwołania: US10.3, NFR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd przy przyjęciu zgłoszenia szybkiego: pole Typ bariery i wybory uciążliwości w ramce koloru błędu, nad Przyjmij Ustal typ bariery i uciążliwość, aby przyjąć zgłoszenie, z kołem ze znakiem „!”, pogrubione](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie-jasny.webp) | [![Ten sam błąd w motywie ciemnym: tekst i ramki jasnołososiowe](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, tekst błędu w kilku wierszach nad Przyjmij](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-przyjecie-jasny-200.webp) |

### R2: błąd przy odrzuceniu

`m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie` · odwołania: US10.2, NFR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd przy odrzuceniu: wybory powodu w ramce koloru błędu, nad Odrzuć Podaj powód odrzucenia, aby autor wiedział, co poprawić, z kołem ze znakiem „!”, pogrubione](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie-jasny.webp) | [![Ten sam błąd w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, tekst błędu w kilku wierszach nad Odrzuć](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-walidacja-odrzucenie-jasny-200.webp) |

### R2: bez internetu przy decyzji

`m-r2-sprawdzenie-zgloszenia-brak-sieci` · odwołania: BR-11, NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Bez internetu przy decyzji: na górze linia Bez internetu, nad Przyjmij komunikat Decyzja wymaga internetu, a zgłoszenie czeka w »Wziętych przeze mnie«. Połącz się z internetem i podejmij decyzję ponownie; wybrane wartości zostają](obrazy/m-r2-sprawdzenie-zgloszenia-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-brak-sieci-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-r2-sprawdzenie-zgloszenia-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach nad Przyjmij](obrazy/m-r2-sprawdzenie-zgloszenia-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-r2-sprawdzenie-zgloszenia-brak-sieci-jasny-200.webp) |

## K1 Postęp inwentaryzacji

Wymagania: [K1 Postęp inwentaryzacji](../wymagania-ekranow.md#k1-postep-inwentaryzacji). Nagłówek w aplikacji: „Postęp inwentaryzacji”.

### K1: lista budynków

`m-k1-postep-inwentaryzacji` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k1-postep-inwentaryzacji) · odwołania: US11.1, US11.2, US11.3, BR-21, NFR-21, PW-11

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Lista budynków w kolejności katalogu: Stan z 30.10.2026, 10:42 i przycisk Odśwież, karty z symbolem i nazwą z danych PWr, np. L-2 Kompleks badawczy GEO-3EM Energia, Ekologia, Edukacja Nieobejrzany, C-18 Strefa Kultury Studenckiej Obejrzany z ikoną, z liczbami Czeka na sprawdzenie i Przyjęte oraz przyciskiem Otwórz](obrazy/m-k1-postep-inwentaryzacji-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-jasny.webp) | [![Ta sama lista w motywie ciemnym](obrazy/m-k1-postep-inwentaryzacji-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, karty na całą szerokość, nazwy budynków w kilku wierszach](obrazy/m-k1-postep-inwentaryzacji-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-jasny-200.webp) |

### K1: budynek szczegółowy A-1

`m-k1-postep-inwentaryzacji-a1` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k1-postep-inwentaryzacji) · odwołania: US11.1, US11.2, US11.4, BR-21, BR-24, PW-3

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Lista przewinięta do A-1 Gmach Główny: 4 z 5 pięter, 80%, Czeka na sprawdzenie: 3, Przyjęte: 4, Wejścia sprawdzone na miejscu: 1 z 6 i Otwórz, obok budynki podstawowe H-5 i A-7 Nieobejrzany oraz A-6 Karlicówka Obejrzany](obrazy/m-k1-postep-inwentaryzacji-a1-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-a1-jasny.webp) | [![Ta sama lista w motywie ciemnym](obrazy/m-k1-postep-inwentaryzacji-a1-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-a1-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, liczby w osobnych wierszach](obrazy/m-k1-postep-inwentaryzacji-a1-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-a1-jasny-200.webp) |

### K1: wczytywanie

`m-k1-postep-inwentaryzacji-ladowanie` · odwołania: PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Ładowanie: wskaźnik i napis Wczytujemy postęp…](obrazy/m-k1-postep-inwentaryzacji-ladowanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-ladowanie-jasny.webp) | [![To samo ładowanie w motywie ciemnym](obrazy/m-k1-postep-inwentaryzacji-ladowanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-ladowanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści](obrazy/m-k1-postep-inwentaryzacji-ladowanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-ladowanie-jasny-200.webp) |

### K1: bez internetu

`m-k1-postep-inwentaryzacji-brak-sieci` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#stany-wspolne) · odwołania: NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Bez internetu: linia Bez internetu, komunikat Ta czynność wymaga internetu. Połącz się z internetem i spróbuj ponownie, przycisk Spróbuj ponownie](obrazy/m-k1-postep-inwentaryzacji-brak-sieci-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-brak-sieci-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-k1-postep-inwentaryzacji-brak-sieci-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-brak-sieci-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-k1-postep-inwentaryzacji-brak-sieci-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-brak-sieci-jasny-200.webp) |

### K1: błąd serwera

`m-k1-postep-inwentaryzacji-blad-serwera` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#stany-wspolne) · odwołania: NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd serwera: Serwer chwilowo nie odpowiada. Spróbuj ponownie za kilka minut, przycisk Spróbuj ponownie](obrazy/m-k1-postep-inwentaryzacji-blad-serwera-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-blad-serwera-jasny.webp) | [![Ten sam błąd w motywie ciemnym](obrazy/m-k1-postep-inwentaryzacji-blad-serwera-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-blad-serwera-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-k1-postep-inwentaryzacji-blad-serwera-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-blad-serwera-jasny-200.webp) |

### K1: brak uprawnień

`m-k1-postep-inwentaryzacji-brak-uprawnien` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#stany-wspolne) · odwołania: BR-2, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Brak uprawnień: Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie, przycisk Wróć do mapy](obrazy/m-k1-postep-inwentaryzacji-brak-uprawnien-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-brak-uprawnien-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-k1-postep-inwentaryzacji-brak-uprawnien-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-brak-uprawnien-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-k1-postep-inwentaryzacji-brak-uprawnien-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k1-postep-inwentaryzacji-brak-uprawnien-jasny-200.webp) |

## K2 Zaproszenie

Wymagania: [K2 Zaproszenie](../wymagania-ekranow.md#k2-zaproszenie). Nagłówek w aplikacji: „Zaproś osobę”.

### K2: formularz zaproszenia

`m-k2-zaproszenie` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k2-zaproszenie) · odwołania: US12.1, BR-2

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Zaproszenie: pole Adres e-mail, Rola z wyborem Zbierający, Weryfikator, Koordynator, przycisk Wyślij zaproszenie](obrazy/m-k2-zaproszenie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-jasny.webp) | [![Ten sam formularz w motywie ciemnym](obrazy/m-k2-zaproszenie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, wybory ról na całą szerokość](obrazy/m-k2-zaproszenie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-jasny-200.webp) |

### K2: zaproszenie wysłane

`m-k2-zaproszenie-sukces` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k2-zaproszenie) · odwołania: US12.1, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Sukces: Zaproszenie wysłane na podany adres nad pustym formularzem](obrazy/m-k2-zaproszenie-sukces-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-sukces-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-k2-zaproszenie-sukces-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-sukces-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w trzech wierszach](obrazy/m-k2-zaproszenie-sukces-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-sukces-jasny-200.webp) |

### K2: konto już istnieje

`m-k2-zaproszenie-konto-istnieje` · odwołania: US12.2, BR-2

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Konto już istnieje: To konto już istnieje. Dodaliśmy mu rolę Weryfikatora, dotychczasowe role zostają, pod nim formularz](obrazy/m-k2-zaproszenie-konto-istnieje-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-konto-istnieje-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-k2-zaproszenie-konto-istnieje-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-konto-istnieje-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-k2-zaproszenie-konto-istnieje-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-konto-istnieje-jasny-200.webp) |

### K2: błąd walidacji

`m-k2-zaproszenie-walidacja` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k2-zaproszenie) · odwołania: US12.1, NFR-15, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd walidacji: przy polu Adres e-mail z wartością anna.nowak.pwr.edu.pl tekst Adres e-mail jest niepełny, np. brakuje znaku @. Popraw go i wyślij ponownie, przy Rola Wybierz rolę; oba z kołem ze znakiem „!”, pogrubione, w kolorze błędu, ramki pola i wyborów też](obrazy/m-k2-zaproszenie-walidacja-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-walidacja-jasny.webp) | [![Ten sam błąd w motywie ciemnym: teksty i ramki jasnołososiowe](obrazy/m-k2-zaproszenie-walidacja-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-walidacja-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, tekst błędu przy polu w czterech wierszach](obrazy/m-k2-zaproszenie-walidacja-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k2-zaproszenie-walidacja-jasny-200.webp) |

## K3 Eksport

Wymagania: [K3 Eksport](../wymagania-ekranow.md#k3-eksport). Nagłówek w aplikacji: „Pobierz eksport”.

### K3: wybór formatu

`m-k3-eksport` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k3-eksport) · odwołania: US13.1, US13.2, US13.3, BR-22

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Eksport: Format pliku z wyborem GeoJSON (WGS84) i CSV (PUWG 1992, EPSG:2180), zaznaczony GeoJSON, napis Plik zawiera tylko obecne, sprawdzone bariery, bez danych autorów, przycisk Pobierz plik](obrazy/m-k3-eksport-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-jasny.webp) | [![Ten sam ekran w motywie ciemnym](obrazy/m-k3-eksport-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, nazwy formatów w dwóch wierszach](obrazy/m-k3-eksport-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-jasny-200.webp) |

### K3: plik zapisany

`m-k3-eksport-sukces` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k3-eksport) · odwołania: US13.1, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Sukces: Plik zapisany w pobranych plikach nad wyborem formatu](obrazy/m-k3-eksport-sukces-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-sukces-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-k3-eksport-sukces-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-sukces-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w dwóch wierszach](obrazy/m-k3-eksport-sukces-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-sukces-jasny-200.webp) |

### K3: błąd serwera

`m-k3-eksport-blad` · [makieta low-fi](../makiety/weryfikacja-koordynacja.md#k3-eksport) · odwołania: NFR-50, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Błąd: zaznaczony CSV, pod napisem o zawartości pliku komunikat Serwer chwilowo nie odpowiada. Spróbuj ponownie za kilka minut z przyciskiem Spróbuj ponownie w miejscu Pobierz plik](obrazy/m-k3-eksport-blad-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-blad-jasny.webp) | [![Ten sam błąd w motywie ciemnym](obrazy/m-k3-eksport-blad-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-blad-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-k3-eksport-blad-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-blad-jasny-200.webp) |

### K3: przygotowanie pliku

`m-k3-eksport-ladowanie` · odwołania: US13.1, PW-9

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Ładowanie: wskaźnik i napis Przygotowujemy plik… nad wyborem formatu](obrazy/m-k3-eksport-ladowanie-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-ladowanie-jasny.webp) | [![To samo ładowanie w motywie ciemnym](obrazy/m-k3-eksport-ladowanie-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-ladowanie-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści](obrazy/m-k3-eksport-ladowanie-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-ladowanie-jasny-200.webp) |

### K3: brak przyjętych barier

`m-k3-eksport-pusty` · odwołania: US13.1, PW-10

| Jasny | Ciemny | Czcionka 200% |
| --- | --- | --- |
| [![Pusty: Nie ma jeszcze przyjętych barier. Pobrany plik będzie pusty, niżej wybór formatu i Pobierz plik](obrazy/m-k3-eksport-pusty-jasny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-pusty-jasny.webp) | [![Ten sam stan w motywie ciemnym](obrazy/m-k3-eksport-pusty-ciemny.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-pusty-ciemny.webp) | [![Ten sam stan przy czcionce 200%: na górze tylko „Wstecz”, nazwa ekranu w treści, komunikat w kilku wierszach](obrazy/m-k3-eksport-pusty-jasny-200.webp){: width="240" loading="lazy" }](obrazy/m-k3-eksport-pusty-jasny-200.webp) |
