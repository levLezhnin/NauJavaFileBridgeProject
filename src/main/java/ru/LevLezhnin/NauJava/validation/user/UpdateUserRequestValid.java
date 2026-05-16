package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UpdateUserRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateUserRequestValid {
    String message() default "Неверно заполнена форма обновления данных аккаунта";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
