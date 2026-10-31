# Dostępny Kampus

Aplikacja mobilna na Androida i serwer do inwentaryzacji barier architektonicznych na kampusie Politechniki Wrocławskiej (PWr). Informacja o barierach jest dziś rozproszona i nieaktualna. Zbierający zaznacza barierę na miejscu, Weryfikator ją sprawdza, a potem bariera pojawia się na mapie. Dzięki temu osoby starsze i osoby z ograniczoną mobilnością mogą zaplanować wizytę bez przeszkód.

## Układ repozytorium

| Katalog | Co tu jest |
| --- | --- |
| `api/` | kontrakt API w OpenAPI, pisany przed kodem; kontrola `redocly` i zmian łamiących |
| `backend/` | serwer Spring Boot (Java 25, Maven, qulice), moduły sprawdzane przez Spring Modulith i ArchUnit |
| `android/` | aplikacja w Kotlinie i Compose: mapa kampusu bez internetu, wyszukiwanie budynku, lista budynków, instrukcja „Pomoc” |
| `data/` | kontury i wejścia budynków PWr z OpenStreetMap (`pwr-buildings.geojson`) i skrypt eksportu |
| `specs/` | scenariusze Gherkin po polsku z tagami kryteriów, np. `@US1.1` |
| `infra/` | środowisko lokalne: PostgreSQL z PostGIS w `docker compose` |
| `docs/` | dokumentacja MkDocs: biznes, wymagania, UX, architektura, makiety hi-fi |
| `.github/workflows/` | przepływy CI uruchamiane według zmienionych katalogów |

## Uruchomienie

Potrzebny jest JDK 25 (Temurin), jak w CI.

**Aplikacja na Androida** (Android 8.0 lub nowszy):

```sh
cd android
./gradlew assembleDebug
./gradlew installDebug
./gradlew check
```

`assembleDebug` buduje plik APK w `android/app/build/outputs/apk/debug/`. `installDebug` instaluje go na uruchomionym emulatorze albo telefonie z włączonym debugowaniem USB. `check` uruchamia testy, lintery i próg pokrycia Kover. Można też otworzyć katalog `android/` w Android Studio i uruchomić konfigurację `app`.

**Serwer:** baza według [`infra/README.md`](infra/README.md), potem `./mvnw verify` w katalogu `backend/`.

## Jak pracujemy

- Gałęzie: `main` ← `sprint/N` ← `feature/s<N>-<nazwa>`. Jedna gałąź to jedno zakończone zadanie.
- Każda zmiana trafia do gałęzi przez pull request z przeglądem i zielonymi kontrolami CI.
- Każdy plik z kodem (`.kt`, `.kts`, `.java`, `.py`, `.sh`, `.sql`) zaczyna się nagłówkiem autorskim, co sprawdza job `naglowki` w przepływie `pr-guard`.
- Kontrola `straznik-jakosci` blokuje wyciszenia analizy i pliki baseline; zmiana konfiguracji linterów, progów pokrycia lub CI wymaga w opisie PR linii `Zmiana reguł jakości: <powód>`, a wyciszeń w kodzie Androida nie dopuszcza w ogóle.
- Commity w stylu [Conventional Commits](https://www.conventionalcommits.org/pl/v1.0.0/), np. `feat(api): …`.

## Dokumentacja

Jak zbudować i sprawdzić dokumentację, opisuje [`docs/README.md`](docs/README.md). Strona dokumentacji jest publikowana na GitHub Pages z gałęzi `main`: <https://dostepny-kampus.github.io/dostepny-kampus/>.

## Zespół

Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko.

## Licencja

Repozytorium, w tym kod i dokumentacja, jest na licencji [MIT](LICENSE). Wyjątkiem są dane w `data/` pochodzące z OpenStreetMap: licencja [ODbL](https://opendatacommons.org/licenses/odbl/) z atrybucją „© OpenStreetMap contributors”.
