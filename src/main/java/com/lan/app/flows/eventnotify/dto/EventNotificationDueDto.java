package com.lan.app.flows.eventnotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** One item from GET {backendUrl}/events/v1/bot/event-notifications/due (EventNotificationDueResponse). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventNotificationDueDto {
    public int id;
    public String messageEn;
    public String messageRu;
    public String eventName;
    public List<EventNotificationRecipientDto> recipients;
}
