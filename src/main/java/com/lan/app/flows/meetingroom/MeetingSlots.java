package com.lan.app.flows.meetingroom;

import com.lan.app.ui.KeyboardBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class MeetingSlots {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE dd.MM");
    private static final DateTimeFormatter ISO_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    static Object calendarKeyboard() {
        return calendarKeyboard(null);
    }

    static Object calendarKeyboard(Map<String, String> homeButton) {
        LocalDate today = LocalDate.now();
        List<List<Map<String, String>>> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.plusDays(i);
            String label = day.format(DATE_FMT);
            String cb    = MeetingFlowDef.CB_DATE_PFX + day.format(ISO_FMT);
            rows.add(KeyboardBuilder.row(KeyboardBuilder.cbCmd(label, cb)));
        }
        if (homeButton != null) {
            rows.add(KeyboardBuilder.row(homeButton));
        }
        return KeyboardBuilder.inline(rows);
    }

    static Object startTimeKeyboard() {
        return startTimeKeyboard(null);
    }

    static Object startTimeKeyboard(Map<String, String> homeButton) {
        return timesKeyboard(MeetingFlowDef.CB_START_PFX,
                generateSlots(MeetingFlowDef.SLOT_START_HOUR, MeetingFlowDef.SLOT_END_HOUR),
                homeButton);
    }

    static Object endTimeKeyboard(String startHHMM) {
        return endTimeKeyboard(startHHMM, null);
    }

    static Object endTimeKeyboard(String startHHMM, Map<String, String> homeButton) {
        LocalTime start = LocalTime.parse(startHHMM, TIME_FMT);
        LocalTime minEnd = start.plusMinutes(MeetingFlowDef.SLOT_STEP_MIN);
        return timesKeyboard(MeetingFlowDef.CB_END_PFX,
                generateSlotsFrom(minEnd, MeetingFlowDef.SLOT_END_HOUR),
                homeButton);
    }

    static boolean isValidInterval(String startHHMM, String endHHMM) {
        try {
            LocalTime s = LocalTime.parse(startHHMM, TIME_FMT);
            LocalTime e = LocalTime.parse(endHHMM, TIME_FMT);
            LocalTime open  = LocalTime.of(MeetingFlowDef.SLOT_START_HOUR, 0);
            LocalTime close = LocalTime.of(MeetingFlowDef.SLOT_END_HOUR, 0);
            return !s.isBefore(open) && !e.isAfter(close) && e.isAfter(s);
        } catch (Exception ex) {
            return false;
        }
    }

    private static Object timesKeyboard(String prefix, List<String> times, Map<String, String> homeButton) {
        int perRow = 4;
        List<List<Map<String, String>>> rows = new ArrayList<>();
        for (int i = 0; i < times.size(); i += perRow) {
            List<Map<String, String>> row = new ArrayList<>();
            for (int j = i; j < Math.min(i + perRow, times.size()); j++) {
                row.add(KeyboardBuilder.cbCmd(times.get(j), prefix + times.get(j)));
            }
            rows.add(row);
        }
        if (homeButton != null) {
            rows.add(KeyboardBuilder.row(homeButton));
        }
        return KeyboardBuilder.inline(rows);
    }

    private static List<String> generateSlots(int startHour, int endHour) {
        List<String> out = new ArrayList<>();
        LocalTime t = LocalTime.of(startHour, 0);
        LocalTime limit = LocalTime.of(endHour, 0);
        while (!t.isAfter(limit)) {
            out.add(t.format(TIME_FMT));
            t = t.plusMinutes(MeetingFlowDef.SLOT_STEP_MIN);
        }
        return out;
    }

    private static List<String> generateSlotsFrom(LocalTime from, int endHour) {
        List<String> out = new ArrayList<>();
        LocalTime limit = LocalTime.of(endHour, 0);
        // выровнять по шагу
        int min = (from.getMinute() / MeetingFlowDef.SLOT_STEP_MIN) * MeetingFlowDef.SLOT_STEP_MIN;
        LocalTime aligned = LocalTime.of(from.getHour(), min);
        if (aligned.isBefore(from)) aligned = aligned.plusMinutes(MeetingFlowDef.SLOT_STEP_MIN);
        LocalTime t = aligned;
        while (!t.isAfter(limit)) {
            out.add(t.format(TIME_FMT));
            t = t.plusMinutes(MeetingFlowDef.SLOT_STEP_MIN);
        }
        return out;
    }

    private MeetingSlots() {}
}