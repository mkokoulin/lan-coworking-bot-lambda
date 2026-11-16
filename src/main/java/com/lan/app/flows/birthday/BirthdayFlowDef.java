package com.lan.app.flows.birthday;

public final class BirthdayFlowDef {

    public static final String FLOW      = "birthday";
    public static final String STEP_HOME = "birthday:home";
    public static final String STEP_NAME = "birthday:name";
    public static final String STEP_TEXT = "birthday:text";

    // callback payloads (без "/")
    public static final String CB_WISH      = "birthday:wish";
    public static final String CB_ANONYMOUS = "birthday:anonymous";

    public static final String KEY_NAME = "birthday.name";

    private BirthdayFlowDef() {}
}
