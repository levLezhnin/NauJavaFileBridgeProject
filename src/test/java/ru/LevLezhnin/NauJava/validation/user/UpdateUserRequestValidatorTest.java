package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.validation.base.ValidationBaseTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UpdateUserRequestValidatorTest extends ValidationBaseTest<UpdateUserRequestDto, ConstraintValidator<UpdateUserRequestValid, UpdateUserRequestDto>> {

    @Mock
    private UpdateUserRequestDto dto;

    @Override
    protected void initValidator() {
        validator = new UpdateUserRequestValidator();
    }

    @Test
    void shouldReturnTrue_whenDtoContainsNewUsernameOnly() {
        when(dto.containsNewUsername()).thenReturn(true);
        when(dto.containsNewPassword()).thenReturn(false);

        assertTrue(validator.isValid(dto, context));
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldReturnTrue_whenDtoContainsNewValidPasswordOnly() {
        when(dto.containsNewUsername()).thenReturn(false);
        when(dto.containsNewPassword()).thenReturn(true);
        when(dto.containsCurrentPassword()).thenReturn(true);
        when(dto.containsConfirmNewPassword()).thenReturn(true);
        when(dto.newPassword()).thenReturn("P@ssw0rd1");
        when(dto.confirmNewPassword()).thenReturn("P@ssw0rd1");

        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void shouldReturnFalse_whenDtoIsEmpty() {
        when(dto.containsNewUsername()).thenReturn(false);
        when(dto.containsNewPassword()).thenReturn(false);

        assertFalse(validator.isValid(dto, context));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(messageCaptor.capture());
        assertEquals("Укажите хотя бы новый логин или новый пароль", messageCaptor.getValue().trim());
    }

    @Test
    void shouldReturnFalse_whenDtoContainsNewPasswordWithoutCurrentPassword() {
        when(dto.containsNewUsername()).thenReturn(false);
        when(dto.containsNewPassword()).thenReturn(true);
        when(dto.containsCurrentPassword()).thenReturn(false);

        assertFalse(validator.isValid(dto, context));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(messageCaptor.capture());
        assertEquals("Для смены пароля укажите текущий пароль", messageCaptor.getValue().trim());
        verify(builder).addPropertyNode("current_password");
    }

    @Test
    void shouldReturnFalse_whenDtoContainsNewPasswordWithoutConfirmPassword() {
        when(dto.containsNewUsername()).thenReturn(false);
        when(dto.containsNewPassword()).thenReturn(true);
        when(dto.containsCurrentPassword()).thenReturn(true);
        when(dto.containsConfirmNewPassword()).thenReturn(false);

        assertFalse(validator.isValid(dto, context));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(messageCaptor.capture());
        assertEquals("Подтвердите новый пароль", messageCaptor.getValue().trim());
        verify(builder).addPropertyNode("confirm_new_password");
    }

    @Test
    void shouldReturnFalse_whenDtoContainsMismatchedPasswords() {
        when(dto.containsNewUsername()).thenReturn(false);
        when(dto.containsNewPassword()).thenReturn(true);
        when(dto.containsCurrentPassword()).thenReturn(true);
        when(dto.containsConfirmNewPassword()).thenReturn(true);
        when(dto.newPassword()).thenReturn("P@ssw0rd1");
        when(dto.confirmNewPassword()).thenReturn("Different1@");

        assertFalse(validator.isValid(dto, context));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(messageCaptor.capture());
        assertEquals("Пароли не совпадают", messageCaptor.getValue().trim());
        verify(builder).addPropertyNode("confirm_new_password");
    }
}