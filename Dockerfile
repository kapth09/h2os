FROM eclipse-temurin:25

WORKDIR /app

RUN mkdir -p ./volume/logs

COPY ./target/h2os-*.*.*-SNAPSHOT.jar ./application.jar

EXPOSE 8080

CMD ["java", "-jar", "application.jar"]
