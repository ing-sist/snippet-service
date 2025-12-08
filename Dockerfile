FROM gradle:8.5.0-jdk21 AS build
COPY  . /home/gradle/src
WORKDIR /home/gradle/src

# Args para credenciales de GitHub Packages
ARG GPR_USER
ARG GPR_KEY

ENV GPR_USER=$GPR_USER
ENV GPR_KEY=$GPR_KEY

RUN gradle assemble
FROM eclipse-temurin:21-jre

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar

RUN mkdir -p /usr/local/newrelic
ADD ./newrelic/newrelic.jar /usr/local/newrelic/newrelic.jar
ADD ./newrelic/newrelic.yml /usr/local/newrelic/newrelic.yml

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production", "-javaagent:/usr/local/newrelic/newrelic.jar", "/app/spring-boot-application.jar"]