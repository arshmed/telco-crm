# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY . .

RUN chmod +x identity-service/mvnw && \
    cd identity-service && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/identity-service/target/identity-service-*.jar app.jar

RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 9001

ENTRYPOINT ["java", "-jar", "app.jar"]
