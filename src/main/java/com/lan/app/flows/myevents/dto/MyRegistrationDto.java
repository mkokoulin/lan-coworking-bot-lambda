package com.lan.app.flows.myevents.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One item from GET {backendUrl}/events/v1/bot/my-registrations (BotRegistrationDto). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MyRegistrationDto {
    @JsonProperty("registration_id")
    public String registrationId;
    @JsonProperty("event_name")
    public String eventName;
    @JsonProperty("date_start")
    public String dateStart;
    @JsonProperty("guest_count")
    public int guestCount;
    @JsonProperty("is_cancelled")
    public boolean isCancelled;
}
