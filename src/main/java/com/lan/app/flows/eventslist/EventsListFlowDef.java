package com.lan.app.flows.eventslist;

public final class EventsListFlowDef {

    public static final String FLOW = "events_list";

    public static final String STEP_LIST     = "events_list:list";
    public static final String STEP_DETAIL   = "events_list:detail";
    public static final String STEP_REGISTER = "events_list:register";
    public static final String STEP_FESTIVAL = "events_list:festival";

    /** Raw callback prefixes — NOT prefixed with "/" so CommandRouter can match startsWith */
    public static final String CB_EVT_PREFIX     = "evt_";
    public static final String CB_EVT_REG_PREFIX = "evt_reg_";
    public static final String CB_EVF_PREFIX     = "evf_";

    /** Raw callback (exact match, no per-guest id needed) — "subscribe to weekly digest" button. */
    public static final String CB_DIGEST_SUB = "events_digest_sub";

    private EventsListFlowDef() {}
}
