# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x notification-service/mvnw && \
    cd notification-service && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service-*.jar app.jar
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
EXPOSE 9009
ENTRYPOINT ["java", "-jar", "app.jar"]
