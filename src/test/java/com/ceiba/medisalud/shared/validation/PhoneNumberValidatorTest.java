package com.ceiba.medisalud.shared.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberValidatorTest {

    private final PhoneNumberValidator validator = new PhoneNumberValidator();

    @ParameterizedTest
    @NullAndEmptySource
    void isValid_debeAceptarValoresNulosOVacios_porqueElTelefonoEsOpcional(String valor) {
        assertThat(validator.isValid(valor, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"555-1001", "3001234567", "(601) 555 1001", "+57 300 123 4567"})
    void isValid_debeAceptarTelefonosConAlMenos7Digitos(String valor) {
        assertThat(validator.isValid(valor, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "555-10", "abcdefg", "1234abc"})
    void isValid_debeRechazarTelefonosInvalidos(String valor) {
        assertThat(validator.isValid(valor, null)).isFalse();
    }

    @Test
    void isValid_noRequiereElContextoDeValidacion() {
        ConstraintValidatorContext context = null;
        assertThat(validator.isValid("555-1001", context)).isTrue();
    }
}
