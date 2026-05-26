package com.lan.app.flows.start;

import com.lan.app.client.baserow.model.CoworkingTariffResponse;
import com.lan.app.config.TelegramConfig;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.service.TariffService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class StartTariffListHandler implements StepHandler {

    private static final String SELECT_PREFIX = "/select_tariff:";

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final TariffService tariffService;
    private final GuestService guestService;
    private final TelegramConfig telegramConfig;

    @Inject
    public StartTariffListHandler(
        TelegramClient telegramClient,
        I18n i18n,
        TariffService tariffService,
        GuestService guestService,
        TelegramConfig telegramConfig
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.tariffService = tariffService;
        this.guestService = guestService;
        this.telegramConfig = telegramConfig;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // Guest selects a tariff from the list
        if (ctx.hasCallback() && ctx.callbackData().startsWith(SELECT_PREFIX)) {
            return handleSelection(ctx, session, lang);
        }

        return showTariffList(session, lang);
    }

    private StepResult showTariffList(Session session, String lang) {
        List<CoworkingTariffResponse> tariffs = tariffService.listAllTariffs();

        if (tariffs.isEmpty()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "tariff_list_empty"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_PROFILE);
            return StepResult.finish();
        }

        var rows = new ArrayList<List<Map<String, String>>>();
        for (CoworkingTariffResponse t : tariffs) {
            String label = buildTariffLabel(t);
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(label, SELECT_PREFIX + t.getId().toString())
            ));
        }
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "tariff_btn_back"), "/profile")
        ));

        telegramClient.sendHtml(
            session.getChatId(),
            i18n.t(lang, "tariff_list_title"),
            KeyboardBuilder.inline(rows)
        );

        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_TARIFF_LIST);
    }

    private StepResult handleSelection(UpdateContext ctx, Session session, String lang) {
        String tariffIdStr = ctx.callbackData().substring(SELECT_PREFIX.length());
        UUID tariffId;
        try {
            tariffId = UUID.fromString(tariffIdStr);
        } catch (IllegalArgumentException e) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "tariff_request_error"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_PROFILE);
            return StepResult.finish();
        }

        String tariffName = tariffService.getTariffName(tariffId).orElse("—");

        String guestIdStr = RegistrationSession.getGuestId(session);
        if (guestIdStr == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "tariff_request_error"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_PROFILE);
            return StepResult.finish();
        }

        UUID guestId = UUID.fromString(guestIdStr);
        var result = tariffService.requestGuestTariff(guestId, tariffId);

        if (result.isEmpty()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "tariff_request_error"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_PROFILE);
            return StepResult.finish();
        }

        notifyAdmin(session, tariffName, lang);

        telegramClient.sendHtml(
            session.getChatId(),
            i18n.t(lang, "tariff_request_success").formatted(tariffName),
            null
        );

        session.setFlow(StartFlowDef.FLOW);
        session.setStep(StartFlowDef.STEP_PROFILE);
        return StepResult.finish();
    }

    private void notifyAdmin(Session session, String tariffName, String lang) {
        try {
            var guest = guestService.findByChatId(session.getChatId());
            String guestName = guest.map(g -> g.getFirstName() + " " + g.getLastName()).orElse("?");
            String contact = guest.map(g -> {
                if (g.getTelegram() != null && !g.getTelegram().isBlank()) return "@" + g.getTelegram();
                if (g.getPhone() != null && !g.getPhone().isBlank()) return g.getPhone();
                return "chatId:" + session.getChatId();
            }).orElse("chatId:" + session.getChatId());

            String msg = i18n.t(lang, "tariff_admin_notify").formatted(guestName, contact, tariffName);
            telegramClient.sendHtml(telegramConfig.adminChatId(), msg, null);
        } catch (Exception e) {
            // Notification failure must not break the guest flow
        }
    }

    private String buildTariffLabel(CoworkingTariffResponse t) {
        String name = t.getName() != null ? t.getName() : "—";
        if (t.getPrice() != null && t.getPrice() > 0) {
            return name + " — " + t.getPrice() + "֏";
        }
        return name;
    }
}
