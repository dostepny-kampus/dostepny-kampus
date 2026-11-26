#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Dopisuje wpis historii wydajności (jedna linia JSON z wydajnosc-statystyki.sh) do pliku wydajnosc.jsonl na gałęzi
# danych i wypisuje podsumowanie (Markdown) z trendem względem poprzedniego wpisu. Gałąź danych nie zawiera kodu i nie
# jest związana z historią sprintu; powstaje przy pierwszym zapisie. Zapis bez force-push, do 3 prób po wyścigu.
# Środowisko: WPIS (plik z jednym wpisem), PLIK (plik historii, domyślnie wydajnosc.jsonl), BEZ_PODSUMOWANIA (niepuste:
# tylko zapis, bez tabeli trendu), ZDALNE (adres lub nazwa zdalnego repozytorium, domyślnie origin),
# GALAZ_DANYCH (domyślnie dane-wydajnosci). Błąd zapisu nie zmienia kodu wyjścia, bo historia nie może czerwienić sprintu.
zdalne="${ZDALNE:-origin}"
galaz="${GALAZ_DANYCH:-dane-wydajnosci}"
plik="${PLIK:-wydajnosc.jsonl}"
katalog="$(mktemp -d)"
trap 'rm -rf "$katalog"' EXIT
git init -q "$katalog"
git -C "$katalog" remote add dane "$zdalne"

podsumowanie() {
  "${PYTHON:-python3}" - "$WPIS" "$katalog/$plik" <<'PY'
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

entry = json.loads(open(sys.argv[1], encoding="utf-8").read())
past = []
if os.path.exists(sys.argv[2]):
    past = [json.loads(line) for line in open(sys.argv[2], encoding="utf-8") if line.strip()]
previous = past[-1] if past else None


def trend(now, before):
    if now is None or before in (None, 0):
        return "-"
    return f"{100 * (now - before) / before:+.0f}%"


rows = [
    ("Start p50 (ms)", ("start_ms", "p50")),
    ("Start p90 (ms)", ("start_ms", "p90")),
    ("Start bez profilu p50 (ms)", ("start_bez_profilu_ms", "p50")),
    ("Klatka p50 (ms)", ("klatka_ms", "p50")),
    ("Klatka p90 (ms)", ("klatka_ms", "p90")),
    ("Klatka p99 (ms)", ("klatka_ms", "p99")),
    ("Wolne klatki (%)", ("wolne_proc",)),
]


def pick(data, keys):
    for key in keys:
        data = None if data is None else data.get(key)
    return data


print("### Wydajność aplikacji")
print("| Metryka | Teraz | Poprzednio | Zmiana |\n|---|---|---|---|")
for label, keys in rows:
    now, before = pick(entry, keys), pick(previous, keys)
    print(f"| {label} | {now} | {before} | {trend(now, before)} |")
print("\nPomiar na emulatorze bez GPU: szum rzędu 10-20%, więc liczy się trend z wielu wpisów, nie jedna zmiana.")
PY
}

if ! grep -qE '"p50(_ms)?": ?[0-9]' "$WPIS"; then
  echo "::warning title=Historia wydajności::Wpis bez metryk nie został dopisany do ${galaz}."
  exit 0
fi
zapisano=0
for proba in 1 2 3; do
  if git -C "$katalog" fetch -q --depth 50 dane "$galaz" 2>/dev/null; then
    git -C "$katalog" checkout -q -B "$galaz" FETCH_HEAD
  elif ! git -C "$katalog" rev-parse -q --verify "$galaz" >/dev/null; then
    git -C "$katalog" checkout -q --orphan "$galaz"
  fi
  if ((proba == 1)) && [[ -z ${BEZ_PODSUMOWANIA:-} ]]; then
    podsumowanie
  fi
  cat "$WPIS" >>"$katalog/$plik"
  git -C "$katalog" add "$plik"
  if git -C "$katalog" -c user.name="github-actions[bot]" \
    -c user.email="41898283+github-actions[bot]@users.noreply.github.com" \
    commit -q -m "dane: wpis wydajności $(date -u +%F)" && git -C "$katalog" push -q dane "$galaz" 2>/dev/null; then
    zapisano=1
    break
  fi
  git -C "$katalog" reset -q --hard HEAD~1 2>/dev/null || true
done
if ((zapisano == 0)); then
  echo "::warning title=Historia wydajności::Nie udało się zapisać wpisu na gałęzi ${galaz}."
fi
