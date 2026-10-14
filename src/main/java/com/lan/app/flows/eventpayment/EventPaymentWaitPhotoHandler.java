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

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EventPaymentWaitPhotoHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final PaymentPendingStore pendingStore;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public EventPaymentWaitPhotoHandler(TelegramClient telegramClient, I18n i18n, PaymentPendingStore pendingStore) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.pendingStore = pendingStore;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (!ctx.hasPhoto()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_payment_no_photo"), null);
            return StepResult.stay(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_WAIT_PHOTO);
        }

        String regId = EventPaymentSession.getRegId(session);
        String price = EventPaymentSession.getPrice(session);
        if (price == null) price = "?";

        // Store chatId for later notification
        pendingStore.store(regId, session.getChatId());

        String username = ctx.username() != null ? "@" + ctx.username() : String.valueOf(session.getChatId());
        String caption = String.format(i18n.t("ru", "event_payment_admin_caption"), username, price, regId);

        // Build inline keyboard for admin
        var approveBtn = Map.of("text", i18n.t("ru", "event_payment_admin_approve_btn"),
                "callback_data", "pay_approve_" + regId);
        var rejectBtn = Map.of("text", i18n.t("ru", "event_payment_admin_reject_btn"),
                "callback_data", "pay_reject_" + regId);
        var keyboard = Map.of("inline_keyboard", List.of(List.of(approveBtn, rejectBtn)));

        telegramClient.sendPhotoByFileId(adminChatId, ctx.fileId(), caption, keyboard);
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "event_payment_waiting"), null);

        // Clear flow so user doesn't stay in payment step
        session.setFlow("");
        session.setStep("");

        return StepResult.finish();
    }
}
