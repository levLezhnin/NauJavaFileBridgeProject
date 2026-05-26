package ru.LevLezhnin.NauJava.validation.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.LevLezhnin.NauJava.validation.base.ValidationBaseTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTTLMinutesValidatorTest extends ValidationBaseTest<Long, FileTTLMinutesValidator> {

    @Override
    protected void initValidator() {
        validator = new FileTTLMinutesValidator();
    }

    @ParameterizedTest
    @ValueSource(longs = {
            1440,
            20160,
            10000,
            1441,
            20159
    })
    @DisplayName("Значения в диапазоне [1440, 20160] должны быть валидными")
    void isValid_validTTLValues_returnTrue(Long value) {
        assertTrue(validator.isValid(value, null));
    }

    @ParameterizedTest
    @ValueSource(longs = {
            0,
            1439,
            20161,
            -1,
            100000
    })
    @DisplayName("Значения за пределами диапазона должны отклоняться")
    void isValid_invalidTTLValues_returnFalse(Long value) {
        assertFalse(validator.isValid(value, null));
    }
}