#!/usr/bin/env bash
set -euo pipefail

base="$1"
head="$2"
limit="${3:-300}"

stats="$(git diff --numstat "$base...$head" -- . \
  ':(glob,exclude)data/**' \
  ':(glob,exclude)docs/docs/_generated/**' \
  ':(glob,exclude)**/generated/**' \
  ':(glob,exclude)**/package-lock.json' \
  ':(glob,exclude)**/*.lock' \
  ':(glob,exclude)**/gradle.lockfile' \
  ':(glob,exclude)**/gradle/wrapper/**' |
  awk -F '\t' '{ a = ($1 == "-") ? 0 : $1; d = ($2 == "-") ? 0 : $2; print a + d "\t" $3 }')"
total="$(awk -F '\t' '{ s += $1 } END { print s + 0 }' <<<"$stats")"

if ((total > limit)); then
  echo "::error title=Rozmiar PR::${total} zmienionych linii, limit ${limit} (bez danych, plików lock i kodu generowanego)."
  echo "Największe pliki (dodane + usunięte):"
  sort -t $'\t' -k1,1nr <<<"$stats" | head -n 10
  exit 1
fi
echo "Rozmiar PR: ${total} zmienionych linii, limit ${limit}."
