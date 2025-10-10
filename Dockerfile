FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY . .

RUN chmod +x ./gradlew && ./gradlew clean bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

USER 1000

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]