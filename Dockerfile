FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/stock-be.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/stock-be.jar"]
