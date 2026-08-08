package com.ceiba.medisalud.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final int MIN_DIGITS = 7;
    private static final String ALLOWED_CHARACTERS_REGEX = "[0-9()+\\-\\s]+";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (!value.matches(ALLOWED_CHARACTERS_REGEX)) {
            return false;
        }
        String digitsOnly = value.replaceAll("\\D", "");
        return digitsOnly.length() >= MIN_DIGITS;
    }
}
