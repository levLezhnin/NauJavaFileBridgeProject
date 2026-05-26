package ru.LevLezhnin.NauJava.exception.common;

/**
 * Исключение, выбрасываемое когда запрашиваемая сущность не найдена в БД.
 * <p>
 * Используется в сервисах и контроллерах для 404-ответов.
 *
 * @author Лев Лежнин
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
