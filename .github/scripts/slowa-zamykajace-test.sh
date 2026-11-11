#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test słów zamykających issue w opisie PR (funkcja numery_zamykanych_issue z slowa-zamykajace.sh).
# shellcheck source=slowa-zamykajace.sh
source "$(cd "$(dirname "$0")" && pwd)/slowa-zamykajace.sh"
failures=0

# run_case <nazwa> <tekst> <oczekiwane numery, rozdzielone \n>
run_case() {
  local name="$1" input="$2" expected="$3" actual
  actual="$(printf '%s\n' "$input" | numery_zamykanych_issue)"
  if [[ $actual == "$(printf '%b' "$expected")" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: wynik „${actual//$'\n'/ }”, oczekiwano „${expected//\\n/ }”"
    failures=$((failures + 1))
  fi
}

for word in close closes closed fix fixes fixed resolve resolves resolved; do
  run_case "słowo ${word}" "${word} #7" '7'
done
run_case "wielkość liter nie ma znaczenia" 'CLOSES #10, Fixes #11, rEsOlVeD #12' '10\n11\n12'
run_case "wszystkie formy w jednym tekście" $'Closes #1\nfixed #2\nresolves #3' '1\n2\n3'
run_case "numery są posortowane rosnąco jako liczby" $'closes #10\nfixes #9' '9\n10'
run_case "ten sam numer wypisany raz" 'closes #5 oraz fixes #5' '5'
run_case "kilka słów w jednej linii" 'Closes #3, fixes #2' '2\n3'
run_case "słowo w środku innego słowa nie liczy się: disclose" 'disclose #12' ''
run_case "słowo w środku innego słowa nie liczy się: prefix" 'prefix #13' ''
run_case "słowo w środku innego słowa nie liczy się: unresolved" 'unresolved #14' ''
run_case "słowo w środku innego słowa nie liczy się: enclosed" 'enclosed #15' ''
run_case "numer bez znaku # nie liczy się" 'closes 16' ''
run_case "numer z innego repozytorium nie liczy się" 'closes owner/repo#17' ''
run_case "sam numer bez słowa nie liczy się" 'zob. #18' ''
run_case "pusty opis" '' ''

exit "$((failures > 0 ? 1 : 0))"
