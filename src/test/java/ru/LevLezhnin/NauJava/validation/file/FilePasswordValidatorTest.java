package ru.LevLezhnin.NauJava.validation.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.LevLezhnin.NauJava.validation.base.StringValidationBaseTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePasswordValidatorTest extends StringValidationBaseTest {

    @Override
    protected void initValidator() {
        validator = new FilePasswordValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456",
            "MyP@ss#1",
            "aA1#?@$%^&*-",
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", // 64 символа
            "Test#Pass"
    })
    @DisplayName("Валидные пароли (6-64 символа, разрешённый набор) должны проходить")
    void isValid_validPasswords_returnTrue(String filePassword) {
        assertTrue(validator.isValid(filePassword, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345", // короче 6 символов
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", //65 символов
            "pass word",
            "pass!word",
            "pass~word",
            "pass/word",
    })
    @DisplayName("Невалидные пароли (короткие, длинные, с запрещёнными символами) должны отклоняться")
    void isValid_invalidPasswords_returnFalse(String filePassword) {
        assertFalse(validator.isValid(filePassword, null));
    }
}