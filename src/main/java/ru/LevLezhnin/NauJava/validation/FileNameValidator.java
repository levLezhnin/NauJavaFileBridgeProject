package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class FileNameValidator implements ConstraintValidator<FileNameValid, String> {

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile(
            "^[\\wА-Яа-яЁё\\.\\_\\-]{1,200}.[a-zA-Z]{1,20}$"
    );

    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

    @Override
    public boolean isValid(String fileName, ConstraintValidatorContext constraintValidatorContext) {
        if (fileName == null) {
            return false;
        }
        if (fileName.isBlank()) {
            return false;
        }
        if (!SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            return false;
        }
        if (fileName.endsWith(".") || fileName.contains("..") || fileName.contains("\0")) {
            return false;
        }
        String nameWithoutExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        return !WINDOWS_RESERVED.contains(nameWithoutExt);
    }
}
