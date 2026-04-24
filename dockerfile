# Build already happened in CI, only the runtime image is needed here.
FROM gcr.io/distroless/java17-debian12:nonroot

WORKDIR /app
COPY target/*.jar app.jar

# Distroless already runs as nonroot and keeps the image surface minimal.
EXPOSE 8086 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
