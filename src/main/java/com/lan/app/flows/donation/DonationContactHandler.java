package com.lan.app.flows.donation;

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
import java.util.Map;

@ApplicationScoped
public class DonationContactHandler implements StepHandler {

    @Inject TelegramClient telegramClient;
    @Inject I18n i18n;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        String text = i18n.t(lang, "donation_contact_title") + "\n\n"
                + i18n.t(lang, "donation_contact_text").formatted(DonationFlowDef.CONTACT_USERNAME) + "\n\n"
                + i18n.t(lang, "donation_thanks");

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        Map.of("text", i18n.t(lang, "donation_btn_open_chat"),
                               "url",  DonationFlowDef.CONTACT_URL)
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_back"), DonationFlowDef.CB_HOME),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_done"), DonationFlowDef.CB_DONE)
                )
        ));

        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(DonationFlowDef.FLOW, DonationFlowDef.STEP_CONTACT);
    }
}
