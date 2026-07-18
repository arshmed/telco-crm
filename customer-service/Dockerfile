# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy entire project (parent pom + all modules)
# Multi-stage build ensures only the final jar ends up in runtime image
COPY . .

# Build only customer-service (skip tests)
RUN chmod +x customer-service/mvnw && \
    cd customer-service && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy jar from build stage
COPY --from=build /workspace/customer-service/target/customer-service-*.jar app.jar

# Non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

EXPOSE 9002

ENTRYPOINT ["java", "-jar", "app.jar"]
