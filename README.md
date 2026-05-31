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
- Выберите профиль: dev для разработка, prod для production окружения
- Укажите учетные данные для PostgreSQL
- Настройте доступ к MinIO (ключи, бакет)
- Настройте доступ к Grafana (логин и пароль администратора)
- Задайте секреты для JWT токенов

## Запуск в режиме разработки (dev)
Для разработки используются изолированные контейнеры с PostgreSQL, MinIO и стеком мониторинга (Prometheus, Promtail, Loki, Grafana):

```bash
docker-compose -f docker-compose-dev.yml up -d
```
После запуска:
- PostgreSQL доступен на порту, указанном в `POSTGRES_PORT` (по умолчанию 5432)
- MinIO API доступен на порту `MINIO_PORT` (по умолчанию 9000)
- MinIO Console доступен на порту `MINIO_CONSOLE_PORT` (по умолчанию 9001)
- Grafana доступна по пути http://localhost:3000

Приложение запускается локально через IDE или Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Проверка работоспособности:
- Приложение будет доступно по пути: http://localhost:8080
- MinIO Console: http://localhost:9001 (логин/пароль из .env)
- Health Check: http://localhost:8080/actuator/health
- Grafana: http://localhost:3000 (логин/пароль из .env)

## Запуск в production-режиме (prod)
Полный запуск всех компонентов (приложение, PostgreSQL, MinIO, мониторинг):

```bash
docker-compose -f docker-compose-prod.yml up -d --build
```
При этом:
- Приложение собирается в Docker-образ и запускается в контейнере
- Выполняется инициализация MinIO (создание бакета, настройка прав)
- Все сервисы общаются друг с другом внутри docker-сети, доступ к сервисам из внешней сети осуществляется через Nginx
- Контейнеры сервисов (приложения, PostgreSQL, MinIO, Grafana) объединены в отдельную локальную docker-подсеть
- Контейнеры, обслуживающие мониторинг (Grafana, Prometheus, Loki, Promtail) объединены в отдельную docker-подсеть monitoring
- Доступ к данным мониторинга осуществляется инструментом визуализации Grafana

Проверка работоспособности:
- Приложение будет доступно по пути: http://localhost:80
- Health Check: http://localhost:80/actuator/health
- Доступ к grafana: http://localhost:80/grafana (логин/пароль из .env + access-токен администратора от приложения)

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