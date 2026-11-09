#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: etykieta-typu.sh <tytuł PR> <obecne etykiety PR, po jednej w wierszu>.
# Z typu Conventional Commits w tytule wylicza etykietę typu i wypisuje polecenia: `dodaj:<etykieta>` albo
# `zdejmij:<etykieta>`. fix → bug, feat → enhancement, docs → documentation, test → test, ci i build → ci;
# pozostałe typy bez etykiety. Zdejmowane są tylko etykiety typu, które wyłącznie typ nadaje (bug, enhancement);
# etykiety documentation, test i ci mogą pochodzić też z mapy plików (labeler), więc nigdy ich tu nie zdejmujemy.
title="${1:?Podaj tytuł PR}"
current="${2:-}"

type=''
if [[ $title =~ ^([a-z]+)(\([^\)]*\))?\!?:[[:space:]] ]]; then
  type="${BASH_REMATCH[1]}"
fi

case "$type" in
  fix) target=bug ;;
  feat) target=enhancement ;;
  docs) target=documentation ;;
  test) target=test ;;
  ci | build) target=ci ;;
  *) target='' ;;
esac

has() { grep -qxF -- "$1" <<<"$current"; }

for owned in bug enhancement; do
  if [[ $owned != "$target" ]] && has "$owned"; then
    echo "zdejmij:${owned}"
  fi
done
if [[ -n $target ]] && ! has "$target"; then
  echo "dodaj:${target}"
fi
exit 0
