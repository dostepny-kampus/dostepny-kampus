#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test wyliczania etykiety złożoności z treści issue (zlozonosc.sh <etykiety> < treść). Kod wyjścia 0 = wszystkie przypadki.
script="$(cd "$(dirname "$0")" && pwd)/zlozonosc.sh"
failures=0

# body <rozwiązanie> <zasięg> <weryfikacja> <ryzyko> [odniesienia] [warianty]: treść issue taka jak z formularza.
body() {
  printf '### Problem\n\nCoś.\n\n### Rozwiązanie\n\n%s\n\n### Zasięg\n\n%s\n\n### Weryfikacja\n\n%s\n\n' "$1" "$2" "$3"
  printf '### Ryzyko\n\n%s\n\n### Odniesienia\n\n%s\n\n### Warianty i otwarte pytania\n\n%s\n' "$4" "${5-Wzorzec: plik.}" "${6-_No response_}"
}

# run_case <nazwa> <treść> <obecne etykiety, \n> <oczekiwane polecenia, \n>
run_case() {
  local name="$1" text="$2" labels="$3" expected="$4" actual
  actual="$(bash "$script" "$(printf '%b' "$labels")" <<<"$text")"
  if [[ $actual == "$(printf '%b' "$expected")" ]]; then
    echo "OK: ${name}"
  else
    echo "BŁĄD: ${name}: wynik „${actual//$'\n'/; }”, oczekiwano „${expected//\\n/; }”"
    failures=$((failures + 1))
  fi
}

l1='złożoność: 1 mechaniczna'
l2='złożoność: 2 według wzorca'
l3='złożoność: 3 wymaga analizy'
l4='złożoność: 4 projektowa'
l5='złożoność: do podziału'

run_case "same jedynki → poziom 1" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'brak')" '' "dodaj:${l1}"
run_case "najwyższy czynnik wyznacza poziom 2" "$(body 'jest wzorzec w kodzie' 'jedno miejsce' 'CI sprawdza w pełni' 'brak')" '' "dodaj:${l2}"
run_case "poziom 3 z wariantami" "$(body 'opisane w issue' 'kilka modułów' 'CI sprawdza w pełni' 'brak' 'Moduły: a, b.' 'A albo B.')" '' "dodaj:${l3}"
run_case "poziom 4 z wariantami" "$(body 'do zaprojektowania' 'jeden moduł' 'CI i krótki przegląd' 'brak' 'x' 'Decyzja: y.')" '' "dodaj:${l4}"
run_case "ryzyko podnosi o 1" "$(body 'jest wzorzec w kodzie' 'jeden moduł' 'CI i krótki przegląd' 'reguły albo CI' 'x' 'y')" '' "dodaj:${l3}"
run_case "kilka ryzyk podnosi tylko o 1" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'bezpieczeństwo, publiczne API')" '' "dodaj:${l2}"
run_case "powyżej 4 → do podziału" "$(body 'do zaprojektowania' 'jeden moduł' 'tylko ręcznie' 'migracja danych')" '' "dodaj:${l5}"
run_case "zmiana poziomu zdejmuje starą etykietę" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'brak')" "${l2}\\nci" "zdejmij:${l2}\\ndodaj:${l1}"
run_case "etykieta już jest" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'brak')" "${l1}" ''
run_case "końce linii CRLF" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'brak' | sed 's/$/\r/')" '' "dodaj:${l1}"
run_case "epik bez oceny" 'Lista podzadań.' "epik\\n${l3}" ''
run_case "brak sekcji" '### Problem\n\nCoś.' "${l1}" "zdejmij:${l1}\\nbrak:Rozwiązanie: brak sekcji „### Rozwiązanie”.\\nbrak:Zasięg: brak sekcji „### Zasięg”.\\nbrak:Weryfikacja: brak sekcji „### Weryfikacja”.\\nbrak:Ryzyko: brak sekcji „### Ryzyko”.\\nbrak:Odniesienia: brak sekcji „### Odniesienia”."
run_case "wartość spoza listy" "$(body 'łatwe' 'jedno miejsce' 'CI sprawdza w pełni' 'brak')" '' 'brak:Rozwiązanie: wartość „łatwe” spoza listy: opisane w issue, jest wzorzec w kodzie, do wyboru spośród znanych, do zaprojektowania.'
run_case "nieznane ryzyko" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'brak, wybuch')" '' 'brak:Ryzyko: wartość „wybuch” spoza listy: brak, reguły albo CI, bezpieczeństwo, migracja danych, publiczne API.\nbrak:Ryzyko: „brak” razem z innym ryzykiem.'
run_case "puste Odniesienia" "$(body 'opisane w issue' 'jedno miejsce' 'CI sprawdza w pełni' 'brak' '_No response_')" '' 'brak:Odniesienia: puste; podaj wzorzec, moduły albo ryzyko.'
run_case "poziom 3 bez wariantów" "$(body 'do wyboru spośród znanych' 'jeden moduł' 'CI sprawdza w pełni' 'brak')" '' 'brak:Warianty i otwarte pytania: wymagane od poziomu 3.'

exit "$((failures > 0 ? 1 : 0))"
