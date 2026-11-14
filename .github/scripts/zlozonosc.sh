#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Użycie: zlozonosc.sh <obecne etykiety issue, po jednej w wierszu> < treść issue.
# Z sekcji `### Rozwiązanie`, `### Zasięg`, `### Weryfikacja`, `### Ryzyko`, `### Odniesienia` i
# `### Warianty i otwarte pytania` (formularze Zadanie i Błąd) wylicza poziom złożoności: najwyższy z trzech
# pierwszych czynników, +1 za każde ryzyko inne niż „brak” (łącznie najwyżej +1), powyżej 4 „do podziału”.
# Wypisuje polecenia `zdejmij:<etykieta>`, `dodaj:<etykieta>` i `brak:<opis>` dla każdego błędu opisu.
# Przy błędzie opisu żadnej etykiety złożoności nie dodaje i zdejmuje obecne. Issue z etykietą `epik` pomija.
current="${1:-}"
text="$(tr -d '\r')"

labels=('złożoność: 1 mechaniczna' 'złożoność: 2 według wzorca' 'złożoność: 3 wymaga analizy'
  'złożoność: 4 projektowa' 'złożoność: do podziału')
solution=('opisane w issue' 'jest wzorzec w kodzie' 'do wyboru spośród znanych' 'do zaprojektowania')
scope=('jedno miejsce' 'jeden moduł' 'kilka modułów' 'cała aplikacja albo kontrakt między modułami')
check=('CI sprawdza w pełni' 'CI i krótki przegląd' 'częściowo ręcznie' 'tylko ręcznie')
risks=('brak' 'reguły albo CI' 'bezpieczeństwo' 'migracja danych' 'publiczne API')

has() { grep -qxF -- "$1" <<<"$current"; }

if has epik; then
  exit 0
fi

errors=()

# section <nagłówek>: treść sekcji bez pustych linii brzegowych; kod 1, gdy sekcji nie ma.
section() {
  awk -v head="### $1" '
    $0 == head { found = 1; inside = 1; next }
    inside && /^### / { inside = 0 }
    inside { lines[++n] = $0 }
    END {
      first = 1; while (first <= n && lines[first] ~ /^[[:space:]]*$/) first++
      last = n; while (last >= first && lines[last] ~ /^[[:space:]]*$/) last--
      for (i = first; i <= last; i++) print lines[i]
      exit !found
    }' <<<"$text"
}

# filled <nagłówek>: sekcja jest i nie jest pusta (formularz wpisuje `_No response_` w puste pole).
filled() {
  local value
  value="$(section "$1")" || return 1
  [[ -n $value && $value != '_No response_' ]]
}

# factor <nagłówek> <nazwa tablicy wartości>: ustawia $rank na numer wartości (1–4) albo zapisuje błąd.
rank=0
factor() {
  local -n allowed="$2"
  local value i
  rank=0
  if ! value="$(section "$1")"; then
    errors+=("$1: brak sekcji „### $1”.")
    return
  fi
  for i in "${!allowed[@]}"; do
    if [[ $value == "${allowed[$i]}" ]]; then
      rank=$((i + 1))
      return
    fi
  done
  local list
  list="$(printf '%s, ' "${allowed[@]}")"
  errors+=("$1: wartość „${value}” spoza listy: ${list%, }.")
}

level=0
for pair in 'Rozwiązanie:solution' 'Zasięg:scope' 'Weryfikacja:check'; do
  factor "${pair%%:*}" "${pair#*:}"
  if ((rank > level)); then level=$rank; fi
done

if value="$(section 'Ryzyko')"; then
  raised=0
  none=0
  IFS=',' read -ra picked <<<"$value"
  for item in "${picked[@]}"; do
    item="$(sed 's/^[[:space:]]*//; s/[[:space:]]*$//' <<<"$item")"
    if [[ $item == brak ]]; then
      none=1
    elif printf '%s\n' "${risks[@]}" | grep -qxF -- "$item"; then
      raised=1
    else
      errors+=("Ryzyko: wartość „${item}” spoza listy: $(printf '%s, ' "${risks[@]}" | sed 's/, $//').")
      raised=1
    fi
  done
  if ((none == 1 && raised == 1)); then
    errors+=('Ryzyko: „brak” razem z innym ryzykiem.')
  fi
  level=$((level + raised))
else
  errors+=('Ryzyko: brak sekcji „### Ryzyko”.')
fi

if ! section 'Odniesienia' >/dev/null; then
  errors+=('Odniesienia: brak sekcji „### Odniesienia”.')
elif ! filled 'Odniesienia'; then
  errors+=('Odniesienia: puste; podaj wzorzec, moduły albo ryzyko.')
fi

if ((${#errors[@]} == 0 && (level == 3 || level == 4))) && ! filled 'Warianty i otwarte pytania'; then
  errors+=('Warianty i otwarte pytania: wymagane od poziomu 3.')
fi

target=''
if ((${#errors[@]} == 0)); then
  target="${labels[$((level > 4 ? 4 : level - 1))]}"
fi

for name in "${labels[@]}"; do
  if [[ $name != "$target" ]] && has "$name"; then
    echo "zdejmij:${name}"
  fi
done
if [[ -n $target ]] && ! has "$target"; then
  echo "dodaj:${target}"
fi
for error in "${errors[@]}"; do
  echo "brak:${error}"
done
exit 0
