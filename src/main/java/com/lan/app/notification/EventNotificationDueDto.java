package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventNotificationDueDto {

    @JsonProperty("id")
    public int id;

    @JsonProperty("message")
    public String message;

    @JsonProperty("eventName")
    public String eventName;

    @JsonProperty("recipients")
    public List<NotificationRecipientDto> recipients;
}
