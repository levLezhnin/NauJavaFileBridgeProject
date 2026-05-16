package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserBanReasonValidator.class)
public @interface UserBanReasonValid {
    String message() default "Неверно заполнена причина блокировки пользователя";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
