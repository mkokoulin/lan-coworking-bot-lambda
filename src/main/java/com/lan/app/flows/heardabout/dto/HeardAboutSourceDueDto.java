package com.lan.app.flows.heardabout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One item from GET {backendUrl}/events/v1/bot/heard-about-source/due. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeardAboutSourceDueDto {
    public Long chatId;
    public int guestRowId;
}
