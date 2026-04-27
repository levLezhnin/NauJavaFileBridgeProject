package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ContentTypeValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContentTypeValid {
    String message() default "Неверный MIME-тип. Допустимы стандартные типы IANA";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
