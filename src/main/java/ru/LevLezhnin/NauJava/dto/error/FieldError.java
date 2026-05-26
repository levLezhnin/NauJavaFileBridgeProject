package ru.LevLezhnin.NauJava.dto.error;

/**
 * DTO ответа, содержащее данные об ошибке заполнения поля некоторой формы.
 * <p>
 * Используется в {@link ValidationError}
 *
 * @param field     название неправильно заполненного поля
 * @param message   подробное описание ошибка заполнения поля
 *
 * @author Лев Лежнин
 */
public record FieldError(String field, String message) {}
