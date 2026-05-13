package com.lan.app.flows.eventconfirm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDetails {

    @JsonProperty("name")
    public String name;

    @JsonProperty("date_start")
    public OffsetDateTime dateStart;

    @JsonProperty("date_end")
    public OffsetDateTime dateEnd;

    @JsonProperty("guests")
    public int guests;
}
