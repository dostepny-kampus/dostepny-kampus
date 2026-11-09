#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Strażnik znaczników zadań, uruchamiany w katalogu repozytorium git. Znacznik TODO, FIXME, HACK lub @todo
# musi mieć postać ZNACZNIK(#N), gdzie issue #N istnieje i jest otwarte. Skanuje pliki śledzone przez git
# o rozszerzeniach kt, kts, java, yml, yaml, sh, z pominięciem plików samego strażnika (zawierają przykłady).
# Stan issue z gh; w teście zamiast sieci: ISSUE_JSON_DIR z plikami <numer>.json (gh issue view --json state).
found=0
declare -A states=()

issue_state() {
  local number="$1" json
  if [[ -z ${states[$number]:-} ]]; then
    if [[ -n ${ISSUE_JSON_DIR:-} ]]; then
      json="$(cat "$ISSUE_JSON_DIR/$number.json" 2>/dev/null)" || json=''
    else
      json="$(gh issue view "$number" --json state 2>/dev/null)" || json=''
    fi
    if [[ -z $json ]]; then
      states[$number]=MISSING
    else
      states[$number]="$(jq -r '.state' <<<"$json")"
    fi
  fi
  printf '%s' "${states[$number]}"
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
      case "$(issue_state "$number")" in
        OPEN) ;;
        MISSING) report "$path" "$line" "${marker}: issue nie istnieje." ;;
        *) report "$path" "$line" "${marker}: issue zamknięte." ;;
      esac
    else
      report "$path" "$line" "${marker}: znacznik bez numeru issue; użyj ${marker}(#N)."
    fi
  done < <(grep -noP '(?<!\w)(?:TODO|FIXME|HACK|@todo)(?!\w)(?:\(#\d+\))?' "$path" || true)
done < <(git ls-files -z -- '*.kt' '*.kts' '*.java' '*.yml' '*.yaml' '*.sh' \
  ':!.github/scripts/todo-issue.sh' ':!.github/scripts/todo-issue-test.sh')

if ((found == 0)); then
  echo "Każdy znacznik zadania wskazuje otwarte issue."
fi
exit "$found"
