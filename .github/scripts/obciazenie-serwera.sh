#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Krótki test obciążenia publicznego odczytu: ZADANIA żądań GET na ADRES, naraz ROWNOLEGLE klientów (curl i xargs -P,
# bez dodatkowych narzędzi). Wypisuje jeden wpis JSON: p50/p90/p99 czasu odpowiedzi w ms, liczbę żądań i błędów
# (status inny niż 2xx albo brak odpowiedzi). Dane są wyłącznie publiczne i syntetyczne: ten sam adres, bez konta.
# Środowisko: ADRES, ZADANIA (domyślnie 600), ROWNOLEGLE (domyślnie 12), SHA, DATA, PYTHON.
adres="${ADRES:?brak ADRES}"
zadania="${ZADANIA:-600}"
rownolegle="${ROWNOLEGLE:-12}"
wyniki="$(mktemp)"
trap 'rm -f "$wyniki"' EXIT

seq "$zadania" | xargs -P "$rownolegle" -I{} \
  curl -s -o /dev/null -m 10 -w '%{time_total} %{http_code}\n' "$adres" >"$wyniki" || true

"${PYTHON:-python3}" - "$wyniki" "$zadania" <<'PY'
import json
import math
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")
times, errors = [], 0
for line in open(sys.argv[1], encoding="utf-8"):
    parts = line.split()
    if len(parts) != 2 or not parts[1].startswith("2"):
        errors += 1
        continue
    times.append(float(parts[0]) * 1000)
errors += max(0, int(sys.argv[2]) - len(times) - errors)


def percentile(share):
    if not times:
        return None
    ordered = sorted(times)
    return round(ordered[max(1, math.ceil(share * len(ordered))) - 1], 1)


print(json.dumps({
    "sha": os.environ.get("SHA", ""),
    "data": os.environ.get("DATA", ""),
    "zadania": int(sys.argv[2]),
    "bledy": errors,
    "p50_ms": percentile(0.5),
    "p90_ms": percentile(0.9),
    "p99_ms": percentile(0.99),
}, ensure_ascii=False))
PY
