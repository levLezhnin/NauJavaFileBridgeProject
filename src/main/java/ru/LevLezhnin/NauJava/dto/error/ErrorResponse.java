package ru.LevLezhnin.NauJava.dto.error;

public record ErrorResponse(
        String localDateTime,
        Integer status,
        String error,
        String message,
        String path
) {}
