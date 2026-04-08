# Сборка уже прошла в CI, берем готовый артефакт
FROM amazoncorretto:17-alpine

# Создаем пользователя, чтобы не запускать от root (безопасность!)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY target/*.jar app.jar

# Ограничиваем память Java, чтобы контейнер не убил систему (OOM Killer)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]