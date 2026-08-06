package com.lan.app.flows.eventnotify.dto;

/** Outgoing body for POST {backendUrl}/events/v1/bot/event-notifications/{id}/action (NotificationActionRequest). */
public class NotificationActionDto {
    public int guestRowId;
    public int registrationRowId;
    public String action;

    public NotificationActionDto(int guestRowId, int registrationRowId, String action) {
        this.guestRowId = guestRowId;
        this.registrationRowId = registrationRowId;
        this.action = action;
    }
}
