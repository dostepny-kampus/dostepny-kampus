#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Strażnik znaczników zadań, uruchamiany w katalogu repozytorium git. Znacznik TODO, FIXME, HACK lub @todo (wielkość liter bez znaczenia)
# musi mieć postać ZNACZNIK(#N), gdzie issue #N istnieje i jest otwarte. Skanuje pliki śledzone przez git
# o rozszerzeniach kt, kts, java, yml, yaml, sh, z pominięciem plików samego strażnika i strażnika komentarzy liniowych
# w android/ (zawierają przykłady).
# Rozwiązania tymczasowe (@Ignore, @Disabled, @Tymczasowe, @RequiresOptIn, tag wykluczający w .feature) też muszą wskazywać otwarte issue.
# Stan issue z gh (bez dostępu do issue skrypt kończy się kodem 2); w teście zamiast sieci: ISSUE_JSON_DIR z plikami <numer>.json (gh issue view --json state,url).
# PR_BODY (opis PR, opcjonalny): znacznik wskazujący issue zamykane przez ten PR (słowa zamykające z slowa-zamykajace.sh, np. Closes, Fixed) to naruszenie,
# bo po scaleniu issue się zamknie, a znacznik zostanie i zaczerwieni cudze PR.
# Kod wyjścia: 0 = czysto, 1 = naruszenie, 2 = błąd narzędzia gh lub jq (nie wiemy, czy issue jest w porządku).
found=0
declare -A states=()
# shellcheck source=slowa-zamykajace.sh
source "$(dirname "${BASH_SOURCE[0]}")/slowa-zamykajace.sh"
declare -A zamykane=()
while read -r numer; do
  [[ -n $numer ]] && zamykane[$numer]=1
done < <(numery_zamykanych_issue <<<"${PR_BODY:-}")
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

# Sprawdza issue wskazane przez znacznik: <ścieżka> <wiersz> <znacznik> <numer>.
check_issue() {
  local path="$1" line="$2" marker="$3" number="$4"
  load_state "$number"
  case "$state" in
    OPEN)
      if [[ -n ${zamykane[$number]:-} ]]; then
        report "$path" "$line" "${marker}: ten PR zamyka #${number}, więc znacznik zostałby z zamkniętym issue; usuń znacznik albo wskaż inne issue."
      fi
      ;;
    PULL) report "$path" "$line" "${marker}: to PR, nie issue." ;;
    MISSING) report "$path" "$line" "${marker}: issue nie istnieje." ;;
    *) report "$path" "$line" "${marker}: issue zamknięte." ;;
  esac
}

while IFS= read -r -d '' path; do
  [[ -f $path ]] || continue
  while IFS=: read -r line marker; do
    if [[ $marker =~ \(#([0-9]+)\)$ ]]; then
      check_issue "$path" "$line" "$marker" "${BASH_REMATCH[1]}"
    else
      report "$path" "$line" "${marker}: znacznik bez numeru issue; użyj ${marker}(#N)."
    fi
  done < <(grep -noPi '(?<![\w/.-])(?:TODO|FIXME|HACK|@todo)(?![\w]|-\w|\.\w)(?:\(#\d+\))?' "$path" || true)
done < <(git ls-files -z -- '*.kt' '*.kts' '*.java' '*.yml' '*.yaml' '*.sh' \
  ':!.github/scripts/todo-issue.sh' ':!.github/scripts/todo-issue-test.sh'   ':!android/app/src/test/kotlin/io/github/dostepnykampus/app/LineCommentScanner.kt'   ':!android/app/src/test/kotlin/io/github/dostepnykampus/app/NoLineCommentsTest.kt')

# Rozwiązania tymczasowe (decyzja 147): @Ignore/@Disabled/@Tymczasowe (powód w nawiasie, do 3 wierszy), tag wykluczający
# w .feature (#N w tej samej linii albo w komentarzu tuż nad nim) i @RequiresOptIn (#N w KDoc nad adnotacją) wskazują
# otwarte issue. Wynik awk: wiersz|znacznik|numer (numer pusty, gdy go brak). Dane testowe dokumentacji są pomijane.
read -r -d '' tymczasowe_awk <<'AWK' || true
function emit(n, tag, text,   num) {
  num = ""
  if (match(text, /#[0-9]+/)) num = substr(text, RSTART + 1, RLENGTH - 1)
  print n "|" tag "|" num
}
{ l[NR] = $0 }
END {
  for (i = 1; i <= NR; i++) {
    line = l[i]
    if (ext == "feature") {
      low = tolower(line)
      if (low !~ /^[[:space:]]*@/) continue
      if (!match(low, /(^|[[:space:]])@(do-zrobienia|ignore|skip|wip|pominiete|tymczasowe)([[:space:]]|$)/)) continue
      tag = substr(low, RSTART, RLENGTH)
      gsub(/[[:space:]]/, "", tag)
      text = line
      if (text !~ /#[0-9]+/ && i > 1 && l[i - 1] ~ /^[[:space:]]*#/) text = l[i - 1]
      emit(i, tag, text)
    } else if (line ~ /^[[:space:]]*@(Ignore|Disabled|Tymczasowe)([^[:alnum:]_]|$)/) {
      name = line
      sub(/^[[:space:]]*/, "", name)
      sub(/[^@[:alnum:]_].*$/, "", name)
      text = line
      if (line ~ /\(/) {
        text = line " " l[i + 1] " " l[i + 2]
        sub(/"\).*$/, "", text)
      }
      emit(i, name, text)
    } else if (line ~ /^[[:space:]]*@RequiresOptIn([^[:alnum:]_]|$)/) {
      j = i - 1
      while (j >= 1 && l[j] ~ /^[[:space:]]*(@|$)/) j--
      text = ""
      if (j >= 1 && l[j] ~ /\*\/[[:space:]]*$/) {
        k = j
        while (k >= 1 && l[k] !~ /\/\*\*/) k--
        for (m = (k < 1 ? 1 : k); m <= j; m++) text = text " " l[m]
      }
      emit(i, "@RequiresOptIn", text)
    }
  }
}
AWK
while IFS= read -r -d '' path; do
  [[ -f $path ]] || continue
  while IFS='|' read -r line marker number; do
    if [[ -z $number ]]; then
      report "$path" "$line" "${marker}: rozwiązanie tymczasowe bez numeru issue; wskaż otwarte issue (#N) w powodzie, tagu, komentarzu albo KDoc."
    else
      check_issue "$path" "$line" "$marker" "$number"
    fi
  done < <(awk -v ext="${path##*.}" "$tymczasowe_awk" "$path")
done < <(git ls-files -z -- '*.kt' '*.java' '*.feature' ':!docs/scripts/tests/dane')

if ((found == 0)); then
  echo "Każdy znacznik zadania wskazuje otwarte issue."
fi
exit "$found"
