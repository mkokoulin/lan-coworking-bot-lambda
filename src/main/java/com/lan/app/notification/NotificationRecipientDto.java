package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRecipientDto {

    @JsonProperty("chatId")
    public Long chatId;

    @JsonProperty("guestRowId")
    public int guestRowId;
}
