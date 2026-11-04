package com.lan.app.flows.registration;

import com.lan.app.session.Session;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationSessionTest {

    private static Session newSession() {
        return Session.newDefault(1L, 2L);
    }

    @Test
    void freshSession_isNotRegisteredAndHasNoFields() {
        Session s = newSession();

        assertThat(RegistrationSession.isRegistered(s)).isFalse();
        assertThat(RegistrationSession.getFirstName(s)).isNull();
        assertThat(RegistrationSession.getGuestId(s)).isNull();
    }

    @Test
    void setters_roundTripThroughPayloadJson() {
        Session s = newSession();

        RegistrationSession.setFirstName(s, "Ann");
        RegistrationSession.setLastName(s, "Smith");
        RegistrationSession.setPhone(s, "+37491123456");
        RegistrationSession.setUsername(s, "annsmith");

        assertThat(RegistrationSession.getFirstName(s)).isEqualTo("Ann");
        assertThat(RegistrationSession.getLastName(s)).isEqualTo("Smith");
        assertThat(RegistrationSession.getPhone(s)).isEqualTo("+37491123456");
        assertThat(RegistrationSession.getUsername(s)).isEqualTo("annsmith");
        assertThat(s.getPayloadJson()).contains("\"reg.firstName\":\"Ann\"");
    }

    @Test
    void markRegistered_setsRegisteredFlag() {
        Session s = newSession();

        RegistrationSession.markRegistered(s);

        assertThat(RegistrationSession.isRegistered(s)).isTrue();
    }

    @Test
    void manualLogout_setAndClear() {
        Session s = newSession();

        RegistrationSession.setManualLogout(s);
        assertThat(RegistrationSession.isManualLogout(s)).isTrue();

        RegistrationSession.clearLogout(s);
        assertThat(RegistrationSession.isManualLogout(s)).isFalse();
    }

    @Test
    void clearAuth_removesRegistrationAndTariffKeysButKeepsOtherFields() {
        Session s = newSession();
        RegistrationSession.setFirstName(s, "Ann");
        RegistrationSession.markRegistered(s);
        RegistrationSession.setGuestId(s, "guest-1");
        RegistrationSession.setDeductTariffId(s, "tariff-1");
        RegistrationSession.setDeductTariffName(s, "Day pass");

        RegistrationSession.clearAuth(s);

        assertThat(RegistrationSession.isRegistered(s)).isFalse();
        assertThat(RegistrationSession.getGuestId(s)).isNull();
        assertThat(RegistrationSession.getDeductTariffId(s)).isNull();
        assertThat(RegistrationSession.getDeductTariffName(s)).isNull();
        assertThat(RegistrationSession.getFirstName(s)).isEqualTo("Ann");
    }

    @Test
    void clearTemp_removesNameAndPhoneFieldsButKeepsRegisteredFlag() {
        Session s = newSession();
        RegistrationSession.setFirstName(s, "Ann");
        RegistrationSession.setLastName(s, "Smith");
        RegistrationSession.setPhone(s, "+37491123456");
        RegistrationSession.setAdditionalPhone(s, "+37491999999");
        RegistrationSession.markRegistered(s);

        RegistrationSession.clearTemp(s);

        assertThat(RegistrationSession.getFirstName(s)).isNull();
        assertThat(RegistrationSession.getLastName(s)).isNull();
        assertThat(RegistrationSession.getPhone(s)).isNull();
        assertThat(RegistrationSession.getAdditionalPhone(s)).isNull();
        assertThat(RegistrationSession.isRegistered(s)).isTrue();
    }

    @Test
    void malformedPayloadJson_treatedAsEmptyMap_doesNotThrow() {
        Session s = newSession();
        s.setPayloadJson("not valid json");

        assertThat(RegistrationSession.getFirstName(s)).isNull();

        RegistrationSession.setFirstName(s, "Ann");
        assertThat(RegistrationSession.getFirstName(s)).isEqualTo("Ann");
    }

    @Test
    void tariffRequestFields_roundTrip() {
        Session s = newSession();

        RegistrationSession.setTariffReqId(s, "req-1");
        RegistrationSession.setTariffReqName(s, "Month pass");

        assertThat(RegistrationSession.getTariffReqId(s)).isEqualTo("req-1");
        assertThat(RegistrationSession.getTariffReqName(s)).isEqualTo("Month pass");
    }
}
