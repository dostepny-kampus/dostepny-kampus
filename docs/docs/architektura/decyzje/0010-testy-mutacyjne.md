# ADR-0010: Testy mutacyjne serwera w PIT

Status: przyjęta. Data: 2026-11-12. Źródło: #900.

## Kontekst i problem

Próg pokrycia nie widzi testu, który wykonuje linię, ale nie sprawdza jej wyniku. Tak przeszedł test sumy kontrolnej importu ze stałą wartością (#845). Testy mutacyjne zmieniają kod i sprawdzają, czy któryś test to zauważy.

## Rozważone opcje

- PIT [1] z wtyczką `pitest-junit5-plugin` [2] dla JUnit 5 i 6.
- Arcmutate [3]: rozszerzenia PIT na licencji komercyjnej.
- Bez testów mutacyjnych.

## Decyzja

- PIT 1.30.2 z `pitest-junit5-plugin` 1.2.3 działa w jobie `serwer` przepływu `backend.yml`, po `verify`, w PR i w grupie kolejki scalania. Do 13 listopada 2026 roku działał w osobnym jobie po wypchnięciu na `sprint/*`; pomiar pokazał około 20 sekund analizy, więc powód kosztu odpadł (#1308).
- Zakres na start: pakiety `*.domain`, bez bazy danych. Pakiet `barrier.json` dojdzie po skróceniu czasu jego testów: lokalnie nie skończył się w 10 minut.
- Próg zabitych mutacji to dzisiejszy wynik, 100% (70 z 70), i może tylko rosnąć. Zmianę `pitest`, `mutationThreshold` albo `excludedClasses` w `pom.xml` zgłasza strażnik jakości.
- Wyjątek od ADR-0004: ostatnie wydanie `pitest-junit5-plugin` jest z 20 maja 2025 roku, starsze niż 12 miesięcy. Innej wtyczki PIT dla JUnit nie ma, a Arcmutate nie ma wolnej licencji. Wtyczka działa z Javą 25 i JUnit 6.0.3 projektu. Wyjątek przyjęty 12 listopada 2026 roku w #900.

## Konsekwencje

- Dobrze: test bez asercji zachowania w dziedzinie daje czerwony job `serwer` w PR, a raport PIT wskazuje linię i mutację.
- Gorzej: wtyczka bez nowych wydań może przestać działać po aktualizacji JUnit. Wtedy krok PIT pokaże błąd uruchomienia, a komunikat odróżni go od mutacji, która przeżyła.

## Źródła

1. [PIT](https://pitest.org/)
2. [pitest-junit5-plugin](https://github.com/pitest/pitest-junit5-plugin)
3. [Arcmutate](https://www.arcmutate.com/)
