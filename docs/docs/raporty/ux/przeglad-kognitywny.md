# Przegląd kognitywny

W metodzie przeglądu kognitywnego oceniający przechodzi zadanie krok po kroku w roli użytkownika i przy każdej akcji zadaje cztery pytania [1]. Przegląd obejmuje [makiety hi-fi](../../ux/ekrany/index.md) i [wymagania do ekranów](../../ux/wymagania-ekranow.md) w stanie z 30 października 2026 roku.

Poprawki z tabeli problemów to propozycje: ani wymagań, ani makiet tu nie zmieniamy (decyzja 57).

## Kto i założenia

Użytkowniczka to [PE-1 Pani Halina](../../ux/persony.md#pe-1-pani-halina-osoba-starsza-planujaca-wizyte-hipoteza-do-potwierdzenia-w-wywiadach). To jedyna persona z grupy Gościa i Odwiedzającego i reprezentuje grupę docelową kursu, więc wybieramy tylko ją.

| Założenie | Źródło |
| --- | --- |
| Ma Androida z powiększoną czcionką, więc każdy krok sprawdzamy na obrazie jasnym i przy czcionce 200%. | PE-1, NFR-20, NFR-52 |
| Słabo czyta drobny tekst, a lekkie drżenie dłoni sprawia, że chybia małe przyciski i unika gestów. | PE-1, NFR-16, NFR-17, NFR-53 |
| W znanych aplikacjach radzi sobie pewnie, a w nowej przy błędzie obwinia siebie. | PE-1, N4 |
| Instrukcję V5 pominęła, bo to trudniejszy przypadek. Tam, gdzie instrukcja pomaga, piszemy to. | US5.1 |
| Persona nie wspomina o czytniku ekranu, więc ścieżek z TalkBack tu nie oceniamy. | PE-1, NFR-18 |

Pytania przeglądu [1]:

1. Czy użytkowniczka będzie chciała osiągnąć ten efekt?
2. Czy zauważy, że akcja jest dostępna?
3. Czy skojarzy akcję z efektem, którego szuka?
4. Czy po akcji zobaczy, że zbliżyła się do celu?

Odpowiedź to „tak”, „ryzyko” albo „nie” i jedno zdanie o elemencie ekranu. Identyfikatory PK-1…PK-13 wskazują problem z tabeli na końcu strony.

## Z1. Wejście dostępne do A-1

| Pole | Treść |
| --- | --- |
| Historyjki i kryteria | US1.1, US2.3, US3.2, US3.3 |
| Sytuacja | Pani Halina w domu przed czwartkowym wykładem sprawdza, którym wejściem wejdzie do A-1 bez schodów. |
| Start | [V1 Mapa kampusu](../../ux/ekrany/index.md#v1-mapa-ze-znakami-barier), internet działa. |

| Krok | Akcja i ekran | Wymaganie | 1. Chce? | 2. Zauważy? | 3. Skojarzy? | 4. Widzi postęp? |
| --- | --- | --- | --- | --- | --- | --- |
| 1.1 | Dotyka pola „Wpisz symbol budynku, np. C-13” i wpisuje `a1`; [V1: wyszukiwanie](../../ux/ekrany/index.md#v1-wyszukiwanie-c13) | US1.1 | tak: wie, że wykład jest w A-1. | tak: pole z lupą jest na górze, także przy 200%. | tak: przykład „C-13” ma ten sam kształt co „A-1”. | tak: pod polem pojawia się karta wyniku z symbolem i nazwą. |
| 1.2 | Dotyka karty wyniku „A-1 Gmach Główny”; [V1: opis budynku na pół ekranu](../../ux/ekrany/index.md#v1-arkusz-budynku) | US1.1, US2.3 | tak: karta pokazuje budynek, którego szuka. | ryzyko: karta nie ma znaku, że da się ją dotknąć (PK-11). | tak: symbol i nazwa na karcie są tymi, które wpisała. | tak: arkusz „A-1 Gmach Główny” z chipem „Wejście dostępne”, a na mapie kontur z wejściami. |
| 1.3 | Dotyka „Sprawdź, którędy wejść”; [V3: zalecane wejście](../../ux/ekrany/index.md#v3-zalecane-wejscie-do-a-1) | US3.2 | tak: chip „Wejście dostępne” mówi, że wejście jest, ale nie które. | tak: to jedyny wypełniony przycisk w przypiętym pasie, a przy 200% stoi pod chipami. | tak: napis powtarza jej pytanie „którym wejściem”. | tak: nagłówek „Którędy wejść do A-1” i zdanie z odpowiedzią na górze. |
| 1.4 | Czyta odpowiedź i sekcję „Zalecane wejście”; [V3: zalecane wejście](../../ux/ekrany/index.md#v3-zalecane-wejscie-do-a-1) | US3.3, US2.1 | tak: odpowiedź to cel zadania. | tak: odpowiedź jest pełnym zdaniem w ramce na górze ekranu. | ryzyko: słowo „dostępne” można odczytać jako „otwarte” (PK-12). | ryzyko: nazwa „Wejście dostosowane” nie mówi, z której strony budynku jest wejście (PK-13). |

Od wyniku wyszukiwania do wejścia są 2 dotknięcia, a próg US3.2 to 3, więc kryterium jest spełnione. Szczegóły wejścia, czyli dojście, drzwi i windę, podaje V3 zgodnie z US3.3.

## Z2. Zgłoszenie zepsutej windy

| Pole | Treść |
| --- | --- |
| Historyjki i kryteria | US4.1, US4.3, US4.5, US14.2 |
| Sytuacja | Pani Halina stoi na 2 piętrze A-1 przy windzie, która nie działa. Jest Gościem niezalogowanym, zgłasza pierwszy raz i nie dała jeszcze zgody na lokalizację. |
| Start | V1, internet działa. |

| Krok | Akcja i ekran | Wymaganie | 1. Chce? | 2. Zauważy? | 3. Skojarzy? | 4. Widzi postęp? |
| --- | --- | --- | --- | --- | --- | --- |
| 2.1 | Dotyka „Zgłoś barierę” w pasku dolnym; [L1: prośba o logowanie](../../ux/ekrany/index.md#l1-prosba-o-logowanie-przy-zgoszeniu) | US14.2 | tak: chce powiedzieć o windzie jednym zdjęciem. | tak: pozycja z ikoną aparatu jest w pasku dolnym mapy, opisu budynku i V3. | tak: „Zgłoś” i aparat pasują do zamiaru. | ryzyko: ekran ma nagłówek „Konto”, a nie nazwę dotkniętej czynności (PK-7). |
| 2.2 | Dotyka „Zaloguj się” albo „Załóż konto” i loguje się w przeglądarce; [L1](../../ux/ekrany/index.md#l1-prosba-o-logowanie-przy-zgoszeniu) | US14.2, US14.3 | ryzyko: zakładanie konta zniechęca ją do aplikacji (PK-5). | tak: trzy duże przyciski jeden pod drugim. | ryzyko: bez konta nie wie, który z dwóch przycisków wybrać (PK-6). | ryzyko: strony logowania nie ma w makietach, więc przegląd jej nie ocenia. |
| 2.3 | Dotyka „Pozwól na lokalizację” i zgadza się w oknie telefonu; [V4: prośba o lokalizację](../../ux/ekrany/index.md#v4-prosba-o-lokalizacje) | US4.1, NFR-27 | ryzyko: L1 obiecało „od razu zrobisz zdjęcie”, a pojawia się inna prośba (PK-8). | tak: wypełniony przycisk pod zdaniem z powodem. | tak: zdanie mówi, że miejsce jest potrzebne do wysłania zdjęcia. | ryzyko: wybór przybliżonej lokalizacji w oknie telefonu otwiera [kolejną prośbę](../../ux/ekrany/index.md#v4-tylko-przyblizona-lokalizacja). |
| 2.4 | Dotyka „Zrób zdjęcie” i robi zdjęcie windy; [V4: krok 1](../../ux/ekrany/index.md#v4-krok-1-z-3-zdjecie) | US4.1 | tak: zdjęcie to sposób, którego chce użyć. | tak: jedyny przycisk, przypięty na dole. | tak: napis mówi, co się stanie. | ryzyko: pasek „Krok 2 z 3” pokazuje postęp, ale zdjęcie widać dopiero w kroku 3 (PK-9). |
| 2.5 | Dotyka „Schody i windy”; [V4: krok 2](../../ux/ekrany/index.md#v4-krok-2-czego-dotyczy-bariera) | US4.3 | tak: pytanie „Czego dotyczy bariera?” wprost o to prosi. | tak: sześć przycisków na całą szerokość. | tak: słowo „windy” jest w napisie. | tak: wybrana grupa stoi w ramce nad pytaniem o piętro. |
| 2.6 | Dotyka „2 piętro”; [V4: krok 2, piętro](../../ux/ekrany/index.md#v4-krok-2-na-ktorym-pietrze) | US4.5 | tak: chce, żeby ktoś znalazł windę. | tak: piętra są dużymi przyciskami, „Nie wiem” na górze. | ryzyko: pytanie nie podaje budynku ani tego, czy chodzi o piętro, na którym stoi (PK-10). | tak: krok 3 pokazuje wybrane piętro. |
| 2.7 | Dotyka „Wyślij”; [V4: krok 3](../../ux/ekrany/index.md#v4-krok-3-sprawdzenie-i-wysanie), potem [V4: wysłane](../../ux/ekrany/index.md#v4-wysane) | US4.1, US4.4 | tak: podsumowanie zgadza się z tym, co wybrała. | tak: jedyny przycisk, przypięty na dole. | tak: „Wyślij” kończy zadanie. | tak: „Dziękujemy.” i zdanie, że sprawdzimy zdjęcie. |

Kroki V4 są trzy, jak wymaga US4.1. Przy pierwszym zgłoszeniu Gość wykonuje jednak przed nimi dwie dodatkowe akcje: logowanie i zgodę na lokalizację.

## Z3. Sala A-1 329

| Pole | Treść |
| --- | --- |
| Historyjki i kryteria | US18.1, US18.2 |
| Sytuacja | Pani Halina w domu sprawdza, jak dojdzie bez schodów do sali 329 w A-1. |
| Start | V1, internet działa. |

| Krok | Akcja i ekran | Wymaganie | 1. Chce? | 2. Zauważy? | 3. Skojarzy? | 4. Widzi postęp? |
| --- | --- | --- | --- | --- | --- | --- |
| 3.1 | Dotyka pola i wpisuje `a1 329`; [V1: wyszukiwanie](../../ux/ekrany/index.md#v1-wyszukiwanie-c13) | US18.1 | tak: chce wiedzieć, jak dojdzie do sali wykładu. | ryzyko: podpowiedź pola i instrukcja V5 mówią tylko o symbolu budynku (PK-1). | ryzyko: nie wie, że pole przyjmuje numer sali. | ryzyko: makiety nie pokazują, co widać pod polem po wpisaniu sali (PK-2). |
| 3.2 | Wybiera wynik sali; [V1: znaleziona sala](../../ux/ekrany/index.md#v1-znaleziona-sala-a-1-329) | US18.1 | tak: chce zobaczyć salę. | ryzyko: nie wiadomo, czy jest karta wyniku, czy trzeba zatwierdzić klawiaturą (PK-2). | tak: wynik z numerem sali to dokładnie to, czego szuka. | tak: plan 2 piętra z salą 329, a wybrane piętro ma obwódkę. |
| 3.3 | Czyta cztery kroki w arkuszu, a przy 200% na całym ekranie; [V1: znaleziona sala](../../ux/ekrany/index.md#v1-znaleziona-sala-a-1-329) | US18.2 | tak: kroki to odpowiedź na pytanie „jak dojdę”. | tak: kroki są numerowaną listą zaraz pod nazwą sali. | tak: „Wejście dostosowane” to ta sama nazwa co na V3. | ryzyko: pierwszy krok nie podaje piętra wejścia (PK-3), a „ok. 85 m” nie mówi, że to linia prosta (PK-4). |

## Wynik

W 14 krokach jest 56 odpowiedzi: 40 „tak”, 16 „ryzyko” i 0 „nie”. Najwięcej ryzyk ma Z2 przy logowaniu i Z3 przy wyszukaniu sali. Każde zadanie da się wykonać do końca, ale dwa problemy o wysokiej ważności mogą zatrzymać osobę starszą przed celem.

| Id | Zadanie i krok | Problem | Ważność | Proponowana poprawka | Dotyczy |
| --- | --- | --- | --- | --- | --- |
| PK-1 | Z3, 3.1 | Podpowiedź „Wpisz symbol budynku, np. C-13” i krok 1 instrukcji V5 nie mówią, że można szukać sali. | wysoka | Podpowiedź „Wpisz budynek albo salę, np. A-1 329” i jedno zdanie o sali w kroku 1 V5; długość sprawdzić przy 200%. | US18.1, US5.1, V1 „Pole symbolu budynku” |
| PK-2 | Z3, 3.1–3.2 | Wymagania i makiety nie opisują wyniku wyszukania sali: karty pod polem albo zatwierdzenia klawiaturą. | średnia | Dopisać kartę wyniku „Sala 329, budynek A-1” jako jeden przycisk, jak wynik budynku, i dodać ten stan do makiet. | US18.1, V1 „Wynik pod polem” |
| PK-3 | Z3, 3.3 | Pierwszy krok to samo „Wejście dostosowane”, bez piętra wejścia, którego wymaga kryterium. | średnia | Dopisać piętro w makiecie: „Wejście dostosowane, parter”; to niezgodność makiety z kryterium. | US18.2, BR-25, V1 „Kroki tekstem” |
| PK-4 | Z3, 3.3; Z1, 1.4 | „ok. 85 m od windy” i „ok. 30 m od wejścia” nie mówią, że to odległość w linii prostej. Droga korytarzem bywa dłuższa, a długie dystanse męczą PE-1. | niska | Napis „ok. 85 m w linii prostej od windy” i tak samo na V3. | US18.2, US3.3 |
| PK-5 | Z2, 2.2 | Prośba o konto w środku zadania zniechęca personę; to znane ryzyko z [Momentu logowania](../../ux/wymagania-ekranow.md#moment-logowania). | wysoka | Bez zmian do testu z użytkownikami; liczyć rezygnacje na L1 i przy rezygnacjach wrócić do wariantu B. | US14.2, US14.4, decyzja 38 |
| PK-6 | Z2, 2.2 | Czynność główna L1 to „Zaloguj się”, a Gość, który zgłasza pierwszy raz, może nie mieć konta. Ekran nie mówi, co wybrać bez konta. | średnia | Zdanie nad przyciskami: „Nie masz konta? Wybierz »Załóż konto«.” | US14.2, US14.3, L1 „Załóż konto” |
| PK-7 | Z2, 2.1 | Po „Zgłoś barierę” otwiera się ekran z nagłówkiem „Konto”, wbrew zasadzie, że nagłówek powtarza napis przycisku. | niska | Przy wejściu z V4 nagłówek „Zgłoś barierę”, przy wejściu z paska „Konto”. | US14.2, L1 „Nagłówek”, PW-14 |
| PK-8 | Z2, 2.3 | L1 obiecuje „Po zalogowaniu od razu zrobisz zdjęcie”, a przy pierwszym zgłoszeniu najpierw jest prośba o lokalizację. | średnia | Zdanie L1: „Po zalogowaniu zapytamy o lokalizację i zrobisz zdjęcie. Całość to 3 krótkie kroki.” | US14.2, US4.1, NFR-27, UC7 |
| PK-9 | Z2, 2.4 | Po zdjęciu aplikacja od razu przechodzi do kroku 2. Zdjęcie widać dopiero w kroku 3, a tam nie ma „Zrób nowe zdjęcie”. | niska | W kroku 3 przy miniaturze „Zrób nowe zdjęcie”; grupa i piętro zostają. | US4.1, US4.8 |
| PK-10 | Z2, 2.6 | Pytanie „Na którym piętrze?” nie podaje budynku, a przy windzie nie mówi, o które piętro chodzi. | niska | Pytanie „Na którym piętrze budynku A-1 jest bariera?”; „Nie wiem” zostaje. | US4.5, BR-14 |
| PK-11 | Z1, 1.2 | Karta wyniku nie ma znaku, że da się ją dotknąć; słownik użytkownika sam to zauważa przy listach R1 i K1. | niska | Strzałka w prawo na karcie wyniku i karcie listy budynków, jak przy „Otwórz” na R1. | US1.1, NFR-17, V1 „Wynik pod polem” |
| PK-12 | Z1, 1.4 | „Dostępne” można odczytać jako „otwarte”, a objaśnia je tylko krok 2 instrukcji V5, którą da się pominąć. | średnia | Zdanie z V5 o napisie „Dostępne” także w panelu „Legenda”. | US2.1, US5.1, US1.4, PW-15 |
| PK-13 | Z1, 1.4 | Nazwa z danych „Wejście dostosowane” nie mówi, gdzie jest wejście, a fragment mapy na V3 nie ma nazw ulic. | średnia | W „Dojście” podać stronę budynku albo ulicę, gdy dane PWr je mają. | US3.3, BR-25 |

## Ograniczenia metody

Metoda daje ocenę ekspercką na makietach, a nie wynik badania z ludźmi. Persona jest hipotezą, więc problemy też są hipotezami do sprawdzenia w teście z użytkownikami w Etapie 7. Przegląd nie ocenia strony logowania w przeglądarce ani ścieżek z TalkBack.

## Źródła

1. C. Wharton, J. Rieman, C. Lewis, P. Polson, „The cognitive walkthrough method: a practitioner's guide”. W: J. Nielsen, R. L. Mack (red.), „Usability Inspection Methods”. Wiley, 1994, s. 105–140.
