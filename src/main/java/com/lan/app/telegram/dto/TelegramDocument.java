package com.lan.app.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramDocument(
    @JsonProperty("file_id")   String fileId,
    @JsonProperty("file_name") String fileName,
    @JsonProperty("mime_type") String mimeType
) {}
