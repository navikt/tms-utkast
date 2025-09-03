FROM gcr.io/distroless/java21-debian12

ENV JAVA_OPTS='-XX:MaxRAMPercentage=75'

COPY app/build/libs/*.jar ./
