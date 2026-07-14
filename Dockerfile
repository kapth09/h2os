FROM eclipse-temurin:25

WORKDIR /app

RUN mkdir logs

COPY ./target/h2os-*.*.*.jar ./application.jar

EXPOSE 8080

CMD ["java", "-jar", "application.jar"]
