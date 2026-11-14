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
# Dla każdego joba każdego workflow:
#   3. brak `continue-on-error` na poziomie joba: zamienia porażkę w sukces dla `needs` i dla całego przebiegu,
#      więc kolejka scala zmianę z czerwonym jobem;
#   4. `continue-on-error` kroku tylko przy zapisie raportu diagnostycznego (`actions/upload-artifact`).
# Dla każdego joba workflow uruchamianego przez pull_request, pull_request_target albo merge_group:
#   5. job jest wymaganą kontrolą albo stoi w `needs` wymaganego joba; inaczej jego porażka nie zatrzymuje kolejki.
#      Wyjątki (automatyzacja, joby tylko po scaleniu, powtórzony sygnał wymaganej kontroli) są w pliku wyjątków:
#      `<plik workflow>/<job> <powód>`; wpis bez powodu albo dla joba, którego nie ma, to błąd.
# Użycie: wymagane-joby.sh [katalog workflow] [plik listy] [plik wyjątków].
# Kod wyjścia: 0 = czysto, 1 = naruszenie, 2 = błąd narzędzia.
workflows="${1:-.github/workflows}"
lista="${2:-$(dirname "${BASH_SOURCE[0]}")/wymagane-kontrole.txt}"
wyjatki="${3:-$(dirname "${BASH_SOURCE[0]}")/joby-bez-kontroli.txt}"

"${PYTHON:-python3}" - "$workflows" "$lista" "$wyjatki" <<'PY'
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
exceptions = {}
for line in pathlib.Path(sys.argv[3]).read_text(encoding="utf-8").splitlines():
    if line.strip() and not line.startswith("#"):
        entry, _, reason = line.strip().partition(" ")
        exceptions[entry] = reason.strip()
PR_EVENTS = {"pull_request", "pull_request_target", "merge_group"}
seen = set()


def report(path, job, message, condition):
    global found
    found = 1
    print(f"::error file={path},title=Wymagany job::{job}: {message} Gotowy warunek: if: ${{{{ {condition} }}}}")


for path in sorted(list(workflows.glob("*.yml")) + list(workflows.glob("*.yaml"))):
    document = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    events = document.get(True, document.get("on")) or {}
    events = {events} if isinstance(events, str) else set(events)
    jobs = document.get("jobs") or {}
    consumed = set()
    for other_key, other in jobs.items():
        if isinstance(other, dict) and {other_key, other.get("name")} & required:
            other_needs = other.get("needs") or []
            consumed.update([other_needs] if isinstance(other_needs, str) else other_needs)
    for key, job in jobs.items():
        entry = f"{path.name}/{key}"
        seen.add(entry)
        if not isinstance(job, dict) or not events & PR_EVENTS or entry in exceptions:
            continue
        if not ({key, job.get("name")} & required) and key not in consumed:
            found = 1
            print(f"::error file={path},title=Job bez wymaganego odbiorcy::{key}: job sprawdza PR, ale nie jest wymagany "
                  "i nie stoi w `needs` wymaganego joba, więc jego porażka nie zatrzymuje kolejki. Dodaj go do "
                  "wymaganych kontroli albo do `needs` wymaganego joba; automatyzacja trafia do pliku wyjątków z powodem.")
    for key, job in (document.get("jobs") or {}).items():
        if not isinstance(job, dict):
            continue
        if job.get("continue-on-error", False) is not False:
            found = 1
            print(f"::error file={path},title=Job z continue-on-error::{key}: `continue-on-error` na poziomie joba "
                  "zamienia jego porażkę w sukces, więc kolejka scala zmianę z czerwonym jobem. Usuń klucz; "
                  "raport, który nie może blokować, nie może też padać.")
        for step in job.get("steps") or []:
            if not isinstance(step, dict) or step.get("continue-on-error", False) is False:
                continue
            if str(step.get("uses") or "").startswith("actions/upload-artifact@"):
                continue
            found = 1
            label = step.get("name") or step.get("uses") or step.get("run", "")[:40]
            print(f"::error file={path},title=Krok z continue-on-error::{key}: krok „{label}” ma `continue-on-error`; "
                  "dozwolone tylko przy zapisie raportu diagnostycznego (actions/upload-artifact).")
        if not ({key, job.get("name")} & required):
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

for entry, reason in exceptions.items():
    if not reason:
        found = 1
        print(f"::error title=Wyjątek joba::{entry}: wyjątek bez powodu.")
    if entry not in seen:
        found = 1
        print(f"::error title=Wyjątek joba::{entry}: wyjątek dla joba, którego nie ma; usuń wpis.")

if not found:
    print("Każdy job sprawdzający PR blokuje kolejkę, a żaden wymagany job nie zostanie pominięty po błędzie.")
sys.exit(found)
PY
