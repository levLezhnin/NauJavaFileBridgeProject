package ru.LevLezhnin.NauJava.validation.user;

import org.junit.jupiter.api.Test;
import ru.LevLezhnin.NauJava.validation.base.StringValidationBaseTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserBanReasonValidatorTest extends StringValidationBaseTest {

    @Override
    protected void initValidator() {
        validator = new UserBanReasonValidator();
    }

    @Test
    void shouldReturnTrueForValidReason() {
        assertThat(validator.isValid(
                "Незаконное распространение защищённых авторским правом файлов без лицензии",
                context
        )).isTrue();

        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void shouldReturnFalseForTooFewWords() {
        assertThat(validator.isValid("Без причины", context)).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    void shouldReturnFalseForExactlyTwoWords() {
        assertThat(validator.isValid("Слишком коротко", context)).isFalse();
    }

    @Test
    void shouldReturnFalseForRepetitiveChars() {
        assertThat(validator.isValid(
                "Пользователь постоянно писал аааааааааааа в чате для накрутки",
                context
        )).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    void shouldReturnFalseForExactBannedPhrase() {
        assertThat(validator.isValid("Обнаружен спам в личных сообщениях", context)).isFalse();
    }

    @Test
    void shouldReturnFalseForBannedPhraseCaseInsensitive() {
        assertThat(validator.isValid("Загружен ВИРУС в архиве tools.zip", context)).isFalse();
    }

    @Test
    void shouldReturnTrueForBannedPhraseAsSubstring() {
        assertThat(validator.isValid(
                "Установил антиспам для защиты от нежелательной почтовой рассылки",
                context
        )).isTrue();
    }

    @Test
    void shouldReturnFalseForBannedPhraseWithPunctuation() {
        assertThat(validator.isValid("Причина: нарушение. Подробности в правилах.", context)).isFalse();
    }

    @Test
    void shouldReturnFalseForTechnicalPhrase() {
        assertThat(validator.isValid("Превышен лимит загрузки файлов за сутки", context)).isFalse();
    }

    @Test
    void shouldReturnTrueForEdgeCaseWithNumbersAndPunctuation() {
        assertThat(validator.isValid(
                "Распространение файла crack.exe (MD5: a1b2c3d4) без согласия правообладателя",
                context
        )).isTrue();
    }
}