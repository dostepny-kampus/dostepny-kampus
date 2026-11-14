#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Uruchamiany z backend/. Ponawia tylko błąd rozwiązywania artefaktu z Central.
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
args=(-B -q -DskipTests -Dqulice.skip=true -Djacoco.skip=true package)
central_error='^\[(ERROR|FATAL)\].*(Could not (find|transfer) artifact|cached in the local repository).*(central \(|repo\.maven\.apache\.org)'

package() {
  ./mvnw "$@" "${args[@]}" 2>&1 | tee "$log"
}

status=0
package || status=$?
if ((status == 0)) || ! grep -qE "$central_error" "$log"; then
  exit "$status"
fi

echo "::warning title=Maven Central::Pobranie zależności nie powiodło się. Ponowienie za 5 sekund."
sleep 5
status=0
package -U || status=$?
if ((status != 0)) && grep -qE "$central_error" "$log"; then
  echo "::error title=Maven Central::Po ponowieniu nie udało się pobrać zależności serwera. Sprawdź dostępność repozytorium i artefaktu."
fi
exit "$status"
