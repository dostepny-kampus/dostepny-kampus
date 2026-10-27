# Role i uprawnienia

Strona opisuje, kto korzysta z systemu i co wolno każdej roli. Ekrany V1…K3 i historyjki US1…US14 pochodzą z opisu produktu. Cykl życia zgłoszenia opisuje strona [Reguły biznesowe](reguly.md).

## Role

| ID | Rola | Kto to | Cel | Jak uzyskuje rolę |
| --- | --- | --- | --- | --- |
| R-GOS | Gość niezalogowany | każdy, kto otworzył aplikację bez logowania | sprawdzić dostępność budynku przed wizytą (N1, N2) | bez konta |
| R-ODW | Odwiedzający | osoba starsza (np. słuchacz UTW), gość uczelni, student | zgłosić nową barierę bez trudnego formularza (N3) | samodzielna rejestracja; rolę dostaje każde nowe konto |
| R-ZBI | Zbierający | student lub pracownik, np. Działu Dostępności PWr | dokładnie zapisać barierę na miejscu, także bez sieci (N5, N6) | zaproszenie od Koordynatora (K2) |
| R-WER | Weryfikator | pracownik uczelni | odsiać błędy i duplikaty przed publikacją (N8) | zaproszenie od Koordynatora (K2) |
| R-KOO | Koordynator | osoba prowadząca inwentaryzację | śledzić postęp, zapraszać ludzi, oddawać dane do QGIS (N9) | pierwszy — nadany administracyjnie przy wdrożeniu; kolejni — zaproszenie (K2) |

Pierwszego Koordynatora ustawia administrator usługi logowania poza aplikacją. Bez tego nikt nie mógłby wysłać pierwszego zaproszenia.

## Macierz uprawnień

Legenda: ✔ — wolno; ✖ — nie wolno; **własne** — tylko zgłoszenia, których osoba jest autorem.

| Czynność | Ekran, US | R-GOS | R-ODW | R-ZBI | R-WER | R-KOO |
| --- | --- | --- | --- | --- | --- | --- |
| Mapa kampusu i wyszukiwanie budynku | V1, US1 | ✔ | ✔ | ✔ | ✔ | ✔ |
| Karta budynku z barierami przyjętymi | V2, US2 | ✔ | ✔ | ✔ | ✔ | ✔ |
| Plan piętra z wyborem poziomu ¹ | V2, C1 | ✔ | ✔ | ✔ | ✔ | ✔ |
| „Zaplanuj wizytę”, pomoc | V3, V5, US3, US5 | ✔ | ✔ | ✔ | ✔ | ✔ |
| Zgłoszenie szybkie | V4, US4 | ✖ ² | ✔ | ✔ | ✔ | ✔ |
| Pomiar precyzyjny z uśrednianiem GNSS i formularz | C1–C2, US6, US7 | ✖ | ✖ | ✔ | ✖ | ✖ |
| Lista zgłoszeń ze statusami | C3, US8 | ✖ | ✖ ³ | własne | ✖ | ✖ |
| Edycja zgłoszenia | C3 | ✖ | ✖ | własne ⁴ | ✖ | ✖ |
| Wysyłka po powrocie sieci, wybór wersji przy konflikcie | C4, US4, US9 | ✖ | własne | własne | ✖ | ✖ |
| Kolejka weryfikacji | R1, US10 | ✖ | ✖ | ✖ | ✔ | ✖ |
| Decyzja: przyjęcie lub odrzucenie z powodem | R2, US10 | ✖ | ✖ | ✖ | ✔ poza własnymi | ✖ |
| Weryfikacja własnego zgłoszenia | R2 | ✖ | ✖ | ✖ | ✖ ⁵ | ✖ |
| Postęp inwentaryzacji według budynków | K1, US11 | ✖ | ✖ | ✖ | ✖ | ✔ |
| Zaproszenie osoby z rolą | K2, US12 | ✖ | ✖ | ✖ | ✖ | ✔ |
| Eksport GeoJSON i CSV ⁶ | K3, US13 | ✖ | ✖ | ✖ | ✖ | ✔ |
| Widok zgłoszeń niezweryfikowanych | R1, C3, K1 | ✖ | ✖ | własne + podpowiedź duplikatów ⁷ | ✔ | tylko liczby (K1) |
| Widok zgłoszeń odrzuconych | C3, R1 | ✖ | ✖ | własne z powodem | ✔ | ✖ |
| Dane osobowe autora zgłoszenia ⁸ | R2 | ✖ | ✖ | własne | ✖ | ✖ |
| Profil i wylogowanie | wspólne | ✖ | ✔ | ✔ | ✔ | ✔ |

Brak uprawnienia działa na dwóch poziomach (US14): aplikacja nie pokazuje ekranu, a serwer odrzuca żądanie.

### Przypisy

1. Tylko budynki szczegółowe. W pozostałych działa BR-G1: przełącznik pięter jest ukryty, piętro wpisuje się ręcznie.
2. Gość widzi przycisk zgłoszenia, ale przed zdjęciem aplikacja prosi o logowanie lub rejestrację. Konto chroni przed spamem i pozwala blokować nadużycia.
3. W opisie produktu nie ma ekranu listy dla R-ODW. Potrzebę sprawdzimy w wywiadach.
4. Edycja jest możliwa do decyzji weryfikatora (ROL-3).
5. Zasada czterech oczu (ROL-4).
6. Eksport zawiera tylko bariery przyjęte i nie zawiera danych autora (ROL-6).
7. Podpowiedź pokazuje typ i położenie zgłoszeń w pobliżu, bez autora. Zbierający nie zapisuje drugi raz tej samej bariery.
8. Imię, e-mail, identyfikator konta. Weryfikator widzi tylko rolę autora i pseudonim, np. „Autor 17” (ROL-5).

## Zasady ról

| ID | Zasada | Dlaczego |
| --- | --- | --- |
| ROL-1 | Jedna osoba może mieć kilka ról. Każde zalogowane konto ma uprawnienia R-ODW, role pracownicze się sumują. | Zespół jest mały: koordynator często też weryfikuje, a zbierający jest też odwiedzającym. Osobne konta na każdą rolę to zbędny kłopot. |
| ROL-2 | Role pracownicze nadaje tylko Koordynator (K2). Zaproszenie na e-mail istniejącego konta dodaje mu rolę. | Rolę z prawem publikacji dostaje osoba znana z imienia. Samodzielna rejestracja daje tylko R-ODW. |
| ROL-3 | Autor edytuje zgłoszenie do decyzji weryfikatora. Zmiana przyjętego zgłoszenia to nowe zgłoszenie, które znów przechodzi weryfikację. Odrzucone można poprawić i wysłać ponownie. | Opublikowana bariera zawsze ma za sobą weryfikację (N2, N8). Edycja w trakcie przeglądu tworzy nową wersję, a konflikt rozwiązuje US9 AC3 (N7). |
| ROL-4 | Nikt nie weryfikuje własnego zgłoszenia, nawet jeśli ma role R-ZBI i R-WER. | Zasada czterech oczu: błędu autora nie wyłapie ten sam autor (N8). |
| ROL-5 | Weryfikator nie widzi imienia ani e-maila autora, tylko jego rolę i pseudonim. Powiązanie z kontem zna tylko serwer. | Do decyzji wystarczą dane, zdjęcie i rola autora. To minimalizacja danych z RODO, art. 5 ust. 1 lit. c. Pseudonim pozwala zauważyć powtarzający się spam. |
| ROL-6 | Gość i R-ODW widzą tylko bariery przyjęte. Eksport też zawiera tylko bariery przyjęte, bez danych autora. | Niesprawdzona informacja wprowadzałaby w błąd osobę planującą wizytę (US1 AC3, N2). Dane poza systemem nie mogą zawierać danych osobowych. |
| ROL-7 | Koordynator nie podejmuje decyzji weryfikacyjnych, jeśli nie ma roli R-WER. | Rozdział obowiązków: kto rozlicza postęp, ten nie zawyża go własnymi decyzjami. W małym zespole Koordynator może mieć też R-WER, ale wtedy widać to w jego profilu. |

**Skutek ROL-4:** zespół potrzebuje co najmniej dwóch osób z rolą R-WER. Inaczej zgłoszenia jedynego weryfikatora czekają bez końca.

## Źródła

- Dział Dostępności PWr: <https://dzd.pwr.edu.pl/>
- Serwis dostępności PWr: <https://dostepnosc.pwr.edu.pl/>
- RODO, art. 5 (zasada minimalizacji danych): <https://standardyprawa.pl/akt/188/art/23806>
- RODO, tekst urzędowy: <https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=pl>
