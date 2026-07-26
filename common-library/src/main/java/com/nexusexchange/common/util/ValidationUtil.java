package com.nexusexchange.common.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

public final class ValidationUtil {
    private static final Validator FACTORY_VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            FACTORY_VALIDATOR = factory.getValidator();
        }
    }

    private ValidationUtil() {}

    public static <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = FACTORY_VALIDATOR.validate(object);
        if (!violations.isEmpty()) {
            String errorMsg = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + errorMsg);
        }
    }
}
