package ru.LevLezhnin.NauJava.validation.base;

import jakarta.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class StringValidationBaseTest extends ValidationBaseTest<String, ConstraintValidator<?, String>> {
    @Test
    void shouldReturnTrue_whenEmptyValue() {
        assertNotNull(validator, "validator должен быть не null");
        assertTrue(validator.isValid("", context));
    }
    @Test
    void shouldReturnTrue_whenBlankValue() {
        assertNotNull(validator, "validator должен быть не null");
        assertTrue(validator.isValid("   ", context));
    }
}
