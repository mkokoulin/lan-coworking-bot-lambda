package com.lan.app.flows.registration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.session.Session;

import java.util.HashMap;
import java.util.Map;

final class RegistrationSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static String getFirstName(Session s) { return get(s, RegistrationFlowDef.KEY_FIRST_NAME); }
    static String getLastName(Session s)  { return get(s, RegistrationFlowDef.KEY_LAST_NAME); }
    static String getPhone(Session s)     { return get(s, RegistrationFlowDef.KEY_PHONE); }
    static String getUsername(Session s)  { return get(s, RegistrationFlowDef.KEY_USERNAME); }
    static boolean isRegistered(Session s){ return "true".equals(get(s, RegistrationFlowDef.KEY_REGISTERED)); }

    static void setFirstName(Session s, String v) { put(s, RegistrationFlowDef.KEY_FIRST_NAME, v); }
    static void setLastName(Session s, String v)  { put(s, RegistrationFlowDef.KEY_LAST_NAME, v); }
    static void setPhone(Session s, String v)     { put(s, RegistrationFlowDef.KEY_PHONE, v); }
    static void setAdditionalPhone(Session s, String v) { put(s, RegistrationFlowDef.KEY_ADDITIONAL_PHONE, v); }
    static void setUsername(Session s, String v)  { put(s, RegistrationFlowDef.KEY_USERNAME, v); }
    static void markRegistered(Session s)         { put(s, RegistrationFlowDef.KEY_REGISTERED, "true"); }

    static void clearTemp(Session s) {
        Map<String, String> data = load(s);
        data.remove(RegistrationFlowDef.KEY_FIRST_NAME);
        data.remove(RegistrationFlowDef.KEY_LAST_NAME);
        data.remove(RegistrationFlowDef.KEY_PHONE);
        data.remove(RegistrationFlowDef.KEY_ADDITIONAL_PHONE);

        save(s, data);
    }

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

    private RegistrationSession() {}
}
