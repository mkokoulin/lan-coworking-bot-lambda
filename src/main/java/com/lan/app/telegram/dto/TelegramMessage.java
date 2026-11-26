package com.lan.app.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramMessage(
    Long message_id,
    TelegramUser from,
    TelegramChat chat,
    String text,
    TelegramContact contact,
    List<TelegramPhotoSize> photo,
    TelegramDocument document
) {
}