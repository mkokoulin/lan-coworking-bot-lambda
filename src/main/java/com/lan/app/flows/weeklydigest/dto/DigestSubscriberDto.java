package com.lan.app.flows.weeklydigest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One item from GET {backendUrl}/events/v1/bot/weekly-digest/subscribers. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigestSubscriberDto {
    public Long chatId;
    public int guestRowId;
}
