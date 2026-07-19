# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x payment-service/mvnw && \
    cd payment-service && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service-*.jar app.jar
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
EXPOSE 9008
ENTRYPOINT ["java", "-jar", "app.jar"]
