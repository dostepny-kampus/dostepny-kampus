#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/mvn-z-ponowieniem.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
cd "$test_dir"
mkdir bin
printf '#!/usr/bin/env bash\nexit 0\n' > bin/sleep
cat > mvnw <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> attempts
if [[ $CASE == transient && $(wc -l < attempts | tr -d " ") == 2 || $CASE == success ]]; then
  exit 0
fi
case "$CASE" in
  compile) echo '[ERROR] Compilation failure' ;;
  *) echo '[FATAL] Could not find artifact org.example:parent:pom:1.0 in central (https://repo.maven.apache.org/maven2)' ;;
esac
exit 17
SH
chmod +x mvnw bin/sleep

run_case() {
  local name="$1" expected="$2" count="$3" status=0
  rm -f attempts
  CASE="$name" PATH="$test_dir/bin:$PATH" bash "$script" -B verify > output 2>&1 || status=$?
  if [[ $status != "$expected" || $(wc -l < attempts | tr -d " ") != "$count" ]]; then
    cat output
    exit 1
  fi
  grep -qE '(^| )-B verify$' attempts
  if ((count == 2)); then
    tail -n 1 attempts | grep -qE '(^| )-U( |$)'
  fi
  if [[ $name == persistent ]]; then
    grep -q '::error title=Maven Central::' output
  fi
  echo "OK: $name"
}

run_case success 0 1
run_case transient 0 2
run_case persistent 17 2
run_case compile 17 1
