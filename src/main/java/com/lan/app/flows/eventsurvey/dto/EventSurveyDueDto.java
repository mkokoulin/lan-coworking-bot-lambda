package com.lan.app.flows.eventsurvey.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One item from GET {backendUrl}/events/v1/bot/event-surveys/due. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventSurveyDueDto {
    public Long chatId;
    public int eventRowId;
    public String eventName;
    public int guestRowId;
    public int registrationRowId;
}
