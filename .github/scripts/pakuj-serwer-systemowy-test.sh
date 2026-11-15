#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test pakuj-serwer-systemowy.sh: skrypt pakuje serwer bez testów i qulice, a pobranie zależności ponawia wspólny
# mvn-z-ponowieniem.sh (jego przypadki sprawdza mvn-z-ponowieniem-test.sh). Tu sprawdzamy tylko argumenty mvnw.
script="$(cd "$(dirname "$0")" && pwd)/pakuj-serwer-systemowy.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
cd "$test_dir"
printf '#!/usr/bin/env bash\nprintf "%%s\\n" "$*" > args\n' > mvnw
chmod +x mvnw

bash "$script"
want='-B -q -DskipTests -Dqulice.skip=true -Djacoco.skip=true package'
if [[ $(cat args) != "$want" ]]; then
  echo "BŁĄD: mvnw dostał „$(cat args)”, oczekiwano „${want}”"
  exit 1
fi
echo "OK: argumenty pakowania serwera"

bash "$script" -o
if [[ $(cat args) != "-o ${want}" ]]; then
  echo "BŁĄD: dodatkowy argument nie trafił przed argumenty pakowania: „$(cat args)”"
  exit 1
fi
echo "OK: dodatkowe argumenty idą przed argumentami pakowania"
