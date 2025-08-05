FROM openjdk:17
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} nowait-app-user-api.jar

ARG PROFILE=user

CMD ["java", "-jar", "nowait-app-user-api.jar", "--spring.profiles.active=user", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Seoul"]
