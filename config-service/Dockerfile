FROM eclipse-temurin:17-jdk-focal

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ARG profile
ENV envPROFILE=$profile

ENTRYPOINT ["java","-Xmx1024m", "-jar", "app.jar","--spring.profiles.active=${envPROFILE}"]

EXPOSE 7720