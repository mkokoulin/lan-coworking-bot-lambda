package com.lan.app.flows.eventnotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One item of RecipientDto from GET {backendUrl}/events/v1/bot/event-notifications/due. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventNotificationRecipientDto {
    public Long chatId;
    public int guestRowId;
    public int registrationRowId;
}
