#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Test limitu długości pliku (dlugosc-pliku.sh) na własnych danych oraz na prawdziwym AGENTS.md,
# który jest czytany w całości przy każdej zmianie i ma najwyżej 150 linii.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
script="$here/dlugosc-pliku.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0

# run_case <nazwa> <oczekiwany kod> <plik> <limit>
run_case() {
  local name="$1" expected="$2" file="$3" limit="$4" status=0 out
  out="$(bash "$script" "$file" "$limit" 2>&1)" || status=$?
  if ((status == expected)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

# lines <plik> <liczba linii>
lines() {
  seq "$2" >"$1"
}

lines "$workdir/150.md" 150
lines "$workdir/151.md" 151
printf '1\n2\n3' >"$workdir/bez-konca.md"
: >"$workdir/pusty.md"

run_case "150 linii przy limicie 150" 0 "$workdir/150.md" 150
run_case "151 linii przy limicie 150" 1 "$workdir/151.md" 150
run_case "ostatnia linia bez znaku końca jest liczona" 1 "$workdir/bez-konca.md" 2
run_case "pusty plik" 0 "$workdir/pusty.md" 1
run_case "brak pliku" 2 "$workdir/nie-ma.md" 150
run_case "limit nie jest liczbą" 2 "$workdir/150.md" abc
run_case "limit zero" 2 "$workdir/150.md" 0
run_case "AGENTS.md repozytorium" 0 "$here/../../AGENTS.md" 150

exit "$((failures > 0 ? 1 : 0))"
