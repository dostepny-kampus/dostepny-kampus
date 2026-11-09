#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontroli bazy sprintu (baza-sprintu.sh <gałąź PR>): uruchamiany w klonie, widzi origin/main,
# origin/sprint/N-1 i origin/sprint/N. Zasady: poprzedni sprint jest w main; sprint/N wyrasta z main.
script="$(cd "$(dirname "$0")" && pwd)/baza-sprintu.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
previous_missing='poprzedni sprint nie jest w main'
not_from_main='nie wyrasta z main'

commit() { git commit --quiet --allow-empty -m "$1"; }

# Układ: origin (bare) i klon z gałęziami wypchniętymi do origin.
setup_origin() {
  git init --quiet --bare "$workdir/origin.git"
  git clone --quiet "$workdir/origin.git" "$workdir/repo" 2>/dev/null
  cd "$workdir/repo"
  git config user.name Test
  git config user.email test@example.com
  git checkout --quiet -b main
  commit start
}

publish() { git push --quiet origin "$@" 2>/dev/null; }

# Poprawny układ: sprint/3 scalony do main, sprint/4 odbity od main.
scenario_ok() {
  git checkout --quiet -b sprint/3
  commit "sprint 3"
  git checkout --quiet main
  git merge --quiet --no-ff sprint/3 -m "scalenie sprintu 3"
  git checkout --quiet -b sprint/4
  commit "sprint 4"
  publish main sprint/3 sprint/4
}

# Sprint 3 ma commity, których nie ma w main; sprint/4 odbity od main.
scenario_previous_not_in_main() {
  git checkout --quiet -b sprint/3
  commit "sprint 3"
  git checkout --quiet main
  commit "zmiana w main"
  git checkout --quiet -b sprint/4
  commit "sprint 4"
  publish main sprint/3 sprint/4
}

# Sprint 3 jest w main, ale sprint/4 odbito od sprint/3 przed scaleniem, a main poszedł dalej.
scenario_not_from_main() {
  git checkout --quiet -b sprint/3
  commit "sprint 3"
  git checkout --quiet -b sprint/4
  commit "sprint 4"
  git checkout --quiet main
  git merge --quiet --no-ff sprint/3 -m "scalenie sprintu 3"
  commit "poprawka w main"
  publish main sprint/3 sprint/4
}

# Pierwszy sprint: brak gałęzi poprzedniego sprintu.
scenario_first_sprint() {
  git checkout --quiet -b sprint/1
  commit "sprint 1"
  publish main sprint/1
}

# Gałąź spoza sprintów (np. numer sprintu, którego nie da się odczytać) nie podlega kontroli.
scenario_other_branch() {
  git checkout --quiet -b 12-zadanie
  commit "zadanie"
  publish main 12-zadanie
}

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <scenariusz> <gałąź PR>
run_case() {
  local name="$1" expected="$2" text="$3" scenario="$4" branch="$5" status=0 out
  rm -rf "$workdir/origin.git" "$workdir/repo"
  (
    setup_origin
    "$scenario"
  )
  out="$(cd "$workdir/repo" && bash "$script" "$branch" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "poprawna baza: poprzedni sprint w main, sprint/4 z main" 0 '' scenario_ok sprint/4
run_case "poprzedni sprint nie jest w main" 1 "$previous_missing" scenario_previous_not_in_main sprint/4
run_case "sprint/4 nie wyrasta z main" 1 "$not_from_main" scenario_not_from_main sprint/4
run_case "pierwszy sprint bez poprzedniego" 0 '' scenario_first_sprint sprint/1
run_case "gałąź spoza sprintów nie podlega kontroli" 0 '' scenario_other_branch 12-zadanie

exit "$((failures > 0 ? 1 : 0))"
