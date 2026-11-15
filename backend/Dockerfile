FROM eclipse-temurin:21-jdk AS build
WORKDIR /build/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml backend/lombok.config ./
COPY backend/src/main src/main
RUN sh ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/backend/target/zimran-backend-0.0.1-SNAPSHOT.jar app.jar
COPY frontend/src/data/catalog.json catalog.json
ENV EVENTS_JSON_SOURCE=/app/catalog.json
USER 10001
EXPOSE 8080
CMD ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-8080}"]
