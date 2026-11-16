#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Liczy statystyki z plików wyników Macrobenchmark (`*-benchmarkData.json`) i wypisuje jeden wpis historii (JSON):
#   start_ms     percentyle p50 i p90 z czasu do pierwszej klatki (timeToInitialDisplayMs) ze wszystkich przebiegów,
#   klatka_ms    p50, p90 i p99 z czasu klatki (frameDurationCpuMs) ze wszystkich próbek,
#   wolne_proc   odsetek klatek dłuższych niż 16,7 ms.
# Brakująca metryka daje null, bo pomiar mógł się nie udać; wpis powstaje zawsze, żeby historia pokazała dziurę.
# Użycie: SHA=<commit> DATA=<RRRR-MM-DD> wydajnosc-statystyki.sh plik.json...
"${PYTHON:-python3}" - "$@" <<'PY'
import json
import math
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

SLOW_MS = 1000 / 60


def percentile(values, share):
    if not values:
        return None
    ordered = sorted(values)
    rank = max(1, math.ceil(share * len(ordered)))
    return round(ordered[rank - 1], 2)


def flat(items):
    for item in items:
        if isinstance(item, list):
            yield from flat(item)
        else:
            yield item


startup, frames = [], []
for path in sys.argv[1:]:
    with open(path, encoding="utf-8") as file:
        for bench in json.load(file).get("benchmarks", []):
            startup += list(flat(bench.get("metrics", {}).get("timeToInitialDisplayMs", {}).get("runs", [])))
            frames += list(flat(bench.get("sampledMetrics", {}).get("frameDurationCpuMs", {}).get("runs", [])))

slow = round(100 * sum(1 for ms in frames if ms > SLOW_MS) / len(frames), 2) if frames else None
entry = {
    "sha": os.environ.get("SHA", ""),
    "data": os.environ.get("DATA", ""),
    "start_ms": {"p50": percentile(startup, 0.5), "p90": percentile(startup, 0.9), "proby": len(startup)},
    "klatka_ms": {
        "p50": percentile(frames, 0.5),
        "p90": percentile(frames, 0.9),
        "p99": percentile(frames, 0.99),
        "probki": len(frames),
    },
    "wolne_proc": slow,
}
print(json.dumps(entry, ensure_ascii=False))
PY
