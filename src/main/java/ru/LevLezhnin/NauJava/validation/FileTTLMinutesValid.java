package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FileTTLMinutesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileTTLMinutesValid {
    String message() default "Срок жизни файла в системе должен быть не меньше 1 дня и не более 14 дней";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
