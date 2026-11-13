# Współtworzenie Dostępnego Kampusu

Ten dokument opisuje tylko reguły, które dziś sprawdzają skrypty i workflow w tym repozytorium. Przy każdej regule
podany jest plik, który ją egzekwuje; gdy reguła się zmienia, zmienia się ten plik i ten dokument w tym samym PR.

## Przebieg zmiany

1. **Issue.** Każda zmiana zaczyna się od issue z formularza [Zadanie](.github/ISSUE_TEMPLATE/zadanie.yml) albo
   [Błąd](.github/ISSUE_TEMPLATE/blad.yml). Issue ma być przypisane do osoby, która robi zmianę.
2. **Gałąź** `N-krotki-opis` (N = numer issue), założona od aktualnej gałęzi sprintu `sprint/N`.
   Początek nazwy sprawdza [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh); „od aktualnej gałęzi” to konwencja,
   a zgodność z gałęzią sprintu sprawdza kolejka scalania (patrz niżej).
3. **PR do `sprint/N`** z opisem według [szablonu](.github/pull_request_template.md) i linią `Closes #N`.
4. **Kontrole** (poniżej) muszą być zielone.
5. **Kolejka scalania** scala PR przez squash do gałęzi sprintu. Do `main` trafiają tylko całe sprinty.
   Kolejka uruchamia kontrole także na wyniku scalenia (zdarzenie `merge_group`, np. w [`android.yml`](.github/workflows/android.yml)
   i [`api.yml`](.github/workflows/api.yml)); squash i kolejka to ustawienia repozytorium, nie plik.
   Aby dodać PR do kolejki, użyj przycisku „Merge when ready” lub polecenia `gh pr merge N` po zalogowaniu
   na konto z prawem scalania w repozytorium.
   Gdy grupa ma to samo drzewo plików, co zielony przebieg na PR, i nie zmienia `.github/` względem bazy, ciężkie joby są
   pomijane ([`drzewo-grupy.yml`](.github/workflows/drzewo-grupy.yml)); zmiana `.github/` albo `sprint/N` wymusza pełny przebieg.
   Ryzyko: test niestabilny, który przypadkiem przeszedł na PR, nie jest w kolejce sprawdzany drugi raz.
6. Po scaleniu issue z opisu PR jest zamykane z komentarzem „Zrobione w #PR”
   ([`zamykanie-issue.yml`](.github/workflows/zamykanie-issue.yml), [`zamknij-issue.sh`](.github/scripts/zamknij-issue.sh)).

Aktualizacje zależności otwiera Dependabot do gałęzi sprintu ([`dependabot.yml`](.github/dependabot.yml)); PR Dependabota
jest zwolniony z kontroli „PR od issue”.
Przepływ strażnika jakości ([`straznik-jakosci.yml`](.github/workflows/straznik-jakosci.yml)) uruchamia się zdarzeniem
`pull_request_target`, więc GitHub bierze go z gałęzi domyślnej (`main`), a skrypt
[`straznik-jakosci.sh`](.github/scripts/straznik-jakosci.sh) z bazy PR (`sprint/N`). Dzięki temu PR nie podmieni ani
jednego, ani drugiego. Zmiana przepływu scalona do `sprint/N` działa dla kolejnych PR dopiero po przeniesieniu pliku do
`main`, a skrypt ze sprintu musi działać z przepływem z `main`; strażnik ostrzega o tym przy zmianie przepływu (#1173).
Strażnik jakości przepuszcza zmianę wersji istniejących pakietów npm przez Dependabota w `.github/commitlint/` i `api/`,
gdy manifest i odpowiadający mu plik lock są aktualizowane razem.

## Reguły PR i jak są egzekwowane

| Reguła | Egzekwuje |
|---|---|
| Opis zawiera `Closes #N` albo inną z form GitHuba: `close`, `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, `resolved` (wielkość liter bez znaczenia). Samo `#N` się nie liczy. | [`slowa-zamykajace.sh`](.github/scripts/slowa-zamykajace.sh) |
| Wskazane issue jest otwarte, jest issue (nie PR), jest przypisane autorowi PR. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh) |
| Nazwa gałęzi zaczyna się od `N-`, gdzie `#N` jest wśród zamykanych issue. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh) |
| PR ma etykietę obszaru, a jeśli zamykane issue ma etykiety obszaru, PR ma co najmniej jedną z nich. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh), obszary z [`labeler.yml`](.github/labeler.yml) |
| Jeden PR to jedna logiczna zmiana, najwyżej 300 zmienionych linii (bez danych, plików lock i kodu generowanego). | [`rozmiar-pr.sh`](.github/scripts/rozmiar-pr.sh), zadanie `rozmiar` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Tytuł PR i każdy commit mają postać Conventional Commits po polsku: `typ(zakres): opis`, typ jeden z `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`; opis małą literą, nagłówek do 100 znaków. | [`commitlint.config.mjs`](commitlint.config.mjs), zadanie `commity` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Commit scalający sprint do gałęzi robocznej ma domyślny komunikat („Merge remote-tracking branch …”) — commitlint go pomija; własny komunikat z typem spoza listy jest odrzucany. | [`commitlint.config.mjs`](commitlint.config.mjs) |
| Gałąź sprintu: poprzedni sprint jest w `main`, a `sprint/N` wyrasta z `main`. | [`baza-sprintu.sh`](.github/scripts/baza-sprintu.sh), zadanie `baza-sprintu` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| PR zmieniający workflow, konfigurację linterów albo pliki reguł jakości ma w opisie linię `Zmiana reguł jakości: <powód>` i etykietę `zmiana-regul-zatwierdzona` nadaną przez osobę inną niż autor PR; właściciel repozytorium może nadać ją także we własnym PR; nowe commity zdejmują etykietę. Wyciszenia analizy w kodzie są zabronione. | [`straznik-jakosci.sh`](.github/scripts/straznik-jakosci.sh), [`straznik-jakosci.yml`](.github/workflows/straznik-jakosci.yml) |
| Znacznik zadania w kodzie (`TODO`, `FIXME`, `HACK`, `@todo`; wielkość liter bez znaczenia) ma postać `ZNACZNIK(#N)` z otwartym issue. | [`todo-issue.sh`](.github/scripts/todo-issue.sh), zadanie `znaczniki-todo` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Każde rozwiązanie tymczasowe wskazuje otwarte issue z docelową poprawką: `@Ignore("… #N")` i `@Disabled("… #N")` (powód z numerem), tag wykluczający w `.feature` (`@do-zrobienia`, `@ignore`, `@skip`, `@wip`, `@pominiete`, `@tymczasowe`) z `#N` w tej samej linii albo w komentarzu nad nim, `@Tymczasowe("#N …")` (Java), `@RequiresOptIn` z `#N` w KDoc (Kotlin). Brak numeru, issue zamknięte albo nieistniejące to błąd; dotyczy to też issue zamykanego przez ten PR. | [`todo-issue.sh`](.github/scripts/todo-issue.sh), zadanie `znaczniki-todo` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Pliki z kodem mają nagłówek autorski. | [`naglowki.sh`](.github/scripts/naglowki.sh) |
| Brak zakazanych słów w gałęzi, tytule, opisie, commitach i dodanych liniach (dla Dependabota opis jest pomijany). | [`zakazane-slowa.sh`](.github/scripts/zakazane-slowa.sh) |
| Brak sekretów w commitach, licencje i podatności nowych zależności. | zadania `sekrety` i `zaleznosci` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Skrypty kontroli mają testy `*-test.sh`, uruchamiane razem. | zadanie `testy-skryptow` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |

### Kontrola „PR od issue”: tryb ostrzegawczy i wymagany

Kontrola działa w [`pr-od-issue.yml`](.github/workflows/pr-od-issue.yml) przez [`pr-od-issue-szkic.sh`](.github/scripts/pr-od-issue-szkic.sh)
i uruchamia się przy otwarciu, edycji, ponownym otwarciu PR oraz zmianie etykiet.

- **Ostrzegawczy** (domyślnie): uwagi tylko w logu jako ostrzeżenia, PR bez zmian.
- **Wymagany** (zmienna repozytorium `PR_OD_ISSUE_WYMAGANE=1`): PR z naruszeniem zamienia się w szkic, a komentarz
  z listą braków jest dodawany lub odświeżany. Po poprawie opisu oznacz PR jako gotowy.
- Błąd narzędzia (`gh`, `jq`) to osobny kod wyjścia 2: PR zostaje bez zmian i bez fałszywej listy braków.

## Etykiety, kamień milowy, przypisanie

Etykiety obszaru i typu oraz kamień milowy ustawiają workflow; nie ustawiaj ich ręcznie (ręczna zmiana zostanie
nadpisana przy kolejnej edycji PR).

| Etykieta | Znaczenie | Kto ustawia |
|---|---|---|
| `android`, `backend`, `api`, `ci`, `test`, `documentation` | obszar zmiany według zmienionych plików | automat: [`etykiety.yml`](.github/workflows/etykiety.yml) z [`labeler.yml`](.github/labeler.yml) |
| `bug`, `enhancement`, `documentation`, `test`, `ci` | typ według tytułu PR: `fix`→`bug`, `feat`→`enhancement`, `docs`→`documentation`, `test`→`test`, `ci` i `build`→`ci` | automat: [`etykieta-typu.sh`](.github/scripts/etykieta-typu.sh) |
| `dependencies`, `python`, `java`, `github_actions` | PR aktualizujący zależności danego ekosystemu | Dependabot |
| `accessibility`, `epik`, `good first issue` | bariera dostępności; zbiorcze zadanie fali; zadanie dla nowych osób | człowiek |
| `help wanted`, `question`, `duplicate`, `invalid`, `wontfix` | etykiety standardowe GitHuba do triażu issue | człowiek |

PR do `sprint/N` dostaje kamień milowy „Sprint N” i, jeśli nikt nie jest przypisany, swojego autora
([`sprint-pr.yml`](.github/workflows/sprint-pr.yml), [`sprint-pr.sh`](.github/scripts/sprint-pr.sh)).
Ręcznie ustawione pola zostają bez zmian.

## Przed otwarciem PR

- Uruchom lokalnie testy skryptów, które zmieniasz, np. `bash .github/scripts/pr-od-issue-test.sh`.
- Pełną budowę aplikacji i serwera wykonuje CI; wynik bierz z zadań PR.
- Wypchnij gałąź i otwórz PR do `sprint/N`; szablon opisu uzupełnia się sam.
