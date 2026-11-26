#!/bin/sh
# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
# Klucz, którym serwer łączy się z magazynem zdjęć: tylko lista, odczyt, zapis i usuwanie
# obiektów kubełka zdjęć. Klucz główny RustFS zostaje do administracji. Skrypt zakłada
# kubełek, politykę i użytkownika; ponowne uruchomienie nadpisuje politykę i sekret.
set -eu

rc alias set glowny "$DK_PHOTOS_ENDPOINT" "$DK_PHOTOS_ROOT_ACCESS_KEY" "$DK_PHOTOS_ROOT_SECRET_KEY" -q
rc bucket create "glowny/$DK_PHOTOS_BUCKET" --ignore-existing -q
cat > /tmp/polityka.json <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::$DK_PHOTOS_BUCKET"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
      "Resource": ["arn:aws:s3:::$DK_PHOTOS_BUCKET/*"]
    }
  ]
}
JSON
rc admin policy create glowny zdjecia-serwera /tmp/polityka.json -q
rc admin user add glowny "$DK_PHOTOS_ACCESS_KEY" "$DK_PHOTOS_SECRET_KEY" -q
rc admin policy attach glowny zdjecia-serwera --user "$DK_PHOTOS_ACCESS_KEY" -q
