# Analiza SWOT

Strona zestawia mocne i słabe strony produktu z szansami i zagrożeniami z otoczenia. Nie wnosi nowych faktów: każdy punkt odsyła do [wizji](../biznes/wizja.md), [procesu AS-IS](../biznes/procesy/as-is.md), [rejestru ryzyk](../biznes/ryzyka.md), [wymagań niefunkcjonalnych](../wymagania/niefunkcjonalne.md) albo historyjki. Numery w nawiasach kwadratowych wskazują źródła na dole strony.

## Macierz

Oznaczenia punktów: MS — mocna strona, SS — słaba strona, SZ — szansa, ZG — zagrożenie.

| Czynniki | Pomagają osiągnąć cel | Przeszkadzają w osiągnięciu celu |
| --- | --- | --- |
| **Wewnętrzne (produkt)** | **Mocne strony**<br>MS1 Mapa, zdjęcie i data sprawdzenia w jednym miejscu, zamiast samego tekstu deklaracji (P-1, P-2, P-4; US2.2).<br>MS2 Każde wejście ma oznaczenie z datą i źródłem, także „brak danych” (US17.1, US17.3, decyzja 36).<br>MS3 Na mapie są tylko bariery przyjęte, a Weryfikator widzi podobne zgłoszenia w promieniu 10 m (US1.3, US10.1, M-7, decyzja 37).<br>MS4 Praca bez sieci bez utraty danych i bez cichego nadpisania zmiany (NFR-5, NFR-9, M-6).<br>MS5 Dostępność to mierzalne wymaganie: WCAG 2.1 AA, cele dotyku 48 dp, tekst z ikoną zamiast samego koloru (NFR-15, NFR-17, NFR-21).<br>MS6 Miejsce bariery to współrzędne i piętro, a nie opis słowny (P-9; US6.1, US7.2). Dane trafiają do QGIS (US13.1, NFR-43).<br>MS7 Zgłoszenie działa w każdym budynku i poza budynkami (BR-G1…BR-G4, NFR-48). | **Słabe strony**<br>SS1 Potrzeby N1–N9 i proces AS-IS to hipotezy, jeszcze bez wywiadów (R-7).<br>SS2 Mały zespół zależny od jednej osoby, już opóźniony wobec harmonogramu kursu (R-1, R-2).<br>SS3 Przy ścianach i w budynkach błąd GNSS sięga 5–15 m (R-4, hipoteza). Cel poniżej 1 m dotyczy terenu otwartego (NFR-1, M-2).<br>SS4 Plan piętra i obchód obejmują tylko 5 budynków szczegółowych (zakres w wizji; US16.2). Wejście bez danych z importu ma oznaczenie „brak danych” (US17.1).<br>SS5 Publikacja zależy od niewielu Weryfikatorów: nikt nie sprawdza własnego zgłoszenia i nie ma zwolnienia zgłoszenia (BR-4, BR-11, R-18, R-19).<br>SS6 Zgłoszenie szybkie wymaga konta, czyli dodatkowego kroku dla N3 (US14.2).<br>SS7 Tylko Android, bez publikacji w Google Play (wizja, „Poza zakresem”; NFR-41). |
| **Zewnętrzne (otoczenie)** | **Szanse**<br>SZ1 Ustawa nakłada na uczelnię obowiązki dostępności i raport co 4 lata [1], [2]. Sprawdzone dane mogą go zasilić (hipoteza z wizji).<br>SZ2 Plan PWr na lata 2025–2028 przewiduje audyty z dokumentacją fotograficzną [8], a żadna deklaracja nie ma zdjęcia [3] (P-1).<br>SZ3 Żaden kanał nie pokazuje bieżącego stanu barier: aktualności Przewodnika są puste [7] (P-5).<br>SZ4 Grupa docelowa jest blisko: Politechnika Trzeciego Wieku, około 200 osób w wieku 60+, biuro w C-13 [4], [5].<br>SZ5 Dane na start są dostępne: kontury budynków z OpenStreetMap [10] i plany pięter PWr [9] (R-11, R-13, R-14). | **Zagrożenia**<br>ZG1 Dane planów PWr są niespójne i niepełne, np. D-1 i D-2 nie mają w danych toalety dla OzN [9] (R-5). Usługa może przestać działać (R-11).<br>ZG2 Niejasne prawa: licencja planów pięter (R-14), share-alike ODbL (R-13), wizerunek na zdjęciach (R-8, NFR-25).<br>ZG3 Warunki pokazu: w sali brak sygnału GNSS i może zabraknąć sieci (R-3). Brak pewnego telefonu do testów (R-10).<br>ZG4 Darmowy hosting ma za mało pamięci dla serwera uwierzytelniania (R-12).<br>ZG5 Mało chętnych do wywiadów, zwłaszcza osób starszych (R-6).<br>ZG6 Deklaracje uczelni nie odsyłają do innych kanałów [3] (P-2). Na tym etapie nie kontaktujemy się z Działem Dostępności [6], więc nasze dane mogą zostać obok deklaracji (hipoteza). |

## Wnioski dla projektu

1. **Braki danych (ZG1, SS4) łagodzi zakres poziomowy.** Budynek bez planów działa jako podstawowy (BR-G3, NFR-48), a „brak danych” mówi wprost, czego nie wiemy (US17.1). Obchód 5 budynków (M-1) zamienia źródło „import” na „obchód” (US17.2, US17.3).
2. **Słabą dokładność GNSS (SS3, ZG3) łagodzą trzy funkcje ekranu C1.** Uśrednianie z widoczną dokładnością (US6.1), potwierdzenie budynku (US6.2) i ręczna korekta punktu (US6.4) ograniczają R-4. Pokaz w sali opiera się na trybie demonstracyjnym z nagraną trasą (R-3).
3. **Hipotezy (SS1, ZG5) są największym ryzykiem dla ekranów fazy B.** Persony i makiety oznaczamy jako hipotezy i poprawiamy po wywiadach (R-7). Szansa SZ4 nie zastępuje rekrutacji: prowadzą ją członkowie zespołu w swoim otoczeniu (R-6).
4. **Weryfikacja daje zaufanie (MS3), ale jest wąskim gardłem (SS5).** Liczbę zgłoszeń czekających widać w K1 (US11.1), a cel M-8 to mediana do 3 dni roboczych. Rolę RO-WER ma każdy członek zespołu (R-19).
5. **Wartość dla uczelni (SZ1, SZ2) to hipoteza, a ZG6 może ją osłabić.** Produkt musi bronić się wartością dla Odwiedzającego: wejściem, datą i zdjęciem (MS1, MS2; N1, N2). Eksport do QGIS (MS6) zostaje dodatkiem dla Koordynatora (N9).

## Źródła

1. Ustawa z dnia 19 lipca 2019 r. o zapewnianiu dostępności osobom ze szczególnymi potrzebami, metryka aktu: <https://api.sejm.gov.pl/eli/acts/DU/2019/1696>
2. Tekst ogłoszony ustawy (art. 3, 4, 6, 11): <https://eli.gov.pl/eli/DU/2019/1696/ogl/pol/pdf>
3. Deklaracje dostępności budynków PWr: <https://dostepnosc.pwr.edu.pl/dostepnosc-architektoniczna/deklaracje-dostepnosci-budynkow>
4. „Historyczna inauguracja. Ruszyła Politechnika Trzeciego Wieku”: <https://pwr.edu.pl/uczelnia/aktualnosci/seniorzy-na-pwr-ruszyla-politechnika-trzeciego-wieku-13912.html>
5. Politechnika Trzeciego Wieku, kontakt: <https://ptw.pwr.edu.pl/kontakt>
6. Dostępność na PWr, Dział Dostępności: <https://dostepnosc.pwr.edu.pl/>
7. Cyfrowy Przewodnik PWr, Aktualności: <https://przewodnik.pwr.edu.pl/pl/aktualnosci>
8. Plan działania na rzecz poprawy dostępności osobom ze szczególnymi potrzebami na lata 2025–2028, PWr (tabela 1 poz. 2–3): <https://bip.pwr.edu.pl/d/gGBUKOTtQKxVvBFh6GDxYBxZIQX14QU8OS39DQQpVVm8QRQxpXhsJFXkSGQcBBxNZBUREenlUWx8/plan_poprawy_dostepnosci_2025-28.pdf>
9. Plany pięter PWr w ArcGIS (usługi „Planbudynku_*”, liczby wind i toalet): <https://services3.arcgis.com/rrLByHvJ7zuJQDVw/arcgis/rest/services/>
10. Licencja danych OpenStreetMap: <https://www.openstreetmap.org/copyright>

Wszystkie źródła są już cytowane na stronach [Wizja produktu](../biznes/wizja.md) i [Ryzyka projektu](../biznes/ryzyka.md). Dostęp do źródeł: 27.10.2026.
