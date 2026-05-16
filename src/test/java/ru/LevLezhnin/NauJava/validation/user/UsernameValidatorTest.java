package ru.LevLezhnin.NauJava.validation.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.LevLezhnin.NauJava.validation.base.StringValidationBaseTest;

import static org.junit.jupiter.api.Assertions.*;

class UsernameValidatorTest extends StringValidationBaseTest {

    @Override
    protected void initValidator() {
        validator = new UsernameValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"John_Doe123", "test-user.name", "12345"})
    void shouldReturnTrue_whenValidUsername(String value) {
        assertTrue(validator.isValid(value, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Иван_Петров1", "АлексейЁж"})
    void shouldReturnTrue_whenValidCyrillicUsername(String value) {
        assertTrue(validator.isValid(value, context));
    }

    @Test
    void shouldReturnFalse_whenTooShortUsername() {
        assertFalse(validator.isValid("ab12", context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@name", "name!", "user name"})
    void shouldReturnFalse_whenUsernameContainsInvalidCharacters(String value) {
        assertFalse(validator.isValid(value, context));
    }
}
