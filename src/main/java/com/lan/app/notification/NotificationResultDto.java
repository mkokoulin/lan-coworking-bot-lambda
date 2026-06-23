package com.lan.app.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResultDto {

    @JsonProperty("guestRowId")
    public int guestRowId;

    @JsonProperty("status")
    public String status;

    @JsonProperty("failureReason")
    public String failureReason;

    public NotificationResultDto(int guestRowId, String status, String failureReason) {
        this.guestRowId = guestRowId;
        this.status = status;
        this.failureReason = failureReason;
    }
}
