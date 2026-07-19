# ---------- BUILD STAGE ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x product-catalog-service/mvnw && \
    cd product-catalog-service && \
    ./mvnw package -B -DskipTests -Djacoco.skip=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/product-catalog-service/target/product-catalog-service-*.jar app.jar
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
EXPOSE 9003
ENTRYPOINT ["java", "-jar", "app.jar"]
