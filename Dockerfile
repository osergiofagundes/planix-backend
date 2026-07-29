FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --create-home --shell /bin/bash planix \
 && mkdir -p /data/uploads \
 && chown -R planix:planix /data/uploads /app

COPY --from=build /build/target/*.jar app.jar

USER planix
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]