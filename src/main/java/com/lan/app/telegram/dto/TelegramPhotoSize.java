package com.lan.app.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramPhotoSize(
    @JsonProperty("file_id") String fileId,
    @JsonProperty("width") int width,
    @JsonProperty("height") int height
) {}
