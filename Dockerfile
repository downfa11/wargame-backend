FROM openjdk:21-slim

ENV TZ=Asia/Seoul
EXPOSE 8080

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
LABEL authors="ns"

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m","-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/heapdump.hprof", "-jar", "/app.jar"]