package com.lan.app.flows.eventslist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.session.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for storing events-flow navigation context in the shared session payloadJson.
 * Keeps track of which festival the user navigated from so EventDetailHandler
 * can render the correct "Back" button.
 */
public final class EventsListSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Key for the festival ID the current event was opened from. Absent when opened from the plain list. */
    static final String KEY_PARENT_FESTIVAL_ID = "evf_parent_festival_id";

    public static String getParentFestivalId(Session s) {
        return get(s, KEY_PARENT_FESTIVAL_ID);
    }

    public static void setParentFestivalId(Session s, String festivalId) {
        put(s, KEY_PARENT_FESTIVAL_ID, festivalId);
    }

    public static void clearParentFestivalId(Session s) {
        remove(s, KEY_PARENT_FESTIVAL_ID);
    }

    // ---- payloadJson helpers (same pattern as RegistrationSession) ----

    private static String get(Session s, String key) {
        return load(s).get(key);
    }

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

    private EventsListSession() {}
}
