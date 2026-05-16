package ru.LevLezhnin.NauJava.validation.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class UserBanReasonValidator implements ConstraintValidator<UserBanReasonValid, String> {

    private static final int MIN_WORDS = 3;
    private static final Pattern CHAR_REPEAT_PATTERN = Pattern.compile("(.)\\1{4,}");
    private static final List<Pattern> BANNED_PATTERNS = Stream.of(
                    "спам", "флуд", "нарушение", "плохое поведение", "неправильно", "жалоба", "репорт",
                    "по правилам", "нарушил условия", "мусор", "хлам", "админ не одобряет",
                    "бан по желанию", "тест", "asdf", "забань", "сам виноват",
                    "лимит", "превышен размер", "бот", "скрипт", "автоматизация", "api abuse",
                    "пиратка", "контрафакт", "18+", "nsfw", "запрещённый контент",
                    "вирус", "троян", "malware"
            )
            .map(phrase -> Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS))
            .toList();

    @Override
    public boolean isValid(String banReason, ConstraintValidatorContext context) {

        if (banReason == null || banReason.isBlank()) {
            return true;
        }

        String trimmed = banReason.trim();

        long wordCount = Arrays.stream(trimmed.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .count();
        if (wordCount < MIN_WORDS) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Причина должна содержать минимум " + MIN_WORDS + " слов"
            ).addConstraintViolation();
            return false;
        }

        if (CHAR_REPEAT_PATTERN.matcher(trimmed).find()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Причина не может содержать длинную последовательность одинаковых символов"
            ).addConstraintViolation();
            return false;
        }

        boolean containsBanned = BANNED_PATTERNS.stream().anyMatch(p -> p.matcher(trimmed).find());
        if (containsBanned) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Используйте конкретные формулировки. Избегайте общих терминов: 'спам', 'пиратка', 'нарушение' и т.д."
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
