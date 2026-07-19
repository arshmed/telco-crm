# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY . .

RUN chmod +x ticket-service/mvnw && \
    cd ticket-service && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/ticket-service/target/ticket-service-*.jar app.jar

RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 9010

ENTRYPOINT ["java", "-jar", "app.jar"]
