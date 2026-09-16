FROM eclipse-temurin:17-jre-jammy

ARG JAR_FILE
WORKDIR /app

RUN mkdir -p /app/log
COPY target/${JAR_FILE} /app/app.jar

ENV MEMORY="1024M"
ENV LOG_FILE_NAME="/app/log/application.log"

VOLUME ["/app/log"]

ENTRYPOINT ["sh", "-c", "exec java -Xmx${MEMORY} -XX:+HeapDumpOnOutOfMemoryError -XX:OnOutOfMemoryError='kill -9 %p' -XX:+UseSerialGC -jar /app/app.jar"]
