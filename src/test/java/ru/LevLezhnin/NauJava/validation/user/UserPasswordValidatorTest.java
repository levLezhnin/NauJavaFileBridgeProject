package ru.LevLezhnin.NauJava.validation.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.LevLezhnin.NauJava.validation.base.StringValidationBaseTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPasswordValidatorTest extends StringValidationBaseTest {

    @Override
    protected void initValidator() {
        validator = new UserPasswordValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"P@ssw0rd1", "Abc123!_"})
    void shouldReturnTrue_whenValidPassword(String password) {
        assertTrue(validator.isValid(password, context));
    }

    @Test
    void shouldReturnFalse_whenPasswordMissingUppercase() {
        assertFalse(validator.isValid("p@ssw0rd1", context));
    }

    @Test
    void shouldReturnFalse_whenPasswordMissingLowercase() {
        assertFalse(validator.isValid("P@SSW0RD1", context));
    }

    @Test
    void shouldReturnFalse_whenPasswordMissingDigit() {
        assertFalse(validator.isValid("Password!", context));
    }

    @Test
    void shouldReturnFalse_whenPasswordMissingSpecialChar() {
        assertFalse(validator.isValid("Password1", context));
    }

    @Test
    void shouldReturnFalse_whenPasswordTooShort() {
        assertFalse(validator.isValid("P@1a", context));
    }

    @Test
    void shouldReturnTrue_whenValidBoundaryLength() {
        assertTrue(validator.isValid("Aa1!____", context));
    }
}
