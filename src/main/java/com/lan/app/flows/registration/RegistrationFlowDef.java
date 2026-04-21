package com.lan.app.flows.registration;

public final class RegistrationFlowDef {
    public static final String FLOW              = "registration";
    public static final String STEP_START        = "reg:start";
    public static final String STEP_WAIT_NAME    = "reg:wait_name";
    public static final String STEP_WAIT_PHONE   = "reg:wait_phone";
    public static final String STEP_WAIT_ADDITIONAL_PHONE   = "reg:wait_additional_phone";
    public static final String STEP_VERIFY_PHONE = "reg:verify_phone";
    public static final String STEP_DONE         = "reg:done";

    public static final String CB_SHARE_PHONE = "reg:share_phone";

    static final String KEY_FIRST_NAME = "reg.firstName";
    static final String KEY_LAST_NAME  = "reg.lastName";
    static final String KEY_PHONE      = "reg.phone";
    static final String KEY_ADDITIONAL_PHONE = "reg.additional_phone";
    static final String KEY_USERNAME   = "reg.username";
    static final String KEY_REGISTERED = "reg.done";

    private RegistrationFlowDef() {}
}
