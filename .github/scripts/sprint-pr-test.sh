#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test ustawiania kamienia milowego i autora na PR do gałęzi sprintu (sprint-pr.sh <numer PR>).
# Zamiast sieci test podstawia własne `gh`: zwraca stan PR i listę kamieni milowych z zmiennych,
# a `gh pr edit` dopisuje argumenty do dziennika, który test porównuje z oczekiwanym.
script="$(cd "$(dirname "$0")" && pwd)/sprint-pr.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'pr view')
    case "$5" in
      milestone) printf '%s\n' "$FAKE_PR_MILESTONE" ;;
      assignees) printf '%s\n' "$FAKE_PR_ASSIGNEES" ;;
      *) echo "nieoczekiwane pole: $5" >&2; exit 2 ;;
    esac ;;
  'api repos/'*) printf '%b\n' "$FAKE_MILESTONES" ;;
  'pr edit') shift 3; printf '%s\n' "$*" >>"$FAKE_LOG" ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

# run_case <nazwa> <baza> <autor> <kamień PR> <liczba przypisanych> <kamienie repo> <oczekiwane polecenia edit: \n> [<oczekiwany tekst>]
run_case() {
  local name="$1" base="$2" author="$3" ms="$4" assignees="$5" repo_ms="$6" expected="$7" text="${8:-}" status=0 actual
  : >"$workdir/log.txt"
  BASE_REF="$base" PR_AUTHOR="$author" GITHUB_REPOSITORY=org/repo FAKE_PR_MILESTONE="$ms" \
    FAKE_PR_ASSIGNEES="$assignees" FAKE_MILESTONES="$repo_ms" FAKE_LOG="$workdir/log.txt" \
    PATH="$workdir/bin:$PATH" bash "$script" 7 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == 0)) && [[ $actual == "$(printf '%b' "$expected")" ]] && { [[ -z $text ]] || grep -qF -- "$text" "$workdir/out.txt"; }; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, polecenia „${actual//$'\n'/; }”, oczekiwano „${expected}”${text:+ z tekstem „$text”}"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

ms='Sprint 3\nSprint 4'
run_case "sprint/4 dostaje Sprint 4 i autora" sprint/4 ala '' 0 "$ms" '--milestone Sprint 4\n--add-assignee ala'
run_case "main bez zmian" main ala '' 0 "$ms" '' 'nie jest do gałęzi sprintu'
run_case "brak kamienia milowego: komunikat, kod 0, autor i tak przypisany" sprint/9 ala '' 0 "$ms" '--add-assignee ala' 'Brak kamienia milowego „Sprint 9”'
run_case "ręczny kamień milowy zostaje" sprint/4 ala 'Sprint 3' 0 "$ms" '--add-assignee ala'
run_case "ręcznie przypisana osoba zostaje" sprint/4 ala '' 1 "$ms" '--milestone Sprint 4'
run_case "wszystko ustawione ręcznie, brak zmian" sprint/4 ala 'Sprint 4' 2 "$ms" ''
run_case "Dependabot: tylko kamień milowy" sprint/4 'dependabot[bot]' '' 0 "$ms" '--milestone Sprint 4' 'Dependabota'
run_case "Sprint 4 nie pasuje do Sprint 40" sprint/4 ala '' 0 'Sprint 40' '--add-assignee ala' 'Brak kamienia milowego'

exit "$((failures > 0 ? 1 : 0))"
