#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test sprawdzania wyników zadań Androida (wyniki-androida.sh): kod wyjścia dla zdarzeń i wyników zadań.
script="$(cd "$(dirname "$0")" && pwd)/wyniki-androida.sh"
failures=0

# run_case <nazwa> <oczekiwany kod> <zdarzenie> <zmiany> <testy> <apk> <emulator>
run_case() {
  local name="$1" want="$2" event="$3" zmiany="$4" testy="$5" apk="$6" emulator="$7" status=0 out
  out="$(ZMIANY="$zmiany" WYNIKI="$testy $apk $emulator" ZDARZENIE="$event" EMULATOR="$emulator" bash "$script" 2>&1)" || status=$?
  if ((status == want)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${want}"
    echo "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "wszystko success na push" 0 push success success success success
run_case "wszystko success na pull_request" 0 pull_request success success success success
run_case "push: emulator pominięty, reszta success" 0 push success success success skipped
run_case "push: zmiany pominięte, wyniki success" 0 push skipped success success success
run_case "zmiany failure" 1 push failure success success success
run_case "push: testy failure" 1 push success failure success skipped
run_case "push: anulowane testy" 0 push success cancelled success skipped
run_case "push: anulowany emulator" 0 push success success success cancelled
run_case "push: anulowane i failure razem" 1 push success cancelled failure skipped
run_case "pull_request: anulowane testy" 1 pull_request success cancelled success success
run_case "merge_group: anulowane testy" 1 merge_group success cancelled success success
run_case "merge_group: emulator pominięty" 1 merge_group success success success skipped

exit "$((failures > 0 ? 1 : 0))"
