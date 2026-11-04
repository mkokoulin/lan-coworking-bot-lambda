package com.lan.app.flows.eventchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.session.Session;

import java.util.HashMap;
import java.util.Map;

final class EventChangeSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static String getRegId(Session s) { return get(s, EventChangeFlowDef.KEY_REG_ID); }

    static void setRegId(Session s, String v) { put(s, EventChangeFlowDef.KEY_REG_ID, v); }

    static void clear(Session s) {
        Map<String, String> data = load(s);
        data.remove(EventChangeFlowDef.KEY_REG_ID);
        save(s, data);
    }

    private static String get(Session s, String key) {
        return load(s).get(key);
    }

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
        try {
            s.setPayloadJson(MAPPER.writeValueAsString(data));
        } catch (Exception ignored) {}
    }

    private EventChangeSession() {}
}
