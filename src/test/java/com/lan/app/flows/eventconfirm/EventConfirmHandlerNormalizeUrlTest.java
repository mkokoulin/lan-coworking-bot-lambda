package com.lan.app.flows.eventconfirm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventConfirmHandlerNormalizeUrlTest {

    @Test
    void doubledHttpsScheme_collapsedToSingle() {
        assertThat(EventConfirmHandler.normalizeUrl("https://https://lancoworking.am"))
                .isEqualTo("https://lancoworking.am");
    }

    @Test
    void trailingSlash_stripped() {
        assertThat(EventConfirmHandler.normalizeUrl("https://lancoworking.am/"))
                .isEqualTo("https://lancoworking.am");
    }

    @Test
    void doubledSchemeAndTrailingSlash_bothFixed() {
        assertThat(EventConfirmHandler.normalizeUrl("https://https://lancoworking.am/"))
                .isEqualTo("https://lancoworking.am");
    }

    @Test
    void alreadyClean_unchanged() {
        assertThat(EventConfirmHandler.normalizeUrl("https://lancoworking.am"))
                .isEqualTo("https://lancoworking.am");
    }

    @Test
    void multipleTrailingSlashes_allStripped() {
        assertThat(EventConfirmHandler.normalizeUrl("https://lancoworking.am///"))
                .isEqualTo("https://lancoworking.am");
    }
}
