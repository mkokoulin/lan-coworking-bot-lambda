package com.lan.app.flows.start;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.PhoneValidator;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class StartLoginPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;

    @Inject
    public StartLoginPhoneHandler(TelegramClient telegramClient, I18n i18n, GuestService guestService) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        String rawPhone = ctx.sharedPhone() != null ? ctx.sharedPhone() : ctx.messageText();
        if (rawPhone == null || rawPhone.isBlank()) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "login_phone_empty"), cancelKb(lang));
            return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE);
        }

        String normalized = PhoneValidator.normalize(rawPhone.trim());
        if (normalized == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "login_phone_invalid"), cancelKb(lang));
            return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE);
        }

        var guest = guestService.linkChat(normalized, session.getChatId());
        if (guest.isEmpty()) {
            var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                    KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_register"), "registration"),
                    KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start")
                )
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "login_not_found"), kb);
            return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_LOGIN_PHONE);
        }

        var g = guest.get();
        RegistrationSession.clearLogout(session);
        RegistrationSession.markRegistered(session);
        if (g.getId() != null) {
            RegistrationSession.setGuestId(session, g.getId().toString());
        }

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "login_btn_profile"), "profile"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start")
            )
        ));

        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, "login_success").formatted(g.getFirstName()), kb);

        session.setFlow(StartFlowDef.FLOW);
        session.setStep(StartFlowDef.STEP_SHOW);
        return StepResult.finish();
    }

    private Object cancelKb(String lang) {
        return KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start"))
        ));
    }
}
