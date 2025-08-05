FROM openjdk:17-slim

WORKDIR /app
COPY build/libs/*.jar nowait-common.jar

CMD ["java", "-jar", "nowait-common.jar"]
