package com.lan.app.flows.donation;

public final class DonationFlowDef {
    public static final String FLOW         = "donation";
    public static final String STEP_HOME    = "donation:home";
    public static final String STEP_CONTACT = "donation:contact";
    public static final String STEP_DONE    = "donation:done";

    // callback payloads (без "/")
    public static final String CB_HOME    = "donation:home";
    public static final String CB_CONTACT = "donation:contact";
    public static final String CB_DONE    = "donation:done";

    public static final String CONTACT_USERNAME = "@lan_yerevan";
    public static final String CONTACT_URL      = "https://t.me/lan_yerevan";

    private DonationFlowDef() {}
}
