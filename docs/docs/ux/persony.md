# Persony

!!! warning "Persony prototypowe (hipoteza, do potwierdzenia w wywiadach)"
    Persony opisują nasze założenia, a nie wyniki badań. Po wywiadach ze [scenariusza](scenariusz-wywiadu.md) poprawimy je albo odrzucimy.

Persona to fikcyjna, ale realistyczna postać, która reprezentuje grupę użytkowników. Imiona i szczegóły są zmyślone. Potrzeby N i historyjki US opisuje strona [Potrzeby i historyjki](../wymagania/potrzeby-i-historie.md), role — [Role i uprawnienia](../biznes/role.md).

| Persona | Rola | Kim jest | Potrzeby | Ekrany |
| --- | --- | --- | --- | --- |
| PE-1 Pani Halina | RO-GOS, potem RO-ODW | osoba starsza, pierwszy raz idzie do budynku uczelni | N1, N2, N4, N3 | V1–V5 |
| PE-2 Kuba | RO-ZBI | student prowadzący obchód budynku | N5, N6, N7 | V2, C1–C4 |
| PE-3 Marta | RO-WER i RO-KOO (BR-1) | pracowniczka uczelni sprawdzająca zgłoszenia i prowadząca inwentaryzację | N8, N9, N7, N2 | R1, R2, K1–K3 |

PE-1 reprezentuje grupę docelową kursu, więc jej potrzeby mają pierwszeństwo przy każdej decyzji o ekranie.

## PE-1 Pani Halina — osoba starsza planująca wizytę (hipoteza, do potwierdzenia w wywiadach)

| Cecha | Opis |
| --- | --- |
| Wiek | 72 lata |
| Kontekst | emerytka z Wrocławia, słuchaczka Politechniki Trzeciego Wieku, programu PWr dla osób 60+ [1]. Pierwszy raz idzie na wykład do C-13 |
| Sprzęt | smartfon z Androidem, duża czcionka ustawiona przez wnuczkę; okulary do czytania |
| Umiejętności cyfrowe | dzwoni, wysyła zdjęcia rodzinie, szuka w wyszukiwarce; nowej aplikacji uczy się powoli i boi się, że coś „zepsuje” |
| Wzrok | słabo czyta drobny i blady tekst, zwłaszcza w słońcu |
| Dłonie | lekkie drżenie; chybia małe przyciski, nie lubi przeciągania i rozsuwania palcami |
| Poruszanie się | chodzi z laską; schody bez poręczy i długie dystanse ją męczą |

**Cele:**

- przed wyjściem z domu wiedzieć, którym wejściem wejdzie do budynku bez schodów (N1);
- mieć pewność, że informacja jest aktualna, np. że winda działa (N2);
- dotrzeć na wykład bez proszenia o pomoc obcych osób.

**Frustracje:**

- opisy budynków to długi tekst bez zdjęć i mapy (P-1, P-2);
- nie wie, czy opis sprzed lat jest jeszcze prawdziwy (P-3, P-4);
- na miejscu zastaje zamknięte drzwi albo nieczynną windę (P-5);
- małe przyciski, długie formularze i zakładanie konta zniechęcają ją do aplikacji (N4).

**Potrzeby i historyjki:**

| Potrzeba | Historyjki | Ekrany |
| --- | --- | --- |
| wejście bez barier (N1) | US1, US2, US3, US16, US17 | V1, V2, V3 |
| zaufanie do informacji (N2) | US2.2, US17.3 | V2 |
| obsługa przy słabym wzroku i niepewnych dłoniach (N4) | US5, US4.3; NFR-17, NFR-19, NFR-20 | V5, wszystkie |
| zgłoszenie bariery bez trudnego formularza (N3), potrzeba poboczna | US4, US14.2 | V4 |

**Scenariusz dnia:** W poniedziałek wieczorem pani Halina sprawdza w telefonie budynek C-13 przed czwartkowym wykładem. Chce znaleźć wejście bez schodów i wiedzieć, czy ktoś je niedawno sprawdzał. W czwartek jedzie tramwajem i idzie prosto do wskazanego wejścia. Jeśli winda nie działa, chce o tym komuś powiedzieć jednym zdjęciem, bez wypełniania formularza.

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

- dwa zgłoszenia tej samej bariery od różnych osób (P-8);
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

## Co sprawdzimy w wywiadzie

Numery pytań pochodzą ze [scenariusza wywiadu](scenariusz-wywiadu.md). Część 10 jest opcjonalna, więc PE-2 i PE-3 mogą zostać hipotezami dłużej niż PE-1.

| Persona | Założenie do sprawdzenia | Pytania |
| --- | --- | --- |
| PE-1 | jak wygląda tydzień i jak często bywa w nowych miejscach | 1.1, 1.2, 1.3 |
| PE-1 | planuje wizytę w domu, dzień lub kilka dni wcześniej | 2.1, 2.2, część 5, sekcja 7 |
| PE-1 | przed wizytą szuka przede wszystkim wejścia bez schodów i windy | 4.1, 4.2, obserwacja w części 5 |
| PE-1 | data sprawdzenia i zdjęcie budzą zaufanie bardziej niż opis | 2.5, 4.3, 4.4, 5.1 |
| PE-1 | w kłopocie prosi o pomoc portiera lub rodzinę, a barier nie zgłasza | 2.4, 4.5, 5.2 |
| PE-1 | zgłosi barierę, jeśli zajmie to chwilę i wystarczy zdjęcie | 3.4, 4.6 |
| PE-1 | ma powiększoną czcionkę i chybia małe przyciski | 3.2, 3.3, obserwacja w częściach 3 i 5 |
| PE-1 | rezygnuje z aplikacji przy zakładaniu konta albo wielu krokach | 4.7, 4.8 |
| PE-1 | najważniejsza zmiana przy wizytach na uczelni | 6.1, 6.2 |
| PE-2 | słowny opis miejsca bariery prowadzi do pomyłek | 10.1, 10.2 |
| PE-2 | brak zasięgu w budynku to codzienność obchodu | 10.3 |
| PE-2, PE-3 | przy pracy kilku osób zdarzają się duplikaty | 10.4 |
| PE-3 | przed publikacją ktoś sprawdza dane i znajduje błędy | 10.5 |
| PE-3 | wyniki trafiają do GIS i postęp śledzi się według budynków | 10.6, 10.7 |

Założenie potwierdzone według reguł z sekcji 8 scenariusza zostaje w personie. Założenie odrzucone usuwamy, a gdy dotyczy potwierdzanej potrzeby N, zgłaszamy jej przegląd (R-7).

## Źródła

1. „Historyczna inauguracja. Ruszyła Politechnika Trzeciego Wieku”, PWr: <https://pwr.edu.pl/uczelnia/aktualnosci/seniorzy-na-pwr-ruszyla-politechnika-trzeciego-wieku-13912.html>
2. P. Laubheimer, „3 Persona Types: Lightweight, Qualitative, and Statistical”, NN/g (persony prototypowe jako hipotezy): <https://www.nngroup.com/articles/persona-types/>
3. L. Kane, „Usability for Older Adults: Challenges and Changes”, NN/g: <https://www.nngroup.com/articles/usability-for-senior-citizens/>

Dostęp do źródeł: 27.10.2026.
