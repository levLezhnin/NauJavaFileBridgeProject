package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UserPasswordValidator implements ConstraintValidator<UserPasswordValid, String> {

    private static final Pattern USER_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[?!@#$%^&*()+\\-_]).{8,255}$"
    );

    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        if (password == null || password.isBlank()) {
            return true;
        }
        return USER_PASSWORD_PATTERN.matcher(password).matches();
    }
}
