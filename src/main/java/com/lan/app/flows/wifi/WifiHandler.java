package com.lan.app.flows.wifi;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import com.lan.app.util.QrCodeGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WifiHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(WifiHandler.class);
    private static final int QR_SIZE_PX = 400;

    @ConfigProperty(name = "app.wifi-guest-ssid", defaultValue = "")
    String guestSsid;

    @ConfigProperty(name = "app.wifi-guest-password", defaultValue = "")
    String guestPassword;

    @ConfigProperty(name = "app.wifi-private-ssid", defaultValue = "")
    String privateSsid;

    @ConfigProperty(name = "app.wifi-private-password", defaultValue = "")
    String privatePassword;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;

    @Inject
    public WifiHandler(TelegramClient telegramClient, I18n i18n, GuestService guestService) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        Long chatId = session.getChatId();

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "wifi_btn_home"), "start"))
        ));

        boolean authenticated = isAuthenticated(session);

        List<Network> networks = new ArrayList<>();
        if (!guestSsid.isBlank()) {
            networks.add(new Network(i18n.t(lang, "wifi_caption_guest"), guestSsid, guestPassword));
        }
        if (authenticated && !privateSsid.isBlank()) {
            networks.add(new Network(i18n.t(lang, "wifi_caption_private"), privateSsid, privatePassword));
        }

        if (networks.isEmpty()) {
            LOG.warn("No Wi-Fi networks configured, cannot generate Wi-Fi QR codes");
            telegramClient.sendHtml(chatId, i18n.t(lang, "wifi_not_configured"), kb);
            return StepResult.finish();
        }

        var fallbackLines = new StringBuilder(i18n.t(lang, "wifi_text_header"));
        for (Network network : networks) {
            String caption = network.captionTemplate().formatted(escapeHtml(network.ssid()));
            try {
                byte[] qr = QrCodeGenerator.png(QrCodeGenerator.wifiPayload(network.ssid(), network.password()), QR_SIZE_PX);
                telegramClient.sendPhoto(chatId, qr, "wifi-qr.png", caption);
            } catch (Exception e) {
                LOG.warnf(e, "Failed to generate Wi-Fi QR code for %s", network.ssid());
            }
            fallbackLines.append('\n')
                    .append(i18n.t(lang, "wifi_network_line").formatted(escapeHtml(network.ssid()), escapeHtml(network.password())));
        }

        telegramClient.sendHtml(chatId, fallbackLines.toString(), kb);

        return StepResult.finish();
    }

    private boolean isAuthenticated(Session session) {
        if (RegistrationSession.isRegistered(session)) return true;
        if (RegistrationSession.isManualLogout(session)) return false;
        var guest = guestService.findByChatId(session.getChatId());
        if (guest.isPresent()) {
            RegistrationSession.markRegistered(session);
            if (guest.get().getId() != null) {
                RegistrationSession.setGuestId(session, guest.get().getId().toString());
            }
            return true;
        }
        return false;
    }

    private record Network(String captionTemplate, String ssid, String password) {}

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
