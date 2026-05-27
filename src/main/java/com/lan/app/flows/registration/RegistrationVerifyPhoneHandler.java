package com.lan.app.flows.registration;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class RegistrationVerifyPhoneHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public RegistrationVerifyPhoneHandler(
        TelegramClient telegramClient,
        I18n i18n
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback()) {
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE);
        }

        String input    = ctx.messageText() == null ? "" : ctx.messageText().trim().replaceAll("\\s+", "");
        String phone    = RegistrationSession.getPhone(session);
        String expected = PhoneValidator.lastFour(phone);

        if (!expected.equals(input)) {
            var cancelKb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start"))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "reg_verify_wrong"), cancelKb);
            return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_VERIFY_PHONE);
        }

        return StepResult.stay(RegistrationFlowDef.FLOW, RegistrationFlowDef.STEP_SUMMARY);
    }
}
