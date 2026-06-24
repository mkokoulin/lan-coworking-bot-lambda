package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResultDto(
    @JsonProperty("guestRowId") int guestRowId,
    @JsonProperty("status") String status,
    @JsonProperty("failureReason") String failureReason
) {}
