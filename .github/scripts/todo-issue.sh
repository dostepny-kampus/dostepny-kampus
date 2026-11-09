#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Strażnik znaczników zadań, uruchamiany w katalogu repozytorium git. Znacznik TODO, FIXME, HACK lub @todo (wielkość liter bez znaczenia)
# musi mieć postać ZNACZNIK(#N), gdzie issue #N istnieje i jest otwarte. Skanuje pliki śledzone przez git
# o rozszerzeniach kt, kts, java, yml, yaml, sh, z pominięciem plików samego strażnika (zawierają przykłady).
# Stan issue z gh (bez dostępu do issue skrypt kończy się kodem 2); w teście zamiast sieci: ISSUE_JSON_DIR z plikami <numer>.json (gh issue view --json state,url).
# Kod wyjścia: 0 = czysto, 1 = naruszenie, 2 = błąd narzędzia gh lub jq (nie wiemy, czy issue jest w porządku).
found=0
declare -A states=()
state=''

tool_error() {
  echo "::error title=Błąd narzędzia::błąd narzędzia $1: $2"
  exit 2
}

# Ustawia $state (OPEN, CLOSED, PULL albo MISSING); każde issue pytamy raz, bez podpowłoki, żeby pamięć działała.
load_state() {
  local number="$1" json err_file
  if [[ -n ${states[$number]:-} ]]; then
    state="${states[$number]}"
    return
  fi
  if [[ -n ${ISSUE_JSON_DIR:-} ]]; then
    if [[ -f $ISSUE_JSON_DIR/$number.json ]]; then
      json="$(cat "$ISSUE_JSON_DIR/$number.json")"
    else
      json=''
    fi
  else
    err_file="$(mktemp)"
    if ! json="$(gh issue view "$number" --json state,url 2>"$err_file")"; then
      if grep -qi 'could not resolve\|not found' "$err_file"; then
        json=''
      else
        tool_error gh "gh issue view ${number}: $(head -n 1 "$err_file")"
      fi
    fi
    rm -f "$err_file"
  fi
  if [[ -z $json ]]; then
    state=MISSING
  else
    # gh issue view zwraca też dane PR; numer PR nie jest issue.
    url="$(jq -r '.url // ""' <<<"$json")" || tool_error jq "nie odczytano url issue #${number}"
    if [[ $url == */pull/* ]]; then
      state=PULL
    else
      state="$(jq -r '.state' <<<"$json")" || tool_error jq "nie odczytano stanu issue #${number}"
    fi
  fi
  states[$number]="$state"
}

report() {
  echo "::error file=$1,line=$2,title=Znacznik zadania::$3"
  found=1
}

while IFS= read -r -d '' path; do
  [[ -f $path ]] || continue
  while IFS=: read -r line marker; do
    if [[ $marker =~ \(#([0-9]+)\)$ ]]; then
      number="${BASH_REMATCH[1]}"
      load_state "$number"
      case "$state" in
        OPEN) ;;
        PULL) report "$path" "$line" "${marker}: to PR, nie issue." ;;
        MISSING) report "$path" "$line" "${marker}: issue nie istnieje." ;;
        *) report "$path" "$line" "${marker}: issue zamknięte." ;;
      esac
    else
      report "$path" "$line" "${marker}: znacznik bez numeru issue; użyj ${marker}(#N)."
    fi
  done < <(grep -noPi '(?<!\w)(?:TODO|FIXME|HACK|@todo)(?!\w)(?:\(#\d+\))?' "$path" || true)
done < <(git ls-files -z -- '*.kt' '*.kts' '*.java' '*.yml' '*.yaml' '*.sh' \
  ':!.github/scripts/todo-issue.sh' ':!.github/scripts/todo-issue-test.sh')

if ((found == 0)); then
  echo "Każdy znacznik zadania wskazuje otwarte issue."
fi
exit "$found"
