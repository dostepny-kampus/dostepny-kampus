#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test rozpoznania wyniku PIT (wynik-pit.sh): kod wyjścia i komunikat dla kodu Mavena i raportu mutacji.
script="$(cd "$(dirname "$0")" && pwd)/wynik-pit.sh"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
failures=0

report() {
  local file="$work/$1.xml"
  shift
  {
    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo '<mutations>'
    for status in "$@"; do
      echo "<mutation detected='false' status='${status}' numberOfTestsRun='1'><sourceFile>Submission.java</sourceFile></mutation>"
    done
    echo '</mutations>'
  } >"$file"
  echo "$file"
}

# run_case <nazwa> <oczekiwany kod> <oczekiwany fragment komunikatu> <kod Mavena> <plik raportu>
run_case() {
  local name="$1" want="$2" text="$3" maven="$4" file="$5" status=0 out
  out="$(bash "$script" "$maven" "$file" 2>&1)" || status=$?
  if ((status == want)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${want} i tekstu „${text}”"
    echo "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "Maven 0 i same zabite mutacje" 0 "Wszystkie mutacje wykryte" 0 "$(report zabite KILLED KILLED)"
run_case "mutacja przeżyła" 1 "Mutacja przeżyła: 1" 1 "$(report przezyla KILLED SURVIVED)"
run_case "mutacja bez pokrycia" 1 "bez pokrycia: 2" 1 "$(report bez-pokrycia NO_COVERAGE KILLED NO_COVERAGE)"
run_case "błąd budowania: brak raportu" 1 "PIT nie zakończył analizy" 1 "$work/nie-ma.xml"
run_case "błąd budowania nie mówi o mutacji" 1 "błąd budowania" 1 "$work/nie-ma.xml"
run_case "błąd Mavena przy samych zabitych mutacjach" 1 "bez mutacji, która przeżyła" 1 "$(report same-zabite KILLED)"
double="$(report podwojne SURVIVED)"
sed -i "s/'/\"/g" "$double"
run_case "raport w podwójnych cudzysłowach" 1 "Mutacja przeżyła: 1" 1 "$double"

exit "$((failures > 0 ? 1 : 0))"
