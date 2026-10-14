package com.lan.app.flows.eventpayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class EventPaymentAdminHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(EventPaymentAdminHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final PaymentPendingStore pendingStore;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    @Inject
    public EventPaymentAdminHandler(TelegramClient telegramClient, I18n i18n, PaymentPendingStore pendingStore) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.pendingStore = pendingStore;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String cb = ctx.callbackData();
        if (cb == null) return StepResult.finish();

        boolean approve = cb.startsWith("pay_approve_");
        String id = approve
                ? cb.substring("pay_approve_".length())
                : cb.substring("pay_reject_".length());

        if (approve) {
            Long userChatId = callApprove(id);

            // Fallback to pending store (bot-originated payments)
            if (userChatId == null) {
                userChatId = pendingStore.getUserChatId(id).orElse(null);
            }

            if (userChatId != null) {
                telegramClient.sendHtml(userChatId, i18n.t("ru", "event_payment_approved"), null);
            }
            telegramClient.sendHtml(session.getChatId(), i18n.t("ru", "event_payment_admin_approved"), null);
        } else {
            Long userChatId = callReject(id);

            if (userChatId == null) {
                userChatId = pendingStore.getUserChatId(id).orElse(null);
            }

            if (userChatId != null) {
                telegramClient.sendHtml(userChatId, i18n.t("ru", "event_payment_rejected"), null);
            }
            telegramClient.sendHtml(session.getChatId(), i18n.t("ru", "event_payment_admin_rejected"), null);
        }

        pendingStore.remove(id);
        session.setFlow("");
        session.setStep("");
        return StepResult.finish();
    }

    private Long callApprove(String id) {
        // Try new payment endpoint first
        Long chatId = callBackend("/events/v1/payments/" + id + "/approve");
        if (chatId != null) return chatId;

        // Fallback: old mark-paid endpoint (bot-originated payments using regId)
        return callBackend("/events/v1/registrations/" + id + "/mark-paid");
    }

    private Long callReject(String id) {
        // Try new payment endpoint first
        Long chatId = callBackend("/events/v1/payments/" + id + "/reject");
        if (chatId != null) return chatId;

        // Bot-originated rejections don't need a backend call — chatId comes from pendingStore
        return null;
    }

    private Long callBackend(String path) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + path))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                var node = mapper.readTree(resp.body());
                var chatIdNode = node.get("chatId");
                if (chatIdNode != null && !chatIdNode.isNull()) {
                    return chatIdNode.asLong();
                }
            }
        } catch (Exception e) {
            log.warnf("Backend call failed [%s]: %s", path, e.getMessage());
        }
        return null;
    }
}
