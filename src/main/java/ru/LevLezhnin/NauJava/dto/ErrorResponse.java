package ru.LevLezhnin.NauJava.dto;

public record ErrorResponse(
        String localDateTime,
        Integer status,
        String error,
        String message,
        String path
) {}
