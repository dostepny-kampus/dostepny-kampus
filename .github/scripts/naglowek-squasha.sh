#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: naglowek-squasha.sh <tytuł PR> <numer PR> [nagłówek jedynego commita]. Kolejka scalania robi squash
# z nagłówkiem „<tytuł> (#N)”, a commitlint odrzuca nagłówek dłuższy niż 100 znaków (znaków, nie bajtów). PR z jednym
# commitem dostaje w squashu nagłówek tego commita zamiast tytułu (#1093), więc wtedy liczy się nagłówek commita.
# Skrypt sprawdza to już na PR, podaje obecną długość i wypisuje nagłówek squasha na ostatniej linii wyjścia.
tytul="${1?Podaj tytuł PR}"
numer="${2:?Podaj numer PR}"
commit="${3:-}"
limit=100

export LC_ALL=C.UTF-8
zrodlo="${commit:-$tytul}"
naglowek="${zrodlo} (#${numer})"
dlugosc="${#naglowek}"
if ((dlugosc > limit)); then
  dopisek=$((${#numer} + 4))
  if [[ -n $commit ]]; then
    co="nagłówek jedynego commita PR może mieć najwyżej $((limit - dopisek)) znaków (jest ${#commit})"
  else
    co="tytuł może mieć najwyżej $((limit - dopisek)) znaków (jest ${#tytul})"
  fi
  echo "::error title=Tytuł PR::Nagłówek squasha „${naglowek}” ma ${dlugosc} znaków, limit ${limit}; ${co}."
  exit 1
fi
echo "Nagłówek squasha ma ${dlugosc} znaków (limit ${limit})."
