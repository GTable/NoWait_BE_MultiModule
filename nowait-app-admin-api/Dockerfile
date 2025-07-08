FROM openjdk:17
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} nowait-app-admin-api.jar

ARG PROFILE=admin

CMD ["java", "-jar", "nowait-app-admin-api.jar", "--spring.profiles.active=admin", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Seoul"]
