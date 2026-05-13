package com.lan.app.flows.myevents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MyRegistrationDto {

    @JsonProperty("event_name")
    public String eventName;

    @JsonProperty("date_start")
    public OffsetDateTime dateStart;

    @JsonProperty("status")
    public String status;
}
