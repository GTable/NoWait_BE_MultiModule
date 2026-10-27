FROM eclipse-temurin:21
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} nowait-app-user-api.jar

ARG PROFILE=user

ENV TZ=Asia/Seoul

CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Seoul", "-jar", "nowait-app-user-api.jar", "--spring.profiles.active=user"]
