# Dostępność w aplikacji

Strona pokazuje w jednym miejscu, jak aplikacja pomaga osobom starszym, z ograniczoną mobilnością i słabowidzącym ([potrzeba N4](../wymagania/potrzeby-i-historie.md#potrzeby), persona [PE-1](persony.md#pe-1-pani-halina-osoba-starsza-planujaca-wizyte-hipoteza-do-potwierdzenia-w-wywiadach)). To tylko indeks: każdy wiersz łączy trudność człowieka z krótkim opisem rozwiązania. Wiążące są podlinkowane wymagania i reguły, a tylko one podają wartości.

| Trudność | Co robi aplikacja | Wiążące reguły |
| --- | --- | --- |
| Ograniczona mobilność: schody i długa droga do sali (PE-1) | Opis budynku wskazuje wejście bez schodów z datą sprawdzenia, a kroki do sali prowadzą od niego windą. | [V3](wymagania-ekranow.md#v3-zaplanuj-wizyte), [V2](wymagania-ekranow.md#v2-karta-budynku), [kroki do sali na V1](wymagania-ekranow.md#v1-mapa-kampusu), US3, US17, BR-25 |
| Drżenie rąk: trudno trafić w mały przycisk (PE-1) | Przyciski są duże i stoją w odstępach, więc łatwo nie trafić w sąsiedni. | [NFR-17](../wymagania/niefunkcjonalne.md#nfr-17), [cele dotyku i odstępy](tokeny.md#wymiary) |
| Drżenie rąk: przeciąganie mapy palcem | Mapę można przesuwać strzałkami i powiększać przyciskami, bez przeciągania palcem. | [przyciski mapy na V1](wymagania-ekranow.md#v1-mapa-kampusu) (WCAG 2.5.7), [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne) |
| Drżenie rąk: mały znak bariery na mapie | Znak bariery łatwo trafić palcem, a jego dotknięcie otwiera opis bariery na dole ekranu. | [arkusz bariery na V1](wymagania-ekranow.md#v1-mapa-kampusu), [NFR-17](../wymagania/niefunkcjonalne.md#nfr-17) |
| Drżenie rąk: gesty dwoma palcami i przesuwanie po ścieżce | Żadna czynność nie wymaga gestu, bo każdy gest ma odpowiednik w przycisku. | [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne), [NFR-15](../wymagania/niefunkcjonalne.md#nfr-15) |
| Słaby wzrok: blady tekst | Tekst, ramki i ikony mocno odcinają się od tła. | [NFR-16](../wymagania/niefunkcjonalne.md#nfr-16), [kontrast tokenów](tokeny.md#kontrast) |
| Słaby wzrok: mylenie podobnych liter | Krój pisma powstał dla osób słabowidzących i różnicuje litery o podobnym kształcie. | [typografia](tokeny.md#typografia) |
| Słaby wzrok: drobny tekst | Tekst jest duży już przy zwykłym ustawieniu telefonu. | [typografia](tokeny.md#typografia), [NFR-20](../wymagania/niefunkcjonalne.md#nfr-20) |
| Słaby wzrok: niewidoczne granice pól i pozycji | Granicę pola, grupy i pozycji listy wyznacza wyraźna ramka albo linia podziału. | [zasady tokenów](tokeny.md#zasady), [NFR-16](../wymagania/niefunkcjonalne.md#nfr-16) |
| Powiększona czcionka w telefonie (PE-1) | Napisy się nie ucinają, a mapa kampusu otwiera się listą budynków. | [NFR-20](../wymagania/niefunkcjonalne.md#nfr-20), [Duża czcionka](wymagania-ekranow.md#duza-czcionka), [V1](wymagania-ekranow.md#v1-mapa-kampusu), [odstępstwo](#odstepstwo-pasek-dolny) |
| Daltonizm | Kolor nigdy nie jest jedynym sygnałem: obok niego zawsze jest kształt albo ikona i słowo. | [NFR-21](../wymagania/niefunkcjonalne.md#nfr-21), [zasady tokenów](tokeny.md#zasady), [metafory i ikony](slownik-uzytkownika.md#metafory-i-ikony) |
| Czytnik ekranu TalkBack | Każdy element, który można dotknąć, ma polską nazwę, a zamiast mapy można przejść listę budynków. | [NFR-18](../wymagania/niefunkcjonalne.md#nfr-18), [NFR-19](../wymagania/niefunkcjonalne.md#nfr-19), [V1](wymagania-ekranow.md#v1-mapa-kampusu) |
| Wolniejsze tempo pracy | Aplikacja nie ponagla, bo żadna czynność nie kończy się z upływem czasu. | [NFR-23](../wymagania/niefunkcjonalne.md#nfr-23) |
| Telefon w poziomie, tablet albo telefon składany | Aplikacja działa w każdej orientacji, a na szerokim ekranie tekst nie rozlewa się na całą szerokość. | [NFR-22](../wymagania/niefunkcjonalne.md#nfr-22), [NFR-51](../wymagania/niefunkcjonalne.md#nfr-51), [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne) |
| Słońce na ekranie (PE-1, PE-2) | Tekst ma kontrast wyższy od wymaganego minimum, a pola mają grubsze ramki. | [zasady tokenów](tokeny.md#zasady), [wymiary](tokeny.md#wymiary) |
| Pierwsze otwarcie aplikacji (PE-1) | Krótka instrukcja pokazuje, od czego zacząć, i można do niej wrócić w pomocy. | [V5](wymagania-ekranow.md#v5-pomoc), US5 |
| Długie formularze i zakładanie konta (PE-1) | Mapę i opisy budynków ogląda się bez konta, a zgłoszenie bariery to zdjęcie i kilka dużych przycisków. | [V4](wymagania-ekranow.md#v4-zgoszenie-szybkie), [logowanie przy zgłoszeniu](wymagania-ekranow.md#jak-ograniczamy-przerwanie-przez-logowanie), US1.2, US4.3 |
| Gubienie się w aplikacji | Na każdym ekranie poza mapą startową przycisk „Wstecz” ze strzałką i słowem stoi w tym samym miejscu. | [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne), [NFR-15](../wymagania/niefunkcjonalne.md#nfr-15) |
| Trudne słowa i niejasne komunikaty | Napisy są proste, bez żargonu, a komunikat mówi, co zrobić, i nie obwinia osoby. | [zasady napisów](slownik-uzytkownika.md#zasady-napisow), [wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne), [NFR-42](../wymagania/niefunkcjonalne.md#przenosnosc) |
| Jasny ekran razi wieczorem | Ciemny motyw telefonu ma w aplikacji osobno dobrane i sprawdzone kolory. | [ciemny motyw](tokeny.md#ciemny-motyw), [NFR-16](../wymagania/niefunkcjonalne.md#nfr-16) |

## Odstępstwo: napisy paska dolnego {#odstepstwo-pasek-dolny}

Wiersz „Powiększona czcionka w telefonie” odsyła do [NFR-20](../wymagania/niefunkcjonalne.md#nfr-20): napisy się nie ucinają i nie nakładają, gdy czcionka ma 200%. Jedno miejsce rośnie mniej: napisy paska dolnego (reguła 8 w sekcji [Duża czcionka](wymagania-ekranow.md#duza-czcionka)). Przy 200% w telefonie nie zawsze osiągają pełny rozmiar:

- Wszystkie napisy paska mają jeden rozmiar.
- Rosną, dopóki najdłuższe słowo wszystkich napisów mieści się w pozycji, ale co najmniej 1,3 raza.
- Pozycja ma odstęp 8 dp od sąsiedniej pozycji i od krawędzi ekranu.
- Po „i” w „Konto i praca” stoi twarda spacja, więc „i praca” liczy się jako jedno słowo. Najdłuższe słowo to dziś „barierę”.

Powód: pasek dolny nigdy się nie chowa ([wymagania wspólne](wymagania-ekranow.md#wymagania-wspolne)), a jego cztery pozycje mają w jednym rzędzie stałą szerokość. Przy pełnym powiększeniu słowo „barierę” nie zmieściłoby się w pozycji i musiałoby się łamać w środku. Jeden rozmiar i odstęp od krawędzi sprawiają, że pasek wygląda tak samo przy każdej pozycji, a ostatni napis nie dotyka krawędzi ekranu.

To świadome odstępstwo od miary NFR-20 i od kryterium WCAG 1.4.4.

Łagodzą je ikona przy każdej pozycji, cały napis bez łamania słów, większy niż przy zwykłej czcionce, i pełna nazwa dla TalkBack. Odstępstwo ocenimy w przeglądzie dostępności według NFR-15.
