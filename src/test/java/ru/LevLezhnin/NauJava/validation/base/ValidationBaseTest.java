package ru.LevLezhnin.NauJava.validation.base;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public abstract class ValidationBaseTest<InputType, ConstraintValidatorType extends ConstraintValidator<?, InputType>> {

    @Mock
    protected ConstraintValidatorContext context;

    @Mock
    protected ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @Mock
    protected ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    protected ConstraintValidatorType validator;

    @BeforeEach
    void setupContextMocks() {
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        lenient().when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @BeforeEach
    final void initValidatorHook() {
        initValidator();
    }

    protected abstract void initValidator();

    @Test
    void shouldReturnTrue_whenNullValue() {
        assertNotNull(validator, "validator должен быть не null");
        assertTrue(validator.isValid(null, context));
    }
}
