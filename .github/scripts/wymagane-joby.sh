#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Kontrola warunków wymaganych jobów: wymagany job nie może zostać pominięty (a więc zaliczony), gdy padnie job,
# od którego zależy. Czyta workflow jako YAML. Dla joba z listy wymaganych kontroli, który ma `needs`:
#   1. warunek `if` istnieje i zawiera `!cancelled()` albo `always()`; bez tego GitHub pomija job po błędzie w `needs`;
#   2. jeśli warunek wymaga wyjścia joba z `needs` (`needs.<job>.outputs.<x> == ...`), ma też gałąź
#      `needs.<job>.result != 'success'`, bo po błędzie wyjścia są puste.
# Użycie: wymagane-joby.sh [katalog workflow] [plik listy]. Kod wyjścia: 0 = czysto, 1 = naruszenie, 2 = błąd narzędzia.
workflows="${1:-.github/workflows}"
lista="${2:-$(dirname "${BASH_SOURCE[0]}")/wymagane-kontrole.txt}"

"${PYTHON:-python3}" - "$workflows" "$lista" <<'PY'
import pathlib
import re
import sys

try:
    import yaml
except ImportError:
    print("::error title=Błąd narzędzia::brak modułu yaml dla python3")
    sys.exit(2)

sys.stdout.reconfigure(encoding="utf-8")
workflows, lista = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
required = {
    line.split()[0]
    for line in lista.read_text(encoding="utf-8").splitlines()
    if line.strip() and not line.startswith("#")
}
found = 0


def report(path, job, message, condition):
    global found
    found = 1
    print(f"::error file={path},title=Wymagany job::{job}: {message} Gotowy warunek: if: ${{{{ {condition} }}}}")


for path in sorted(list(workflows.glob("*.yml")) + list(workflows.glob("*.yaml"))):
    document = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    for key, job in (document.get("jobs") or {}).items():
        if not isinstance(job, dict) or not ({key, job.get("name")} & required):
            continue
        needs = job.get("needs") or []
        needs = [needs] if isinstance(needs, str) else needs
        if not needs:
            continue
        condition = str(job.get("if") or "")
        own = re.sub(r"^\$\{\{|\}\}$", "", condition.strip()).strip() or "<warunek>"
        if "!cancelled()" not in condition and "always()" not in condition:
            report(path, key, "ma `needs`, ale warunek `if` nie zawiera `!cancelled()`; po błędzie joba z `needs` "
                   "zostanie pominięty i zaliczony.", f"!cancelled() && ({own})")
            continue
        for need in needs:
            uses_output = re.search(rf"needs\.{re.escape(need)}\.outputs\.[\w-]+\s*==", condition)
            has_branch = re.search(rf"needs\.{re.escape(need)}\.result\s*!=\s*'success'", condition)
            if uses_output and not has_branch:
                report(path, key, f"wymaga wyjścia joba `{need}`, ale nie ma gałęzi dla jego błędu; po błędzie "
                       "wyjście jest puste i job zostanie pominięty.",
                       f"!cancelled() && (needs.{need}.result != 'success' || {own})")

if not found:
    print("Żaden wymagany job nie zostanie pominięty po błędzie joba, od którego zależy.")
sys.exit(found)
PY
