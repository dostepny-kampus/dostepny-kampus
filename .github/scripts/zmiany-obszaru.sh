#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zmiany-obszaru.sh <klucz> <ścieżka>... Zapisuje do $GITHUB_OUTPUT „<klucz>=true”, gdy ostatni commit
# (względem pierwszego rodzica, checkout z fetch-depth 2) zmienia którąś ścieżkę (także pathspec git, np. ':(glob)…'),
# inaczej „<klucz>=false”. Na PR to commit scalenia, w grupie kolejki commit tej pozycji na szczycie poprzedniej:
# zmiany PR stojących wyżej sprawdzają ich własne grupy, a reguła kolejki ALLGREEN nie scala grupy przed zielonymi
# grupami przed nią. Przy HEADGREEN grupa musiałaby porównywać od bazy gałęzi.
key="${1:?Podaj klucz wyniku}"
shift
if (($# == 0)); then
  echo "Podaj co najmniej jedną ścieżkę obszaru." >&2
  exit 2
fi

if git diff --quiet HEAD^1 HEAD -- "$@"; then
  echo "${key}=false" >>"$GITHUB_OUTPUT"
else
  echo "${key}=true" >>"$GITHUB_OUTPUT"
fi
