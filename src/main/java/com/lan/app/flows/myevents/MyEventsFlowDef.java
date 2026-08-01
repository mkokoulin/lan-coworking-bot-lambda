package com.lan.app.flows.myevents;

public final class MyEventsFlowDef {

    public static final String FLOW = "myevents";
    public static final String STEP_SHOW = "myevents:show";
    public static final String STEP_CANCEL_CONFIRM = "myevents:cancel_confirm";
    public static final String STEP_GUEST_COUNT_WAIT = "myevents:guest_count_wait";

    // Raw callback prefixes — NOT prefixed with "/" so CommandRouter can match startsWith.
    // "<prefix><registration id>" stays well under Telegram's 64-byte callback_data cap.
    public static final String CB_CANCEL_PFX = "me_c_";
    public static final String CB_CANCEL_YES_PFX = "me_y_";
    public static final String CB_CANCEL_NO_PFX = "me_n_";
    public static final String CB_GUEST_COUNT_PFX = "me_g_";

    static final String KEY_PENDING_REG_ID = "myevents.pending_reg_id";

    private MyEventsFlowDef() {}
}
