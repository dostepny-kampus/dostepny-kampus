# Dostępność w aplikacji

Strona pokazuje w jednym miejscu, jak aplikacja pomaga osobom starszym, z ograniczoną mobilnością i słabowidzącym ([potrzeba N4](../wymagania/potrzeby-i-historie.md#potrzeby), persona [PE-1](persony.md#pe-1-pani-halina-osoba-starsza-planujaca-wizyte-hipoteza-do-potwierdzenia-w-wywiadach)). To tylko indeks: każdy wiersz łączy trudność człowieka z krótkim opisem rozwiązania. Wiążące są podlinkowane wymagania i reguły, a tylko one podają wartości.

| Trudność | Co robi aplikacja | Wiążące reguły |
| --- | --- | --- |
| Drżenie rąk: trudno trafić w mały przycisk (PE-1) | Przyciski są duże i stoją w odstępach, więc łatwo nie trafić w sąsiedni. | [NFR-17](../wymagania/niefunkcjonalne.md#nfr-17), [cele dotyku i odstępy](tokeny.md#wymiary) |
| Drżenie rąk: przeciąganie mapy palcem | Mapę przesuwa się strzałkami ułożonymi w krzyż, a powiększa przyciskami. | [przyciski mapy na V1](wymagania-ekranow.md#v1-mapa-kampusu) (WCAG 2.5.7), [NFR-15](../wymagania/niefunkcjonalne.md#nfr-15) |
| Drżenie rąk: mały znak bariery na mapie | Dotknięcie znaku otwiera na dole ekranu arkusz z opisem bariery, bez szukania jej w napisach. | [arkusz bariery na V1](wymagania-ekranow.md#v1-mapa-kampusu), [NFR-17](../wymagania/niefunkcjonalne.md#nfr-17) |
| Drżenie rąk: gesty dwoma palcami i przesuwanie po ścieżce | Żadna czynność nie wymaga gestu, bo każdy gest ma odpowiednik w przycisku. | [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne), [NFR-15](../wymagania/niefunkcjonalne.md#nfr-15) |
| Słaby wzrok: blady tekst | Tekst, ramki i ikony mocno odcinają się od tła. | [NFR-16](../wymagania/niefunkcjonalne.md#nfr-16), [kontrast tokenów](tokeny.md#kontrast) |
| Słaby wzrok: mylenie podobnych liter | Krój pisma powstał dla osób słabowidzących i różnicuje litery o podobnym kształcie. | [typografia](tokeny.md#typografia) |
| Słaby wzrok: drobny tekst | Tekst jest duży już przy zwykłym ustawieniu telefonu. | [typografia](tokeny.md#typografia), [NFR-20](../wymagania/niefunkcjonalne.md#nfr-20) |
| Słaby wzrok: niewidoczne granice pól i pozycji | Ramki pól i linie podziału mają kontrast, który wyznacza granicę bez grubej kreski. | [zasady tokenów](tokeny.md#zasady), [NFR-16](../wymagania/niefunkcjonalne.md#nfr-16) |
| Powiększona czcionka w telefonie (PE-1) | Napisy się nie ucinają, a mapa kampusu otwiera się listą budynków. | [NFR-20](../wymagania/niefunkcjonalne.md#nfr-20), [Duża czcionka](wymagania-ekranow.md#duza-czcionka), [V1](wymagania-ekranow.md#v1-mapa-kampusu) |
| Daltonizm | Kolor nigdy nie jest jedynym sygnałem: status i oznaczenie wejścia mają kształt i słowo. | [NFR-21](../wymagania/niefunkcjonalne.md#nfr-21), [zasady tokenów](tokeny.md#zasady), [metafory i ikony](slownik-uzytkownika.md#metafory-i-ikony) |
| Czytnik ekranu TalkBack | Każdy element ma polską nazwę, a zamiast mapy można przejść listę budynków. | [NFR-18](../wymagania/niefunkcjonalne.md#nfr-18), [NFR-19](../wymagania/niefunkcjonalne.md#nfr-19), [V1](wymagania-ekranow.md#v1-mapa-kampusu) |
| Wolniejsze tempo pracy | Aplikacja nie ponagla, bo żadna czynność nie kończy się z upływem czasu. | [NFR-23](../wymagania/niefunkcjonalne.md#nfr-23) |
| Telefon w poziomie, tablet albo telefon składany | Aplikacja działa w każdej orientacji, a na szerokim ekranie tekst nie rozlewa się na całą szerokość. | [NFR-22](../wymagania/niefunkcjonalne.md#nfr-22), [NFR-51](../wymagania/niefunkcjonalne.md#nfr-51), [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne) |
| Słońce na ekranie (PE-1, PE-2) | Tekst ma kontrast wyższy od wymaganego minimum, a pola mają grubsze ramki. | [zasady tokenów](tokeny.md#zasady), [wymiary](tokeny.md#wymiary) |
| Trudne słowa i niejasne komunikaty | Napisy są proste, bez żargonu, a komunikat mówi, co zrobić, i nie obwinia osoby. | [zasady napisów](slownik-uzytkownika.md#zasady-napisow), [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne) |
| Jasny ekran razi wieczorem | Ciemny motyw telefonu ma w aplikacji osobno dobrane i sprawdzone kolory. | [ciemny motyw](tokeny.md#ciemny-motyw), [NFR-16](../wymagania/niefunkcjonalne.md#nfr-16) |
