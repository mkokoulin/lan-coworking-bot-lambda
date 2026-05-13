package com.lan.app.flows.eventconfirm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * One item from GET {EVENTS_API_URL}/bot/pending-reminders.
 *
 * The backend returns registrations where:
 *   status = 'confirmed'
 *   AND telegram_chat_id IS NOT NULL
 *   AND reminder_sent = false
 *   AND event.date_start BETWEEN now()+1h45m AND now()+2h15m
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PendingReminderDto {

    @JsonProperty("registration_id")
    public Long registrationId;

    @JsonProperty("telegram_chat_id")
    public Long telegramChatId;

    @JsonProperty("event_name")
    public String eventName;

    @JsonProperty("date_start")
    public OffsetDateTime dateStart;
}
