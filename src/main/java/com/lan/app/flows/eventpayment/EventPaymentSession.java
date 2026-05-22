package com.lan.app.flows.eventpayment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.session.Session;

import java.util.HashMap;
import java.util.Map;

public final class EventPaymentSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String getRegId(Session s)  { return get(s, EventPaymentFlowDef.KEY_REG_ID); }
    public static String getPrice(Session s)  { return get(s, EventPaymentFlowDef.KEY_PRICE); }

    public static void setRegId(Session s, String v) { put(s, EventPaymentFlowDef.KEY_REG_ID, v); }
    public static void setPrice(Session s, String v) { put(s, EventPaymentFlowDef.KEY_PRICE, v); }

    private static String get(Session s, String key) { return load(s).get(key); }

    private static void put(Session s, String key, String value) {
        Map<String, String> data = load(s);
        data.put(key, value);
        save(s, data);
    }

    private static Map<String, String> load(Session s) {
        try {
            String json = s.getPayloadJson();
            if (json == null || json.isBlank() || json.equals("{}")) return new HashMap<>();
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static void save(Session s, Map<String, String> data) {
        try { s.setPayloadJson(MAPPER.writeValueAsString(data)); }
        catch (Exception ignored) {}
    }

    private EventPaymentSession() {}
}
