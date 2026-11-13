#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: wynik-pit.sh <kod wyjścia Mavena> <plik mutations.xml>. Zamienia wynik PIT na komunikat, który mówi,
# co się stało: mutacja przeżyła, mutacja nie ma testu albo PIT nie doszedł do analizy (błąd budowania, testów
# lub narzędzia). Kod 0 tylko wtedy, gdy Maven zakończył się sukcesem.
maven="${1:?Podaj kod wyjścia Mavena}"
report="${2:?Podaj plik raportu mutations.xml}"

if ((maven == 0)); then
  echo "Wszystkie mutacje wykryte przez testy."
  exit 0
fi

if [[ ! -f $report ]]; then
  echo "::error title=PIT::PIT nie zakończył analizy: błąd budowania, testów albo narzędzia. Przyczyna jest w logu tego kroku, raportu mutacji nie ma."
  exit 1
fi

count() {
  { grep -o "status=['\"]$1['\"]" "$report" || true; } | wc -l | tr -d ' '
}
survived="$(count SURVIVED)"
uncovered="$(count NO_COVERAGE)"

if ((survived == 0 && uncovered == 0)); then
  echo "::error title=PIT::PIT zakończył się błędem bez mutacji, która przeżyła: przyczyna jest w logu tego kroku (np. przekroczony czas albo błąd narzędzia)."
  exit 1
fi

echo "::error title=PIT::Mutacja przeżyła: ${survived}, bez pokrycia: ${uncovered}. Raport w artefakcie pit-raport wskazuje linię i mutację. Dopisz w teście asercję zachowania, którą ta mutacja psuje."
exit 1
