# Проект FileBridge

## Документация
Онлайн документация проекта: https://drive.google.com/drive/folders/1aLdS3SVgoTM9-1QqQKRqHG9_g4MDGv0J?usp=sharing

## Запуск приложения

### Требования
- Docker и Docker Compose
- Java 21
- Maven
- Spring Boot 4.0.3

### Настройка окружения

1. Скопируйте файл `.env.example` в `.env`:
```bash
cp .env.example .env
```

2. Отредактируйте .env, заполнив необходимые значения:
- Выберите профиль: dev для разработка, prod для 
- Укажите учетные данные для PostgreSQL
- Настройте доступ к MinIO (ключи, бакет)
- Задайте секреты для JWT токенов

## Запуск в режиме разработки (dev)
Для разработки используются изолированные контейнеры с PostgreSQL и MinIO:

```bash
docker-compose -f docker-compose-dev.yml up -d
```
После запуска:
- PostgreSQL доступен на порту, указанном в `POSTGRES_PORT` (по умолчанию 5432)
- MinIO API доступен на порту `MINIO_PORT` (по умолчанию 9000)
- MinIO Console доступен на порту `MINIO_CONSOLE_PORT` (по умолчанию 9001)

Приложение запускается локально через IDE или Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Запуск в production-режиме (prod)
Полный запуск всех компонентов (приложение, PostgreSQL, MinIO):

```bash
docker-compose -f docker-compose-prod.yml up -d
```
При этом:
- Приложение собирается в Docker-образ и запускается в контейнере
- Выполняется инициализация MinIO (создание бакета, настройка прав)
- Все сервисы объединены в общую сеть backend

## Проверка работоспособности
- Приложение: http://localhost:8080
- MinIO Console: http://localhost:9001 (логин/пароль из .env)
- Health Check: http://localhost:8080/actuator/health

## Остановка и очистка
- Остановка контейнеров:
    ```bash
    # Для dev окружения
    docker-compose -f docker-compose-dev.yml down
    ```
    ```bash
    # Для prod окружения
    docker-compose -f docker-compose-prod.yml down
    ```
- Полное удаление томов с данными:
    ```bash
    # В develop-режиме
    docker-compose -f docker-compose-dev.yml down -v
    ```
    ```bash
    # В production-режиме
    docker-compose -f docker-compose-prod.yml down -v
    ```

## Особенности конфигурации
- Профили Spring: активируются через переменную `SPRING_PROFILES_ACTIVE` в .env
- Миграции БД: выполняются автоматически через Flyway
- Проверки здоровья:
  - PostgreSQL: pg_isready 
  - MinIO: HTTP endpoint /minio/health/live

# Copyright

Создан в 2026 году в рамках курса "Промышленная разработка на Java" от Naumen.