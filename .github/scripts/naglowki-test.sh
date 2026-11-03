#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/naglowki.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0

copyright="Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko"

kotlin_file() {
  printf '/*\n * Dostępny Kampus\n%s\n * %s\n */\nfun main() {}\n' "$1" "$copyright"
}

hash_file() {
  printf '#!/usr/bin/env bash\n# Dostępny Kampus\n%s\n# %s\nset -euo pipefail\n' "$1" "$copyright"
}

run_case() {
  local name="$1" expected_status="$2" path="$3" content="$4" status=0
  rm -rf "${workdir:?}/repo"
  mkdir -p "$workdir/repo"
  (
    cd "$workdir/repo"
    git init --quiet
    mkdir -p "$(dirname "$path")"
    printf '%s' "$content" >"$path"
    git -c core.autocrlf=false add "$path" 2>/dev/null
    bash "$script" >/dev/null 2>&1
  ) || status=$?
  if [[ "$status" -ne "$expected_status" ]]; then
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected_status}"
    failures=$((failures + 1))
  else
    echo "OK: ${name}"
  fi
}

for author in "Vladyslav Yevkov" "Myroslav Hryniuka" "Yevhenii Lukianenko"; do
  run_case "kotlin, autor ${author}" 0 src/A.kt "$(kotlin_file " * Author: ${author}")"
  run_case "bash, autor ${author}" 0 scripts/a.sh "$(hash_file "# Author: ${author}")"
done

run_case "kotlin, obcy autor" 1 src/A.kt "$(kotlin_file " * Author: Jan Kowalski")"
run_case "bash, obcy autor" 1 scripts/a.sh "$(hash_file "# Author: Jan Kowalski")"
run_case "kotlin, brak linii Author" 1 src/A.kt \
  "$(printf '/*\n * Dostępny Kampus\n * %s\n */\nfun main() {}\n' "$copyright")"
run_case "bash, brak linii Author" 1 scripts/a.sh \
  "$(printf '#!/usr/bin/env bash\n# Dostępny Kampus\n# %s\nset -euo pipefail\n' "$copyright")"

exit "$((failures > 0 ? 1 : 0))"
