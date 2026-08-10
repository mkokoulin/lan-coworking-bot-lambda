package com.lan.app.flows.registration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.session.Session;

import java.util.HashMap;
import java.util.Map;

public final class RegistrationSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String getFirstName(Session s)       { return get(s, RegistrationFlowDef.KEY_FIRST_NAME); }
    public static String getLastName(Session s)        { return get(s, RegistrationFlowDef.KEY_LAST_NAME); }
    public static String getPhone(Session s)           { return get(s, RegistrationFlowDef.KEY_PHONE); }
    public static String getAdditionalPhone(Session s) { return get(s, RegistrationFlowDef.KEY_ADDITIONAL_PHONE); }
    public static String getUsername(Session s)        { return get(s, RegistrationFlowDef.KEY_USERNAME); }
    public static boolean isRegistered(Session s)      { return "true".equals(get(s, RegistrationFlowDef.KEY_REGISTERED)); }
    public static boolean isManualLogout(Session s)    { return "true".equals(get(s, RegistrationFlowDef.KEY_MANUAL_LOGOUT)); }
    public static String getGuestId(Session s)         { return get(s, RegistrationFlowDef.KEY_GUEST_ID); }

    public static void setFirstName(Session s, String v) { put(s, RegistrationFlowDef.KEY_FIRST_NAME, v); }
    public static void setLastName(Session s, String v)  { put(s, RegistrationFlowDef.KEY_LAST_NAME, v); }
    public static void setPhone(Session s, String v)     { put(s, RegistrationFlowDef.KEY_PHONE, v); }
    public static void setAdditionalPhone(Session s, String v) { put(s, RegistrationFlowDef.KEY_ADDITIONAL_PHONE, v); }
    public static void setUsername(Session s, String v)  { put(s, RegistrationFlowDef.KEY_USERNAME, v); }
    public static void markRegistered(Session s)              { put(s, RegistrationFlowDef.KEY_REGISTERED, "true"); }
    public static void setManualLogout(Session s)             { put(s, RegistrationFlowDef.KEY_MANUAL_LOGOUT, "true"); }
    public static void clearLogout(Session s)                 { remove(s, RegistrationFlowDef.KEY_MANUAL_LOGOUT); }
    public static void setGuestId(Session s, String id)       { put(s, RegistrationFlowDef.KEY_GUEST_ID, id); }
    public static String getDeductTariffId(Session s)         { return get(s, RegistrationFlowDef.KEY_DEDUCT_TARIFF_ID); }
    public static void setDeductTariffId(Session s, String id){ put(s, RegistrationFlowDef.KEY_DEDUCT_TARIFF_ID, id); }
    public static String getDeductTariffName(Session s)       { return get(s, RegistrationFlowDef.KEY_DEDUCT_TARIFF_NAME); }
    public static void setDeductTariffName(Session s, String n){ put(s, RegistrationFlowDef.KEY_DEDUCT_TARIFF_NAME, n); }
    public static String getTariffReqId(Session s)            { return get(s, RegistrationFlowDef.KEY_TARIFF_REQ_ID); }
    public static void setTariffReqId(Session s, String id)   { put(s, RegistrationFlowDef.KEY_TARIFF_REQ_ID, id); }
    public static String getTariffReqName(Session s)          { return get(s, RegistrationFlowDef.KEY_TARIFF_REQ_NAME); }
    public static void setTariffReqName(Session s, String n)  { put(s, RegistrationFlowDef.KEY_TARIFF_REQ_NAME, n); }

    public static void clearAuth(Session s) {
        Map<String, String> data = load(s);
        data.remove(RegistrationFlowDef.KEY_REGISTERED);
        data.remove(RegistrationFlowDef.KEY_GUEST_ID);
        data.remove(RegistrationFlowDef.KEY_DEDUCT_TARIFF_ID);
        data.remove(RegistrationFlowDef.KEY_DEDUCT_TARIFF_NAME);
        save(s, data);
    }

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

    private static void remove(Session s, String key) {
        Map<String, String> data = load(s);
        data.remove(key);
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
