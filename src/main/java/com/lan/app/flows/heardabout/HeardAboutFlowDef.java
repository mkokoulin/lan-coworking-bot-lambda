package com.lan.app.flows.heardabout;

public final class HeardAboutFlowDef {

    public static final String FLOW = "heard_about";
    public static final String STEP_CHOICE = "heard_about:choice";
    public static final String STEP_COMMENT = "heard_about:comment";

    // Callback-data prefixes: "/ha_ig_<guestRowId>" etc. Kept short because Telegram caps
    // callback_data at 64 bytes. Entered exclusively via HeardAboutScheduler's proactive
    // message — never via a user-typed command — so CommandRouter routes these prefixes
    // directly, the same way it does EventNotifyFlowDef.PREFIX_YES/PREFIX_NO.
    public static final String PREFIX_INSTAGRAM = "ha_ig_";
    public static final String PREFIX_GOOGLE = "ha_gg_";
    public static final String PREFIX_FRIENDS = "ha_fr_";
    public static final String PREFIX_OTHER = "ha_ot_";

    public static final String CB_SKIP = "ha_skip";

    public static final String KEY_SOURCE = "heard_about.source";
    public static final String KEY_GUEST_ROW_ID = "heard_about.guest_row_id";

    private HeardAboutFlowDef() {}
}
