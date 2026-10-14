package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventCapacityAlertDueDto(
    @JsonProperty("eventName") String eventName,
    @JsonProperty("registeredCount") int registeredCount,
    @JsonProperty("maxCapacity") int maxCapacity
) {}
