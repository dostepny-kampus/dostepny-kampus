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
patterns='tajneslowo'

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
  out="$(cd "$repo" && FORBIDDEN_PATTERNS="$patterns" PR_AUTHOR="$author" PR_BRANCH=gałąź \
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

# Dopisek składamy z części, żeby ten plik sam nie zawierał sprawdzanego słowa.
trailer="Co-auth""ored-by"
patterns="tajneslowo|$trailer"
bot_line="$trailer: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>"
run_case "dependabot: dopisek GitHuba o współautorze-bocie jest pomijany" 0 "$bot" 'build(deps): podbij' 'opis'   "$(printf 'build(deps): podbij

%s
' "$bot_line")"
run_case "dopisek o innym współautorze nadal jest błędem" 1 "$bot" 'build(deps): podbij' 'opis'   "$(printf 'build(deps): podbij

%s
' "$trailer: Ktoś <ktos@example.com>")"
run_case "dependabot: zakazane słowo obok dopisku nadal jest błędem" 1 "$bot" 'build(deps): podbij' 'opis'   "$(printf 'build(deps): tajneslowo

%s
' "$bot_line")"
patterns='tajneslowo'

# authors_case <nazwa> <oczekiwany kod> <wzorce> <autor PR> <e-mail autora drugiego commitu>
# Gałąź PR ma dwa commity; drugi ma podanego autora. Przy innym autorze GitHub dopisze w kolejce wiersz
# o współautorze, więc kontrola na PR ma to zgłosić, gdy ten wiersz jest zakazany.
authors_case() {
  local name="$1" expected="$2" forbidden="$3" author="$4" email="$5" status=0 out
  rm -rf "$repo"
  mkdir -p "$repo"
  (
    cd "$repo"
    git init --quiet
    git config user.name Test
    git config user.email test@example.com
    git commit --quiet --allow-empty -m baza
    git commit --quiet --allow-empty -m 'build: zmiana'
    git -c user.name=Inny -c user.email="$email" commit --quiet --allow-empty -m 'build: druga zmiana'
  )
  out="$(cd "$repo" && FORBIDDEN_PATTERNS="$forbidden" PR_AUTHOR="$author" PR_BRANCH=gałąź \
    PR_TITLE='build: zmiana' PR_BODY=opis bash "$script" HEAD~2 HEAD 2>&1)" || status=$?
  if ((status == expected)); then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected}"
    printf '%s\n' "$out" | sed 's/^/    /'
    failures=$((failures + 1))
  fi
}

authors_case "#1447: commit innej osoby w gałęzi PR" 1 "tajneslowo|$trailer" autor-pr inny@example.com
authors_case "#1447: wszystkie commity jednej osoby" 0 "tajneslowo|$trailer" autor-pr test@example.com
authors_case "#1447: dopisek o współautorze nie jest zakazany" 0 tajneslowo autor-pr inny@example.com
authors_case "#1447: grupa kolejki bez autora PR" 0 "tajneslowo|$trailer" '' inny@example.com
authors_case "#1447: Dependabot ma własny wyjątek" 0 "tajneslowo|$trailer" "$bot" inny@example.com

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
