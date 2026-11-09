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
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'pr view') cat "$FAKE_BODY_FILE" ;;
  'issue close') printf '%s|%s\n' "$3" "$5" >>"$FAKE_LOG" ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

# run_case <nazwa> <opis PR> <oczekiwany dziennik: wiersze numer|komentarz, rozdzielone \n>
run_case() {
  local name="$1" body="$2" expected="$3" status=0 actual
  printf '%b' "$body" >"$workdir/body.txt"
  : >"$workdir/log.txt"
  PATH="$workdir/bin:$PATH" FAKE_BODY_FILE="$workdir/body.txt" FAKE_LOG="$workdir/log.txt" \
    bash "$script" 7 >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == 0)) && [[ $actual == "$(printf '%b' "$expected")" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, zamknięto „${actual//$'\n'/; }”, oczekiwano „${expected}”"
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

exit "$((failures > 0 ? 1 : 0))"
