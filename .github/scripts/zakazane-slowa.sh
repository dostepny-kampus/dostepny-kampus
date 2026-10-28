#!/usr/bin/env bash
set -euo pipefail

base="$1"
head="$2"
found=0

if [[ -z "${FORBIDDEN_PATTERNS:-}" ]]; then
  echo "::error title=Zakazane słowa::Brak sekretu FORBIDDEN_PATTERNS (repozytorium albo Dependabota)."
  exit 2
fi

check() {
  local where="$1" text="$2" matches status=0
  matches="$(printf '%s\n' "$text" | grep -oP -- "$FORBIDDEN_PATTERNS" 2>/dev/null)" || status=$?
  if ((status == 1)); then
    return 0
  fi
  if ((status > 1)); then
    echo "::error title=Zakazane słowa::Niepoprawny wzorzec FORBIDDEN_PATTERNS."
    exit 2
  fi
  echo "::error title=Zakazane słowa::${where}: $(sort -uf <<<"$matches" | paste -sd ',' -)"
  found=1
}

check "gałąź" "${PR_BRANCH:-}"
check "tytuł PR" "${PR_TITLE:-}"
check "opis PR" "${PR_BODY:-}"
commits="$(git rev-list "$base..$head")"
for sha in $commits; do
  check "commit ${sha:0:7}" "$(git log -1 --format=%B "$sha")"
done

if ((found == 0)); then
  echo "Brak zakazanych słów w gałęzi, tytule, opisie i commitach PR."
fi
exit "$found"
