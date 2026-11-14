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
# Joby próbne starszych przypadków są wyjątkami, żeby reguła wymaganego odbiorcy ich nie zgłaszała.
echo 'proba.yml/zmiany job próbny' >"$workdir/wyjatki.txt"
echo 'proba.yml/raport job próbny' >"$workdir/wyjatki-raport.txt"

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
  out="$(bash "$script" "$workdir/wf" "$workdir/lista.txt" "$workdir/wyjatki.txt" 2>&1)" || status=$?
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

# run_raw <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <treść joba `raport` (wcięcie 4 spacje)>
run_raw() {
  local name="$1" expected="$2" text="$3" body="$4" status=0 out
  rm -rf "$workdir/wf"
  mkdir -p "$workdir/wf"
  printf 'name: proba\non: pull_request\njobs:\n  raport:\n    runs-on: ubuntu-latest\n%s\n' "$body" >"$workdir/wf/proba.yml"
  out="$(bash "$script" "$workdir/wf" "$workdir/lista.txt" "$workdir/wyjatki-raport.txt" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_raw "continue-on-error joba: błąd" 1 'raport: `continue-on-error` na poziomie joba' \
  $'    continue-on-error: true\n    steps: [{run: "true"}]'
run_raw "continue-on-error joba z wyrażeniem: błąd" 1 'raport: `continue-on-error` na poziomie joba' \
  $'    continue-on-error: ${{ matrix.zestaw == \'system\' }}\n    steps: [{run: "true"}]'
run_raw "continue-on-error kroku poza zapisem raportu: błąd" 1 'raport: krok „licz” ma `continue-on-error`' \
  $'    steps:\n      - name: licz\n        run: "false"\n        continue-on-error: true'
run_raw "continue-on-error przy zapisie raportu diagnostycznego" 0 '' \
  $'    steps:\n      - uses: actions/upload-artifact@v7.0.2\n        continue-on-error: true\n        with: {name: r, path: r}'
run_raw "continue-on-error: false" 0 '' $'    continue-on-error: false\n    steps: [{run: "true"}]'

# run_odbiorca <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <zdarzenie> <wyjątki> <needs joba `kontrakt`>
run_odbiorca() {
  local name="$1" expected="$2" text="$3" event="$4" exceptions="$5" needs="$6" status=0 out
  rm -rf "$workdir/wf"
  mkdir -p "$workdir/wf"
  {
    printf 'name: proba\non: %s\njobs:\n  analiza:\n    runs-on: ubuntu-latest\n    steps: [{run: "true"}]\n' "$event"
    printf '  kontrakt:\n    runs-on: ubuntu-latest\n'
    [[ -n $needs ]] && printf '    needs: %s\n    if: ${{ !cancelled() }}\n' "$needs"
    printf '    steps: [{run: "true"}]\n'
  } >"$workdir/wf/proba.yml"
  printf '%b' "$exceptions" >"$workdir/wyjatki-odbiorcy.txt"
  out="$(bash "$script" "$workdir/wf" "$workdir/lista.txt" "$workdir/wyjatki-odbiorcy.txt" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_odbiorca "job sprawdzający PR bez wymaganego odbiorcy: błąd" 1 'analiza: job sprawdza PR, ale nie jest wymagany' \
  pull_request '' ''
run_odbiorca "job w kolejce bez wymaganego odbiorcy: błąd" 1 'analiza: job sprawdza PR' merge_group '' ''
run_odbiorca "job zasilający wymagany job" 0 '' pull_request '' '[analiza]'
run_odbiorca "job z listy wyjątków" 0 '' pull_request 'proba.yml/analiza automatyzacja\n' ''
run_odbiorca "wyjątek bez powodu: błąd" 1 'proba.yml/analiza: wyjątek bez powodu' pull_request 'proba.yml/analiza\n' ''
run_odbiorca "wyjątek dla nieistniejącego joba: błąd" 1 'proba.yml/brak: wyjątek dla joba, którego nie ma' \
  pull_request 'proba.yml/analiza automatyzacja\nproba.yml/brak stary wpis\n' ''
run_odbiorca "przepływ tylko po scaleniu" 0 '' push '' ''

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
