package com.lan.app.flows.eventchange;

public final class EventChangeFlowDef {

    public static final String FLOW = "event_change";
    public static final String STEP_WAIT_MESSAGE = "event_change:wait_message";
    public static final String STEP_MENU = "event_change:menu";

    /** Raw callback prefixes — NOT prefixed with "/" so CommandRouter can match startsWith */
    public static final String CB_PREFIX = "evtchg_";
    public static final String CB_MENU_PREFIX = "ecm_";

    static final String KEY_REG_ID = "event_change.reg_id";

    private EventChangeFlowDef() {}
}
