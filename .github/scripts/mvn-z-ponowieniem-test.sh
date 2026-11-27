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
printf '#!/usr/bin/env bash\nprintf "%%s\\n" "$1" >> delays\n' > bin/sleep
cat > mvnw <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> attempts
attempt=$(wc -l < attempts | tr -d " ")
if [[ $CASE == transient && $attempt == 2 || $CASE == transient-wrapper && $attempt == 3 || $CASE == success ]]; then
  exit 0
fi
if [[ $CASE == wrapper-then-test && $attempt == 2 ]]; then
  echo '[ERROR] Tests run: 1, Failures: 1'
  exit 17
fi
case "$CASE" in
  compile) echo '[ERROR] Compilation failure' ;;
  *wrapper*) echo 'wget: Failed to fetch https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.10.2/apache-maven-3.10.2-bin.zip' ;;
  *) echo '[FATAL] Could not find artifact org.example:parent:pom:1.0 in central (https://repo.maven.apache.org/maven2)' ;;
esac
exit 17
SH
chmod +x mvnw bin/sleep

run_case() {
  local name="$1" expected="$2" count="$3" status=0
  rm -f attempts delays
  CASE="$name" PATH="$test_dir/bin:$PATH" bash "$script" -B verify > output 2>&1 || status=$?
  if [[ $status != "$expected" || $(wc -l < attempts | tr -d " ") != "$count" ]]; then
    cat output
    exit 1
  fi
  grep -qE '(^| )-B verify$' attempts
  if ((count > 1)); then
    tail -n 1 attempts | grep -qE '(^| )-U( |$)'
  fi
  if ((count == 3)); then
    diff -u <(printf '5\n20\n') delays
  fi
  if [[ $name == persistent* ]]; then
    grep -q '::error title=Maven Central::' output
  fi
  echo "OK: $name"
}

run_case success 0 1
run_case transient 0 2
run_case transient-wrapper 0 3
run_case persistent-wrapper 17 3
run_case wrapper-then-test 17 2
run_case persistent 17 2
run_case compile 17 1
