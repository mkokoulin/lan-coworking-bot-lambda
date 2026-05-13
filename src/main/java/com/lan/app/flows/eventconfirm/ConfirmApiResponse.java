package com.lan.app.flows.eventconfirm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from POST {EVENTS_API_URL}/bot/confirm.
 *
 * result values:
 *   confirmed              – registration successfully confirmed
 *   already_confirmed_same – already confirmed by this telegram_chat_id (show E3)
 *   already_confirmed_other – confirmed by a different user (show E4)
 *   token_not_found        – token does not exist in DB (show E1)
 *   token_expired          – token_expires_at < now (show E2)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmApiResponse {

    @JsonProperty("result")
    public String result;

    /** Present for confirmed / already_confirmed_same; may be null otherwise. */
    @JsonProperty("event")
    public EventDetails event;
}
