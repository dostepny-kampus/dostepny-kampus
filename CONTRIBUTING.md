# Współtworzenie Dostępnego Kampusu

Ten dokument opisuje tylko reguły, które dziś sprawdzają skrypty i workflow w tym repozytorium. Przy każdej regule
podany jest plik, który ją egzekwuje; gdy reguła się zmienia, zmienia się ten plik i ten dokument w tym samym PR.
Jak prowadzić zmianę tam, gdzie skrypt niczego nie sprawdzi (kolejność pracy, podział na PR, gdzie kłaść kod,
opisy w kodzie), mówi [AGENTS.md](AGENTS.md).

## Przebieg zmiany

1. **Issue.** Każda zmiana zaczyna się od issue z formularza [Zadanie](.github/ISSUE_TEMPLATE/zadanie.yml) albo
   [Błąd](.github/ISSUE_TEMPLATE/blad.yml). Issue ma być przypisane do osoby, która robi zmianę.
2. **Gałąź** `N-krotki-opis` (N = numer issue), założona od aktualnej gałęzi sprintu `sprint/N`.
   Początek nazwy sprawdza [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh); „od aktualnej gałęzi” to konwencja,
   a zgodność z gałęzią sprintu sprawdza kolejka scalania (patrz niżej).
3. **PR do `sprint/N`** z opisem według [szablonu](.github/pull_request_template.md) i linią `Closes #N`.
4. **Kontrole** (poniżej) muszą być zielone.
5. **Kolejka scalania** scala PR przez squash do gałęzi sprintu. Do `main` trafiają tylko całe sprinty, a reguły `main`
   wymagają na głowie sprintu zielonego statusu `kontrola-sprintu` (punkt 7).
   PR z `sprint/N` tego repozytorium do `main` nie musi zamykać pojedynczego issue.
   Kolejka uruchamia kontrole także na wyniku scalenia (zdarzenie `merge_group`, np. w [`android.yml`](.github/workflows/android.yml)
   i [`api.yml`](.github/workflows/api.yml)); squash i kolejka to ustawienia repozytorium, nie plik.
   Android w grupie kolejki uruchamia tylko `testy` (kompilacja, lint, detekt, testy jednostkowe i architektury, Roborazzi);
   `buildHealth` biegnie na PR i w kontroli sprintu (punkt 7), APK i emulatory tylko w kontroli sprintu albo po ręcznym
   uruchomieniu `android.yml` z commitem (`gh workflow run android.yml -f sha=<commit>`); push po scaleniu tylko przekłada cache.
   Aby dodać PR do kolejki, użyj przycisku „Merge when ready” lub polecenia `gh pr merge N` po zalogowaniu
   na konto z prawem scalania w repozytorium.
   Gdy grupa ma to samo drzewo plików, co zielony przebieg na PR, i nie zmienia `.github/` względem bazy, ciężkie joby są
   pomijane ([`drzewo-grupy.yml`](.github/workflows/drzewo-grupy.yml)); zmiana `.github/` albo `sprint/N` wymusza pełny przebieg.
   Ryzyko: test niestabilny, który przypadkiem przeszedł na PR, nie jest w kolejce sprawdzany drugi raz.
6. Po scaleniu issue z opisu PR jest zamykane z komentarzem „Zrobione w #PR”
   ([`zamykanie-issue.yml`](.github/workflows/zamykanie-issue.yml), [`zamknij-issue.sh`](.github/scripts/zamknij-issue.sh)).
7. **Kontrola sprintu** ([`kontrola-sprintu.yml`](.github/workflows/kontrola-sprintu.yml),
   [`kontrola-sprintu-cel.sh`](.github/scripts/kontrola-sprintu-cel.sh)) trzy razy na godzinę bierze głowę każdej gałęzi `sprint/N`,
   której nie ma jeszcze w `main`, uruchamia na niej pełny przebieg [`android.yml`](.github/workflows/android.yml) (APK,
   emulatory, pełny test systemowy, `buildHealth`; bez `testy`, które głowa przeszła w kolejce) i zapisuje wynik
   jako status commita `kontrola-sprintu`. Głowę z tym statusem pomija; gdy od ostatniego sprawdzonego commita
   nie zmienił się Android, serwer, `infra/` ani dane budynków, stawia zielony status bez budowy. Ręczne uruchomienie
   z nazwą gałęzi sprawdza ją zawsze.
   Pierwsza porażka daje status `error` i ponowienie w następnym przebiegu; druga — status `failure` i jedno issue
   `sprint-czerwony` dla gałęzi z nieudanymi zadaniami i PR scalonymi od ostatniego wyniku; kolejne porażki dopisują
   komentarz, a zielony wynik zamyka issue ([`kontrola-sprintu-wynik.sh`](.github/scripts/kontrola-sprintu-wynik.sh)).

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

Lekkie kontrole z [`pr-guard.yml`](.github/workflows/pr-guard.yml) to kroki jednego zadania `pr-guard`, więc przebieg
zajmuje jedną maszynę. Każdy krok biegnie także po porażce wcześniejszego: wynik pokazuje naraz wszystkie czerwone kontrole.

| Reguła | Egzekwuje |
|---|---|
| Opis zawiera `Closes #N` albo inną z form GitHuba: `close`, `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, `resolved` (wielkość liter bez znaczenia). Samo `#N` się nie liczy. | [`slowa-zamykajace.sh`](.github/scripts/slowa-zamykajace.sh) |
| Wskazane issue jest otwarte, jest issue (nie PR), jest przypisane autorowi PR. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh) |
| Nazwa gałęzi zaczyna się od `N-`, gdzie `#N` jest wśród zamykanych issue. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh) |
| Nazwa gałęzi PR ma format `N-krotki-opis`: numer zamykanego issue, łącznik i opis z małych liter ASCII, cyfr i łączników. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh) |
| PR ma etykietę obszaru, a jeśli zamykane issue ma etykiety obszaru, PR ma co najmniej jedną z nich. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh), obszary z [`labeler.yml`](.github/labeler.yml) |
| PR do `sprint/N` zamyka tylko issue z kamieniem milowym `Sprint N`. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh) |
| Zamykane issue (poza epikiem) ma dokładnie jedną etykietę `złożoność: 1 …` – `złożoność: 4 …`; issue „do podziału” trzeba najpierw rozbić. Kontrola nie rusza sama po zmianie etykiety issue: po ocenie edytuj PR albo ponów przebieg. | [`pr-od-issue.sh`](.github/scripts/pr-od-issue.sh), skala w sekcji „Ocena złożoności” |
| Jeden PR to jedna logiczna zmiana, najwyżej 300 zmienionych linii (bez danych, plików lock i kodu generowanego). | [`rozmiar-pr.sh`](.github/scripts/rozmiar-pr.sh), krok `rozmiar` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Tytuł PR i każdy commit mają postać Conventional Commits po polsku: `typ(zakres): opis`, typ jeden z `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`; opis małą literą, nagłówek do 100 znaków. | [`commitlint.config.mjs`](commitlint.config.mjs), krok `commity` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Commit scalający sprint do gałęzi robocznej ma domyślny komunikat („Merge remote-tracking branch …”) — commitlint go pomija; własny komunikat z typem spoza listy jest odrzucany. | [`commitlint.config.mjs`](commitlint.config.mjs) |
| Gałąź sprintu: poprzedni sprint jest w `main`, a `sprint/N` wyrasta z `main`. | [`baza-sprintu.sh`](.github/scripts/baza-sprintu.sh), krok `baza-sprintu` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| PR zmieniający workflow, konfigurację linterów albo pliki reguł jakości ma w opisie linię `Zmiana reguł jakości: <powód>` i etykietę `zmiana-regul-zatwierdzona` nadaną przez osobę inną niż autor PR; osoby ustalające reguły (lista `RULES_OWNERS` w [`straznik-jakosci.yml`](.github/workflows/straznik-jakosci.yml)) mogą nadać ją także we własnym PR; nowe commity zdejmują etykietę. Wyciszenia analizy w kodzie są zabronione. | [`straznik-jakosci.sh`](.github/scripts/straznik-jakosci.sh), [`straznik-jakosci.yml`](.github/workflows/straznik-jakosci.yml) |
| Znacznik zadania w kodzie (`TODO`, `FIXME`, `HACK`, `@todo`; wielkość liter bez znaczenia) ma postać `ZNACZNIK(#N)` z otwartym issue. | [`todo-issue.sh`](.github/scripts/todo-issue.sh), krok `znaczniki-todo` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Każde rozwiązanie tymczasowe wskazuje otwarte issue z docelową poprawką: `@Ignore("… #N")` i `@Disabled("… #N")` (powód z numerem), tag wykluczający w `.feature` (`@do-zrobienia`, `@ignore`, `@skip`, `@wip`, `@pominiete`, `@tymczasowe`) z `#N` w tej samej linii albo w komentarzu nad nim, `@Tymczasowe("#N …")` (Java), `@RequiresOptIn` z `#N` w KDoc (Kotlin). Brak numeru, issue zamknięte albo nieistniejące to błąd; dotyczy to też issue zamykanego przez ten PR. | [`todo-issue.sh`](.github/scripts/todo-issue.sh), krok `znaczniki-todo` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Każda asercja w testach serwera ma komunikat: `assert…` z JUnit kończy się komunikatem, `assertThat` z Hamcrest zaczyna się powodem; `assertAll` tylko grupuje. Klasy z dzisiejszymi naruszeniami są na liście wyjątków z numerem issue, a lista tylko maleje. | [`AssertionMessageArchitectureIT`](backend/src/test/java/io/github/dostepnykampus/AssertionMessageArchitectureIT.java) |
| Nazwa testu Androida to zdanie o zachowaniu od czasownika w czasie teraźniejszym, w camelCase od małej litery, bez podkreśleń i słowa `test` (np. `showsNewestSubmissionFirst`); identyfikatory wymagań stoją tylko w linii `Wymagania:` opisu. Moduły z dzisiejszymi naruszeniami są na liście wyjątków z numerem issue, a lista tylko maleje. | [`TestNameArchitectureTest`](android/app/src/test/kotlin/io/github/dostepnykampus/app/TestNameArchitectureTest.kt) |
| Każda asercja w testach aplikacji ma komunikat: JUnit 5 kończy się komunikatem, JUnit 4 zaczyna się komunikatem; `assertAll` tylko grupuje. Pliki z dzisiejszymi naruszeniami są na liście wyjątków, a lista tylko maleje. | [`AssertionMessage`](android/detekt-rules/src/main/kotlin/io/github/dostepnykampus/detekt/AssertionMessage.kt) |
| Pliki z kodem mają nagłówek autorski. | [`naglowki.sh`](.github/scripts/naglowki.sh) |
| Brak zakazanych słów w gałęzi, tytule, opisie, commitach i dodanych liniach (dla Dependabota opis jest pomijany). Commity jednego autora w gałęzi PR: cudzy commit (np. scalenie gałęzi sprintu przez inną osobę) daje w kolejce zakazany dopisek GitHuba o współautorze, więc cudzą gałąź aktualizuje jej autor albo `git rebase`. | [`zakazane-slowa.sh`](.github/scripts/zakazane-slowa.sh) |
| Brak sekretów w commitach, licencje i podatności nowych zależności. | kroki `sekrety` i `zaleznosci` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |
| Skrypty kontroli mają testy `*-test.sh`, uruchamiane razem. | krok `testy-skryptow` zadania `pr-guard` w [`pr-guard.yml`](.github/workflows/pr-guard.yml) |

### Kontrola „PR od issue”: tryb ostrzegawczy i wymagany

Kontrola działa w [`pr-od-issue.yml`](.github/workflows/pr-od-issue.yml) przez [`pr-od-issue-szkic.sh`](.github/scripts/pr-od-issue-szkic.sh)
i uruchamia się przy otwarciu, edycji, ponownym otwarciu PR oraz zmianie etykiet.

- **Wymagany** (zawsze w CI): PR z naruszeniem dostaje komentarz z listą braków (dodawany lub odświeżany),
  a check jest czerwony. PR nie zmienia się w szkic. Po poprawie opisu check przechodzi przy edycji PR.
- **Ostrzegawczy** (`PR_OD_ISSUE_WYMAGANE=0` przy ręcznym uruchomieniu skryptu): uwagi tylko w logu, PR bez zmian.
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
| `złożoność: 1 mechaniczna` … `złożoność: 4 projektowa`, `złożoność: do podziału` | poziom złożoności issue według sekcji oceny (niżej) | automat: [`zlozonosc.sh`](.github/scripts/zlozonosc.sh), [`zlozonosc.yml`](.github/workflows/zlozonosc.yml) |

### Ocena złożoności

Issue zadania i błędu (poza epikiem) ma sekcje oceny z formularza; issue zakładane bez formularza ma te same
nagłówki `### …`. Poziom to najwyższy z trzech czynników; każde ryzyko inne niż „brak” podnosi go o 1.

| Czynnik | 1 | 2 | 3 | 4 |
|---|---|---|---|---|
| Rozwiązanie | opisane w issue | jest wzorzec w kodzie | do wyboru spośród znanych | do zaprojektowania |
| Zasięg | jedno miejsce | jeden moduł | kilka modułów | cała aplikacja albo kontrakt między modułami |
| Weryfikacja | CI sprawdza w pełni | CI i krótki przegląd | częściowo ręcznie | tylko ręcznie |

Ryzyko: `brak` albo dowolne z `reguły albo CI`, `bezpieczeństwo`, `migracja danych`, `publiczne API`.
Odniesienia (gdzie wzorzec, które moduły, jakie ryzyko) są zawsze wymagane, „Warianty i otwarte pytania”
od poziomu 3. Poziomy: 1 mechaniczna — wiadomo co i jak, CI sprawdzi; 2 według wzorca — podobne rozwiązanie
już jest w kodzie; 3 wymaga analizy — trzeba zrozumieć istniejący projekt; 4 projektowa — potrzebna decyzja
projektowa albo nowa abstrakcja; powyżej 4 „do podziału” — rozbij issue na podzadania. Ocenę wystawia autor
issue; wykonawca poprawia ją tylko przy oczywistym błędzie. Przy błędnym opisie automat nie nadaje etykiety
i zostawia jeden komentarz z listą braków, usuwany po poprawie. Zdarzenia issue działają z gałęzi `main`.

PR do `sprint/N` dostaje kamień milowy „Sprint N” i, jeśli nikt nie jest przypisany, swojego autora
([`sprint-pr.yml`](.github/workflows/sprint-pr.yml), [`sprint-pr.sh`](.github/scripts/sprint-pr.sh)).
Ręcznie ustawione pola zostają bez zmian.

## Kontrola zawieszona w stanie „in_progress”

Zdarza się, że job kończy wszystkie kroki, przebieg ma `completed`, a check run tego jobu zostaje w stanie `in_progress`
(z ustawionym `completed_at` i `conclusion`). PR czeka wtedy na wymaganą kontrolę bez końca, a kolejka scalania go nie
przyjmuje (#1006). Przyczyna leży po stronie GitHub Actions.

Rozpoznanie: check run w toku, który ma już datę zakończenia.

```sh
sha="$(gh pr view <numer> --json headRefOid --jq .headRefOid)"
gh api "repos/{owner}/{repo}/commits/${sha}/check-runs" --paginate   --jq '.check_runs[] | select(.status == "in_progress" and .completed_at != null) | "\(.name) \(.html_url)"'
```

Naprawa: ponów przebieg z tym jobem, np. `gh run rerun <id przebiegu> --failed` albo przyciskiem „Re-run all jobs”;
nowa próba tworzy nowe check runy, a wynik z poprzedniej przestaje się liczyć. Każde wystąpienie dopisz w #1006
z odnośnikiem do jobu.

## Przed otwarciem PR

- Uruchom lokalnie testy skryptów, które zmieniasz, np. `bash .github/scripts/pr-od-issue-test.sh`.
- Testy, analizę i pełną budowę aplikacji i serwera wykonuje CI; wynik bierz z zadań PR ([AGENTS.md](AGENTS.md), punkt 8).
- Wypchnij gałąź i otwórz PR do `sprint/N`; szablon opisu uzupełnia się sam.
