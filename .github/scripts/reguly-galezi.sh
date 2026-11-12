#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Kontrola zgodności reguł gałęzi z listą wymaganych kontroli (wymagane-kontrole.txt). Każdy job z listy musi
# istnieć w jakimś workflow (klucz joba albo jego `name`) i być wymaganą kontrolą w każdym zestawie reguł,
# który lista przy nim wymienia. Reguły gałęzi zmienia się poza PR, więc kontrola działa po wypchnięciu
# na sprint/* i raz dziennie.
# Zestawy reguł: `gh api repos/$GH_REPO/rulesets` (lista) i jedno wywołanie na zestaw; w teście zamiast sieci
# RULESET_JSON_DIR z plikami <zestaw>.json (odpowiedź `gh api repos/.../rulesets/<id>`).
# Użycie: reguly-galezi.sh [katalog workflow] [plik listy]. Kod wyjścia: 0 = zgodne, 1 = niezgodność, 2 = błąd narzędzia.
workflows="${1:-.github/workflows}"
lista="${2:-$(dirname "${BASH_SOURCE[0]}")/wymagane-kontrole.txt}"
dane="${RULESET_JSON_DIR:-}"

if [[ -z $dane ]]; then
  dane="$(mktemp -d)"
  trap 'rm -rf "$dane"' EXIT
  zestawy="$(awk '!/^#/ { for (i = 2; i <= NF; i++) print $i }' "$lista" | sort -u)"
  if ! spis="$(gh api "repos/${GH_REPO}/rulesets" --jq '.[] | "\(.name) \(.id)"' 2>&1)"; then
    echo "::error title=Błąd narzędzia::gh api rulesets: $(head -n 1 <<<"$spis")"
    exit 2
  fi
  for zestaw in $zestawy; do
    id="$(awk -v z="$zestaw" '$1 == z { print $2 }' <<<"$spis")"
    [[ -n $id ]] || continue
    if ! gh api "repos/${GH_REPO}/rulesets/${id}" >"$dane/$zestaw.json" 2>"$dane/blad.txt"; then
      echo "::error title=Błąd narzędzia::gh api rulesets/${id}: $(head -n 1 "$dane/blad.txt")"
      exit 2
    fi
  done
fi

"${PYTHON:-python3}" - "$workflows" "$lista" "$dane" <<'PY'
import json
import pathlib
import sys

try:
    import yaml
except ImportError:
    print("::error title=Błąd narzędzia::brak modułu yaml dla python3")
    sys.exit(2)

sys.stdout.reconfigure(encoding="utf-8")
workflows, lista, dane = (pathlib.Path(arg) for arg in sys.argv[1:4])
jobs = set()
for path in list(workflows.glob("*.yml")) + list(workflows.glob("*.yaml")):
    document = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    for key, job in (document.get("jobs") or {}).items():
        jobs.add(key)
        if isinstance(job, dict) and job.get("name"):
            jobs.add(str(job["name"]))

required = {}
found = 0


def report(message):
    global found
    found = 1
    print(f"::error title=Reguły gałęzi::{message}")


def contexts(name):
    if name not in required:
        path = dane / f"{name}.json"
        if not path.is_file():
            report(f"zestaw reguł `{name}` nie istnieje w repozytorium.")
            required[name] = None
        else:
            rules = json.loads(path.read_text(encoding="utf-8")).get("rules") or []
            required[name] = {
                check["context"]
                for rule in rules
                if rule.get("type") == "required_status_checks"
                for check in rule["parameters"]["required_status_checks"]
            }
    return required[name]


for line in lista.read_text(encoding="utf-8").splitlines():
    if not line.strip() or line.startswith("#"):
        continue
    job, *rulesets = line.split()
    if job not in jobs:
        report(f"job `{job}` z listy wymaganych kontroli nie istnieje w żadnym workflow.")
    for name in rulesets:
        known = contexts(name)
        if known is not None and job not in known:
            report(f"kontekst `{job}` nie jest wymaganą kontrolą w zestawie reguł `{name}`.")

if not found:
    print("Reguły gałęzi wymagają wszystkich jobów z listy.")
sys.exit(found)
PY
