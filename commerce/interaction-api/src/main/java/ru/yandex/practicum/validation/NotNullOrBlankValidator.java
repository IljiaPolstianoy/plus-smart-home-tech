package ru.yandex.practicum.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

public class NotNullOrBlankValidator implements ConstraintValidator<NotNullOrBlank, String> {

    private ExceptionType exceptionType;
    private int minLetters;
    private boolean allowDigits;
    private boolean allowSpecialChars;
    private String pattern;

    @Override
    public void initialize(NotNullOrBlank constraintAnnotation) {
        this.exceptionType = constraintAnnotation.exceptionType();
        this.minLetters = constraintAnnotation.minLetters();
        this.allowDigits = constraintAnnotation.allowDigits();
        this.allowSpecialChars = constraintAnnotation.allowSpecialChars();

        // Строим регулярное выражение на основе параметров
        this.pattern = buildValidationPattern();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            buildConstraintViolation(context, "Value cannot be null");
            return false;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            buildConstraintViolation(context, "Value cannot be empty or contain only whitespace");
            return false;
        }

        // Проверяем количество букв
        int letterCount = countLetters(trimmed);
        if (letterCount < minLetters) {
            buildConstraintViolation(context,
                    String.format("Value must contain at least %d letter(s), found %d",
                            minLetters, letterCount));
            return false;
        }

        // Проверяем разрешенные символы (если нужно ограничение)
        if (!allowDigits || !allowSpecialChars) {
            if (!trimmed.matches(pattern)) {
                String restriction = buildRestrictionMessage();
                buildConstraintViolation(context,
                        String.format("Value contains invalid characters. %s", restriction));
                return false;
            }
        }

        return true;
    }

    private int countLetters(String str) {
        // Считаем только буквы (русские и английские)
        int count = 0;
        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) {
                count++;
            }
        }
        return count;
    }

    private String buildValidationPattern() {
        StringBuilder patternBuilder = new StringBuilder("^[");

        // Всегда разрешаем буквы
        patternBuilder.append("a-zA-Zа-яА-ЯёЁ");

        if (allowDigits) {
            patternBuilder.append("0-9");
        }

        if (allowSpecialChars) {
            // Разрешаем основные спецсимволы
            patternBuilder.append("\\s\\-\\.\\,_@");
        }

        patternBuilder.append("]*$");
        return patternBuilder.toString();
    }

    private String buildRestrictionMessage() {
        StringBuilder message = new StringBuilder("Allowed: letters");
        if (!allowDigits) message.append(", no digits");
        if (!allowSpecialChars) message.append(", no special characters");
        return message.toString();
    }

    private void buildConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();

        HibernateConstraintValidatorContext hibernateContext =
                context.unwrap(HibernateConstraintValidatorContext.class);

        hibernateContext
                .addMessageParameter("exceptionType", exceptionType)
                .addMessageParameter("userMessage", exceptionType.getUserMessage())
                .addMessageParameter("originalMessage", message)
                .buildConstraintViolationWithTemplate("{ru.yandex.practicum.validation.NotNullOrBlank.message}")
                .addConstraintViolation();
    }
}