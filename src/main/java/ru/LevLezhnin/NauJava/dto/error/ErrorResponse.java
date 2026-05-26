package ru.LevLezhnin.NauJava.dto.error;

/**
 * DTO ответа с содержанием возникшей ошибки
 * @param localDateTime дата и время регистрации ошибки
 * @param status        HTTP статус-код возврата ошибки
 * @param error         краткое описание ошибки
 * @param message       подробное описание ошибки
 * @param path          путь запроса, после обращения к которому возникла ошибка
 * @author Лев Лежнин
 */
public record ErrorResponse(
        String localDateTime,
        Integer status,
        String error,
        String message,
        String path
) {}
