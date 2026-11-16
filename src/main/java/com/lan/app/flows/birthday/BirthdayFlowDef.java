package com.lan.app.flows.birthday;

public final class BirthdayFlowDef {

    public static final String FLOW      = "birthday";
    public static final String STEP_HOME = "birthday:home";
    public static final String STEP_TEXT = "birthday:text";

    // callback payload (без "/")
    public static final String CB_WISH = "birthday:wish";

    private BirthdayFlowDef() {}
}
