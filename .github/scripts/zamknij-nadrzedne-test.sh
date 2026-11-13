#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test zamykania issue nadrzędnych (zamknij-nadrzedne.sh <numer zamkniętego issue>). Podstawione `gh`:
# `gh api graphql` odpowiada z FAKE_PARENTS (wiersz: issue nadrzędne stan sub:STAN,sub:STAN…), a `gh issue close`
# dopisuje numer i komentarz do dziennika.
script="$(cd "$(dirname "$0")" && pwd)/zamknij-nadrzedne.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'issue close')
    if [[ " ${FAKE_FAIL:-} " == *" $3 "* ]]; then echo "gh: błąd zamykania #$3" >&2; exit 1; fi
    printf '%s|%s\n' "$3" "$5" >>"$FAKE_LOG" ;;
  'api graphql')
    for arg in "$@"; do [[ $arg == number=* ]] && child="${arg#number=}"; done
    if [[ " ${FAKE_PARENT_FAIL:-} " == *" $child "* ]]; then echo "gh: błąd odczytu nadrzędnego #$child" >&2; exit 1; fi
    while read -r of parent state subs; do
      [[ $of == "$child" ]] || continue
      echo "$parent $state"
      tr ',' '\n' <<<"$subs" | tr ':' ' '
    done <<<"${FAKE_PARENTS:-}"
    ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

# run_case <nazwa> <oczekiwany dziennik: wiersze numer|komentarz>
run_case() {
  local name="$1" expected="$2" want="${WANT_STATUS:-0}" status=0 actual
  : >"$workdir/log.txt"
  PATH="$workdir/bin:$PATH" GH_REPO=o/r FAKE_LOG="$workdir/log.txt" bash "$script" 12 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == want)) && [[ $actual == "$expected" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status} (oczekiwano ${want}), zamknięto „${actual//$'\n'/; }”, oczekiwano „${expected}”"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

up='Wszystkie sub-issue zamknięte, ostatnie:'
run_case "issue bez nadrzędnego" ''
FAKE_PARENTS='12 20 OPEN 11:CLOSED,12:CLOSED,13:CLOSED' run_case "ostatnie sub-issue zamyka nadrzędne" "20|$up #12."
FAKE_PARENTS='12 20 OPEN 11:CLOSED,12:OPEN' run_case "API jeszcze pokazuje zamknięte issue jako otwarte" "20|$up #12."
FAKE_PARENTS='12 20 OPEN 12:OPEN' run_case "jedyne sub-issue, API nieodświeżone" "20|$up #12."
FAKE_PARENTS='12 20 OPEN 11:OPEN,12:CLOSED' run_case "nadrzędne z innym otwartym sub-issue zostaje" ''
FAKE_PARENTS='12 20 CLOSED 12:CLOSED' run_case "zamknięte nadrzędne nie jest zamykane ponownie" ''
FAKE_PARENTS=$'12 20 OPEN 12:CLOSED\n20 30 OPEN 20:OPEN' run_case "dwa poziomy w górę zamykają się kolejno" "20|$up #12.
30|$up #20."
FAKE_PARENTS=$'12 20 OPEN 12:CLOSED\n20 30 OPEN 20:CLOSED,21:OPEN' run_case "drugi poziom z otwartym sub-issue zostaje" "20|$up #12."
FAKE_PARENTS='12 20 OPEN 12:CLOSED' FAKE_FAIL=20 WANT_STATUS=1 run_case "błąd zamykania nadrzędnego kończy błędem" ''
FAKE_PARENT_FAIL=12 WANT_STATUS=1 run_case "błąd odczytu nadrzędnego kończy błędem" ''

exit "$((failures > 0 ? 1 : 0))"
