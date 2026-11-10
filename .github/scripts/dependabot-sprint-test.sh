#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test przestawiania PR Dependabota na bieżący sprint (dependabot-sprint.sh <numer PR>).
# Zamiast sieci test podstawia własne `gh`: zwraca listę gałęzi i kamieni milowych ze zmiennych,
# a `gh pr edit` dopisuje argumenty do dziennika, który test porównuje z oczekiwanym.
script="$(cd "$(dirname "$0")" && pwd)/dependabot-sprint.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'api repos/'*'/branches'*) printf '%b\n' "$FAKE_BRANCHES" ;;
  'api repos/'*'/milestones'*)
    # Otwarte kamienie milowe zawsze; zamknięte tylko, gdy zapytanie nie zawęża do state=open.
    printf '%b\n' "$FAKE_MILESTONES"
    [[ $2 == *'state=open'* ]] || printf '%b\n' "${FAKE_CLOSED_MILESTONES:-}" ;;
  'pr edit') shift 3; printf '%s\n' "$*" >>"$FAKE_LOG" ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

# run_case <nazwa> <autor> <gałęzie: \n> <kamienie: \n> <oczekiwany kod> <oczekiwane polecenia edit: \n> [<oczekiwany tekst>]
run_case() {
  local name="$1" author="$2" branches="$3" milestones="$4" code="$5" expected="$6" text="${7:-}" status=0 actual
  : >"$workdir/log.txt"
  PR_AUTHOR="$author" GITHUB_REPOSITORY=org/repo FAKE_BRANCHES="$branches" FAKE_MILESTONES="$milestones" \
    FAKE_LOG="$workdir/log.txt" PATH="$workdir/bin:$PATH" bash "$script" 7 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == code)) && [[ $actual == "$(printf '%b' "$expected")" ]] && { [[ -z $text ]] || grep -qF -- "$text" "$workdir/out.txt"; }; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status} (oczekiwano ${code}), polecenia „${actual//$'\n'/; }”, oczekiwano „${expected}”${text:+ z tekstem „$text”}"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

bot='dependabot[bot]'
branches='main\nsprint/3\nsprint/10\nsprint/4\nsprint/4-poprawka\nfeature/sprint/99'
milestones='Sprint 3\nSprint 4\nSprint 10'
run_case "najwyższe sprint/N liczbowo, nie alfabetycznie" "$bot" "$branches" "$milestones" 0 '--base sprint/10 --milestone Sprint 10' 'sprint/10'
run_case "tylko jeden sprint" "$bot" 'main\nsprint/4' "$milestones" 0 '--base sprint/4 --milestone Sprint 4'
run_case "inne gałęzie z sprint w nazwie są pomijane" "$bot" 'main\nsprint/4\nsprint/9-stara\nsprint/x' "$milestones" 0 '--base sprint/4 --milestone Sprint 4'
run_case "PR innego autora pominięty" ala "$branches" "$milestones" 0 '' 'nie jest PR Dependabota'
run_case "brak gałęzi sprintu: błąd, bez zmian" "$bot" 'main\nfeature/x' "$milestones" 1 '' 'Brak gałęzi sprintu'
run_case "brak kamienia milowego: błąd, bez zmian" "$bot" "$branches" 'Sprint 3\nSprint 4' 1 '' 'Brak kamienia milowego „Sprint 10”'
run_case "Sprint 1 nie pasuje do Sprint 10" "$bot" 'main\nsprint/1' 'Sprint 10' 1 '' 'Brak kamienia milowego „Sprint 1”'

FAKE_CLOSED_MILESTONES='Sprint 4' run_case "zamknięty kamień milowy to brak kamienia: błąd, bez zmian" "$bot" 'main
sprint/4' 'Sprint 3' 1 '' 'Brak kamienia milowego „Sprint 4”'

exit "$((failures > 0 ? 1 : 0))"
