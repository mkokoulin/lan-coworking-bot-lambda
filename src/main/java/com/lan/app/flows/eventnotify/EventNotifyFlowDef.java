package com.lan.app.flows.eventnotify;

public final class EventNotifyFlowDef {

    public static final String FLOW = "event_notify";
    public static final String STEP_ACTION = "event_notify:action";

    // Callback-data prefixes: "/en_y_<notificationId>_<guestRowId>_<registrationRowId>" (yes)
    // and "/en_n_..." (no). Kept short because Telegram caps callback_data at 64 bytes.
    public static final String PREFIX_YES = "en_y_";
    public static final String PREFIX_NO = "en_n_";

    private EventNotifyFlowDef() {}
}
