package com.lan.app.flows.eventsurvey;

public class EventSurveyFlowDef {
    public static final String FLOW = "event_survey";
    public static final String STEP_RATING = "event_survey:rating";
    public static final String STEP_TEXT = "event_survey:text";

    /** Raw callback prefix — NOT prefixed with "/" so CommandRouter can match startsWith. */
    public static final String CB_SURVEY_RATE_PREFIX = "survey_rate_";

    public static final String KEY_RATING = "event_survey.rating";
    public static final String KEY_EVENT_ROW_ID = "event_survey.eventRowId";
    public static final String KEY_GUEST_ROW_ID = "event_survey.guestRowId";
    public static final String KEY_REGISTRATION_ROW_ID = "event_survey.registrationRowId";
}
