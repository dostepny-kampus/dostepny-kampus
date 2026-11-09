#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test wyliczania etykiety typu z tytułu PR (etykieta-typu.sh <tytuł> <etykiety>). Kod wyjścia 0 = wszystkie przypadki.
script="$(cd "$(dirname "$0")" && pwd)/etykieta-typu.sh"
failures=0

# run_case <nazwa> <tytuł> <obecne etykiety, \n> <oczekiwane polecenia, \n>
run_case() {
  local name="$1" title="$2" labels="$3" expected="$4" actual
  actual="$(bash "$script" "$title" "$(printf '%b' "$labels")")"
  if [[ $actual == "$(printf '%b' "$expected")" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: wynik „${actual//$'\n'/; }”, oczekiwano „${expected//\n/; }”"
    failures=$((failures + 1))
  fi
}

run_case "fix z zakresem → bug" 'fix(android): popraw obrót' '' 'dodaj:bug'
run_case "zmiana tytułu na feat zdejmuje bug i dodaje enhancement" 'feat: nowa mapa' 'bug\nandroid' 'zdejmij:bug\ndodaj:enhancement'
run_case "chore bez etykiety typu zdejmuje bug" 'chore: porządki' 'bug' 'zdejmij:bug'
run_case "chore bez etykiet nic nie robi" 'chore: porządki' '' ''
run_case "docs → documentation" 'docs(raporty): raport' '' 'dodaj:documentation'
run_case "test → test" 'test(backend): przypadek' '' 'dodaj:test'
run_case "ci → ci" 'ci(android): cache' '' 'dodaj:ci'
run_case "build → ci" 'build(deps): aktualizacja' '' 'dodaj:ci'
run_case "etykieta już jest, bez duplikatu" 'fix: x' 'bug' ''
run_case "typ z wykrzyknikiem" 'feat(api)!: zmiana kontraktu' '' 'dodaj:enhancement'
run_case "etykiety obszaru (ci, test, documentation) zostają przy innym typie" 'refactor: x' 'ci\ntest\ndocumentation' ''
run_case "tytuł bez typu" 'Poprawki różne' 'bug' 'zdejmij:bug'
run_case "typ wielkimi literami nie jest typem" 'Fix: x' '' ''

exit "$((failures > 0 ? 1 : 0))"
