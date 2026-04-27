package ru.LevLezhnin.NauJava.dto.error;

import java.util.List;

public record ValidationError(
        String localDateTime,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {}