package ru.LevLezhnin.NauJava.validation.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.LevLezhnin.NauJava.validation.base.StringValidationBaseTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNameValidatorTest extends StringValidationBaseTest {

    @Override
    protected void initValidator() {
        validator = new FileNameValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "document.pdf",
            "АБВГД_ЕЖЗ.docx",
            "my-report.txt",
            "file with spaces.csv",
            "a.b",
            "_test_123.json",
            "video.mp4",
            "audio.mp3"
    })
    @DisplayName("Валидные имена файлов должны проходить проверку")
    void isValid_validFileNames_returnTrue(String fileName) {
        assertTrue(validator.isValid(fileName, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CON",
            "PRN.txt",
            "AUX",
            "NUL.md",
            "COM1.zip",
            "LPT9.log"
    })
    @DisplayName("Имена из списка зарезервированных Windows должны быть отклонены")
    void isValid_windowsReservedNames_returnFalse(String fileName) {
        assertFalse(validator.isValid(fileName, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:name.txt", // запрещённые специсимволы
            "file?name.txt",
            "file<name>.txt",
            "file.", // нет расширения
            "file..txt", // две точки
            "file\u0000.txt", // нуль-байт
            "name.abcdefghijklmnopqrstuvwxyz" // слишком длинное расширение файла
    })
    @DisplayName("Невалидные паттерны (спецсимволы, точки, нуль-байты) должны отклоняться")
    void isValid_invalidPatterns_returnFalse(String fileName) {
        assertFalse(validator.isValid(fileName, null));
    }

    @Test
    @DisplayName("Имя файла не должно превышать 200 символов (без расширения)")
    void isValid_toLongFileName_returnFalse() {
        String exactLimit = "a".repeat(200) + ".txt";
        assertTrue(validator.isValid(exactLimit, null)); // 200 разрешено

        String overLimit = "a".repeat(201) + ".txt";
        assertFalse(validator.isValid(overLimit, null));
    }
}