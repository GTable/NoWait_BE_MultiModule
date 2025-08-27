FROM openjdk:21-slim
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} nowait-app-admin-api.jar

ARG PROFILE=admin

ENV TZ=Asia/Seoul

CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Seoul", "-jar", "nowait-app-admin-api.jar", "--spring.profiles.active=admin"]
