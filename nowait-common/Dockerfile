FROM openjdk:17.0.8-jdk

WORKDIR /app
COPY build/libs/*.jar nowait-common.jar

CMD ["java", "-jar", "nowait-common.jar"]
