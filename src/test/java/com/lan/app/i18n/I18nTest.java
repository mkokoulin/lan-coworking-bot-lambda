package com.lan.app.i18n;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class I18nTest {

    private static final String KEY = "help_message";

    @Inject
    I18n i18n;

    @Test
    void ru_and_en_returnNonBlankDifferentStringsForSameKey() {
        String ru = i18n.t("ru", KEY);
        String en = i18n.t("en", KEY);

        assertThat(ru).isNotBlank();
        assertThat(en).isNotBlank();
        assertThat(ru).isNotEqualTo(en);
    }

    @Test
    void ruIsCaseInsensitive() {
        String ru = i18n.t("ru", KEY);
        String ruUpper = i18n.t("RU", KEY);

        assertThat(ruUpper).isEqualTo(ru);
    }

    @Test
    void unrecognizedLangCode_fallsBackToEnglish() {
        String en = i18n.t("en", KEY);
        String fr = i18n.t("fr", KEY);

        assertThat(fr).isEqualTo(en);
    }

    @Test
    void nullLang_fallsBackToEnglishWithoutThrowing() {
        String en = i18n.t("en", KEY);
        String nullLang = i18n.t(null, KEY);

        assertThat(nullLang).isEqualTo(en);
    }
}
