package com.lan.app.flows.eventpayment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPendingStoreTest {

    @Test
    void unknownRegId_returnsEmpty() {
        PaymentPendingStore store = new PaymentPendingStore();

        assertThat(store.getUserChatId("missing")).isEmpty();
    }

    @Test
    void store_thenGetUserChatId_returnsStoredValue() {
        PaymentPendingStore store = new PaymentPendingStore();

        store.store("reg-1", 555L);

        assertThat(store.getUserChatId("reg-1")).contains(555L);
    }

    @Test
    void remove_clearsStoredValue() {
        PaymentPendingStore store = new PaymentPendingStore();
        store.store("reg-1", 555L);

        store.remove("reg-1");

        assertThat(store.getUserChatId("reg-1")).isEmpty();
    }

    @Test
    void store_overwritesExistingValueForSameRegId() {
        PaymentPendingStore store = new PaymentPendingStore();
        store.store("reg-1", 555L);

        store.store("reg-1", 777L);

        assertThat(store.getUserChatId("reg-1")).contains(777L);
    }
}
