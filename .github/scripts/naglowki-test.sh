#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/naglowki.sh"
detekt="$(git rev-parse --show-toplevel)/android/config/detekt/detekt.yml"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
failures=0

copyright="Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko"

kotlin_file() {
  printf '/*\n * Dostępny Kampus\n%s\n * %s\n */\nfun main() {}\n' "$1" "$copyright"
}

hash_file() {
  printf '#!/usr/bin/env bash\n# Dostępny Kampus\n%s\n# %s\nset -euo pipefail\n' "$1" "$copyright"
}

run_case() {
  local name="$1" expected_status="$2" path="$3" content="$4" status=0
  rm -rf "${workdir:?}/repo"
  mkdir -p "$workdir/repo"
  (
    cd "$workdir/repo"
    git init --quiet
    mkdir -p "$(dirname "$path")"
    printf '%s' "$content" >"$path"
    git -c core.autocrlf=false add "$path" 2>/dev/null
    bash "$script" >/dev/null 2>&1
  ) || status=$?
  if [[ "$status" -ne "$expected_status" ]]; then
    echo "BŁĄD: ${name}: kod wyjścia ${status}, oczekiwano ${expected_status}"
    failures=$((failures + 1))
  else
    echo "OK: ${name}"
  fi
}

author_case() {
  local author="$1" expected="$2"
  printf '%s\t%s\n' "$expected" "$author" >>"$workdir/authors.tsv"
  run_case "kotlin, autor ${author}" "$expected" src/A.kt "$(kotlin_file " * Author: ${author}")"
  run_case "bash, autor ${author}" "$expected" scripts/a.sh "$(hash_file "# Author: ${author}")"
}

authors=("Vladyslav Yevkov" "Myroslav Hryniuka" "Yevhenii Lukianenko")
for author in "${authors[@]}"; do
  author_case "$author" 0
  for second in "${authors[@]}"; do
    if [[ "$author" != "$second" ]]; then
      author_case "$author, $second" 0
      for third in "${authors[@]}"; do
        if [[ "$third" != "$author" && "$third" != "$second" ]]; then
          author_case "$author, $second, $third" 0
        fi
      done
    fi
  done
done

for invalid in \
  "Jan Kowalski" \
  "Vladyslav Yevkov, Jan Kowalski" \
  "Jan Kowalski, Vladyslav Yevkov" \
  "Vladyslav Yevkov, Vladyslav Yevkov" \
  "Myroslav Hryniuka, Vladyslav Yevkov, Myroslav Hryniuka" \
  "Yevhenii Lukianenko, Yevhenii Lukianenko" \
  "Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov" \
  "Vladyslav Yevkov," \
  "Vladyslav Yevkov, " \
  ", Vladyslav Yevkov" \
  "Vladyslav Yevkov,, Myroslav Hryniuka" \
  "Vladyslav Yevkov,Myroslav Hryniuka" \
  "Vladyslav Yevkov,  Myroslav Hryniuka" \
  " Vladyslav Yevkov" \
  "Vladyslav Yevkov " \
  "Vladyslav Yevkov i ktoś" \
  ""; do
  author_case "$invalid" 1
done
run_case "kotlin, brak linii Author" 1 src/A.kt \
  "$(printf '/*\n * Dostępny Kampus\n * %s\n */\nfun main() {}\n' "$copyright")"
run_case "bash, brak linii Author" 1 scripts/a.sh \
  "$(printf '#!/usr/bin/env bash\n# Dostępny Kampus\n# %s\nset -euo pipefail\n' "$copyright")"

cat >"$workdir/LicensePatternTest.java" <<'JAVA'
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

class LicensePatternTest {
  public static void main(String[] args) throws Exception {
    String prefix = "\\* Author: ";
    String regex = Files.readAllLines(Path.of(args[0])).stream()
        .map(String::stripLeading).filter(line -> line.startsWith(prefix))
        .findFirst().orElseThrow().substring(prefix.length());
    Pattern pattern = Pattern.compile(regex);
    int failures = 0;
    for (String line : Files.readAllLines(Path.of(args[1]))) {
      String[] parts = line.split("\t", 2);
      boolean expected = parts[0].equals("0");
      if (pattern.matcher(parts[1]).matches() != expected) {
        System.out.println("BLAD: detekt, autor " + parts[1]);
        failures++;
      }
    }
    if (failures != 0) {
      throw new AssertionError("Niezgodne przypadki wzorca detekt: " + failures);
    }
    System.out.println("OK: wzorzec detekt dla wszystkich autorów");
  }
}
JAVA
java "$workdir/LicensePatternTest.java" "$detekt" "$workdir/authors.tsv" || failures=$((failures + 1))

exit "$((failures > 0 ? 1 : 0))"
