package ru.LevLezhnin.NauJava.validation.file;

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
    String message() default """
                     Имя файла должно удовлетворять всем перечисленным ниже условиям:
                     + Имя файла должно иметь длину от 1 до 200 символов
                     + Имя файла должно иметь расширение длины от 1 до 20 символов
                     + Имя файла должно содержать только буквы верхнего и нижнего регистров, цифры, точки, тире, нижние подчёркивания
                     + Имя файла не должно быть зарезервированным (например: 'CON', 'PRN', '.', '..')""";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
