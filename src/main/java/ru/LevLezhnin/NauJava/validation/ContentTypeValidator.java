package ru.LevLezhnin.NauJava.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;

@Component
public class ContentTypeValidator implements ConstraintValidator<ContentTypeValid, String> {
    @Override
    public boolean isValid(String contentType, ConstraintValidatorContext constraintValidatorContext) {

        if (contentType == null) {
            return false;
        }

        if (contentType.isBlank()) {
            return false;
        }

        try {
            MimeTypeUtils.parseMimeType(contentType);
            return true;
        } catch (InvalidMimeTypeException e) {
            return false;
        }
    }
}
