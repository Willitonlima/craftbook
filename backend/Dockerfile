FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /app
COPY backend/pom.xml pom.xml
COPY backend/src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-noble

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
