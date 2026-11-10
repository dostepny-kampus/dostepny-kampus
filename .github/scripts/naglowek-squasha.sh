#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: naglowek-squasha.sh <tytuł PR> <numer PR>. Kolejka scalania robi squash z nagłówkiem „<tytuł> (#N)”, a commitlint
# odrzuca nagłówek dłuższy niż 100 znaków (znaków, nie bajtów). Skrypt sprawdza to już na PR i podaje obecną długość.
tytul="${1?Podaj tytuł PR}"
numer="${2:?Podaj numer PR}"
limit=100

export LC_ALL=C.UTF-8
naglowek="${tytul} (#${numer})"
dlugosc="${#naglowek}"
if ((dlugosc > limit)); then
  dopisek=$((${#numer} + 4))
  echo "::error title=Tytuł PR::Nagłówek squasha „${naglowek}” ma ${dlugosc} znaków, limit ${limit}; tytuł może mieć najwyżej $((limit - dopisek)) znaków (jest ${#tytul})."
  exit 1
fi
echo "Nagłówek squasha ma ${dlugosc} znaków (limit ${limit})."
