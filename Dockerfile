FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/reactive-rates-api-*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]