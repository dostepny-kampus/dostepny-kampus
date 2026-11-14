#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test sprawdzania wyników zadań Androida (wyniki-androida.sh): kod wyjścia dla zdarzeń i wyników zadań.
script="$(cd "$(dirname "$0")" && pwd)/wyniki-androida.sh"
failures=0

# run_case <nazwa> <oczekiwany kod> <zmiany> <testy> <apk> <emulator> <analiza zależności>
run_case() {
  local name="$1" want="$2" zmiany="$3" testy="$4" apk="$5" emulator="$6" analiza="$7" status=0 out
  out="$(ZMIANY="$zmiany" WYNIKI="$testy $apk $emulator $analiza" bash "$script" 2>&1)" || status=$?
  if ((status == want)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${want}"
    echo "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "wszystko success" 0 success success success success success
run_case "zmiany pominięte, wyniki success" 0 skipped success success success success
run_case "zmiany failure" 1 failure success success success success
run_case "testy failure" 1 success failure success success success
run_case "apk failure" 1 success success failure skipped success
run_case "anulowane testy" 1 success cancelled success success success
run_case "emulator pominięty" 1 success success success skipped success
run_case "anulowany emulator" 1 success success success cancelled success
run_case "analiza zależności failure" 1 success success success success failure
run_case "anulowana analiza zależności" 1 success success success success cancelled

exit "$((failures > 0 ? 1 : 0))"
