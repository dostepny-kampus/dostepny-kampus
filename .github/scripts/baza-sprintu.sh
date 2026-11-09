#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: baza-sprintu.sh <gałąź PR>, w klonie z origin/main, origin/sprint/N-1 i origin/sprint/N.
# Kod 0 = baza poprawna, 1 = naruszenie, 2 = nie da się ocenić (brak gałęzi, numer z zerem wiodącym).
# Dla gałęzi sprint/N: poprzedni sprint (sprint/N-1) musi być w main, a sprint/N wyrasta z main.
# Inne gałęzie nie podlegają kontroli.
branch="${1:?Podaj nazwę gałęzi PR}"

if [[ ! $branch =~ ^sprint/([0-9]+)$ ]]; then
  echo "Gałąź ${branch} nie jest gałęzią sprintu, kontrola pominięta."
  exit 0
fi
number="${BASH_REMATCH[1]}"
if [[ $number == 0* ]]; then
  echo "::error title=Baza sprintu::${branch}: numer sprintu ma zero wiodące; użyj sprint/<numer> bez zer."
  exit 2
fi
previous="sprint/$((number - 1))"
found=0

# Bez tych gałęzi nie da się ocenić bazy; to nie jest naruszenie reguły, więc osobny kod i komunikat.
for ref in main "$branch"; do
  if ! git rev-parse --quiet --verify "refs/remotes/origin/${ref}" >/dev/null; then
    echo "::error title=Baza sprintu::brak gałęzi origin/${ref} w klonie (pobierz ją, np. fetch-depth: 0)."
    exit 2
  fi
done

if git rev-parse --quiet --verify "refs/remotes/origin/${previous}" >/dev/null &&
  ! git merge-base --is-ancestor "origin/${previous}" origin/main; then
  echo "::error title=Baza sprintu::${previous}: poprzedni sprint nie jest w main."
  found=1
fi

if ! git merge-base --is-ancestor origin/main "origin/${branch}"; then
  echo "::error title=Baza sprintu::${branch} nie wyrasta z main."
  found=1
fi

if ((found == 0)); then
  echo "Baza sprintu poprawna: poprzedni sprint jest w main, ${branch} wyrasta z main."
fi
exit "$found"
