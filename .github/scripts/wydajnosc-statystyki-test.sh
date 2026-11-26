#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test wydajnosc-statystyki.sh na zmyślonych wynikach: 10 czasów startu, 100 klatek (3 wolne) i przypadek bez metryk.
script="$(cd "$(dirname "$0")" && pwd)/wydajnosc-statystyki.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0

"${PYTHON:-python3}" - "$workdir" <<'PY'
import json
import sys

folder = sys.argv[1]
frames = [8.0] * 97 + [20.0, 30.0, 40.0]
data = {
    "benchmarks": [
        {"name": "measuresStartWithoutProfile", "metrics": {"timeToInitialDisplayMs": {"runs": [700.0, 900.0]}}},
        {"name": "measuresColdStart", "metrics": {"timeToInitialDisplayMs": {"runs": [float(n) for n in range(100, 1100, 100)]}}},
        {"name": "klatki", "sampledMetrics": {"frameDurationCpuMs": {"runs": [frames[:50], frames[50:]]}}},
    ]
}
json.dump(data, open(f"{folder}/pelny.json", "w"))
json.dump({"benchmarks": []}, open(f"{folder}/pusty.json", "w"))
PY

sprawdz() {
  local nazwa="$1" oczekiwane="$2" wyrazenie="$3" plik="$4" wynik
  wynik="$(SHA=abc DATA=2026-11-16 bash "$script" "$workdir/$plik" | "${PYTHON:-python3}" -c "import json,sys; d=json.load(sys.stdin); print($wyrazenie)")"
  if [[ $wynik != "$oczekiwane" ]]; then
    echo "NIEUDANY ${nazwa}: oczekiwano ${oczekiwane}, jest ${wynik}"
    failures=$((failures + 1))
  else
    echo "OK: ${nazwa}"
  fi
}

sprawdz "start p50" 500.0 "d['start_ms']['p50']" pelny.json
sprawdz "start p90" 900.0 "d['start_ms']['p90']" pelny.json
sprawdz "start bez profilu p50" 700.0 "d['start_bez_profilu_ms']['p50']" pelny.json
sprawdz "klatka p50" 8.0 "d['klatka_ms']['p50']" pelny.json
sprawdz "klatka p99" 30.0 "d['klatka_ms']['p99']" pelny.json
sprawdz "wolne" 3.0 "d['wolne_proc']" pelny.json
sprawdz "sha" abc "d['sha']" pelny.json
sprawdz "pusty start" None "d['start_ms']['p50']" pusty.json
sprawdz "pusty wolne" None "d['wolne_proc']" pusty.json

if ((failures > 0)); then
  exit 1
fi
