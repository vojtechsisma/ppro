# --- Fáze 1: sestavení spustitelného JAR pomocí Maven Wrapperu ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Nejprve jen build konfigurace – závislosti se stáhnou do cache vrstvy
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Zdrojové kódy a sestavení (testy běží zvlášť přes ./mvnw test / CI)
COPY src src
RUN ./mvnw -B -q package -DskipTests

# --- Fáze 2: běhový image pouze s JRE ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl pro healthcheck v docker-compose
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Běh pod neprivilegovaným uživatelem
RUN useradd --system --uid 1001 horyavoda
USER horyavoda

COPY --from=build /workspace/target/horyavoda-*.jar app.jar

EXPOSE 8000

# Flyway migrace i seed syntetických dat proběhnou automaticky při startu aplikace
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
