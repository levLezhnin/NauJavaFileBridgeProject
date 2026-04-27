package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FileTTLMinutesValidator implements ConstraintValidator<FileTTLMinutesValid, Long> {

    private static final Long TTL_MINUTES_MIN = Duration.ofDays(1).toMinutes();
    private static final Long TTL_MINUTES_MAX = Duration.ofDays(14).toMinutes();

    @Override
    public boolean isValid(Long fileTtlMinutes, ConstraintValidatorContext constraintValidatorContext) {

        if (fileTtlMinutes == null) {
            return false;
        }

        return TTL_MINUTES_MIN.compareTo(fileTtlMinutes) <= 0 && fileTtlMinutes.compareTo(TTL_MINUTES_MAX) <= 0;
    }
}
