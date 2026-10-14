package com.lan.app.flows.eventpayment;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EventPaymentStartHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @ConfigProperty(name = "payment.card-number", defaultValue = "—")
    String cardNumber;

    @Inject
    public EventPaymentStartHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String price = EventPaymentSession.getPrice(session);
        if (price == null) price = "?";

        String text = String.format(i18n.t(lang, "event_payment_instructions"), price, cardNumber);
        telegramClient.sendHtml(session.getChatId(), text, null);

        session.setStep(EventPaymentFlowDef.STEP_WAIT_PHOTO);
        return StepResult.stay(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_WAIT_PHOTO);
    }
}
