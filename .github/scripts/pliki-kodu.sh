#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

# Jedna lista typów plików z kodem dla kontroli nagłówków (naglowki.sh) i znaczników zadań (todo-issue.sh).
# Plik do wczytania przez `source`; wzorce w postaci przyjmowanej przez `git ls-files`.
wzorce_plikow_kodu() {
  printf '%s\n' '*.kt' '*.kts' '*.java' '*.py' '*.sh' '*.sql'
}
