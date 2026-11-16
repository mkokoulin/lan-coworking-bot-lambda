package com.lan.app.flows.heardabout.dto;

/** Body for POST {backendUrl}/events/v1/bot/heard-about-source/{guestRowId}/answer. */
public record HeardAboutSourceAnswerDto(String source, String comment) {}
