package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationRecipientDto(
    @JsonProperty("chatId") Long chatId,
    @JsonProperty("guestRowId") int guestRowId,
    @JsonProperty("registrationRowId") int registrationRowId
) {}
