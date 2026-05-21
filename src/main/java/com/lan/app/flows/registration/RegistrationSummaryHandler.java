package com.lan.app.flows.registration;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RegistrationSummaryHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(RegistrationSummaryHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;

    @ConfigProperty(name = "telegram.admin-chat-id")
    Long adminChatId;

    @Inject
    public RegistrationSummaryHandler(
        TelegramClient telegramClient,
        I18n i18n,
        GuestService guestService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        String firstName = RegistrationSession.getFirstName(session);
        String lastName  = RegistrationSession.getLastName(session);
        String phone     = RegistrationSession.getPhone(session);
        if (phone == null || phone.isBlank()) {
            phone = RegistrationSession.getAdditionalPhone(session);
        }
        if (phone == null) phone = "";

        String username = RegistrationSession.getUsername(session);
        if ((username == null || username.isBlank()) && ctx.username() != null) {
            username = ctx.username();
            RegistrationSession.setUsername(session, username);
        }

        String telegramHandle = (username != null && !username.isBlank()) ? username : "";
        String lastNameForApi = (lastName != null && !lastName.isBlank()) ? lastName : "-";
        String telegramForApi = !telegramHandle.isBlank() ? telegramHandle : "tg_" + session.getUserId();

        boolean created = false;
        try {
            var createdGuest = guestService.createGuest(firstName, lastNameForApi, telegramForApi, phone, session.getChatId());
            if (createdGuest != null && createdGuest.getId() != null) {
                RegistrationSession.setGuestId(session, createdGuest.getId().toString());
            }
            created = true;
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == 409) {
                LOG.infof("Coworking guest already exists: %s %s / %s — linking chatId", firstName, lastName, phone);
                var linked = guestService.linkChat(phone, session.getChatId());
                if (linked.isPresent()) {
                    if (linked.get().getId() != null) {
                        RegistrationSession.setGuestId(session, linked.get().getId().toString());
                    }
                    created = true;
                } else {
                    LOG.warnf("linkChat failed for phone=%s chatId=%d", phone, session.getChatId());
                }
            } else {
                LOG.errorf(e, "Failed to create coworking guest: %s %s / %s, status=%d", firstName, lastName, phone, status);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error creating coworking guest: %s %s / %s", firstName, lastName, phone);
        }

        if (!created) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "reg_error"), null);
            session.setFlow("");
            session.setStep("");
            return StepResult.finish();
        }

        RegistrationSession.markRegistered(session);

        String contactLine = !telegramHandle.isBlank()
                ? "@" + telegramHandle
                : "tg://user?id=" + session.getUserId();

        String adminMsg = "🆕 Новый гость:\n"
                + "👤 " + firstName + " " + lastName + "\n"
                + "📞 " + phone + "\n"
                + "✈️ " + contactLine;
        telegramClient.sendHtml(adminChatId, adminMsg, null);

        telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "reg_success").formatted(firstName), null);

        RegistrationSession.clearTemp(session);
        session.setFlow("");
        session.setStep("");
        return StepResult.finish();
    }
}
