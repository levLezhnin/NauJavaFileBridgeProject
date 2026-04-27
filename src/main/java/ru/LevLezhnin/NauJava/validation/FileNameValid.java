package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FileNameValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileNameValid {
    String message() default "Имя файла: 1–200 символов, только буквы, цифры, _ - . (без зарезервированных имён вроде 'CON', 'PRN', '.', '..')";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
