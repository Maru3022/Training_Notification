# Этап 1: Сборка
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
# Используем проверенный образ Amazon Corretto (на базе Alpine для легкости)
FROM amazoncorretto:17-alpine
WORKDIR /app

# Проверка наличия jar файла (поможет при отладке в логах CI/CD)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]