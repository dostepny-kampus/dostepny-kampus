# Persony

!!! info "Skąd pochodzą persony"
    PE-1 i PE-4 opierają się na badaniach z etapu 2 kursu „Projektowanie doświadczeń użytkownika”. PE-1 łączy wywiady z 6 osobami w wieku 61–75 lat z wcześniejszymi założeniami zespołu. PE-4 powstała z analizy źródeł o osobach na wózku, bez rozmowy z taką osobą. PE-2 i PE-3 to persony prototypowe ról zespołu: opisują nasze założenia, bo osób zbierających i sprawdzających dane nie badaliśmy.

Persona to fikcyjna, ale realistyczna postać, która reprezentuje grupę użytkowników. Imiona i szczegóły są zmyślone. Potrzeby N i historyjki US opisuje strona [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), role — [Role i uprawnienia](../biznes/role.md).

| Persona | Rola | Kim jest | Potrzeby | Ekrany | Podstawa |
| --- | --- | --- | --- | --- | --- |
| PE-1 Pani Halina | RO-GOS, potem RO-ODW | osoba starsza, pierwszy raz idzie do nieznanego budynku uczelni; wizytę planuje z córką | N1, N2, N4 | V1–V5 | wywiady i założenia |
| PE-4 Tomasz | RO-GOS, potem RO-ODW | osoba na wózku, studiuje podyplomowo i sama planuje każdą wizytę | N1, N2, N3 | V1–V4 | analiza źródeł |
| PE-2 Kuba | RO-ZBI | student prowadzący obchód budynku | N5, N6, N7 | V2, C1–C4 | hipoteza |
| PE-3 Marta | RO-WER i RO-KOO (BR-1) | pracowniczka uczelni sprawdzająca zgłoszenia i prowadząca inwentaryzację | N8, N9, N7, N2 | R1, R2, K1–K3 | hipoteza |

PE-1 i PE-4 reprezentują grupę docelową kursu, więc ich potrzeby mają pierwszeństwo przy każdej decyzji o ekranie. Gdy ich potrzeby się różnią, najpierw liczy się PE-1: osoba starsza ma najmniej doświadczenia z aplikacjami.

PE-2 i PE-3 to persony ról zespołu. Nie są administratorami, tylko użytkownikami z innym zadaniem: Kuba zbiera dane na obchodzie, a Marta je sprawdza i prowadzi inwentaryzację. Ich ekrany też muszą być wygodne, bo od ich pracy zależy, czy PE-1 i PE-4 zobaczą aktualne dane.

## PE-1 Pani Halina — osoba starsza planująca wizytę

Persona łączy wywiady z 6 osobami w wieku 61–75 lat (etap 2 kursu PDU) z wcześniejszymi założeniami zespołu. Kolumna „Podstawa” mówi, skąd pochodzi każda cecha.

| Cecha | Opis | Podstawa |
| --- | --- | --- |
| Wiek | 72 lata | założenie |
| Kontekst | emerytka z Wrocławia, słuchaczka Politechniki Trzeciego Wieku, programu PWr dla osób 60+ [1]. Mieszka sama, córka Ania mieszka w tym samym mieście. Pierwszy raz idzie na wykład do A-1 | założenie; córka jako pomoc — wywiady |
| Jak planuje wizytę | nie szuka informacji sama: przed wizytą córka sprawdza adres i salę i zapisuje je na kartce. Na miejscu pyta portiera albo przechodniów | wywiady: 2 z 6 osób planuje z rodziną, 6 z 6 pyta ludzi na miejscu |
| Sprzęt | smartfon z Androidem z powiększoną czcionką, którą ustawił wnuk; okulary do czytania | wywiady: 6 z 6 ma powiększone litery albo uproszczony ekran |
| Umiejętności cyfrowe | dzwoni do rodziny i ogląda zdjęcia od wnuków; pisanie w telefonie jest dla niej niewygodne. W nowej aplikacji myli się przy małych przyciskach i niejasnych komunikatach, czasem przypadkowo coś naciśnie i nie wie, jak wrócić. Winę przypisuje sobie | wywiady; obwinianie siebie — założenie i [3] |
| Wzrok | słabo czyta drobny i blady tekst, zwłaszcza w słońcu; prosi o większe oznaczenia w budynkach | założenie; oznaczenia — wywiady |
| Dłonie | lekkie drżenie; chybia małe przyciski, nie lubi przeciągania i rozsuwania palcami | założenie i [3] |
| Poruszanie się | chodzi z laską; schody, długie dystanse i długie stanie ją męczą | wywiady: schody i brak miejsc do odpoczynku |

**Cele:**

- przed wyjściem z domu wiedzieć, którym wejściem wejdzie do budynku bez schodów, i sprawdzić to razem z córką (N1);
- mieć pewność, że informacja jest aktualna, np. że winda działa (N2);
- w budynku znaleźć salę, windę i toaletę bez błądzenia po korytarzach;
- rzadziej prosić córkę i obcych o pomoc.

**Frustracje:**

- opisy budynków to długi tekst bez zdjęć i mapy (P-1, P-2);
- informacja nie zgadza się z rzeczywistością: złe piętro, zamknięte wejście, nieczynna winda (P-3, P-4, P-5; wywiady: 3 z 6 osób);
- błądzenie po korytarzach i małe oznaczenia (wywiady);
- małe przyciski, pisanie, długie formularze i zakładanie konta zniechęcają ją do aplikacji (N4).

**Potrzeby i historyjki:**

| Potrzeba | Historyjki | Ekrany |
| --- | --- | --- |
| wejście bez barier (N1) | US1, US2, US3, US16, US17 | V1, V2, V3 |
| zaufanie do informacji (N2) | US2.2, US17.3 | V2 |
| obsługa przy słabym wzroku i niepewnych dłoniach (N4) | US5, US4.3; NFR-17, NFR-19, NFR-20, NFR-23 | V5, wszystkie |
| orientacja w budynku: sala, winda, toaleta (nowa potrzeba z wywiadów) | US16, US18 | V1, V2 |
| zgłoszenie bariery bez trudnego formularza (N3), potrzeba poboczna: w wywiadach nikt nie zgłaszał barier | US4, US14.2 | V4 |

**Scenariusz dnia:** W niedzielę córka Ania sprawdza z panią Haliną budynek A-1 przed czwartkowym wykładem. Szukają wejścia bez schodów, windy i sali 329 i patrzą na datę sprawdzenia wejścia. W czwartek pani Halina jedzie prosto do wskazanego wejścia, a w budynku sprawdza w aplikacji toaletę. Gdy winda nie działa, pyta portiera, a barierę zgłosi tylko wtedy, gdy wystarczy jedno zdjęcie.

## PE-2 Kuba — Zbierający na obchodzie (hipoteza, do potwierdzenia w wywiadach)

| Cecha | Opis |
| --- | --- |
| Wiek | 22 lata |
| Kontekst | student III roku, członek koła naukowego; rolę Zbierającego dostał z zaproszenia Koordynatora (US12); obchodzi budynek piętro po piętrze |
| Sprzęt | własny smartfon z Androidem ze średniej półki, powerbank |
| Umiejętności cyfrowe | wysokie; zna mapy i podstawy GIS, szybko uczy się nowej aplikacji |
| Wzrok | bez ograniczeń; przeszkadza mu słońce na ekranie przed budynkiem |
| Dłonie | bez ograniczeń; często jedna ręka zajęta, np. drzwiami albo miarką |
| Poruszanie się | bez ograniczeń; chodzi szybko, ma na obchód 2–3 godziny między zajęciami |

**Cele:**

- dokładnie zapisać miejsce bariery, a nie opisywać je słowami (N5, P-9);
- pracować w piwnicy i na klatce schodowej bez zasięgu i nie tracić zgłoszeń (N6);
- nie nadpisać pracy kolegi, który był w tym budynku wcześniej (N7);
- widzieć, które zgłoszenia trzeba poprawić.

**Frustracje:**

- przy ścianach budynku pozycja z telefonu „skacze” o kilka metrów (NFR-3);
- długi formularz na stojąco, z jedną wolną ręką;
- niepewność, czy zgłoszenia zapisane bez sieci dotarły na serwer;
- na karcie budynku widzi barierę, której już nie ma (P-7).

**Potrzeby i historyjki:**

| Potrzeba | Historyjki | Ekrany |
| --- | --- | --- |
| dokładne miejsce bariery (N5) | US6, US7 | C1, C2 |
| praca bez sieci (N6) | US8, US9, US15 | C3, C4, C1 |
| bez nadpisywania zmian (N7) | US9.3 | C4 |
| aktualne dane dla Odwiedzających (N1, N2) | US8.4, US17.2 | V2 i ekran zmiany |

**Scenariusz dnia:** We wtorek po zajęciach Kuba obchodzi A-1. Przy wejściu od strony bramy sprawdza oznaczenie wejścia i robi nowe zdjęcie. W piwnicy nie ma zasięgu, więc trzy zgłoszenia czekają w telefonie. Po każdym piętrze oznacza je jako obejrzane, a wieczorem sprawdza, czy wszystko się wysłało i czy któreś zgłoszenie odrzucono.

## PE-3 Marta — Weryfikator i Koordynator (hipoteza, do potwierdzenia w wywiadach)

| Cecha | Opis |
| --- | --- |
| Wiek | 46 lat |
| Kontekst | pracowniczka uczelni; prowadzi inwentaryzację i ma role Weryfikatora i Koordynatora (BR-1); sama nie zbiera zgłoszeń |
| Sprzęt | smartfon służbowy z Androidem; komputer z QGIS do pracy z eksportem |
| Umiejętności cyfrowe | wysokie w pracy biurowej, podstawowe w QGIS |
| Wzrok | okulary progresywne; drobny tekst na telefonie czyta z wysiłkiem |
| Dłonie | bez ograniczeń |
| Poruszanie się | bez ograniczeń; większość pracy przy biurku, w krótkich przerwach |

**Cele:**

- odsiać błędy i duplikaty, zanim trafią na mapę (N8);
- wiedzieć, w którym budynku i na którym piętrze trzeba jeszcze być (N9);
- przekazać sprawdzone dane do QGIS bez ręcznego przepisywania (N9);
- zaprosić nowych Zbierających bez pomocy administratora (US12).

**Frustracje:**

- dwa zgłoszenia tej samej bariery od różnych osób (N8, US10.1);
- zdjęcia z twarzami przechodniów (NFR-25);
- zgłoszenie szybkie bez typu bariery i uciążliwości (US10.3);
- dane w kilku miejscach, które sobie przeczą (P-8).

**Potrzeby i historyjki:**

| Potrzeba | Historyjki | Ekrany |
| --- | --- | --- |
| odsianie błędów i duplikatów (N8) | US10 | R1, R2 |
| postęp i eksport do QGIS (N9) | US11, US12, US13 | K1, K2, K3 |
| bez nadpisywania zmian (N7) | US10.4 | R1 |
| zaufanie Odwiedzających (N2) | US10.6, US17.2 | R2 |

**Scenariusz dnia:** Rano Marta otwiera kolejkę weryfikacji i bierze pięć zgłoszeń z A-1. Przy jednym widzi podobne zgłoszenie 5 m dalej i odrzuca duplikat z powodem. Zgłoszenie szybkie od Odwiedzającego uzupełnia o typ bariery i uciążliwość, potem je przyjmuje. W piątek widzi w postępie, że w A-1 brakuje jednego piętra, i otwiera eksport w QGIS.

## PE-4 Tomasz — osoba na wózku planująca zajęcia

Persona powstała z analizy źródeł o osobach na wózku (etap 2 kursu PDU). Nie rozmawialiśmy z taką osobą, więc PE-4 sprawdzimy w badaniach z użytkownikami.

| Cecha | Opis | Podstawa |
| --- | --- | --- |
| Wiek | 47 lat | założenie |
| Kontekst | inżynier budownictwa z Opola. Od 12 lat porusza się na wózku manualnym. Studiuje podyplomowo na PWr i ma zajęcia w różnych budynkach. Na kampus dojeżdża samochodem | założenie |
| Jak planuje wizytę | sprawdza każde nowe miejsce przed wyjazdem: czyta deklaracje dostępności, ogląda zdjęcia w mapach, a gdy to nie wystarcza, dzwoni do portierni. Miejsca bez informacji omija | [4]: 94% sprawdza przed pierwszą wizytą; [5]: 77% unikało miejsc bez informacji |
| Sprzęt | nowy smartfon z Androidem w uchwycie przy wózku | założenie |
| Umiejętności cyfrowe | wysokie; korzysta z map i z Wheelmap, chętnie zgłasza bariery, żeby inni o nich wiedzieli | [8]: Wheelmap, ponad milion miejsc ocenionych przez użytkowników |
| Wzrok | bez ograniczeń | założenie |
| Dłonie | sprawne, ale obie służą do jazdy; telefon obsługuje jedną ręką, zimą w rękawiczkach | założenie |
| Poruszanie się | wózek manualny; nie pokona stopnia, zbyt stromego podjazdu, wąskiego korytarza ani za małej windy | [6]: wejście bez schodów nie wystarcza, gdy dalej jest wąski korytarz albo za mała winda |

**Cele:**

- przed wyjazdem wiedzieć, którym wejściem wjedzie do budynku i czy dojedzie do sali bez schodów (N1);
- wiedzieć, czy winda działa, i ufać opisowi wejścia (N2);
- nie tracić czasu na objazdy wokół budynku;
- szybko zgłosić nową barierę, żeby inni o niej wiedzieli (N3).

**Frustracje:**

- opis „budynek dostosowany”, który nie zgadza się z rzeczywistością ([4]: 86% trafiło na mylące informacje);
- zepsute windy, o których nikt nie informuje ([7]);
- wejście bez schodów, za którym jest wąski korytarz albo za mała winda ([6]);
- wąskie przejścia do wejść i auta zastawiające drogę do windy ([9]).

**Potrzeby i historyjki:**

| Potrzeba | Historyjki | Ekrany |
| --- | --- | --- |
| wejście i droga do sali bez schodów (N1) | US2, US3, US17, US18 | V2, V3 |
| zaufanie do informacji: data, zdjęcie, sprawdzone awarie wind (N2) | US2.2, US17.3, US10 | V2 |
| szybkie zgłoszenie bariery zdjęciem (N3) | US4 | V4 |

**Scenariusz dnia:** W piątek wieczorem Tomasz sprawdza A-1 przed sobotnimi zajęciami. Aplikacja wskazuje wejście dostępne z datą sprawdzenia, zdjęciem i szerokością drzwi, a przy windzie zgłoszenie sprawdzone dwa dni temu: winda nie działa. Tomasz prosi prowadzącego o salę na parterze. W sobotę przy wejściu zauważa za stromy podjazd, robi zdjęcie i wysyła zgłoszenie.

## Co sprawdziły wywiady

Wywiady z etapu 2 sprawdziły założenia o PE-1. Zadaliśmy 8 pytań o ostatnią wizytę w nieznanym budynku, prostszych niż pełny [scenariusz wywiadu](scenariusz-wywiadu.md), więc nie podajemy numerów pytań. Pełne wyniki są w raporcie z kursu PDU.

| Założenie o PE-1 | Wynik | Co zmieniliśmy w personie |
| --- | --- | --- |
| planuje wizytę sama, w domu, dzień lub kilka dni wcześniej | zmienione: 2 z 6 osób planuje z rodziną, tylko 1 z 6 sama sprawdziła coś w telefonie | wizytę planuje z córką |
| przed wizytą szuka przede wszystkim wejścia bez schodów i windy | częściowo: schody przeszkadzały 2 z 6 osobom, a 3 z 6 błądziło w środku budynku | doszła orientacja w budynku: sala, winda, toaleta |
| data sprawdzenia i zdjęcie budzą zaufanie bardziej niż opis | częściowo: 3 z 6 osób trafiło na informację niezgodną z rzeczywistością; o zdjęciach nikt nie mówił | bez zmian |
| w kłopocie prosi o pomoc portiera lub rodzinę, a barier nie zgłasza | potwierdzone: 6 z 6 pyta ludzi na miejscu, nikt nie zgłaszał barier | zgłoszenie to potrzeba poboczna |
| zgłosi barierę, jeśli zajmie to chwilę i wystarczy zdjęcie | niepotwierdzone: nikt o tym nie mówił, a pisanie jest dla rozmówców niewygodne | zgłosi tylko wtedy, gdy wystarczy jedno zdjęcie |
| ma powiększoną czcionkę i chybia małe przyciski | potwierdzone: 6 z 6 ma powiększone litery albo uproszczony ekran; zdarzają się przypadkowe dotknięcia | doszło: nie wie, jak wrócić po przypadkowym dotknięciu |
| rezygnuje z aplikacji przy zakładaniu konta albo wielu krokach | nie badaliśmy | bez zmian |

Założenia o PE-2 i PE-3 nadal czekają na sprawdzenie (sekcja 10 scenariusza wywiadu):

| Persona | Założenie do sprawdzenia | Pytania |
| --- | --- | --- |
| PE-2 | słowny opis miejsca bariery prowadzi do pomyłek | 10.1, 10.2 |
| PE-2 | brak zasięgu w budynku to codzienność obchodu | 10.3 |
| PE-2, PE-3 | przy pracy kilku osób zdarzają się duplikaty | 10.4 |
| PE-3 | przed publikacją ktoś sprawdza dane i znajduje błędy | 10.5 |
| PE-3 | wyniki trafiają do GIS i postęp śledzi się według budynków | 10.6, 10.7 |

Założenia o PE-4 sprawdzimy w badaniach z użytkownikami, gdy uda się zaprosić osobę na wózku.

## Źródła

1. „Historyczna inauguracja. Ruszyła Politechnika Trzeciego Wieku”, PWr: <https://pwr.edu.pl/uczelnia/aktualnosci/seniorzy-na-pwr-ruszyla-politechnika-trzeciego-wieku-13912.html>
2. P. Laubheimer, „3 Persona Types: Lightweight, Qualitative, and Statistical”, NN/g (persony prototypowe jako hipotezy): <https://www.nngroup.com/articles/persona-types/>
3. L. Kane, „Usability for Older Adults: Challenges and Changes”, NN/g: <https://www.nngroup.com/articles/usability-for-senior-citizens/>
4. Euan's Guide, „The Access Survey 2018” (903 osoby z niepełnosprawnościami): <https://www.euansguide.com/media/10927699/the-access-survey-2018-euans-pdf.pdf>
5. AccessAble, „Accessibility and You Survey Results 2023–2024”: <https://accessable-temp-e0b02960d09d.herokuapp.com/articles/accessibility-and-you-survey-results-2023-2024>
6. H. Noyce i in., „Free-Will vs Free-Wheel: Understanding Community Accessibility Requirements of Wheelchair Users”, arXiv 2512.19898: <https://arxiv.org/html/2512.19898v1>
7. NL Times, „Out-of-order lifts often stranding wheelchair users at Dutch train stations”, 16.12.2024: <https://nltimes.nl/2024/12/16/order-lifts-often-stranding-wheelchair-users-dutch-train-stations>
8. European Investment Bank, „A free wheelchair map app” (Wheelmap), 10.02.2022: <https://www.eib.org/en/stories/wheelchair-map-app>
9. K. Gajda, „Diagnoza dostępności przestrzeni publicznej ze szczególnym uwzględnieniem potrzeb osób z niepełnosprawnościami”, Słupsk 2016: <https://www.maszglos.pl/wp-content/uploads/2016/12/raport-ze-spacerow.pdf>

Dostęp do źródeł [1]–[3]: 27.10.2026, [4]–[9]: 03.11.2026.
