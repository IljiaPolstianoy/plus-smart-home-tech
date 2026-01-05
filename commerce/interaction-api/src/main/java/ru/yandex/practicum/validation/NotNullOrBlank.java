package ru.yandex.practicum.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotNullOrBlankValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNullOrBlank {
    String message() default "Value must contain letters and cannot be null or empty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // Дополнительный параметр для передачи ExceptionType
    ExceptionType exceptionType() default ExceptionType.NOT_AUTHORIZED;

    int minLetters() default 1;

    boolean allowDigits() default true;

    boolean allowSpecialChars() default true;
}