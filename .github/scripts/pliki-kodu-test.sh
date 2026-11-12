#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test wspólnej listy typów plików z kodem (pliki-kodu.sh): lista ma sześć wzorców i korzystają z niej
# obie kontrole, więc nowy typ pliku trafia naraz do nagłówków i do znaczników zadań.
dir="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=pliki-kodu.sh
source "$dir/pliki-kodu.sh"
failures=0

check() {
  if [[ $2 == "$3" ]]; then
    echo "OK: $1"
  else
    echo "BŁĄD: $1: jest „$2”, oczekiwano „$3”"
    failures=$((failures + 1))
  fi
}

check "lista wzorców" "$(wzorce_plikow_kodu | tr '\n' ' ')" '*.kt *.kts *.java *.py *.sh *.sql '
for user in naglowki.sh todo-issue.sh; do
  check "${user} wczytuje wspólną listę" "$(grep -c 'wzorce_plikow_kodu' "$dir/$user")" 1
  check "${user} nie ma własnej listy typów" "$(grep -c "'\*\.kts'" "$dir/$user" || true)" 0
done

exit "$((failures > 0))"
