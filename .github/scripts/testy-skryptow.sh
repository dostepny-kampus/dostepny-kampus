#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Uruchamia wszystkie pliki *-test.sh z katalogu ($1, domyślnie .github/scripts) i sprawdza, że każdy skrypt
# z tego katalogu ma swój plik testu. Kod 0 = wszystkie testy zaliczone, każdy z co najmniej jednym przypadkiem.
# Test podstawia własne `gh`, a reszta narzędzi (jq, java) pochodzi z obrazu runnera.
set -o pipefail
shopt -s nullglob

dir="${1:-.github/scripts}"
summary="${GITHUB_STEP_SUMMARY:-/dev/null}"

tests=("$dir"/*-test.sh)
if ((${#tests[@]} == 0)); then
  echo "::error title=Testy skryptów::Nie znaleziono żadnego pliku ${dir}/*-test.sh."
  exit 1
fi

failed=0
for script in "$dir"/*.sh; do
  [[ $script == *-test.sh ]] && continue
  if [[ ! -f "${script%.sh}-test.sh" ]]; then
    echo "::error title=Testy skryptów::${script} nie ma pliku testu ${script%.sh}-test.sh."
    failed=1
  fi
done

for t in "${tests[@]}"; do
  echo "::group::${t}"
  log="$(mktemp)"
  status=0
  bash "$t" 2>&1 | tee "$log" || status=$?
  cases="$(grep -c '^OK:' "$log" || true)"
  errors="$(grep -c '^BŁĄD:' "$log" || true)"
  rm -f "$log"
  echo "::endgroup::"
  # Test, który wypisał BŁĄD, ale zwrócił kod 0, też jest porażką.
  if ((status == 0 && errors > 0)); then
    status=1
  fi
  # Test, który zakończył się kodem 0 bez żadnego przypadku (np. przedwczesne `exit 0`), niczego nie sprawdził.
  if ((status == 0 && cases == 0)); then
    echo "::error title=Testy skryptów::${t} zakończył się kodem 0, ale nie wykonał żadnego przypadku (brak linii OK:)."
    status=1
  elif ((status == 0)); then
    echo "${t}: kod 0, zaliczone przypadki: ${cases}"
    echo "- ${t}: ${cases} przypadków" >>"$summary"
  else
    echo "::error title=Testy skryptów::${t} zakończył się kodem ${status} (zaliczone przypadki: ${cases}, błędy: ${errors})."
  fi
  ((status == 0)) || failed=1
done
exit "$failed"
