package com.lan.app.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramContact(
    String phone_number,
    String first_name,
    String last_name,
    Long   user_id
) {
}