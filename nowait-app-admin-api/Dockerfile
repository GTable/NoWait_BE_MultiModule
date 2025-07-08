FROM openjdk:17
ARG JAR_FILE=./build/libs/*.jar
COPY ${JAR_FILE} nowait-app-admin-api.jar

ARG PROFILE=admin
ENV PROFILE=${PROFILE}

CMD ["java", "-jar", "nowait-app-admin-api.jar", "--spring.profiles.active=${PROFILE}", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Seoul"]
