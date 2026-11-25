# Dostępny Kampus
# Author: Vladyslav Yevkov
# Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko

# Obraz serwera z pliku JAR zbudowanego wcześniej w backend/target (./mvnw -DskipTests package).
FROM eclipse-temurin:25-jre

RUN groupadd --system serwer && useradd --system --gid serwer --home-dir /app serwer

WORKDIR /app
COPY --chown=serwer:serwer target/backend-*.jar serwer.jar

USER serwer
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'

ENTRYPOINT ["java", "-jar", "/app/serwer.jar"]
