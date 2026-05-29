package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;

@Component
public class UpdateUserRequestValidator implements ConstraintValidator<UpdateUserRequestValid, UpdateUserRequestDto> {

    @Override
    public boolean isValid(UpdateUserRequestDto updateUserRequestDto, ConstraintValidatorContext constraintValidatorContext) {

        if (updateUserRequestDto == null) {
            return true;
        }

        boolean hasNewUsername = updateUserRequestDto.containsNewUsername();
        boolean hasNewPassword = updateUserRequestDto.containsNewPassword();

        if (!(hasNewUsername || hasNewPassword)) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Укажите хотя бы новый логин или новый пароль")
                    .addConstraintViolation();
            return false;
        }

        if (!hasNewPassword) {
            return true;
        }

        if (!updateUserRequestDto.containsCurrentPassword()) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Для смены пароля укажите текущий пароль")
                    .addPropertyNode("current_password")
                    .addConstraintViolation();
            return false;
        }

        if (!updateUserRequestDto.containsConfirmNewPassword()) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Подтвердите новый пароль")
                    .addPropertyNode("confirm_new_password")
                    .addConstraintViolation();
            return false;
        }

        if (!updateUserRequestDto.newPassword().equals(updateUserRequestDto.confirmNewPassword())) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("Пароли не совпадают")
                    .addPropertyNode("confirm_new_password")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
