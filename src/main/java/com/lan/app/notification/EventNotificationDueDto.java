package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventNotificationDueDto(
    @JsonProperty("id") int id,
    @JsonProperty("message") String message,
    @JsonProperty("eventName") String eventName,
    @JsonProperty("recipients") List<NotificationRecipientDto> recipients
) {}
