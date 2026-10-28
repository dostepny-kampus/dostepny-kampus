# Dostępny Kampus

Aplikacja mobilna na Androida i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr). Informacja o barierach jest dziś rozproszona i nieaktualna. Zbierający zaznacza barierę na miejscu, Weryfikator ją sprawdza, a potem bariera pojawia się na mapie. Dzięki temu osoby starsze i osoby z ograniczoną mobilnością mogą zaplanować wizytę bez przeszkód.

## Układ repozytorium

| Katalog | Co tu jest | Stan |
| --- | --- | --- |
| `api/` | kontrakt API w OpenAPI, pisany przed kodem | powstanie w Sprincie 2 |
| `backend/` | serwer Spring Boot | powstanie w Sprincie 2 |
| `android/` | aplikacja w Kotlinie i Compose | powstanie w Sprincie 2 |
| `data/` | kontury budynków PWr z OpenStreetMap i skrypt eksportu | powstanie w Sprincie 2 |
| `specs/` | scenariusze Gherkin po polsku z tagami kryteriów, np. `@US1.1` | jest |
| `infra/` | konfiguracja środowiska lokalnego (`docker compose`) | powstanie w Sprincie 2 |
| `docs/` | dokumentacja MkDocs: biznes, wymagania, UX | jest |
| `.github/workflows/` | przepływy CI uruchamiane według zmienionych katalogów | powstanie w Sprincie 2 |

## Jak pracujemy

- Gałęzie: `main` ← `sprint/N` ← `feature/s<N>-<nazwa>`. Jedna gałąź to jedno zakończone zadanie.
- Każda zmiana trafia do gałęzi przez pull request z przeglądem i zielonymi kontrolami CI.
- Każdy plik z kodem (`.kt`, `.kts`, `.java`, `.py`, `.sh`, `.sql`) zaczyna się nagłówkiem autorskim, co sprawdza job `naglowki` w przepływie `pr-guard`.
- Commity w stylu [Conventional Commits](https://www.conventionalcommits.org/pl/v1.0.0/), np. `feat(api): …`.

## Dokumentacja

Jak zbudować i sprawdzić dokumentację, opisuje [`docs/README.md`](docs/README.md). Strona dokumentacji będzie publikowana na GitHub Pages z gałęzi `main`.

## Zespół

Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko.

## Licencja

Repozytorium, w tym kod i dokumentacja, jest na licencji [MIT](LICENSE). Wyjątkiem będą dane w `data/` pochodzące z OpenStreetMap: licencja [ODbL](https://opendatacommons.org/licenses/odbl/) z atrybucją „© OpenStreetMap contributors”.
