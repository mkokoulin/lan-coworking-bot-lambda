package com.lan.app.flows.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "+37491123456",
        "37491123456",
        "091123456",
        "91123456"
    })
    void normalize_validArmenianNumbers_normalizeToCanonicalForm(String input) {
        assertThat(PhoneValidator.normalize(input)).isEqualTo("+37491123456");
    }

    @Test
    void normalize_withSpacesAndDashes_stripsFormatting() {
        assertThat(PhoneValidator.normalize("+374 91-12 34 56")).isEqualTo("+37491123456");
    }

    @Test
    void normalize_null_returnsNull() {
        assertThat(PhoneValidator.normalize(null)).isNull();
    }

    @Test
    void normalize_invalidOperatorPrefix_returnsNull() {
        // "12" is not a valid AM mobile operator code
        assertThat(PhoneValidator.normalize("+37412123456")).isNull();
    }

    @Test
    void normalize_tooShort_returnsNull() {
        assertThat(PhoneValidator.normalize("12345")).isNull();
    }

    @Test
    void normalize_garbage_returnsNull() {
        assertThat(PhoneValidator.normalize("not a phone")).isNull();
    }

    @Test
    void normalize_wrongLengthLocalNumber_returnsNull() {
        // starts with 0 but isn't 9 digits total
        assertThat(PhoneValidator.normalize("0912345")).isNull();
    }

    @Test
    void lastFour_returnsLastFourDigits() {
        assertThat(PhoneValidator.lastFour("+37491123456")).isEqualTo("3456");
    }

    @Test
    void lastFour_shorterThanFour_returnsEmptyString() {
        assertThat(PhoneValidator.lastFour("123")).isEmpty();
    }

    @Test
    void lastFour_null_returnsEmptyString() {
        assertThat(PhoneValidator.lastFour(null)).isEmpty();
    }
}
