package ru.LevLezhnin.NauJava.dto.error;

import java.util.List;

/**
 * DTO ответа, содержащее данные об ошибках заполнения некоторой формы.
 * @param localDateTime дата и время регистрации ошибки
 * @param error         краткое описание ошибки
 * @param message       подробное описание ошибки
 * @param path          путь запроса, при обращении к которому возникла ошибка
 * @param fieldErrors   набор ошибок заполнения полей формы {@link FieldError}
 *
 * @author Лев Лежнин
 */
public record ValidationError(
        String localDateTime,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {}