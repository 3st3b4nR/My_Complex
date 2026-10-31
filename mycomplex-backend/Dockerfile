FROM maven:3.10.0-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine-3.24

WORKDIR /app

RUN addgroup -S mycomplex \
    && adduser -S mycomplex -G mycomplex \
    && mkdir -p /var/log/mycomplex \
    && chown -R mycomplex:mycomplex /var/log/mycomplex

COPY --from=build /workspace/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"

USER mycomplex:mycomplex

EXPOSE 8080 9080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
