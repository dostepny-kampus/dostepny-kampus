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
cp "$(dirname "$script")/falszywe-gh-zamykanie" "$workdir/bin/gh"
chmod +x "$workdir/bin/gh"

# Kod gałęzi domyślnej: repo <nazwa> <A.kt> [a.feature]. W każdym b.kt ma #200, #2000 i kolor #20A0FF, które
# tylko przypominają numer #20.
repo() {
  git init -q "$workdir/$1"
  printf '%s\n' "$2" >"$workdir/$1/A.kt"
  printf '%s\n' "${3:-}" >"$workdir/$1/a.feature"
  printf '%s\n' 'val x = "#200 #2000 #20A0FF"' >"$workdir/$1/b.kt"
  git -C "$workdir/$1" add -A
}
scenariusz=$'# granica terenu: #20\n@do-zrobienia'
repo czysty 'val a = 1'
repo z-todo "// T""ODO(#20): dokończyć" "$scenariusz"
repo tylko-scenariusz 'val a = 1' "$scenariusz"

# run_case <nazwa> <oczekiwany dziennik: wiersze numer|komentarz>; KOD wybiera repozytorium (domyślnie czysty).
run_case() {
  local name="$1" expected="$2" want="${WANT_STATUS:-0}" status=0 actual
  : >"$workdir/log.txt"
  (cd "$workdir/${KOD:-czysty}" && PATH="$workdir/bin:$PATH" GH_REPO=o/r FAKE_LOG="$workdir/log.txt" bash "$script" 12) \
    >"$workdir/out.txt" 2>&1 || status=$?
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
zostaje='Wszystkie sub-issue zamknięte (ostatnie: #12), ale kod nadal wskazuje to issue, więc zostaje otwarte:'
FAKE_PARENTS='12 20 OPEN 12:CLOSED' KOD=z-todo run_case "nadrzędne wskazane znacznikiem zadania zostaje z komentarzem" \
  "20|komentarz: $zostaje A.kt:1, a.feature:1"
FAKE_PARENTS='12 20 OPEN 12:CLOSED' KOD=tylko-scenariusz run_case "nadrzędne wskazane w scenariuszu zostaje" \
  "20|komentarz: $zostaje a.feature:1"
FAKE_PARENTS=$'12 20 OPEN 12:CLOSED\n20 30 OPEN 20:OPEN' KOD=z-todo run_case "wskazane nadrzędne nie zamyka wyższych" \
  "20|komentarz: $zostaje A.kt:1, a.feature:1"

exit "$((failures > 0 ? 1 : 0))"
