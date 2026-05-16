package ru.LevLezhnin.NauJava.validation.file;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FilePasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilePasswordValid {
    String message() default """
            Пароль для файла должен удовлетворять всем перечисленным ниже условиям:
            + Пароль должен иметь длину от 6 до 64 символов
            + Пароль должен содержать только латинские буквы верхнего и нижнего регистров, цифры и один из специальных символов ('#', '?', '@', '$', '%', '^', '&', '*', '-')
            """;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
