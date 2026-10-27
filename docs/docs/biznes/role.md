# Role i uprawnienia

Strona opisuje, kto korzysta z systemu i co wolno każdej roli. Ekrany V1…K3 i historyjki US1…US16 pochodzą z opisu produktu. Cykl życia zgłoszenia opisuje strona [Reguły biznesowe](reguly.md).

## Role

| ID | Rola | Kto to | Cel | Jak uzyskuje rolę |
| --- | --- | --- | --- | --- |
| RO-GOS | Gość niezalogowany | każdy, kto otworzył aplikację bez logowania | sprawdzić dostępność budynku przed wizytą (N1, N2) | bez konta |
| RO-ODW | Odwiedzający | osoba starsza (np. słuchacz Politechniki Trzeciego Wieku), gość uczelni, student | zgłosić nową barierę bez trudnego formularza (N3) | samodzielna rejestracja; rolę dostaje każde nowe konto |
| RO-ZBI | Zbierający | student lub pracownik, np. Działu Dostępności PWr | dokładnie zapisać barierę na miejscu, także bez sieci (N5, N6) | zaproszenie od Koordynatora (K2) |
| RO-WER | Weryfikator | pracownik uczelni | odsiać błędy i duplikaty przed publikacją (N8) | zaproszenie od Koordynatora (K2) |
| RO-KOO | Koordynator | osoba prowadząca inwentaryzację | śledzić postęp, zapraszać ludzi, przekazywać dane do QGIS (N9) | pierwszy — nadany administracyjnie przy wdrożeniu; kolejni — zaproszenie (K2) |

Pierwszego Koordynatora nadaje administrator usługi logowania, bo inaczej nikt nie wyśle pierwszego zaproszenia.

## Macierz uprawnień

Legenda: ✔ — wolno; ✖ — nie wolno; **własne** — tylko zgłoszenia i oznaczenia piętra obejrzanego, których osoba jest autorem. Kolumny ról pracowniczych zawierają już uprawnienia RO-ODW, a osoba z kilkoma takimi rolami ma ich sumę (BR-1), widoczną w jej profilu.

| Czynność | Ekran, US | RO-GOS | RO-ODW | RO-ZBI | RO-WER | RO-KOO |
| --- | --- | --- | --- | --- | --- | --- |
| Mapa kampusu i wyszukiwanie budynku | V1, US1 | ✔ | ✔ | ✔ | ✔ | ✔ |
| Karta budynku ze zgłoszeniami przyjętymi | V2, US2 | ✔ | ✔ | ✔ | ✔ | ✔ |
| Plan piętra z przełącznikiem pięter ¹ | V2, C1, US16 | ✔ | ✔ | ✔ | ✔ | ✔ |
| „Zaplanuj wizytę”, pomoc | V3, V5, US3, US5 | ✔ | ✔ | ✔ | ✔ | ✔ |
| Zgłoszenie szybkie | V4, US4 | ✖ ² | ✔ | ✔ | ✔ | ✔ |
| Pomiar precyzyjny z uśrednianiem GNSS i formularz | C1–C2, US6, US7 | ✖ | ✖ | ✔ | ✖ | ✖ |
| Lista zgłoszeń ze statusami | C3, US8 | ✖ | ✖ ³ | własne | ✖ | ✖ |
| Edycja zgłoszenia | C3, US8.3 | ✖ | ✖ | własne ⁴ | ✖ | ✖ |
| Wysyłka po powrocie sieci | C4, US4, US9 | ✖ | własne | własne | własne | własne |
| Wybór wersji przy konflikcie | C4, US9 | ✖ | ✖ | własne | ✖ | ✖ |
| Zmiana „bariera usunięta” zgłoszenia przyjętego ⁹ | V2, C3, US8.4 | ✖ | ✖ | ✔ także cudze | ✖ | ✖ |
| Oznaczenie piętra obejrzanego ¹⁰ | C1, US15.1 | ✖ | ✖ | ✔ | ✖ | ✖ |
| Cofnięcie oznaczenia piętra obejrzanego | C1, K1, US15.2, US11.3 | ✖ | ✖ | własne | ✖ | ✔ także cudze |
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
3. W opisie produktu nie ma ekranu listy dla RO-ODW. Potrzebę sprawdzimy w wywiadach.
4. Edycja jest możliwa do decyzji Weryfikatora (BR-3). Zmiana zgłoszenia przyjętego to nowe zgłoszenie. Dwie równoległe zmiany tego samego zgłoszenia rozstrzyga BR-23.
5. Zasada czterech oczu (BR-4).
6. Eksport zawiera tylko bieżące zgłoszenia przyjęte i nie zawiera danych autora (BR-6, BR-22).
7. Podpowiedź pokazuje typ i położenie zgłoszeń w pobliżu, bez autora. Zbierający nie zapisuje drugi raz tej samej bariery.
8. Imię, e-mail, identyfikator konta. Weryfikator widzi tylko rolę autora i pseudonim, np. „Autor 17” (BR-5).
9. Zmiana to nowe zgłoszenie, a nie edycja cudzego, więc może ją złożyć każdy Zbierający (BR-19). Sprawdza ją Weryfikator, który jej nie złożył (BR-4, US10.5). Punkt startu z karty budynku ustalimy w fazie B.
10. W budynku podstawowym oznaczenie dotyczy całego budynku. Ekran oznaczenia ustalimy w fazie B, a oznaczenie działa bez sieci (BR-20, US15.3).

## Zasady ról

| ID | Zasada | Dlaczego |
| --- | --- | --- |
| BR-1 | Jedna osoba może mieć kilka ról. Każde zalogowane konto ma uprawnienia RO-ODW, role pracownicze się sumują. | Zespół jest mały: koordynator często też weryfikuje, a zbierający jest też odwiedzającym. Osobne konta na każdą rolę to zbędny kłopot. |
| BR-2 | Role pracownicze nadaje tylko Koordynator (K2). Zaproszenie na e-mail istniejącego konta dodaje mu rolę. | Rolę z wpływem na publikację dostaje tylko osoba znana Koordynatorowi. Samodzielna rejestracja daje tylko RO-ODW. |
| BR-3 | Zbierający edytuje własne zgłoszenie do decyzji weryfikatora. Zmiana przyjętego zgłoszenia to nowe zgłoszenie, które znów przechodzi weryfikację. Odrzucone można poprawić i wysłać ponownie. | Opublikowana bariera zawsze ma za sobą weryfikację (N2, N8). Edycja w trakcie przeglądu tworzy nową wersję, a konflikt rozwiązuje US9.3. Weryfikator decyduje wtedy o nowej wersji, nie o starej (N7). |
| BR-4 | Nikt nie weryfikuje własnego zgłoszenia, nawet jeśli ma role RO-ZBI i RO-WER. | Zasada czterech oczu: błędu autora nie wyłapie ten sam autor (N8). |
| BR-5 | Weryfikator nie widzi imienia ani e-maila autora, tylko jego rolę i pseudonim. Powiązanie z kontem zna tylko serwer. | Do decyzji wystarczą dane, zdjęcie i rola autora. To minimalizacja danych z RODO, art. 5 ust. 1 lit. c. Pseudonim pozwala zauważyć powtarzający się spam. |
| BR-6 | Gość i RO-ODW widzą tylko zgłoszenia przyjęte. Eksport też zawiera tylko zgłoszenia przyjęte, bez danych autora. | Niesprawdzona informacja wprowadzałaby w błąd osobę planującą wizytę (US1.3, N2). Dane poza systemem nie mogą zawierać danych osobowych. |

**Skutek BR-4:** zespół potrzebuje co najmniej dwóch osób z rolą RO-WER. Inaczej zgłoszenia jedynego weryfikatora czekają bez końca.

## Źródła

- Dział Dostępności PWr: <https://dzd.pwr.edu.pl/>
- Serwis dostępności PWr: <https://dostepnosc.pwr.edu.pl/>
- RODO, art. 5 (zasada minimalizacji danych): <https://standardyprawa.pl/akt/188/art/23806>
- RODO, tekst urzędowy: <https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=pl>
