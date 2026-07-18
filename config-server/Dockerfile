# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x config-server/mvnw && \
    cd config-server && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server-*.jar app.jar
COPY configs/ ./configs/
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
