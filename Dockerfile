FROM openjdk:17-slim

ENV TZ=Asia/Seoul
EXPOSE 8080

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
LABEL authors="ns"

ENTRYPOINT ["java", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/heapdump.hprof", "-jar", "/app.jar"]