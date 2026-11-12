#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontroli zgodności reguł gałęzi (reguly-galezi.sh) na danych próbnych: lista, workflow i odpowiedzi API z plików.
dir="$(cd "$(dirname "$0")" && pwd)"
script="$dir/reguly-galezi.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir -p "$workdir/wf"
printf 'name: proba\non: pull_request\njobs:\n  rozmiar:\n    runs-on: ubuntu-latest\n    steps: [{run: "true"}]\n  b:\n    name: budowanie\n    runs-on: ubuntu-latest\n    steps: [{run: "true"}]\n' >"$workdir/wf/proba.yml"

ruleset() {
  local name="$1" checks=''
  shift
  for context in "$@"; do checks+="${checks:+,}{\"context\":\"$context\"}"; done
  printf '{"name":"%s","rules":[{"type":"deletion"},{"type":"required_status_checks","parameters":{"required_status_checks":[%s]}}]}\n' \
    "$name" "$checks" >"$workdir/api/$name.json"
}

# run_case <nazwa> <oczekiwany kod> <oczekiwany tekst lub pusty> <lista>; zestawy reguł przygotowuje wywołujący w $workdir/api
run_case() {
  local name="$1" expected="$2" text="$3" status=0 out
  printf '%s\n' "$4" >"$workdir/lista.txt"
  out="$(RULESET_JSON_DIR="$workdir/api" bash "$script" "$workdir/wf" "$workdir/lista.txt" 2>&1)" || status=$?
  if ((status == expected)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}${text:+ z tekstem „$text”}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

rm -rf "$workdir/api" && mkdir -p "$workdir/api"
ruleset main rozmiar budowanie
ruleset sprint rozmiar budowanie
run_case "lista zgodna z regułami, job rozpoznany po name" 0 '' $'# komentarz\nrozmiar main sprint\nbudowanie main sprint'
ruleset sprint rozmiar
run_case "brak joba w odpowiedzi API: błąd z nazwą i zestawem" 1 'kontekst `budowanie` nie jest wymaganą kontrolą w zestawie reguł `sprint`' \
  $'rozmiar main sprint\nbudowanie main sprint'
run_case "job wymagany tylko w jednym zestawie" 0 '' $'rozmiar main sprint\nbudowanie main'
ruleset sprint rozmiar sekrety
run_case "job z listy bez workflow: błąd" 1 'job `sekrety` z listy wymaganych kontroli nie istnieje w żadnym workflow' \
  $'rozmiar main sprint\nsekrety sprint'
rm "$workdir/api/sprint.json"
run_case "brak zestawu reguł: błąd" 1 'zestaw reguł `sprint` nie istnieje' 'rozmiar main sprint'

exit "$((failures > 0))"
