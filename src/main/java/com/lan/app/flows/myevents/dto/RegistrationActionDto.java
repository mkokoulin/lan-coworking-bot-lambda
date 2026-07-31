package com.lan.app.flows.myevents.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body of the bot cancel / guest-count backend endpoints (BotRegistrationActionResponse). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationActionDto {
    @JsonProperty("event_name")
    public String eventName;
    @JsonProperty("date_start")
    public String dateStart;
    @JsonProperty("previous_guest_count")
    public int previousGuestCount;
    @JsonProperty("guest_count")
    public int guestCount;
    @JsonProperty("guest_first_name")
    public String guestFirstName;
    @JsonProperty("guest_last_name")
    public String guestLastName;
    @JsonProperty("guest_phone")
    public String guestPhone;
    @JsonProperty("guest_telegram")
    public String guestTelegram;
}
