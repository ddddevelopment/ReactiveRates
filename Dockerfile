FROM openjdk:21-jdk-slim AS build
WORKDIR /build
COPY . .
RUN ./mvnw clean package -DskipTests

FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /build/target/reactive-rates-api-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]