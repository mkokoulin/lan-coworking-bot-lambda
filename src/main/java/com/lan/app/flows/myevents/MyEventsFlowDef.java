package com.lan.app.flows.myevents;

public final class MyEventsFlowDef {

    public static final String FLOW = "my_events";

    public static final String STEP_LIST          = "my_events:list";
    public static final String STEP_CANCEL_ACTION = "my_events:cancel_action";
    public static final String STEP_GUESTS_PROMPT = "my_events:guests_prompt";
    public static final String STEP_GUESTS_WAIT   = "my_events:guests_wait";

    // callback_data prefixes: "<prefix><registration external UUID>" (~41 bytes, well under
    // Telegram's 64-byte callback_data cap). Mutually exclusive by construction, order-independent.
    public static final String CB_CANCEL_PFX     = "me_c_";
    public static final String CB_CANCEL_YES_PFX = "me_y_";
    public static final String CB_CANCEL_NO_PFX  = "me_n_";
    public static final String CB_GUESTS_PFX     = "me_g_";

    static final String KEY_PENDING_REG_ID = "my_events.pending_reg_id";

    private MyEventsFlowDef() {}
}
