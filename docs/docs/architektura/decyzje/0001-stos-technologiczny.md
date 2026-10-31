# ADR-0001: Stos technologiczny

Status: przyjęta. Data: 2026-10-28. Źródło: decyzje projektu nr 2, 3, 28 i 45.

## Kontekst i problem

Aplikacja czyta stan satelitów GNSS do mapy nieba (US6.5) i pracuje bez sieci (N6). Serwer liczy zapytania przestrzenne: budynek dla punktu, podobne zgłoszenia w promieniu, przeliczenie na PUWG 1992. Pierwszy opis tematu zakładał Flutter i FastAPI.

## Rozważone opcje

- Aplikacja: Kotlin z Jetpack Compose albo Flutter.
- Serwer: Spring Boot z PostgreSQL i PostGIS, FastAPI z PostGIS, Supabase albo dane tylko w pakiecie aplikacji.
- Supabase przeniósłby przebieg weryfikacji i konflikty wersji do SQL. Bez serwera nie ma weryfikacji ani synchronizacji.
- Java na serwerze: 21 albo 25 (obie LTS [1]).
- Spring Boot: linia 3.x albo 4.x.

## Decyzja

- Aplikacja: Kotlin 2.x, Jetpack Compose, Material 3, od Androida 8.0 (API 26).
- Serwer: Spring Boot 4.1.2 na Java 25, budowany Mavenem. Linia 3.x nie ma już wsparcia OSS, dlatego 4.x [2].
- Baza: PostgreSQL z rozszerzeniem PostGIS.
- Java 25 zastępuje Java 21 z decyzji projektu nr 3. Narzędzie, które nie działa na JDK 25, zostaje na JDK 21 z dowodem w PR.

## Konsekwencje

- Dobrze: natywny dostęp do `GnssStatus` [3] i ostre lintery Kotlina ([ADR-0004](0004-gotowe-reguly-i-biblioteki.md)).
- Dobrze: Spring Modulith i ArchUnit pilnują granic modułów serwera, a geometrię liczy PostGIS.
- Dobrze: Spring to stos najlepiej znany autorowi, a własny serwer z logowaniem pokazuje pełną część serwerową.
- Gorzej: dwa języki (Kotlin, Java) i dwa systemy budowania (Gradle, Maven).
- Gorzej: tylko Android, bez iOS.
- Gorzej: Spring wymaga więcej kodu niż FastAPI.

## Źródła

1. [Oracle: plan wsparcia Java SE](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
2. [Spring Boot: okresy wsparcia](https://spring.io/projects/spring-boot#support)
3. [Android: GnssStatus](https://developer.android.com/reference/android/location/GnssStatus)
