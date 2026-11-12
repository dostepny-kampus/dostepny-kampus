#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontroli warunków wymaganych jobów (wymagane-joby.sh) na workflow próbnych i na workflow repozytorium.
dir="$(cd "$(dirname "$0")" && pwd)"
script="$dir/wymagane-joby.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
printf 'kontrakt main sprint\nserwer main sprint\n' >"$workdir/lista.txt"

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <needs> <if>
run_case() {
  local name="$1" expected="$2" text="$3" needs="$4" condition="$5" status=0 out
  rm -rf "$workdir/wf"
  mkdir -p "$workdir/wf"
  {
    printf 'name: proba\non: pull_request\njobs:\n  zmiany:\n    runs-on: ubuntu-latest\n    steps: [{run: "true"}]\n'
    printf '  kontrakt:\n    runs-on: ubuntu-latest\n'
    [[ -n $needs ]] && printf '    needs: %s\n' "$needs"
    [[ -n $condition ]] && printf '    if: %s\n' "$condition"
    printf '    steps: [{run: "true"}]\n'
  } >"$workdir/wf/proba.yml"
  out="$(bash "$script" "$workdir/wf" "$workdir/lista.txt" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

safe="\${{ !cancelled() && needs.porownanie.outputs.pomin != 'true' && (needs.zmiany.result != 'success' || needs.zmiany.outputs.api == 'true') }}"
run_case "warunek tylko z wyjścia joba zmiany: błąd z nazwą joba" 1 'kontrakt: wymaga wyjścia joba `zmiany`' \
  '[zmiany]' "\${{ !cancelled() && needs.zmiany.outputs.api == 'true' }}"
run_case "komunikat podaje gotowy warunek" 1 "needs.zmiany.result != 'success' ||" \
  '[zmiany]' "\${{ !cancelled() && needs.zmiany.outputs.api == 'true' }}"
run_case "warunek bez !cancelled(): błąd" 1 'nie zawiera `!cancelled()`' '[zmiany]' "needs.zmiany.outputs.api == 'true'"
run_case "needs bez warunku: błąd" 1 'nie zawiera `!cancelled()`' 'zmiany' ''
run_case "warunek z gałęzią dla błędu joba zmiany" 0 '' '[porownanie, zmiany]' "$safe"
run_case "always() z gałęzią dla błędu" 0 '' '[zmiany]' "\${{ always() && (needs.zmiany.result != 'success' || needs.zmiany.outputs.api == 'true') }}"
run_case "job bez needs" 0 '' '' "github.event_name == 'pull_request'"

status=0
out="$(cd "$dir/../.." && bash "$script" 2>&1)" || status=$?
if ((status == 0)); then
  echo "OK: workflow repozytorium bez wyjątków"
else
  echo "BŁĄD: workflow repozytorium: kod wyjścia ${status}"
  printf '%s\n' "$out" | sed 's/^/    /'
  failures=$((failures + 1))
fi

exit "$((failures > 0))"
