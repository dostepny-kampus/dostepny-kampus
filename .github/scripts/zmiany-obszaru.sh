#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zmiany-obszaru.sh <klucz> <ścieżka>... Zapisuje do $GITHUB_OUTPUT „<klucz>=true”, gdy PR albo grupa
# w kolejce zmienia którąś ścieżkę (także pathspec git, np. ':(glob)…'), inaczej „<klucz>=false”. PR: ostatni
# commit względem pierwszego rodzica (checkout z fetch-depth 2). Grupa w kolejce (EVENT=merge_group): zmiany
# od GROUP_BASE_SHA do GROUP_HEAD_SHA.
key="${1:?Podaj klucz wyniku}"
shift
if (($# == 0)); then
  echo "Podaj co najmniej jedną ścieżkę obszaru." >&2
  exit 2
fi

from=HEAD^1
to=HEAD
if [[ ${EVENT:-} == merge_group ]]; then
  git fetch --no-tags --depth=1 origin "$GROUP_BASE_SHA"
  from=$GROUP_BASE_SHA
  to=$GROUP_HEAD_SHA
fi
if git diff --quiet "$from" "$to" -- "$@"; then
  echo "${key}=false" >>"$GITHUB_OUTPUT"
else
  echo "${key}=true" >>"$GITHUB_OUTPUT"
fi
