#!/usr/bin/env bash
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
set -euo pipefail

# Test uruchom-serwer-systemowy.sh na atrapach docker, java i curl: kolejność startu usług, konto magazynu
# osobno od --wait, serwer z pliku JAR bez -plain i błąd z dziennikiem, gdy serwer nie odpowiada.
script="$(cd "$(dirname "$0")" && pwd)/uruchom-serwer-systemowy.sh"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
mkdir -p "$test_dir/bin" "$test_dir/repo/infra" "$test_dir/repo/backend/target" "$test_dir/tmp"
printf 'DK_TEST=1\n' > "$test_dir/repo/infra/.env.example"
touch "$test_dir/repo/backend/target/serwer.jar" "$test_dir/repo/backend/target/serwer-plain.jar"
printf '#!/usr/bin/env bash\nprintf "docker %%s\n" "$*" >> "%s/log"\n' "$test_dir" > "$test_dir/bin/docker"
printf '#!/usr/bin/env bash\nprintf "java %%s DK_TEST=%%s\n" "$*" "$DK_TEST" >> "%s/log"\necho dziennik-serwera\n' \
  "$test_dir" > "$test_dir/bin/java"
printf '#!/usr/bin/env bash\nexit "${CURL_KOD:-0}"\n' > "$test_dir/bin/curl"
chmod +x "$test_dir/bin/"*
export PATH="$test_dir/bin:$PATH" RUNNER_TEMP="$test_dir/tmp"
cd "$test_dir/repo"

bash "$script"
sleep 1
want="docker compose -f infra/docker-compose.yml up -d --wait postgis zdjecia logowanie
docker compose -f infra/docker-compose.yml run --rm zdjecia-konto
java -jar backend/target/serwer.jar DK_TEST=1"
if [[ $(cat "$test_dir/log") != "$want" ]]; then
  echo "BŁĄD: wywołania „$(cat "$test_dir/log")”, oczekiwano „${want}”"
  exit 1
fi
echo "OK: usługi z --wait, konto magazynu osobno, serwer z JAR bez -plain i ze zmiennymi z infra/.env"

if out="$(CURL_KOD=7 CZAS_STARTU=1 bash "$script")"; then
  echo "BŁĄD: skrypt przeszedł, choć serwer nie odpowiadał"
  exit 1
fi
if [[ $out != *"::error title=Test systemowy::"* || $out != *dziennik-serwera* ]]; then
  echo "BŁĄD: brak błędu albo dziennika serwera w „${out}”"
  exit 1
fi
echo "OK: brak odpowiedzi serwera kończy się błędem z dziennikiem"
