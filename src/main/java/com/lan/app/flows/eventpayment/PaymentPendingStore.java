package com.lan.app.flows.eventpayment;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PaymentPendingStore {

    private final Map<String, Long> regToUserChatId = new ConcurrentHashMap<>();

    public void store(String regId, Long chatId) {
        regToUserChatId.put(regId, chatId);
    }

    public Optional<Long> getUserChatId(String regId) {
        return Optional.ofNullable(regToUserChatId.get(regId));
    }

    public void remove(String regId) {
        regToUserChatId.remove(regId);
    }
}
