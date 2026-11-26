#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test wydajnosc-historia.sh na lokalnym repozytorium bez gałęzi danych: pierwszy zapis tworzy gałąź, drugi dopisuje
# linię i pokazuje trend, a niedostępne zdalne repozytorium kończy się ostrzeżeniem i kodem 0.
script="$(cd "$(dirname "$0")" && pwd)/wydajnosc-historia.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
git init -q --bare "$workdir/zdalne.git"
export ZDALNE="$workdir/zdalne.git" WPIS="$workdir/wpis.json"

echo '{"sha":"a","start_ms":{"p50":100.0,"p90":120.0},"klatka_ms":{"p50":8.0,"p90":9.0,"p99":12.0},"wolne_proc":1.0}' >"$WPIS"
bash "$script" >/dev/null
echo '{"sha":"b","start_ms":{"p50":150.0,"p90":120.0},"klatka_ms":{"p50":8.0,"p90":9.0,"p99":12.0},"wolne_proc":1.0}' >"$WPIS"
podsumowanie="$(bash "$script")"

linii="$(git -C "$workdir/zdalne.git" show dane-wydajnosci:wydajnosc.jsonl | wc -l | tr -d ' ')"
if [[ $linii != 2 ]]; then
  echo "NIEUDANY liczba wpisów: oczekiwano 2, jest ${linii}"
  failures=$((failures + 1))
else
  echo "OK: dwa zapisy dają dwa wpisy na gałęzi danych"
fi
if ! grep -q 'Start p50 (ms) | 150.0 | 100.0 | +50%' <<<"$podsumowanie"; then
  echo "NIEUDANY trend: $podsumowanie"
  failures=$((failures + 1))
else
  echo "OK: trend względem poprzedniego wpisu"
fi
autor="$(git -C "$workdir/zdalne.git" log -1 --format='%an <%ae>|%cn <%ce>' dane-wydajnosci)"
bot='github-actions[bot] <41898283+github-actions[bot]@users.noreply.github.com>'
if [[ $autor != "${bot}|${bot}" ]]; then
  echo "NIEUDANY autor wpisu: oczekiwano bota GitHub Actions, jest ${autor}"
  failures=$((failures + 1))
else
  echo "OK: wpis zapisany jako bot GitHub Actions"
fi

echo '{"sha":"c","p50_ms":5.0}' >"$WPIS"
if [[ -z "$(PLIK=obciazenie.jsonl BEZ_PODSUMOWANIA=1 bash "$script")" ]]   && git -C "$workdir/zdalne.git" show dane-wydajnosci:obciazenie.jsonl | grep -q '"sha":"c"'; then
  echo "OK: osobny plik historii bez tabeli trendu"
else
  echo "NIEUDANY zapis do osobnego pliku historii"
  failures=$((failures + 1))
fi

if ZDALNE="$workdir/brak.git" bash "$script" | grep -q 'Historia wydajności'; then
  echo "OK: niedostępne zdalne repozytorium daje ostrzeżenie"
else
  echo "NIEUDANY brak ostrzeżenia przy niedostępnym zdalnym repozytorium"
  failures=$((failures + 1))
fi

echo '{"sha":"d","start_ms":{},"klatka_ms":{},"wolne_proc":null}' >"$WPIS"
bash "$script" >/dev/null
linii="$(git -C "$workdir/zdalne.git" show dane-wydajnosci:wydajnosc.jsonl | wc -l | tr -d ' ')"
if [[ $linii == 2 ]]; then
  echo "OK: wpis bez metryk nie trafia do historii"
else
  echo "NIEUDANY wpis bez metryk został dopisany (wpisów: ${linii})"
  failures=$((failures + 1))
fi

if ((failures > 0)); then
  exit 1
fi
