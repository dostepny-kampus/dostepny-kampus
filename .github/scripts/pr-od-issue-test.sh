#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontroli „PR od issue” (pr-od-issue.sh). Skrypt czyta dane z gh; w teście zamiast sieci dostaje pliki:
#   PR_JSON_FILE   - odpowiedź `gh pr view --json author,closingIssuesReferences`,
#   ISSUE_JSON_DIR - katalog z plikami <numer>.json, każdy to odpowiedź `gh issue view --json state,assignees`.
# Kod wyjścia 0 = PR spełnia zasady, 1 = naruszenie (tekst w wyjściu).
script="$(cd "$(dirname "$0")" && pwd)/pr-od-issue.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
no_closes='brak Closes'
closed='issue zamknięte'
unassigned='issue nieprzypisane autorowi'

# pr_json <autor> [numer issue ...]
pr_json() {
  local author="$1" refs='' number
  shift
  for number in "$@"; do
    refs="${refs:+$refs,}{\"number\":$number}"
  done
  printf '{"author":{"login":"%s"},"closingIssuesReferences":[%s]}\n' "$author" "$refs" >"$workdir/pr.json"
}

# issue_json <numer> <OPEN|CLOSED> [osoba przypisana ...]
issue_json() {
  local number="$1" state="$2" people='' person
  shift 2
  for person in "$@"; do
    people="${people:+$people,}{\"login\":\"$person\"}"
  done
  printf '{"state":"%s","assignees":[%s]}\n' "$state" "$people" >"$workdir/issues/$number.json"
}

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <polecenie przygotowujące dane...>
run_case() {
  local name="$1" expected="$2" text="$3" status=0 out
  shift 3
  rm -rf "$workdir/issues" "$workdir/pr.json"
  mkdir "$workdir/issues"
  "$@"
  out="$(PR_JSON_FILE="$workdir/pr.json" ISSUE_JSON_DIR="$workdir/issues" bash "$script" 7 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

case_ok() { pr_json vlladevk 12; issue_json 12 OPEN vlladevk; }
case_no_closes() { pr_json vlladevk; }
case_closed_issue() { pr_json vlladevk 12; issue_json 12 CLOSED vlladevk; }
case_unassigned() { pr_json vlladevk 12; issue_json 12 OPEN; }
case_other_assignee() { pr_json vlladevk 12; issue_json 12 OPEN inna-osoba; }
case_author_among_assignees() { pr_json vlladevk 12; issue_json 12 OPEN inna-osoba vlladevk; }
case_dependabot() { pr_json 'dependabot[bot]'; }
case_second_issue_closed() { pr_json vlladevk 12 13; issue_json 12 OPEN vlladevk; issue_json 13 CLOSED vlladevk; }

run_case "poprawny PR: otwarte issue przypisane autorowi" 0 '' case_ok
run_case "brak Closes" 1 "$no_closes" case_no_closes
run_case "issue zamknięte" 1 "$closed" case_closed_issue
run_case "issue bez przypisanej osoby" 1 "$unassigned" case_unassigned
run_case "issue przypisane komuś innemu" 1 "$unassigned" case_other_assignee
run_case "autor jest jedną z przypisanych osób" 0 '' case_author_among_assignees
run_case "dependabot bez issue" 0 '' case_dependabot
run_case "dwa issue, jedno zamknięte" 1 "$closed" case_second_issue_closed

exit "$((failures > 0 ? 1 : 0))"
