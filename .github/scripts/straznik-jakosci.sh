#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

base="$1"
head="$2"
range="$base...$head"
merge_base="$(git merge-base "$base" "$head")"
title='Strażnik jakości'
android_rule='Wyciszenia w kodzie Androida są zabronione; sprzeczność reguł rozwiązuje się w konfiguracji'
android_rule+=' z powodem w PR.'

# Samo słowo, bez wymogu `@`: łapie też pełne nazwy (`@kotlin.Suppress`), aliasy importu i typealias.
suppression='\b(Suppress(Lint|Warnings|FBWarnings)?|TargetApi)\b|@\s*([\w.]+\.)?Generated\b'
suppression+='|NOPMD|CHECKSTYLE[:.]OFF|@checkstyle|ktlint-disable|noinspection|<!--\s*suppress\b'
suppression+='|detekt:disable|lint:ignore|[\w-]+:(ignore|targetApi)\s*='

rule_files='^android/config/|^android/build-logic/|^android/buildSrc/|(^|/)gradle\.properties$'
rule_files+='|^\.github/(workflows|scripts|commitlint)/|^\.github/dependabot\.ya?ml$|^commitlint\.config\.'
rule_files+='|^\.commitlintrc|(^|/)\.gitleaks|(^|/)\.editorconfig$|(^|/)\.gitattributes$|(^|/)lint\.xml$'
rule_files+='|^api/redocly\.yaml$|^docs/(\.vale\.ini$|\.markdownlint|styles/|scripts/verify\.py$|scripts/check_ids\.py$)'

pom_words='jacoco|qulice|<minimum>|<exclude|skip|haltOnFailure|testFailureIgnore'
gradle_words='kover|detekt|\blint\b|baseline|ignoreFailures|abortOnError|[wW]arningsAsErrors'
gradle_words+='|checkAllWarnings|suppressWarnings|nowarn|Xsuppress|Xwarning-level|failOnSeverity'
gradle_words+='|verifyRoborazzi|enabled\s*=\s*false|[iI]sEnabled|setEnabled|onlyIf|excludedTaskNames'
gradle_words+='|setDependsOn|tasks\.(named|getByName)\("check"\)'

hard=()
soft=()
dependabot=0
[[ "${PR_AUTHOR:-}" == 'dependabot[bot]' ]] && dependabot=1

sep=$'\x1f'
add_hard() { hard+=("$1$sep$2$sep$3 ${android_rule}"); }
add_soft() { soft+=("$1$sep$2$sep$3"); }

# Zmienione linie: "+<TAB>numer w head<TAB>treść" oraz "-<TAB>numer w bazie<TAB>treść".
# `--text` nie pozwala ukryć linii atrybutem `-diff` albo `binary` w `.gitattributes`.
diff_lines() {
  git --literal-pathspecs diff --text -M -U0 --no-color --no-ext-diff --no-textconv "$range" -- "$@" | awk '
    /^@@/ {
      h = 1
      match($0, /-[0-9]+/); o = substr($0, RSTART + 1, RLENGTH - 1)
      match($0, /\+[0-9]+/); n = substr($0, RSTART + 1, RLENGTH - 1)
      next
    }
    !h { next }
    /^\+/ { print "+\t" n++ "\t" substr($0, 2); next }
    /^-/ { print "-\t" o++ "\t" substr($0, 2) }'
}

# Numery linii wewnątrz <plugin> JaCoCo albo qulice.
pom_context() {
  awk '
    /<plugin>/ { inside = 1; n = 0; aid = "" }
    inside {
      buf[++n] = NR
      if (aid == "" && match($0, /<artifactId>[^<]*/)) aid = substr($0, RSTART + 12, RLENGTH - 12)
    }
    /<\/plugin>/ { if (aid ~ /jacoco|qulice/) for (i = 1; i <= n; i++) print buf[i]; inside = 0 }'
}

# Numery linii wewnątrz bloków kover, detekt, lint i opcji kompilatora skryptu Gradle.
gradle_context() {
  awk '
    {
      line = $0; sub(/\/\/.*/, "", line)
      for (i = 1; i <= depth; i++) {
        if (stack[i] ~ /(^|\.)(kover|detekt|lint|compilerOptions|kotlinOptions)$/) { print NR; break }
      }
      while (match(line, /[A-Za-z_][A-Za-z0-9_.]*[ \t]*(\([^)]*\))?[ \t]*\{|\{|\}/)) {
        tok = substr(line, RSTART, RLENGTH); line = substr(line, RSTART + RLENGTH)
        if (tok == "}") { if (depth > 0) depth-- } else { sub(/[ \t]*(\(.*)?\{$/, "", tok); stack[++depth] = tok }
      }
    }'
}

# Zmieniona linia dotyczy reguł, gdy pasuje do słów kluczowych albo leży w bloku reguł.
context_findings() {
  local path="$1" old="$2" lines="$3" words="$4" ctx="$5" side num text old_ctx new_ctx
  old_ctx=" $(git show "$merge_base:$old" 2>/dev/null | "$ctx" | tr '\n' ' ' || true) "
  new_ctx=" $(git show "$head:$path" | "$ctx" | tr '\n' ' ') "
  while IFS=$'\t' read -r side num text; do
    if grep -qP -- "$words" <<<"$text" ||
      { [[ $side == + && $new_ctx == *" $num "* ]]; } ||
      { [[ $side == - && $old_ctx == *" $num "* ]]; }; then
      if [[ $side == + ]]; then
        add_soft "$path" "$num" "zmiana konfiguracji reguł jakości: ${text:0:120}"
      else
        add_soft "$path" "" "usunięta linia ${num} konfiguracji reguł jakości: ${text:0:120}"
      fi
    fi
  done <<<"$lines"
}

normalize_versions() {
  cut -f3- | sed -E 's/@[^[:space:]#]+([[:space:]]+#.*)?$/@/; s/>[^<]*</></' | sort
}

# Dependabot: każda zmieniona linia to wersja w `uses:` albo w znaczniku <...version>.
only_version_bumps() {
  local lines="$1"
  local uses='^[[:space:]]*(-[[:space:]]+)?uses:[[:space:]]*[^@[:space:]]+@[^[:space:]#]+([[:space:]]+#.*)?$'
  local version='^[[:space:]]*<([A-Za-z0-9.-]*version)>[^<]*</\1>[[:space:]]*$'
  [[ -n $lines ]] || return 1
  if cut -f3- <<<"$lines" | grep -vE "$uses" | grep -vqE "$version"; then
    return 1
  fi
  [[ "$(grep -P '^-\t' <<<"$lines" | normalize_versions)" == "$(grep -P '^\+\t' <<<"$lines" | normalize_versions)" ]]
}

check_file() {
  local status="$1" path="$2" old="${3:-$2}" rule=0 lines="" name side num text
  [[ $path =~ (^|/)(build|target)/ ]] && return 0
  if grep -qE -- "$rule_files" <<<"$path"$'\n'"$old"; then rule=1; fi
  if [[ $path == */GenerateDesignTokens.kt && $old == "$path" ]]; then rule=0; fi
  if [[ $status == D ]]; then
    if ((rule)); then add_soft "$path" "" "usunięty plik reguł jakości"; fi
    return 0
  fi
  if ((rule && dependabot)) || [[ $path =~ (^|/)pom\.xml$|\.gradle$|\.(kt|kts|java|xml)$ ]]; then
    lines="$(diff_lines "$old" "$path")"
  fi
  if ((dependabot)) && [[ $status == M ]] && only_version_bumps "$lines"; then
    echo "Dependabot: w ${path} zmieniły się tylko wersje."
  elif ((rule)); then
    add_soft "$path" "" "zmieniony plik reguł jakości"
  elif [[ $path =~ (^|/)pom\.xml$ ]]; then
    context_findings "$path" "$old" "$lines" "$pom_words" pom_context
  elif [[ $path =~ \.gradle(\.kts)?$ ]]; then
    context_findings "$path" "$old" "$lines" "$gradle_words" gradle_context
  fi
  name="${path##*/}"
  name="${name,,}"
  if [[ $name =~ baseline[^/]*\.(xml|txt|json|sarif|ya?ml)$ && ! $name =~ ^(baseline|startup)-prof\.txt$ ]]; then
    if [[ $path == android/* ]]; then add_hard "$path" "" "plik baseline."; else add_soft "$path" "" "plik baseline"; fi
  fi
  if [[ $path == android/* && $path =~ \.gradle(\.kts)?$ ]]; then
    while IFS=$'\t' read -r side num text; do
      add_hard "$path" "$num" "plik baseline w konfiguracji: ${text:0:120}."
    done < <(grep -P -- '^\+\t[0-9]+\t.*\bbaseline\b' <<<"$lines" || true)
  fi
  [[ $path =~ \.(kt|kts|java|xml)$ ]] || return 0
  while IFS=$'\t' read -r side num text; do
    if [[ $path == android/* ]]; then
      add_hard "$path" "$num" "wyciszenie analizy: ${text:0:120}."
    else
      add_soft "$path" "$num" "wyciszenie analizy: ${text:0:120}"
    fi
  done < <(grep -P -- "^\+\t[0-9]+\t.*(?:${suppression})" <<<"$lines" || true)
}

while IFS= read -r -d '' status; do
  IFS= read -r -d '' path
  old=""
  if [[ $status == R* || $status == C* ]]; then
    old="$path"
    IFS= read -r -d '' path
  fi
  check_file "${status:0:1}" "$path" "$old"
done < <(git diff -z -M --name-status --diff-filter=ACDMR "$range")

esc_data() {
  local s="${1//%/%25}"
  s="${s//$'\r'/%0D}"
  printf '%s' "${s//$'\n'/%0A}"
}

esc_prop() {
  local s
  s="$(esc_data "$1")"
  s="${s//:/%3A}"
  printf '%s' "${s//,/%2C}"
}

report() {
  local level="$1" entry path line msg
  shift
  for entry in "$@"; do
    IFS="$sep" read -r path line msg <<<"$entry"
    msg="$(esc_data "${path}${line:+:${line}}: ${msg}")"
    echo "::${level} file=$(esc_prop "$path")${line:+,line=${line}},title=${title}::${msg}"
  done
}

# Linia w komentarzu HTML albo w bloku kodu nie jest widoczna w opisie PR, więc się nie liczy.
gate=0
body="$(printf '%s\n' "${PR_BODY:-}" | tr -d '\r' | perl -0pe 's/<!--.*?-->//gs; s/^```.*?^```//gms')"
if ((!dependabot)) && grep -qE '^Zmiana reguł jakości:[[:space:]]*[^[:space:]]' <<<"$body"; then
  gate=1
fi

result=0
if ((${#hard[@]})); then
  report error "${hard[@]}"
  result=1
fi
if ((${#soft[@]} && gate)); then
  report warning "${soft[@]}"
  echo "Opis PR ma linię „Zmiana reguł jakości:”, więc zmiany reguł są ostrzeżeniami do przeglądu."
elif ((${#soft[@]})); then
  report error "${soft[@]}"
  echo "Zmiana reguł jakości wymaga w opisie PR linii „Zmiana reguł jakości: <powód>”."
  result=1
fi
if ((${#hard[@]} + ${#soft[@]} == 0)); then
  echo "Brak wyciszeń analizy, plików baseline i zmian reguł jakości."
fi
exit "$result"
