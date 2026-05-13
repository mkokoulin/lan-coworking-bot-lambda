FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew dependencies --no-daemon -q

COPY src/ src/
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/quarkus-app/lib/ lib/
COPY --from=build /app/build/quarkus-app/app/ app/
COPY --from=build /app/build/quarkus-app/quarkus/ quarkus/
COPY --from=build /app/build/quarkus-app/quarkus-run.jar quarkus-run.jar

EXPOSE 8088

ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
