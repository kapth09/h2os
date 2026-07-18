FROM eclipse-temurin:25

WORKDIR /app

COPY ./target/h2os-*.*.*-SNAPSHOT.jar ./application.jar

EXPOSE 8080

CMD ["java", "-jar", "application.jar"]
