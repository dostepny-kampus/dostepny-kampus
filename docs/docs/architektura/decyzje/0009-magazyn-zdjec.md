# ADR-0009: Magazyn zdjęć zgłoszeń

Status: przyjęta. Data: 2026-11-03.

## Kontekst i problem

Każde zgłoszenie ma co najmniej jedno zdjęcie (BR-7, BR-8). Serwer usuwa z niego metadane przed udostępnieniem (NFR-26). Diagram kontenerów miał osobny magazyn zdjęć MinIO, zgodny z S3.

Obrazów MinIO nie da się już pobrać ani z Docker Hub (`minio/minio`), ani z `quay.io/minio/minio`. Sprawdziliśmy to 3 listopada 2026 roku. Bez obrazu MinIO nie działa w `infra/docker-compose.yml` ani w testach serwera.

## Rozważone opcje

- RustFS [1]: magazyn zgodny z S3 na licencji Apache 2.0, z obrazem `rustfs/rustfs`.
- SeaweedFS [2]: magazyn plików z bramą S3 na licencji Apache 2.0.
- Zdjęcia w PostgreSQL w kolumnie `bytea`.
- Stary obraz MinIO z lokalnej kopii.

## Decyzja

Wybieramy RustFS 1.0.4 w miejsce MinIO.

- Serwer łączy się z magazynem przez protokół S3 z biblioteki AWS SDK for Java 2.x [3], bez klienta jednego dostawcy. Zmiana magazynu to zmiana adresu i kluczy.
- Wersja obrazu jest przypięta w `infra/docker-compose.yml`. Test czyta ją z tego pliku tak samo jak obraz PostGIS.
- RustFS uruchamia się jak MinIO: jeden kontener, klucz i sekret w zmiennych `RUSTFS_ACCESS_KEY` i `RUSTFS_SECRET_KEY`, port 9000.

SeaweedFS odrzucamy, bo dostęp do S3 wymaga osobnego pliku z kluczami. PostgreSQL odrzucamy, bo zdjęcia rosłyby w kopiach zapasowych bazy i odchodzimy od diagramu kontenerów bez potrzeby. Starego obrazu MinIO nie da się pobrać na nowej maszynie ani w CI.

## Konsekwencje

- Dobrze: magazyn działa lokalnie i w CI bez logowania do rejestru.
- Dobrze: kod serwera nie zależy od RustFS, więc powrót do MinIO albo inny magazyn S3 nie zmienia kodu.
- Gorzej: RustFS to młody projekt. Wersję podnosimy tylko po teście zapisu i odczytu zdjęcia.
- Gorzej: AWS SDK jest większy niż klient jednego magazynu.

Przy odczycie zdjęcia brak obiektu daje HTTP `404`. Brak skonfigurowanego zasobnika daje HTTP `500`, a szczegóły błędu trafiają do logu serwera.

## Źródła

1. [RustFS: repozytorium i licencja](https://github.com/rustfs/rustfs)
2. [SeaweedFS: repozytorium](https://github.com/seaweedfs/seaweedfs)
3. [AWS SDK for Java 2.x: Amazon S3](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html)
