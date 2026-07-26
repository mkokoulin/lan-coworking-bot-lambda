package com.lan.app.flows.eventnotify.dto;

/** Outgoing body for POST {backendUrl}/events/v1/bot/event-notifications/{id}/results (NotificationResultRequest). */
public class NotificationResultDto {
    public int guestRowId;
    public int registrationRowId;
    public String status;
    public String failureReason;

    public NotificationResultDto(int guestRowId, int registrationRowId, String status, String failureReason) {
        this.guestRowId = guestRowId;
        this.registrationRowId = registrationRowId;
        this.status = status;
        this.failureReason = failureReason;
    }
}
