# -------- Stage 1: Build --------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. Кэширование зависимостей
COPY pom.xml .
RUN mvn -B dependency:resolve

# 2. Сборка
COPY src ./src
RUN mvn -B -DskipTests package

# -------- Stage 2: Runtime --------
# 3. Используем JRE вместо JDK для уменьшения размера
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 4. Создаем пользователя ДО копирования файлов, чтобы сразу настроить права
RUN useradd -r -s /bin/false springuser

# 5. Копируем конкретный jar
COPY --from=builder /app/target/*.jar app.jar

# 6. Передаем права пользователю
RUN chown springuser:springuser app.jar

USER springuser

EXPOSE 8080

# 7. Используем exec form для правильной передачи сигналов (SIGTERM)
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]