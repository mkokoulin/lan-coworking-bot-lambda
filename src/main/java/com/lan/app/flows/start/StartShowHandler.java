package com.lan.app.flows.start;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import org.jboss.logging.Logger;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class StartShowHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(StartShowHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;

    @Inject
    public StartShowHandler(
        TelegramClient telegramClient,
        I18n i18n,
        GuestService guestService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
    }

    private enum AuthState { REGISTERED, LOGGED_OUT, UNREGISTERED }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        AuthState state = resolveState(session);

        if (state != AuthState.REGISTERED) {
            return showGuestMenu(session, lang, state);
        }

        if (hasPendingLinkSession(session)) {
            return sendLoginConfirmation(session, lang);
        }

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_booking"), "booking"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_coworking"), "coworking")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_meetingroom"), "meetingroom"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_printout"), "printout")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_events"), "events"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_wifi"), "wifi")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_menu"), "menu"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_about"), "about")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_language"), "language"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_donation"), "donation")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_profile"), "profile"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_review"), "review")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_news"), "news")
            )
        ));

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "start_message"), kb);
        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW);
    }

    private StepResult showGuestMenu(Session session, String lang, AuthState state) {
        List<List<Map<String, String>>> rows = new java.util.ArrayList<>();
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_wifi"), "wifi")
        ));
        if (state == AuthState.LOGGED_OUT) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_language"), "language"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_login"), "login")
            ));
        } else {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_login"), "login"),
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_register"), "registration")
            ));
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_language"), "language")
            ));
        }

        var kb = KeyboardBuilder.inline(rows);

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "start_message_guest"), kb);
        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW);
    }

    private boolean hasPendingLinkSession(Session session) {
        String guestIdStr = RegistrationSession.getGuestId(session);
        if (guestIdStr == null) return false;
        try {
            UUID guestId = UUID.fromString(guestIdStr);
            GuestService.LinkStatus status = guestService.getLinkStatus(guestId);
            return status == GuestService.LinkStatus.PENDING;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            LOG.warnf(e, "Failed to check link status for guestId=%s", guestIdStr);
            return false;
        }
    }

    private StepResult sendLoginConfirmation(Session session, String lang) {
        String guestIdStr = RegistrationSession.getGuestId(session);
        // Use raw callback_data (without "/") so CommandRouter routes these to CwLoginConfirmHandler
        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                Map.of("text", i18n.t(lang, "cw_login_confirm_yes"), "callback_data", "cw_confirm_" + guestIdStr),
                Map.of("text", i18n.t(lang, "cw_login_confirm_no"),  "callback_data", "cw_reject_" + guestIdStr)
            )
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "cw_login_confirm_text"), kb);
        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW);
    }

    private AuthState resolveState(Session session) {
        if (RegistrationSession.isRegistered(session)) return AuthState.REGISTERED;
        if (RegistrationSession.isManualLogout(session)) return AuthState.LOGGED_OUT;
        var guest = guestService.findByChatId(session.getChatId());
        if (guest.isPresent()) {
            RegistrationSession.markRegistered(session);
            if (guest.get().getId() != null) {
                RegistrationSession.setGuestId(session, guest.get().getId().toString());
            }
            return AuthState.REGISTERED;
        }
        return AuthState.UNREGISTERED;
    }
}
