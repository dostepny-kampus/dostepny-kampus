#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test kontrola-sprintu-cel.sh na repozytorium: main = A; sprint/3 = A (już w main); sprint/4 = A, B (android),
# C (docs); sprint/10 = C, D (android). Własne `gh` czyta i dopisuje statusy w pliku „sha stan czas”.
script="$(cd "$(dirname "$0")" && pwd)/kontrola-sprintu-cel.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0
mkdir "$workdir/bin"
cat >"$workdir/bin/gh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
if [[ $1 == api && $2 == -X ]]; then
  sha="${4##*/}"
  shift 4
  while (($#)); do
    [[ $2 == state=* ]] && state="${2#state=}"
    shift 2
  done
  echo "$sha $state $(date -u +%Y-%m-%dT%H:%M:%SZ)" >>"$FAKE_STATUSY"
  echo "$sha $state" >>"$FAKE_LOG"
elif [[ $1 == api && $2 == repos/*/status ]]; then
  sha="${2%/status}"
  grep "^${sha##*/} " "$FAKE_STATUSY" | tail -n 1 | cut -d' ' -f2- || true
else
  echo "nieoczekiwane wywołanie gh: $*" >&2
  exit 2
fi
SH
chmod +x "$workdir/bin/gh"

origin="$workdir/origin"
git init -q -b main "$origin"
commit() {
  mkdir -p "$origin/$(dirname "$1")"
  echo "$1" >"$origin/$1"
  git -C "$origin" add -A
  git -C "$origin" -c user.name=t -c user.email=t@t commit -qm "$1"
  git -C "$origin" rev-parse HEAD
}
a="$(commit README.md)"
git -C "$origin" branch sprint/3
git -C "$origin" checkout -q -b sprint/4
b="$(commit android/a.kt)"
c="$(commit docs/x.md)"
git -C "$origin" checkout -q -b sprint/10
d="$(commit android/b.kt)"
clone="$workdir/klon"
git clone -q "file://$origin" "$clone"

# przypadek <nazwa> <statusy „sha stan czas”, \n> <oczekiwane wyjście> <oczekiwane statusy „sha stan”, \n> [GALAZ]
przypadek() {
  local name="$1" status=0 out
  printf '%b\n' "$2" | sed '/^$/d' >"$workdir/statusy"
  : >"$workdir/output"
  : >"$workdir/log"
  out="$(cd "$clone" && GITHUB_REPOSITORY=org/repo GITHUB_OUTPUT="$workdir/output" GALAZ="${5:-}" \
    TERAZ="$(date -u -d 2026-11-15T12:00:00Z +%s)" FAKE_STATUSY="$workdir/statusy" FAKE_LOG="$workdir/log" \
    PATH="$workdir/bin:$PATH" bash "$script" 2>&1)" || status=$?
  if ((status == 0)) && [[ $(cat "$workdir/output") == "$(printf '%b' "$3")" ]] &&
    [[ $(cat "$workdir/log") == "$(printf '%b' "$4")" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod ${status}, wyjście „$(cat "$workdir/output")”, statusy „$(cat "$workdir/log")”"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

przypadek "zmiana Androida od ostatniego statusu: sprawdzenie najniższego sprintu" \
  "$a success 2026-11-15T10:00:00Z" "jest=true\ngalaz=sprint/4\nsha=$c\nod=$a" "$c pending"
przypadek "tylko dokumentacja: success bez budowy i następna gałąź" \
  "$b success 2026-11-15T10:00:00Z" "jest=true\ngalaz=sprint/10\nsha=$d\nod=$c" "$c success\n$d pending"
przypadek "głowy ze statusem są pomijane" \
  "$c success 2026-11-15T10:00:00Z\n$d failure 2026-11-15T10:00:00Z" "jest=false" ""
przypadek "świeży pending to przebieg w toku" \
  "$c pending 2026-11-15T11:00:00Z\n$d success 2026-11-15T10:00:00Z" "jest=false" ""
przypadek "pending sprzed ponad 3 godzin to przerwany przebieg" \
  "$c pending 2026-11-15T08:00:00Z\n$d success 2026-11-15T10:00:00Z" "jest=true\ngalaz=sprint/4\nsha=$c\nod=" \
  "$c pending"
przypadek "error na głowie: ponowne sprawdzenie mimo braku zmian" \
  "$b success 2026-11-15T10:00:00Z\n$c error 2026-11-15T11:00:00Z" "jest=true\ngalaz=sprint/4\nsha=$c\nod=$b" \
  "$c pending"
przypadek "brak statusów w historii: sprawdzenie bez punktu odniesienia" "" \
  "jest=true\ngalaz=sprint/4\nsha=$c\nod=" "$c pending"
przypadek "GALAZ wymusza sprawdzenie mimo statusu i braku zmian" \
  "$b success 2026-11-15T10:00:00Z\n$d success 2026-11-15T10:00:00Z" "jest=true\ngalaz=sprint/10\nsha=$d\nod=$b" \
  "$d pending" sprint/10

status=0
(cd "$clone" && GITHUB_REPOSITORY=org/repo GITHUB_OUTPUT="$workdir/output" GALAZ=sprint/99 PATH="$workdir/bin:$PATH" \
  bash "$script" >/dev/null 2>&1) || status=$?
if ((status == 1)); then
  echo "OK: nieistniejąca GALAZ to błąd"
else
  echo "BŁĄD: nieistniejąca GALAZ: kod ${status}, oczekiwano 1"
  failures=$((failures + 1))
fi

exit "$((failures > 0 ? 1 : 0))"
