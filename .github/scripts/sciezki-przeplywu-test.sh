#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test zgodności ścieżek workflow (sciezki-przeplywu.sh): przykładowe workflow oraz prawdziwy backend.yml
# z listą ścieżek, które czytają testy serwera.
here="$(cd "$(dirname "$0")" && pwd)"
script="$here/sciezki-przeplywu.sh"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
failures=0

# workflow <plik> <ścieżki push, po spacji> <ścieżki diff, po spacji>
workflow() {
  local file="$work/$1.yml" path
  set -f
  {
    echo 'on:'
    echo '  pull_request:'
    echo '    branches:'
    echo '      - main'
    echo '  push:'
    echo '    branches:'
    echo '      - main'
    echo '    paths:'
    for path in $2; do echo "      - \"${path}\""; done
    echo '  merge_group:'
    echo 'jobs:'
    echo '  zmiany:'
    echo '    steps:'
    echo '      - run: |'
    echo "          if git diff --quiet \"\$from\" \"\$to\" -- $3; then"
    echo '            echo "serwer=false"'
    echo '          fi'
  } >"$file"
  set +f
  echo "$file"
}

# run_case <nazwa> <oczekiwany kod> <fragment wyjścia> <plik> [wymagane ścieżki]...
run_case() {
  local name="$1" want="$2" text="$3" file="$4" status=0 out
  shift 4
  out="$(bash "$script" "$file" "$@" 2>&1)" || status=$?
  if ((status == want)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${want} i tekstu „${text}”"
    echo "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "te same ścieżki, katalog z gwiazdkami" 0 "ścieżki zgodne" \
  "$(workflow zgodne 'backend/** data/** api/openapi.yaml' 'backend data api/openapi.yaml')"
run_case "ścieżka tylko w filtrze push" 1 "tylko w on.push.paths: specs" \
  "$(workflow tylko-push 'backend/** specs/**' 'backend')"
run_case "ścieżka tylko w zadaniu zmiany" 1 "tylko w zadaniu zmiany: specs" \
  "$(workflow tylko-diff 'backend/**' 'backend specs')"
run_case "brak wymaganej ścieżki" 1 "brak ścieżki specs" \
  "$(workflow bez-wymaganej 'backend/**' 'backend')" backend specs
run_case "wymagana ścieżka obecna" 0 "ścieżki zgodne" \
  "$(workflow z-wymagana 'backend/** specs/**' 'backend specs')" backend specs
echo 'on: {}' >"$work/pusty.yml"
run_case "workflow bez filtra i bez zadania zmiany" 1 "nie znaleziono filtra" "$work/pusty.yml"

# Testy serwera czytają: kontrakt (OpenApiContract), scenariusze (ServerSpecs), obrazy i realm z infra/ (konfiguracje testów).
run_case "backend.yml repozytorium" 0 "ścieżki zgodne" "$here/../workflows/backend.yml" \
  backend data api/openapi.yaml specs infra .github/workflows/backend.yml

exit "$((failures > 0 ? 1 : 0))"
