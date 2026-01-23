# Этап 1: Сборка
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# Копируем pom.xml и скачиваем зависимости (кэширование)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код и собираем jar
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM openjdk:17-jdk-slim
WORKDIR /app
# Копируем только собранный jar из первого этапа
COPY --from=build /app/target/*.jar app.jar

# Порт, на котором работает твое приложение (из application.properties)
EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]