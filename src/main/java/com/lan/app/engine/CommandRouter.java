    package com.lan.app.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lan.app.domain.UpdateContext;
import com.lan.app.flows.cwbooking.CwBookingFlowDef;
import com.lan.app.flows.cwlink.CwLinkFlowDef;
import com.lan.app.flows.cwlink.CwLoginConfirmHandler;
import com.lan.app.flows.eventconfirm.EventConfirmFlowDef;
import com.lan.app.flows.eventpayment.EventPaymentFlowDef;
import com.lan.app.flows.eventslist.EventsListFlowDef;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.i18n.I18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
public class CommandRouter {

    private static final Logger logger = LoggerFactory.getLogger(CommandRouter.class);

    /** Commands accessible without authentication */
    private static final Set<String> GUEST_COMMANDS = Set.of(
        "start", "wifi", "password", "language", "lang",
        "registration", "register", "reg", "login", "help"
    );

    private final FlowRegistry registry;
    private final GuestService guestService;
    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final CwLoginConfirmHandler cwLoginConfirmHandler;

    @Inject
    public CommandRouter(
        FlowRegistry registry,
        GuestService guestService,
        TelegramClient telegramClient,
        I18n i18n,
        CwLoginConfirmHandler cwLoginConfirmHandler
    ) {
        this.registry = registry;
        this.guestService = guestService;
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.cwLoginConfirmHandler = cwLoginConfirmHandler;
    }

    public StepResult route(UpdateContext ctx, Session session) {
        String command = normalizeCommand(ctx.command());

        logger.info(
            "Routing command: '{}', session: {}, chatId: {}", 
            command != null ? command : "<none>",
            session,
            session.getChatId()
        );

        if (command != null) {
            String args = ctx.commandArgs();
            if ("start".equals(command) && args != null && args.startsWith("reg_")) {
                session.setFlow(EventConfirmFlowDef.FLOW);
                session.setStep(EventConfirmFlowDef.STEP_CONFIRM);
            } else if ("start".equals(command) && args != null && args.startsWith("cwbooking_")) {
                session.setFlow(CwBookingFlowDef.FLOW);
                session.setStep(CwBookingFlowDef.STEP_CONFIRM);
            } else if ("start".equals(command) && args != null && args.startsWith("cwlink_")) {
                session.setFlow(CwLinkFlowDef.FLOW);
                session.setStep(CwLinkFlowDef.STEP_LINK);
            } else {
                FlowEntry entry = registry.getCommand(command).orElse(null);
                if (entry != null) {
                    session.setFlow(entry.flow());
                    session.setStep(entry.step());
                }
            }
        }

        // If no slash-command was resolved, try plain callback data as a registered command.
        // This handles legacy inline buttons that omit the leading "/" (e.g. "tariff_list",
        // "profile", "deduct_confirm") and makes all navigation buttons forward-compatible.
        if (command == null && ctx.hasCallback()) {
            String cb = ctx.callbackData();
            if (!isBlank(cb)) {
                FlowEntry cbEntry = registry.getCommand(cb).orElse(null);
                if (cbEntry != null) {
                    command = cb;
                    session.setFlow(cbEntry.flow());
                    session.setStep(cbEntry.step());
                }
            }
        }

        // Route pay_approve_/pay_reject_ callbacks to admin payment handler
        // Route cw_confirm_/cw_reject_ callbacks directly — bypass flow system
        // Route evt_reg_/evt_/evf_ callbacks to the events-list flow
        if (ctx.hasCallback()) {
            String cb = ctx.callbackData();
            if (cb != null && (cb.startsWith("pay_approve_") || cb.startsWith("pay_reject_"))) {
                session.setFlow(EventPaymentFlowDef.FLOW);
                session.setStep(EventPaymentFlowDef.STEP_ADMIN);
            } else if (cb != null && (cb.startsWith("cw_confirm_") || cb.startsWith("cw_reject_"))) {
                return cwLoginConfirmHandler.handle(ctx, session);
            } else if (cb != null && cb.startsWith(EventsListFlowDef.CB_EVT_REG_PREFIX)) {
                session.setFlow(EventsListFlowDef.FLOW);
                session.setStep(EventsListFlowDef.STEP_REGISTER);
            } else if (cb != null && cb.startsWith(EventsListFlowDef.CB_EVT_PREFIX)) {
                session.setFlow(EventsListFlowDef.FLOW);
                session.setStep(EventsListFlowDef.STEP_DETAIL);
            } else if (cb != null && cb.startsWith(EventsListFlowDef.CB_EVF_PREFIX)) {
                session.setFlow(EventsListFlowDef.FLOW);
                session.setStep(EventsListFlowDef.STEP_FESTIVAL);
            }
        }

        if (isBlank(session.getFlow()) || isBlank(session.getStep())) {
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_DONE);
        }

        // Block access to restricted flows for unauthenticated users
        if (command != null && !GUEST_COMMANDS.contains(command) && !isAuthenticated(session)) {
            String lang = session.getLang();
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "auth_required"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
        }

        StepHandler handler = registry.getStep(session.getFlow(), session.getStep()).orElse(null);
        if (handler == null) {
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            handler = registry.getStep(session.getFlow(), session.getStep()).orElse(null);
            if (handler == null) {
                return StepResult.finish();
            }
        }

        return handler.handle(ctx, session);
    }

    private boolean isAuthenticated(Session session) {
        if (RegistrationSession.isRegistered(session)) {
            // Ensure guestId is always present — may be missing in older sessions
            if (RegistrationSession.getGuestId(session) == null) {
                var guest = guestService.findByChatId(session.getChatId());
                guest.ifPresent(g -> {
                    if (g.getId() != null) {
                        RegistrationSession.setGuestId(session, g.getId().toString());
                    }
                });
            }
            return true;
        }
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

    private String normalizeCommand(String command) {
        if (isBlank(command)) {
            return null;
        }

        String raw = command.trim();

        int atIdx = raw.indexOf('@');
        if (atIdx >= 0) {
            raw = raw.substring(0, atIdx);
        }

        return raw.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}