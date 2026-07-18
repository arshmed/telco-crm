# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy entire project (parent pom + all modules)
COPY . .

# Build only discovery-server (skip tests)
RUN chmod +x discovery-server/mvnw && \
    cd discovery-server && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy jar from build stage
COPY --from=build /workspace/discovery-server/target/discovery-server-*.jar app.jar

# Non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
