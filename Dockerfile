FROM --platform=linux/amd64  eclipse-temurin:17-jre-jammy

# 라즈베리파이 server 배포 시 arm 적용
#FROM --platform=linux/arm64 eclipse-temurin:17-jre-jammy

RUN mkdir -p /app/data /app/etc /app/log /app/assets/images /app/assets/documents

ENV MEMORY="1024M"
ENV CPUS=2
ENV LOG_FILE_NAME="/app/log/application.log"
ENV ASSET_IMAGE_STORAGE_PATH="/app/assets/images"
ENV ASSET_DOCUMENT_STORAGE_PATH="/app/assets/documents"
ARG JAR_FILE
ADD target/${JAR_FILE} /myapp/app.jar
WORKDIR /myapp
VOLUME ["/app/log", "/app/etc", "/app/assets" ]
CMD java -Xmx$MEMORY -XX:+HeapDumpOnOutOfMemoryError -XX:OnOutOfMemoryError="kill -9 %p" -XX:CICompilerCount="$(($CPUS>2?$CPUS:2))" -XX:+UseSerialGC -jar /myapp/app.jar
