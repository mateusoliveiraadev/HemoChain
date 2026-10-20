# syntax=docker/dockerfile:1

# ---------- Estagio 1: compila E RODA OS TESTES ----------
# Se algum teste falhar, o build da imagem falha e nada e publicado.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Dependencias primeiro: esta camada fica em cache enquanto o pom.xml nao mudar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B verify

# ---------- Estagio 2: runtime enxuto (so o artefato) ----------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Usuario sem privilegios: o processo nao roda como root.
RUN addgroup -S hemochain && adduser -S hemochain -G hemochain
COPY --from=build --chown=hemochain:hemochain /workspace/target/hemochain-*.jar /app/app.jar
USER hemochain

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=70"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
