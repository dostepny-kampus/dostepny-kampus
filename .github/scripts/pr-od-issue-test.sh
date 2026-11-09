#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontroli „PR od issue” (pr-od-issue.sh). Skrypt czyta dane z gh; w teście zamiast sieci dostaje pliki:
#   PR_JSON_FILE   - odpowiedź `gh pr view --json author,body`; issue skrypt wyszukuje w opisie
#                    (Closes|Fixes|Resolves #N, wielkość liter bez znaczenia), bo dla PR do gałęzi
#                    niedomyślnej GitHub nie wypełnia closingIssuesReferences,
#   ISSUE_JSON_DIR - katalog z plikami <numer>.json, każdy to odpowiedź `gh issue view --json state,assignees,url`.
#   PR_OD_ISSUE_OSTRZEGAJ=1 - tryb ostrzegawczy: te same uwagi jako ::warning, ale kod 0.
# Kod wyjścia 0 = PR spełnia zasady, 1 = naruszenie (tekst w wyjściu).
script="$(cd "$(dirname "$0")" && pwd)/pr-od-issue.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
no_closes='brak Closes'
closed='issue zamknięte'
unassigned='issue nieprzypisane autorowi'
is_pull='to PR, nie issue'
warn=0

# pr_json <autor> <opis PR (w JSON, nowy wiersz jako \n)>
pr_json() {
  printf '{"author":{"login":"%s"},"body":"%s"}\n' "$1" "$2" >"$workdir/pr.json"
}

# issue_json <numer> <OPEN|CLOSED> [osoba przypisana ...]; kind (issues|pull) mówi, czym jest numer
kind=issues
issue_json() {
  local number="$1" state="$2" people='' person
  shift 2
  for person in "$@"; do
    people="${people:+$people,}{\"login\":\"$person\"}"
  done
  printf '{"state":"%s","assignees":[%s],"url":"https://github.com/o/r/%s/%s"}\n' \
    "$state" "$people" "$kind" "$number" >"$workdir/issues/$number.json"
}

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <polecenie przygotowujące dane...>
run_case() {
  local name="$1" expected="$2" text="$3" status=0 out
  shift 3
  warn=0
  kind=issues
  rm -rf "$workdir/issues" "$workdir/pr.json"
  mkdir "$workdir/issues"
  "$@"
  out="$(PR_OD_ISSUE_OSTRZEGAJ="$warn" PR_JSON_FILE="$workdir/pr.json" ISSUE_JSON_DIR="$workdir/issues" bash "$script" 7 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

case_ok() { pr_json autor-pr 'Opis.\n\nCloses #12'; issue_json 12 OPEN autor-pr; }
case_lowercase() { pr_json autor-pr 'closes #12'; issue_json 12 OPEN autor-pr; }
case_fixes() { pr_json autor-pr 'Fixes #12'; issue_json 12 OPEN autor-pr; }
case_no_closes() { pr_json autor-pr 'Opis bez słowa kluczowego.'; }
case_bare_reference() { pr_json autor-pr 'Część #12, zob. też #13'; issue_json 12 OPEN autor-pr; }
case_closed_issue() { pr_json autor-pr 'Closes #12'; issue_json 12 CLOSED autor-pr; }
case_unassigned() { pr_json autor-pr 'Closes #12'; issue_json 12 OPEN; }
case_other_assignee() { pr_json autor-pr 'Closes #12'; issue_json 12 OPEN inna-osoba; }
case_author_among_assignees() { pr_json autor-pr 'Closes #12'; issue_json 12 OPEN inna-osoba autor-pr; }
case_dependabot() { pr_json 'dependabot[bot]' 'Notatki wydania'; }
case_two_issues_ok() {
  pr_json autor-pr 'Closes #12\nresolves #13'
  issue_json 12 OPEN autor-pr
  issue_json 13 OPEN autor-pr
}
case_second_issue_closed() {
  pr_json autor-pr 'Closes #12\nFixes #13'
  issue_json 12 OPEN autor-pr
  issue_json 13 CLOSED autor-pr
}

case_pull_number() { pr_json autor-pr 'Closes #12'; kind=pull; issue_json 12 OPEN autor-pr; }
case_warn_ok() { warn=1; case_ok; }
case_warn_closed() { warn=1; case_closed_issue; }
case_warn_no_closes() { warn=1; case_no_closes; }
case_warn_pull_number() { warn=1; case_pull_number; }

run_case "poprawny PR: otwarte issue przypisane autorowi" 0 '' case_ok
run_case "słowo kluczowe z małej litery" 0 '' case_lowercase
run_case "słowo kluczowe Fixes" 0 '' case_fixes
run_case "brak Closes" 1 "$no_closes" case_no_closes
run_case "samo #N bez słowa kluczowego nie liczy się" 1 "$no_closes" case_bare_reference
run_case "issue zamknięte" 1 "$closed" case_closed_issue
run_case "issue bez przypisanej osoby" 1 "$unassigned" case_unassigned
run_case "issue przypisane komuś innemu" 1 "$unassigned" case_other_assignee
run_case "autor jest jedną z przypisanych osób" 0 '' case_author_among_assignees
run_case "dependabot bez issue" 0 '' case_dependabot
run_case "kilka issue w opisie, wszystkie poprawne" 0 '' case_two_issues_ok
run_case "kilka issue w opisie, jedno zamknięte" 1 "$closed" case_second_issue_closed
run_case "numer jest PR, nie issue" 1 "$is_pull" case_pull_number
run_case "tryb ostrzegawczy: poprawny PR" 0 '' case_warn_ok
run_case "tryb ostrzegawczy: issue zamknięte" 0 '::warning' case_warn_closed
run_case "tryb ostrzegawczy: brak Closes" 0 '::warning' case_warn_no_closes
run_case "tryb ostrzegawczy: numer jest PR" 0 "$is_pull" case_warn_pull_number

exit "$((failures > 0 ? 1 : 0))"
