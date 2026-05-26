package ru.LevLezhnin.NauJava.validation.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import ru.LevLezhnin.NauJava.validation.base.StringValidationBaseTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTypeValidatorTest extends StringValidationBaseTest {

    @Override
    protected void initValidator() {
        validator = new ContentTypeValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            MediaType.IMAGE_PNG_VALUE,
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            "text/plain; charset=utf-8",
            "video/mp4"
    })
    @DisplayName("Валидные MIME-типы должны возвращать true")
    void isValid_validMimeTypes_returnTrue(String contentType) {
        assertTrue(validator.isValid(contentType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-mime",
            "image",
            "pdf/",
            "application/json; charset="
    })
    @DisplayName("Невалидные MIME-типы должны возвращать false")
    void isValid_invalidMimeTypes_returnFalse(String contentType) {
        assertFalse(validator.isValid(contentType, null));
    }
}