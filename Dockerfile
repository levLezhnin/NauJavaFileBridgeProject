# -------- Stage 1: Build --------
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 1. Кэширование зависимостей
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 2. Сборка
COPY src ./src
RUN mvn -B -DskipTests package

# -------- Stage 2: Runtime --------
# 3. Используем JRE вместо JDK для уменьшения размера
FROM eclipse-temurin:21-jre-alpine-3.20

WORKDIR /app

# 4. Создаем пользователя ДО копирования файлов, чтобы сразу настроить права
RUN addgroup -S springuser && adduser -S springuser -G springuser

# 5. Копируем конкретный jar
COPY --from=builder --chown=springuser:springuser /app/target/*.jar app.jar

USER springuser

EXPOSE 8080

# 6. Используем exec form для правильной передачи сигналов (SIGTERM)
ENTRYPOINT ["java", "-jar", "app.jar"]