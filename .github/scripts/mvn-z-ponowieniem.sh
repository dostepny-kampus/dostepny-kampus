#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: mvn-z-ponowieniem.sh <argumenty mvnw…>, z katalogu z ./mvnw (backend/). Ponawia raz, z -U, tylko błąd
# pobrania z Maven Central: artefaktu albo samej dystrybucji Mavena przez wrapper. Błąd kompilacji, testu albo qulice
# nie jest ponawiany. Gdy ponowienie też zawiedzie na Central, błąd mówi o sieci (`::error title=Maven Central::`),
# a nie o budowie.
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
central_error='^\[(ERROR|FATAL)\].*(Could not (find|transfer) artifact|cached in the local repository).*(central \(|repo\.maven\.apache\.org)'
central_error+='|^(wget|curl):.*repo\.maven\.apache\.org'

run() {
  ./mvnw "$@" 2>&1 | tee "$log"
}

status=0
run "$@" || status=$?
if ((status == 0)) || ! grep -qE "$central_error" "$log"; then
  exit "$status"
fi

echo "::warning title=Maven Central::Pobranie zależności nie powiodło się. Ponowienie za 5 sekund."
sleep 5
status=0
run -U "$@" || status=$?
if ((status != 0)) && grep -qE "$central_error" "$log"; then
  echo "::error title=Maven Central::Po ponowieniu nie udało się pobrać zależności serwera. Sprawdź dostępność repozytorium i artefaktu."
fi
exit "$status"
