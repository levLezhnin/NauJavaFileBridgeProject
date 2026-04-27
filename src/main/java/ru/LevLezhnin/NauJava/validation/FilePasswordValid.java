package ru.LevLezhnin.NauJava.validation;

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
    String message() default "Пароль файла должен быть от 6 до 64 символов";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
