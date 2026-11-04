package com.lan.app.notify;

/**
 * @param channel which chat the message targets: {@code "event"} routes to the event-registrations
 *                chat ({@code telegram.event-chat-id}), anything else (including null) routes to the
 *                admin chat. Lets the site send event registrations to the same chat as bot-initiated
 *                registrations while keeping bookings, reviews and support in the admin chat.
 */
public record NotifyRequest(String message, String retryId, String channel) {}
