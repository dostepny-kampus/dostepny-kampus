# ADR-0004: Gotowe zestawy reguł i biblioteki

Status: przyjęta. Data: 2026-10-28. Źródło: decyzje projektu nr 8, 22, 35, 41 i 46.

## Kontekst i problem

Własne progi linterów są arbitralne i każdy można podważyć. Własny kod infrastruktury (mapowanie bazy, migracje, format błędów, przeliczanie układów) trzeba utrzymywać, a nie daje wartości dla użytkownika.

## Rozważone opcje

- Gotowe ostre zestawy reguł i dojrzałe biblioteki.
- Własne progi, np. własna konfiguracja detekt i Checkstyle.
- Własne implementacje infrastruktury.

## Decyzja

- Serwer: qulice [1], zamknięty zestaw Checkstyle, PMD i Error Prone. Działa tylko z Mavenem, stąd Maven na serwerze.
- Aplikacja: detekt z `allRules` [2], ktlint w stylu `ktlint_official` [3], compose-rules [4], Android Lint z ostrzeżeniami jako błędami i Konsist [5].
- Jakość i pokrycie testami: profil i bramka „Sonar way” w SonarCloud [6], bez własnych progów.
- Regułę wyłączamy tylko przy sprzeczności z inną regułą albo z frameworkiem, z powodem w konfiguracji i w PR.
- Własny kod piszemy tylko dla logiki dziedziny. Infrastrukturę dają biblioteki, np. Hibernate Spatial, Flyway, MapStruct, proj4j, Turf.
- Własne rozwiązanie problemu ogólnego wymaga osobnego ADR.
- Zależność autor zadania dodaje sam przy licencji MIT, Apache-2.0, BSD, EPL-2.0, MPL-2.0 lub ISC.
- Zależność musi mieć wydanie z ostatnich 12 miesięcy, dokładną wersję i brak znanych podatności. Licencje i podatności sprawdza kontrola zależności w PR, aktualizacje proponuje Dependabot.
- Narzędzia budowania i CI, które nie trafiają do aplikacji ani serwera, mogą mieć licencję GPL lub LGPL. Pobrany plik narzędzia ma w CI przypiętą sumę SHA-256.

## Konsekwencje

- Dobrze: brak sporów o progi, reguły sprawdzone w wielu projektach.
- Dobrze: mniej własnego kodu do testowania i utrzymania.
- Gorzej: qulice wymaga m.in. klas `final`, co może kolidować ze Springiem; wyjątki opisujemy z powodem.
- Gorzej: detekt z `allRules` daje na starcie dużo uwag.
- Gorzej: więcej zależności do aktualizacji.

## Źródła

1. [qulice](https://github.com/yegor256/qulice)
2. [detekt: konfiguracja dla Gradle](https://detekt.dev/docs/gettingstarted/gradle/)
3. [ktlint: style kodu](https://pinterest.github.io/ktlint/latest/rules/code-styles/)
4. [compose-rules](https://mrmans0n.github.io/compose-rules/)
5. [Konsist](https://docs.konsist.lemonappdev.com/)
6. [SonarQube Cloud: bramki jakości](https://docs.sonarsource.com/sonarqube-cloud/standards/managing-quality-gates/introduction-to-quality-gates/)
