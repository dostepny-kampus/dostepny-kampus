# Dokumentacja projektu Dostępny Kampus

Strona MkDocs z dokumentacją biznesową, wymaganiami i projektem UX. Strony są w `docs/`, skrypty kontroli w `scripts/`. Scenariusze Gherkin leżą w katalogu `specs/` w korzeniu repozytorium (`../specs/`), bo to testy uruchamiane w CI razem z kodem aplikacji i serwera. Polecenia poniżej uruchamiaj w tym katalogu (`docs/`).

## Środowisko

| Narzędzie | Do czego | Instalacja |
| --- | --- | --- |
| Python 3.11+ | MkDocs i skrypty | `pip install -r requirements.txt` |
| Node.js 20+ | markdownlint, bpmn-to-image | `npm ci` (tworzy `node_modules/`) |
| Java 17+ | PlantUML i LanguageTool | z pakietu systemu albo [Adoptium](https://adoptium.net/) |
| PlantUML | render `.puml` | plik `plantuml.jar` z [plantuml.com](https://plantuml.com/download) |
| Vale | reguły stylu z `styles/` | [vale.sh](https://vale.sh/docs/install/) |

LanguageTool przy pierwszym uruchomieniu pobiera lokalny serwer, więc potrzebuje sieci.

## Zmienne środowiskowe

| Zmienna | Znaczenie | Bez zmiennej |
| --- | --- | --- |
| `PLANTUML_JAR` | ścieżka do `plantuml.jar` | render `.puml` kończy się błędem z prośbą o ustawienie |
| `VALE_BIN` | ścieżka do programu `vale` | `vale` z `PATH` |
| `DK_NODE_MODULES` | katalog `node_modules` z narzędziami | `node_modules/` w tym katalogu |
| `JAVA`, `NODE` | inne programy `java` i `node` | `java` i `node` z `PATH` |

Przykład (bash):

```sh
export PLANTUML_JAR="$HOME/narzedzia/plantuml.jar"
export VALE_BIN="$HOME/narzedzia/vale"
```

## Kontrola

```sh
python scripts/verify.py
```

Skrypt uruchamia po kolei: aktualność obrazów, skróty, Vale, markdownlint, LanguageTool, identyfikatory, macierz śledzenia, kontrast tokenów i `mkdocs build --strict`. Wynik to tabela kroków PASS, WARN, FAIL albo SKIP. LanguageTool sprawdza tylko pliki zmienione względem `main` i daje najwyżej WARN; wszystkie strony sprawdza opcja `--lang-all`.

Opcja `--screens <katalog>` robi zrzuty każdej strony w szerokości 1280 i 390 px do katalogu poza repozytorium. Przed pierwszym użyciem zainstaluj przeglądarkę: `python -m playwright install chromium`.

## Diagramy i makiety

Źródła `.puml` i `.bpmn` leżą obok stron. Gotowe obrazy SVG i PNG są w repozytorium, w `docs/_generated/`, razem z manifestem `zrodla.sha256`. Manifest zawiera sha256 każdego źródła i plików `.iuml`, od których zależy.

```sh
python scripts/render.py           # renderuje tylko zmienione źródła
python scripts/render.py --force   # renderuje wszystko
python scripts/render.py --check   # tylko sprawdza aktualność
```

Po zmianie źródła uruchom `render.py` i dodaj do commita obrazy i manifest. Krok `obrazy` w `verify.py` zgłasza FAIL, gdy manifest nie zgadza się ze źródłami, brakuje obrazu albo strona odwołuje się do nieistniejącego pliku. Kontrola nie porównuje samych obrazów, bo PNG różnią się między systemami.

## Podgląd strony

```sh
python -m mkdocs serve
```
