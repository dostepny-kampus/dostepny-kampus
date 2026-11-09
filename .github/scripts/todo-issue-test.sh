#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test strażnika znaczników zadań (todo-issue.sh), uruchamianego w katalogu repozytorium git.
# Strażnik sprawdza pliki śledzone przez git o rozszerzeniach kt, kts, java, yml, yaml, sh (dokumentacja
# i inne pliki nie są skanowane; pomijane są też pliki samego strażnika, bo opisują znaczniki).
# Znacznik to TODO, FIXME, HACK lub @todo jako osobne słowo, bez względu na wielkość liter; poprawny ma postać ZNACZNIK(#N), gdzie
# issue #N istnieje i jest otwarte. Dane issue: pliki ISSUE_JSON_DIR/<numer>.json (gh issue view --json state);
# brak pliku oznacza, że issue nie istnieje. Kod wyjścia 0 = czysto, 1 = naruszenie (tekst w wyjściu).
script="$(cd "$(dirname "$0")" && pwd)/todo-issue.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
repo="$workdir/repo"
failures=0
no_number='znacznik bez numeru issue'
closed='issue zamknięte'
missing='issue nie istnieje'

issue_json() { printf '{"state":"%s"}\n' "$2" >"$workdir/issues/$1.json"; }

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <ścieżka pliku> <zawartość pliku>
run_case() {
  local name="$1" expected="$2" text="$3" path="$4" content="$5" status=0 out
  rm -rf "$repo" "$workdir/issues"
  mkdir -p "$repo" "$workdir/issues"
  issue_json 5 OPEN
  issue_json 6 CLOSED
  (
    cd "$repo"
    git init --quiet
    git config user.name Test
    git config user.email test@example.com
    git config core.autocrlf false
    mkdir -p "$(dirname "$path")"
    printf '%s\n' "$content" >"$path"
    git add -A
    git commit --quiet -m baza
  )
  out="$(cd "$repo" && ISSUE_JSON_DIR="$workdir/issues" bash "$script" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "Kotlin: TODO(#5) z otwartym issue" 0 '' app/Main.kt '// TODO(#5): obsłuż brak sieci'
run_case "Java: TODO(#5) z otwartym issue" 0 '' backend/Main.java '    // TODO(#5): obsłuż brak sieci'
run_case "YAML: TODO(#5) z otwartym issue" 0 '' infra/compose.yml '# TODO(#5): dodaj kontrolę zdrowia'
run_case "shell: TODO(#5) z otwartym issue" 0 '' scripts/run.sh '# TODO(#5): sprawdź kod wyjścia'
run_case "Kotlin: TODO: bez numeru" 1 "$no_number" app/Main.kt '// TODO: obsłuż brak sieci'
run_case "Kotlin: samo TODO bez dwukropka" 1 "$no_number" app/Main.kt '// TODO obsłuż brak sieci'
run_case "Java: FIXME bez numeru" 1 "$no_number" backend/Main.java '// FIXME popraw'
run_case "YAML: HACK bez numeru" 1 "$no_number" infra/compose.yml '# HACK: obejście'
run_case "shell: @todo bez numeru" 1 "$no_number" scripts/run.sh '# @todo wróć do tego'
run_case "Kotlin: TODO bez numeru po numerze w innym miejscu" 1 "$no_number" app/Main.kt 'val x = 1 // TODO: sprawdź (#5)'
run_case "Kotlin: TODO(#6) z zamkniętym issue" 1 "$closed" app/Main.kt '// TODO(#6): usuń obejście'
run_case "Java: FIXME(#99) z nieistniejącym issue" 1 "$missing" backend/Main.java '// FIXME(#99): popraw'
run_case "Kotlin: todo: małymi literami bez numeru" 1 "$no_number" app/Main.kt '// todo: później'
run_case "Java: Fixme bez numeru" 1 "$no_number" backend/Main.java '// Fixme coś'
run_case "YAML: hack bez numeru" 1 "$no_number" infra/compose.yml '# hack: obejście'
run_case "shell: @Todo bez numeru" 1 "$no_number" scripts/run.sh '# @Todo wróć'
run_case "Kotlin: todo(#6) z zamkniętym issue" 1 "$closed" app/Main.kt '// todo(#6): usuń'
run_case "Kotlin: Todo(#5) z otwartym issue" 0 '' app/Main.kt '// Todo(#5): obsłuż brak sieci'
run_case "Kotlin: identyfikatory Todos i todoItem nie są znacznikami" 0 '' app/Main.kt 'val Todos = todoItem()'
run_case "Kotlin: identyfikator todoList nie jest znacznikiem" 0 '' app/Main.kt 'val todoList = emptyList<String>()'
run_case "Kotlin: identyfikator TODOS i MyTODO nie są znacznikami" 0 '' app/Main.kt 'val TODOS = MyTODO()'
run_case "Markdown poza zakresem skanowania" 0 '' docs/notatki.md 'TODO: opisać'
run_case "pliki samego strażnika są pomijane" 0 '' .github/scripts/todo-issue.sh '# TODO: przykład znacznika'
run_case "plik tekstowy poza zakresem skanowania" 0 '' NOTES.txt 'FIXME bez numeru'

exit "$((failures > 0 ? 1 : 0))"
