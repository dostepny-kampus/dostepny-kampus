#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test runnera testów skryptów (testy-skryptow.sh <katalog>) na sztucznych katalogach ze skryptami.
runner="$(cd "$(dirname "$0")" && pwd)/testy-skryptow.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
dir="$workdir/skrypty"
failures=0

fresh() {
  rm -rf "$dir"
  mkdir "$dir"
}

# write_script <plik> <treść>
write_script() {
  printf '#!/usr/bin/env bash\n%s\n' "$2" >"$dir/$1"
}

# run_case <nazwa> <oczekiwany kod> <fragment wyniku>
run_case() {
  local name="$1" want="$2" text="$3" out status=0
  out="$(GITHUB_STEP_SUMMARY=/dev/null bash "$runner" "$dir" 2>&1)" || status=$?
  if ((status == want)) && [[ $out == *"$text"* ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status} (oczekiwano ${want}), szukano „${text}” w: ${out//$'\n'/ | }"
    failures=$((failures + 1))
  fi
}

fresh
write_script a.sh 'true'
write_script a-test.sh 'echo "OK: przypadek"'
run_case "test z przypadkiem i skrypt z testem są zaliczone" 0 "zaliczone przypadki: 1"

fresh
write_script a.sh 'true'
write_script a-test.sh 'exit 0'
run_case "test z kodem 0 bez przypadku jest porażką" 1 "nie wykonał żadnego przypadku"

fresh
write_script a.sh 'true'
write_script a-test.sh 'echo "OK: przypadek"'
write_script b.sh 'true'
run_case "skrypt bez pliku testu jest porażką" 1 "b.sh nie ma pliku testu"

fresh
write_script a.sh 'true'
write_script a-test.sh 'echo "OK: przypadek"; exit 3'
run_case "test z kodem różnym od zera jest porażką" 1 "zakończył się kodem 3"

fresh
write_script a.sh 'true'
write_script a-test.sh 'echo "OK: przypadek"; echo "BŁĄD: inny przypadek"'
run_case "BŁĄD przy kodzie 0 jest porażką" 1 "zakończył się kodem 1"

fresh
run_case "katalog bez testów jest porażką" 1 "Nie znaleziono żadnego pliku"

exit "$((failures > 0 ? 1 : 0))"
