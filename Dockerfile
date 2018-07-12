FROM openjdk:8-jre-alpine
COPY /build/libs/escrow-orders-1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
