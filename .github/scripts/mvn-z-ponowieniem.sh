#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: mvn-z-ponowieniem.sh <argumenty mvnw…>, z katalogu z ./mvnw (backend/). Błąd pobrania z Maven Central
# ponawia z -U po 5 sekundach, a błąd pobrania dystrybucji wrappera dodatkowo po 20 sekundach. Błąd kompilacji,
# testu albo qulice kończy przebieg. Wyczerpanie prób pobrania zgłasza błąd Maven Central.
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
central_error='^\[(ERROR|FATAL)\].*(Could not (find|transfer) artifact|cached in the local repository).*(central \(|repo\.maven\.apache\.org)'
central_error+='|^(wget|curl):.*repo\.maven\.apache\.org'
wrapper_error='^(wget|curl):.*repo\.maven\.apache\.org/maven2/org/apache/maven/apache-maven/'

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
if ((status != 0)) && grep -qE "$wrapper_error" "$log"; then
  echo "::warning title=Maven Central::Pobranie dystrybucji Mavena nadal się nie powiodło. Ponowienie za 20 sekund."
  sleep 20
  status=0
  run -U "$@" || status=$?
fi
if ((status != 0)) && grep -qE "$central_error" "$log"; then
  echo "::error title=Maven Central::Po ponowieniu nie udało się pobrać zależności serwera. Sprawdź dostępność repozytorium i artefaktu."
fi
exit "$status"
