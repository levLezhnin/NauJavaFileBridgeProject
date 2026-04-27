package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class FilePasswordValidator implements ConstraintValidator<FilePasswordValid, String> {

    private static final Pattern FILE_PASSWORD_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9#?@$%^&*-]{6,64}$"
    );

    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        if (password == null) {
            return true;
        }
        if (password.isEmpty()) {
            return true;
        }
        return FILE_PASSWORD_PATTERN.matcher(password).matches();
    }
}
