package com.lan.app.flows.myevents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.session.Session;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

final class MyEventsSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Asia/Yerevan"));

    static String getPendingRegId(Session s) {
        return load(s).get(MyEventsFlowDef.KEY_PENDING_REG_ID);
    }

    static void setPendingRegId(Session s, String regId) {
        Map<String, String> data = load(s);
        data.put(MyEventsFlowDef.KEY_PENDING_REG_ID, regId);
        save(s, data);
    }

    static void clear(Session s) {
        Map<String, String> data = load(s);
        data.remove(MyEventsFlowDef.KEY_PENDING_REG_ID);
        save(s, data);
    }

    /** Russian plural form for "гость": 1 гость, 2-4 гостя, 5-20/0 гостей (11-14 always "гостей"). */
    static String guestsLabel(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "гость";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "гостя";
        return "гостей";
    }

    static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    static String formatDate(String iso) {
        Instant instant = parseInstant(iso);
        return instant != null ? DATE_FMT.format(instant) : "—";
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

    private MyEventsSession() {}
}
