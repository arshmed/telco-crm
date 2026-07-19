# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY . .

RUN chmod +x bff-server/mvnw && \
    cd bff-server && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/bff-server/target/bff-server-*.jar app.jar

RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 9011

ENTRYPOINT ["java", "-jar", "app.jar"]
