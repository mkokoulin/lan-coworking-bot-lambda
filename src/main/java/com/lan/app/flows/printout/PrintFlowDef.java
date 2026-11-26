package com.lan.app.flows.printout;

public final class PrintFlowDef {

    public static final String FLOW             = "printout";
    public static final String STEP_PROMPT      = "print:prompt";
    public static final String STEP_WAIT_DETAILS = "print:wait_details";
    public static final String STEP_WAIT_FILE   = "print:wait_file";
    public static final String STEP_DONE        = "print:done";

    static final String KEY_DETAILS = "print.details";

    private PrintFlowDef() {}
}
