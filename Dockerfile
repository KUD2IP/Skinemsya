# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY pom.xml .
COPY skinemsya_parent/pom.xml skinemsya_parent/pom.xml
COPY skinemsya_parent/app/pom.xml skinemsya_parent/app/pom.xml
COPY skinemsya_parent/common/pom.xml skinemsya_parent/common/pom.xml
COPY skinemsya_parent/auth/pom.xml skinemsya_parent/auth/pom.xml
COPY skinemsya_parent/users/pom.xml skinemsya_parent/users/pom.xml
COPY skinemsya_parent/integrations/pom.xml skinemsya_parent/integrations/pom.xml
COPY skinemsya_parent/groups/pom.xml skinemsya_parent/groups/pom.xml
COPY skinemsya_parent/events/pom.xml skinemsya_parent/events/pom.xml
COPY skinemsya_parent/files/pom.xml skinemsya_parent/files/pom.xml
COPY skinemsya_parent/receipts/pom.xml skinemsya_parent/receipts/pom.xml
COPY skinemsya_parent/debts/pom.xml skinemsya_parent/debts/pom.xml
COPY skinemsya_parent/payments/pom.xml skinemsya_parent/payments/pom.xml
COPY skinemsya_parent/notifications/pom.xml skinemsya_parent/notifications/pom.xml

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/* \
    && mvn -pl skinemsya_parent/app -am dependency:go-offline -B

COPY skinemsya_parent skinemsya_parent
RUN mvn -pl skinemsya_parent/app -am -DskipTests package -B

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app
COPY --from=build /workspace/skinemsya_parent/app/target/app-*.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
