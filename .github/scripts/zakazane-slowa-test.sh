#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/zakazane-slowa.sh"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
repo="$workdir/repo"
failures=0
bot='dependabot[bot]'

# run_case <nazwa> <oczekiwany kod> <autor> <tytuł> <opis> <komunikat commitu>
run_case() {
  local name="$1" expected="$2" author="$3" title="$4" body="$5" message="$6" status=0 out
  rm -rf "$repo"
  mkdir -p "$repo"
  (
    cd "$repo"
    git init --quiet
    git config user.name Test
    git config user.email test@example.com
    git commit --quiet --allow-empty -m baza
    git commit --quiet --allow-empty -m "$message"
  )
  out="$(cd "$repo" && FORBIDDEN_PATTERNS='tajneslowo' PR_AUTHOR="$author" PR_BRANCH=gałąź \
    PR_TITLE="$title" PR_BODY="$body" bash "$script" HEAD~1 HEAD 2>&1)" || status=$?
  if ((status == expected)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

run_case "zwykły PR: czysty tytuł, opis i commit" 0 autor-pr 'build: zmiana' 'opis' 'build: zmiana'
run_case "zwykły PR: zakazane słowo w opisie" 1 autor-pr 'build: zmiana' 'opis z tajneslowo' 'build: zmiana'
run_case "dependabot: zakazane słowo w opisie jest pomijane" 0 "$bot" 'build(deps): podbij' 'notatki z tajneslowo' 'build(deps): podbij'
run_case "dependabot: zakazane słowo w tytule" 1 "$bot" 'build(deps): tajneslowo' 'opis' 'build(deps): podbij'
run_case "dependabot: zakazane słowo w commicie" 1 "$bot" 'build(deps): podbij' 'opis' 'build(deps): tajneslowo'
run_case "autor podobny do dependabota: opis nadal sprawdzany" 1 'dependabot' 'build: zmiana' 'tajneslowo' 'build: zmiana'

begin_tree() {
  rm -rf "$repo"
  mkdir -p "$repo"
  git -C "$repo" init --quiet
  git -C "$repo" config user.name Test
  git -C "$repo" config user.email test@example.com
  git -C "$repo" commit --quiet --allow-empty -m baza
}

end_tree() {
  local name="$1" expected="$2" status=0 out
  git -C "$repo" add -A
  git -C "$repo" commit --quiet -m 'build: zmiana'
  out="$(cd "$repo" && FORBIDDEN_PATTERNS='tajneslowo' PR_AUTHOR=autor-pr PR_BRANCH=gałąź \
    PR_TITLE='build: zmiana' PR_BODY=opis bash "$script" HEAD~1 HEAD 2>&1)" || status=$?
  if ((status == expected)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

begin_tree
printf 'czysto\ntajneslowo\n' >"$repo/plik.txt"
end_tree "zakazane słowo w dodanej linii" 1

begin_tree
printf 'czysto\n' >"$repo/tajneslowo.txt"
end_tree "zakazane słowo w nazwie pliku" 1

begin_tree
printf 'tajneslowo\nczysto\n' >"$repo/plik.txt"
git -C "$repo" add plik.txt
git -C "$repo" commit --quiet -m 'build: wersja ze slowem'
printf 'czysto\n' >"$repo/plik.txt"
end_tree "słowo tylko w usuniętej linii" 0

begin_tree
printf '* -diff\n' >"$repo/.gitattributes"
git -C "$repo" add .gitattributes
git -C "$repo" commit --quiet -m 'build: atrybut'
printf 'tajneslowo\n' >"$repo/ukryty.txt"
end_tree "zakazane słowo w pliku z atrybutem -diff" 1

exit "$((failures > 0 ? 1 : 0))"
