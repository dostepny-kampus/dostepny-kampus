#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test zamykania issue po scaleniu PR do gałęzi sprintu (zamknij-issue.sh <numer PR>).
# Zamiast sieci test podstawia własne `gh`: `gh pr view` zwraca opis PR z pliku, a `gh issue close`
# dopisuje numer issue i komentarz do dziennika, który test porównuje z oczekiwanym.
script="$(cd "$(dirname "$0")" && pwd)/zamknij-issue.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cp "$(dirname "$script")/falszywe-gh-zamykanie" "$workdir/bin/gh"
chmod +x "$workdir/bin/gh"

# run_case <nazwa> <opis PR> <oczekiwany dziennik: wiersze numer|komentarz, rozdzielone \n>
run_case() {
  local name="$1" body="$2" expected="$3" pulls="${4:-}" text="${5:-}" want="${WANT_STATUS:-0}" status=0 actual
  printf '%b' "$body" >"$workdir/body.txt"
  : >"$workdir/log.txt"
  PATH="$workdir/bin:$PATH" GH_REPO=o/r FAKE_BODY_FILE="$workdir/body.txt" FAKE_LOG="$workdir/log.txt" FAKE_PULLS="$pulls" \
    bash "$script" 7 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == want)) && [[ $actual == "$(printf '%b' "$expected")" ]] &&
    [[ $(cat "$workdir/out.txt") == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status} (oczekiwano ${want}), zamknięto „${actual//$'\n'/; }”, oczekiwano „${expected}”"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

done7='Zrobione w #7.'
run_case "jedno issue z Closes" 'Opis.\n\nCloses #12' "12|$done7"
run_case "słowa Fixes i Resolves, wielkość liter bez znaczenia" 'fixes #3\nRESOLVES #4' "3|$done7\n4|$done7"
run_case "kilka issue w opisie" 'Closes #12\nCloses #13' "12|$done7\n13|$done7"
run_case "to samo issue dwa razy zamykane raz" 'Closes #12\ncloses #12' "12|$done7"
run_case "samo #N bez słowa kluczowego nie zamyka" 'Część #12, zob. też #13' ''
run_case "słowo kluczowe jako część innego słowa" 'Discloses #12' ''
run_case "opis bez odwołań" 'Zwykły opis.' ''
run_case "pusty opis" '' ''
run_case "numer PR zamiast issue nie jest zamykany" 'Closes #12' '' 12 'to PR, nie issue'
run_case "numer PR obok issue: issue zamknięte, PR pominięty" 'Closes #12\nCloses #13' "13|$done7" 12 'to PR, nie issue'
FAKE_FAIL=1 WANT_STATUS=1 run_case "błąd pierwszego issue nie zostawia drugiego otwartego" 'Closes #1\nCloses #2' "2|$done7" '' 'niezamknięte: #1'
FAKE_FAIL=2 WANT_STATUS=1 run_case "błąd drugiego issue: pierwsze zamknięte, kod różny od 0" 'Closes #1\nCloses #2' "1|$done7" '' 'niezamknięte: #2'
FAKE_FAIL='1 2' WANT_STATUS=1 run_case "błąd obu issue: lista obu" 'Closes #1\nCloses #2' '' '' 'niezamknięte: #1 #2'
FAKE_VIEW_FAIL=1 WANT_STATUS=1 run_case "błąd gh issue view: kolejne issue nadal zamykane" 'Closes #1\nCloses #2' "2|$done7" '' 'niezamknięte: #1'
run_case "formy Fixed i resolve" 'Fixed #3\nresolve #4' "3|$done7\n4|$done7"
run_case "wszystkie dziewięć form słów GitHuba" 'close #1\nCloses #2\nCLOSED #3\nfix #4\nfixes #5\nFixed #6\nresolve #7\nResolves #8\nresolved #9' "1|$done7\n2|$done7\n3|$done7\n4|$done7\n5|$done7\n6|$done7\n7|$done7\n8|$done7\n9|$done7"

# Szczegółowe przypadki liczenia sub-issue są w zamknij-nadrzedne-test.sh; tu tylko połączenie obu skryptów.
up='Wszystkie sub-issue zamknięte, ostatnie:'
FAKE_PARENTS='12 20 OPEN 11:CLOSED,12:OPEN' run_case "ostatnie sub-issue zamyka nadrzędne" 'Closes #12' "12|$done7
20|$up #12."
FAKE_PARENTS='12 20 OPEN 11:OPEN,12:CLOSED' run_case "nadrzędne z otwartym sub-issue zostaje" 'Closes #12' "12|$done7"
FAKE_PARENTS='12 20 OPEN 12:CLOSED' FAKE_FAIL=20 WANT_STATUS=1 run_case "błąd zamykania nadrzędnego kończy krok błędem" \
  'Closes #12' "12|$done7" '' 'niezamknięte: nadrzędne-nad-#12'
FAKE_PARENT_FAIL=12 WANT_STATUS=1 run_case "błąd odczytu nadrzędnego kończy krok błędem" 'Closes #12' "12|$done7" '' \
  'niezamknięte: nadrzędne-nad-#12'

exit "$((failures > 0 ? 1 : 0))"
