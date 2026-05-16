package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UserPasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPasswordValid {
    String message() default """
            Пароль должен удовлетворять всем перечисленным ниже условиям:
            + Длина пароля должна быть от 8 до 255 символов
            + Пароль должен содержать латинские буквы верхнего и нижнего регистров
            + Пароль должен содержать хотя бы одну цифру
            + Пароль должен содержать хотя бы один специальный символ ('!', '@', '#', '(', ')', '$', '%', '^', '&', '*')""";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
