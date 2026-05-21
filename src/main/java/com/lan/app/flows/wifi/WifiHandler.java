package com.lan.app.flows.wifi;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class WifiHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final String guestPassword;
    private final String residentPassword;

    @Inject
    public WifiHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @ConfigProperty(name = "app.wifi.guest-password", defaultValue = "—") String guestPassword,
        @ConfigProperty(name = "app.wifi.resident-password", defaultValue = "") String residentPassword
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestPassword = guestPassword;
        this.residentPassword = residentPassword;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        boolean hasResident = residentPassword != null && !residentPassword.isBlank();
        String text = hasResident
            ? i18n.t(lang, "wifi_message_full").formatted(guestPassword, residentPassword)
            : i18n.t(lang, "wifi_message_guest").formatted(guestPassword);

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "wifi_btn_back"), "start")
            )
        ));

        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(WifiFlowDef.FLOW, WifiFlowDef.STEP_SHOW);
    }
}
