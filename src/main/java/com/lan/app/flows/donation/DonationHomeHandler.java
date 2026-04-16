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
public class DonationHomeHandler implements StepHandler {

    @Inject TelegramClient telegramClient;
    @Inject I18n i18n;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        String text = i18n.t(lang, "donation_title") + "\n\n"
                + i18n.t(lang, "donation_howto") + "\n"
                + "• " + i18n.t(lang, "donation_opt_events") + "\n"
                + "• " + i18n.t(lang, "donation_opt_bar") + "\n"
                + "• " + i18n.t(lang, "donation_opt_cowork") + "\n"
                + "• " + i18n.t(lang, "donation_opt_contact") + "\n\n"
                + i18n.t(lang, "donation_choose");

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_contact"), DonationFlowDef.CB_CONTACT)
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_events"),  "events"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_cowork"),  "booking")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_home"),    "start"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "donation_btn_done"),    DonationFlowDef.CB_DONE)
                )
        ));

        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(DonationFlowDef.FLOW, DonationFlowDef.STEP_HOME);
    }
}
