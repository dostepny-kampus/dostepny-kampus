#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test strażnika znaczników zadań (todo-issue.sh), uruchamianego w katalogu repozytorium git.
# Strażnik sprawdza pliki śledzone przez git o rozszerzeniach kt, kts, java, yml, yaml, sh (dokumentacja
# i inne pliki nie są skanowane; pomijane są też pliki samego strażnika, bo opisują znaczniki).
# Znacznik to TODO, FIXME, HACK lub @todo jako osobne słowo, bez względu na wielkość liter; poprawny ma postać ZNACZNIK(#N), gdzie
# issue #N istnieje i jest otwarte. Dane issue: pliki ISSUE_JSON_DIR/<numer>.json (gh issue view --json state,url);
# brak pliku oznacza, że issue nie istnieje. Kod wyjścia 0 = czysto, 1 = naruszenie, 2 = błąd narzędzia gh lub jq (tekst w wyjściu).
script="$(cd "$(dirname "$0")" && pwd)/todo-issue.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
repo="$workdir/repo"
failures=0
no_number='znacznik bez numeru issue'
closed='issue zamknięte'
missing='issue nie istnieje'
is_pull='to PR, nie issue'

issue_json() { printf '{"state":"%s","url":"https://github.com/o/r/%s/%s"}\n' "$2" "${3:-issues}" "$1" >"$workdir/issues/$1.json"; }

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <ścieżka pliku> <zawartość pliku>
run_case() {
  local name="$1" expected="$2" text="$3" path="$4" content="$5" status=0 out
  rm -rf "$repo" "$workdir/issues"
  mkdir -p "$repo" "$workdir/issues"
  issue_json 5 OPEN
  issue_json 6 CLOSED
  issue_json 7 OPEN pull
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
run_case "Kotlin: TODO(#7) wskazuje PR, nie issue" 1 "$is_pull" app/Main.kt '// TODO(#7): usuń obejście'
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

# Przypadki z podstawionymi narzędziami: fałszywe gh (dziennik wywołań w calls.txt) i jq w katalogu bin.
# run_tool_case <nazwa> <oczekiwany kod> <oczekiwany tekst> <liczba wywołań gh lub pusta> <treść gh> <treść jq lub pusta> <plik>
run_tool_case() {
  local name="$1" expected="$2" text="$3" calls="$4" gh_body="$5" jq_body="$6" content="$7" status=0 out
  rm -rf "$repo" "$workdir/bin"
  mkdir -p "$repo/app" "$workdir/bin"
  printf '#!/usr/bin/env bash
echo "$*" >>"%s/calls.txt"
%s
' "$workdir" "$gh_body" >"$workdir/bin/gh"
  chmod +x "$workdir/bin/gh"
  if [[ -n $jq_body ]]; then
    printf '#!/usr/bin/env bash
%s
' "$jq_body" >"$workdir/bin/jq"
    chmod +x "$workdir/bin/jq"
  fi
  : >"$workdir/calls.txt"
  (
    cd "$repo"
    git init --quiet
    git config user.name Test
    git config user.email test@example.com
    git config core.autocrlf false
    printf '%s
' "$content" >app/Main.kt
    git add app/Main.kt
    git commit --quiet -m baza
  )
  out="$(cd "$repo" && PATH="$workdir/bin:$PATH" bash "$script" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]] &&
    { [[ -z $calls ]] || (($(wc -l <"$workdir/calls.txt") == calls)); }; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected} z tekstem „${text}”, wywołań gh: $(wc -l <"$workdir/calls.txt")"
    printf '%s
' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

gh_ok="echo '{\"state\":\"OPEN\",\"url\":\"https://github.com/o/r/issues/5\"}'"
two_markers=$'// TODO(#5): a
// FIXME(#5): b'
run_tool_case "gh zawodzi: komunikat o narzędziu, kod 2" 2 'błąd narzędzia gh' '' 'echo "HTTP 500" >&2; exit 1' '' '// TODO(#5): a'
run_tool_case "gh: issue nie istnieje to naruszenie, nie błąd narzędzia" 1 "$missing" '' 'echo "GraphQL: Could not resolve to an issue" >&2; exit 1' '' '// TODO(#5): a'
run_tool_case "jq zawodzi: komunikat o narzędziu, kod 2" 2 'błąd narzędzia jq' '' "$gh_ok" 'exit 3' '// TODO(#5): a'
run_tool_case "stan issue pobierany raz dla dwóch znaczników" 0 '' 1 "$gh_ok" '' "$two_markers"

exit "$((failures > 0 ? 1 : 0))"
