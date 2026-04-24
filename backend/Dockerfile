FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw -o package -DskipTests 2>/dev/null || (apt-get update && apt-get install -y maven && mvn package -DskipTests)

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
