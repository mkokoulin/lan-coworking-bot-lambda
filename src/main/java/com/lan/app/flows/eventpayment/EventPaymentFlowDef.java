package com.lan.app.flows.eventpayment;

public final class EventPaymentFlowDef {

    public static final String FLOW          = "event_payment";
    public static final String STEP_START    = "event_payment:start";
    public static final String STEP_WAIT_PHOTO = "event_payment:wait_photo";
    public static final String STEP_ADMIN    = "event_payment:admin";

    public static final String KEY_REG_ID = "pay_reg_id";
    public static final String KEY_PRICE  = "pay_price";

    private EventPaymentFlowDef() {}
}
