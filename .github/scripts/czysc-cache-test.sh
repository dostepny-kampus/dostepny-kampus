#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test czyszczenia cache (czysc-cache.sh). Zamiast sieci test podstawia własne `gh`: `gh cache list`
# zwraca identyfikatory (najnowszy pierwszy) z zmiennej FAKE_<n>, a `gh cache delete` dopisuje numer do dziennika.
script="$(cd "$(dirname "$0")" && pwd)/czysc-cache.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
case "$1 $2" in
  'cache list')
    key=""
    while (($#)); do [[ $1 == --key ]] && key="$2"; shift; done
    case "$key" in
      budowanie-) printf '%b' "${FAKE_BUDOWANIE:-}" ;;
      zaleznosci-) printf '%b' "${FAKE_ZALEZNOSCI:-}" ;;
      *) echo "nieoczekiwany prefiks: $key" >&2; exit 2 ;;
    esac ;;
  'cache delete') echo "$3" >>"$FAKE_LOG" ;;
  *) echo "nieoczekiwane wywołanie gh: $*" >&2; exit 2 ;;
esac
SH
chmod +x "$workdir/bin/gh"

# run_case <nazwa> <budowanie> <zależności> <oczekiwane usunięte, rozdzielone \n> [prefiksy]
run_case() {
  local name="$1" build="$2" deps="$3" expected="$4" prefixes="${5:-budowanie- zaleznosci-}" status=0 actual
  : >"$workdir/log.txt"
  # shellcheck disable=SC2086
  PATH="$workdir/bin:$PATH" GITHUB_REF=refs/heads/sprint/4 FAKE_LOG="$workdir/log.txt" \
    FAKE_BUDOWANIE="$build" FAKE_ZALEZNOSCI="$deps" bash "$script" $prefixes >"$workdir/out.txt" 2>&1 || status=$?
  actual="$(cat "$workdir/log.txt")"
  if ((status == 0)) && [[ $actual == "$(printf '%b' "$expected")" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, usunięto „${actual//$'\n'/ }”, oczekiwano „${expected//\n/ }”"
    sed 's/^/    /' "$workdir/out.txt"
    failures=$((failures + 1))
  fi
}

run_case "trzy wpisy: usunięte dwa najstarsze" '30\n20\n10\n' '' '20\n10' budowanie-
run_case "jeden wpis: nic nie usunięte" '30\n' '' '' budowanie-
run_case "brak wpisów: nic nie usunięte" '' '' '' budowanie-
run_case "zależności: usunięte starsze, najnowszy zostaje" '' '5\n4\n3\n2\n1\n' '4\n3\n2\n1' zaleznosci-
run_case "oba prefiksy niezależnie" '30\n20\n' '7\n6\n' '20\n6'
run_case "brak podanego prefiksu: nic nie robi" '30\n20\n' '7\n6\n' '' ' '

exit "$((failures > 0 ? 1 : 0))"
