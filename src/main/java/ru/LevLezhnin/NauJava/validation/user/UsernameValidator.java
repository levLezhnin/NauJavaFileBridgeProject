package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UsernameValidator implements ConstraintValidator<UsernameValid, String> {

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Zа-яА-ЯЁё\\d\\-\\_\\.]{5,255}$"
    );

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        if (username == null || username.isBlank()) {
            return true;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }
}
