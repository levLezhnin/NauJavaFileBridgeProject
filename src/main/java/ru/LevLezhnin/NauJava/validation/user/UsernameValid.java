package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UsernameValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsernameValid {
    String message() default """
            Логин должен удовлетворять всем перечисленным ниже условиям:
            + Логин должен иметь длину от 5 до 255 символов
            + Логин должен содержать только буквы верхнего и нижнего регистров, цифры, точки, тире и нижние подчёркивания
            """;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
