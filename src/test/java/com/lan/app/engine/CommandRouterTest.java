package com.lan.app.engine;

import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.flows.cwbooking.CwBookingFlowDef;
import com.lan.app.flows.cwlink.CwLinkFlowDef;
import com.lan.app.flows.cwlink.CwLoginConfirmHandler;
import com.lan.app.flows.eventchange.EventChangeFlowDef;
import com.lan.app.flows.eventconfirm.EventConfirmFlowDef;
import com.lan.app.flows.eventnotify.EventNotifyFlowDef;
import com.lan.app.flows.eventpayment.EventPaymentFlowDef;
import com.lan.app.flows.eventslist.EventsListFlowDef;
import com.lan.app.flows.myevents.MyEventsFlowDef;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.flows.start.StartFlowDef;
import com.lan.app.flows.weeklydigest.WeeklyDigestUnsubscribeHandler;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests CommandRouter's own routing/auth-gate logic in isolation: FlowRegistry is mocked
 * so routing decisions don't cascade into real downstream flow handlers (which have their
 * own side effects like sending messages, unrelated to what CommandRouter itself decides).
 */
@QuarkusTest
class CommandRouterTest {

    @Inject
    CommandRouter router;

    @InjectMock
    FlowRegistry registry;

    @InjectMock
    GuestService guestService;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    CwLoginConfirmHandler cwLoginConfirmHandler;

    @InjectMock
    WeeklyDigestUnsubscribeHandler weeklyDigestUnsubscribeHandler;

    private final StepHandler stubHandler = mock(StepHandler.class);

    @BeforeEach
    void setup() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
        lenient().when(guestService.findByChatId(anyLong())).thenReturn(Optional.empty());
        lenient().when(registry.getCommand(any())).thenReturn(Optional.empty());
        lenient().when(registry.getStep(any(), any())).thenReturn(Optional.empty());
        lenient().when(stubHandler.handle(any(), any())).thenReturn(StepResult.finish());
        // Auth-gate/fallback tests commonly land here.
        lenient().when(registry.getStep(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW)).thenReturn(Optional.of(stubHandler));
    }

    private void registerCommand(String command, String flow, String step) {
        when(registry.getCommand(command)).thenReturn(Optional.of(new FlowEntry(flow, step)));
        when(registry.getStep(flow, step)).thenReturn(Optional.of(stubHandler));
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext textCtx(String text) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setText(text);
        return UpdateContext.fromIncomingUpdate(u);
    }

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @Test
    void unrecognizedCommand_fallsBackToStartShow() {
        Session s = session();

        router.route(textCtx("/totally_unknown_command_xyz"), s);

        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
    }

    @Test
    void startCommand_withRegDeepLink_routesToEventConfirm() {
        Session s = session();
        when(registry.getStep(EventConfirmFlowDef.FLOW, EventConfirmFlowDef.STEP_CONFIRM)).thenReturn(Optional.of(stubHandler));

        router.route(textCtx("/start reg_abc123"), s);

        assertThat(s.getFlow()).isEqualTo(EventConfirmFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(EventConfirmFlowDef.STEP_CONFIRM);
    }

    @Test
    void startCommand_withCwBookingDeepLink_routesToCwBookingConfirm() {
        Session s = session();
        when(registry.getStep(CwBookingFlowDef.FLOW, CwBookingFlowDef.STEP_CONFIRM)).thenReturn(Optional.of(stubHandler));

        router.route(textCtx("/start cwbooking_xyz"), s);

        assertThat(s.getFlow()).isEqualTo(CwBookingFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(CwBookingFlowDef.STEP_CONFIRM);
    }

    @Test
    void startCommand_withCwLinkDeepLink_routesToCwLink() {
        Session s = session();
        when(registry.getStep(CwLinkFlowDef.FLOW, CwLinkFlowDef.STEP_LINK)).thenReturn(Optional.of(stubHandler));

        router.route(textCtx("/start cwlink_xyz"), s);

        assertThat(s.getFlow()).isEqualTo(CwLinkFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(CwLinkFlowDef.STEP_LINK);
    }

    @Test
    void callback_yesPrefix_routesToEventNotifyAction() {
        Session s = session();
        RegistrationSession.markRegistered(s);
        when(registry.getStep(EventNotifyFlowDef.FLOW, EventNotifyFlowDef.STEP_ACTION)).thenReturn(Optional.of(stubHandler));

        router.route(callbackCtx("/" + EventNotifyFlowDef.PREFIX_YES + "10_20_30"), s);

        assertThat(s.getFlow()).isEqualTo(EventNotifyFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(EventNotifyFlowDef.STEP_ACTION);
    }

    @Test
    void callback_noPrefix_routesToEventNotifyAction() {
        Session s = session();
        RegistrationSession.markRegistered(s);
        when(registry.getStep(EventNotifyFlowDef.FLOW, EventNotifyFlowDef.STEP_ACTION)).thenReturn(Optional.of(stubHandler));

        router.route(callbackCtx("/" + EventNotifyFlowDef.PREFIX_NO + "10_20_30"), s);

        assertThat(s.getFlow()).isEqualTo(EventNotifyFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(EventNotifyFlowDef.STEP_ACTION);
    }

    @Test
    void callback_cancelFamilyPrefixes_routeToMyEventsCancelAction() {
        when(registry.getStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_CANCEL_ACTION)).thenReturn(Optional.of(stubHandler));

        for (String prefix : new String[] {
                MyEventsFlowDef.CB_CANCEL_PFX,
                MyEventsFlowDef.CB_CANCEL_YES_PFX,
                MyEventsFlowDef.CB_CANCEL_NO_PFX
        }) {
            Session session = session();
            RegistrationSession.markRegistered(session);
            router.route(callbackCtx("/" + prefix + "reg-1"), session);
            assertThat(session.getFlow()).isEqualTo(MyEventsFlowDef.FLOW);
            assertThat(session.getStep()).isEqualTo(MyEventsFlowDef.STEP_CANCEL_ACTION);
        }
    }

    @Test
    void callback_guestsPrefix_routesToMyEventsGuestsPrompt() {
        Session s = session();
        RegistrationSession.markRegistered(s);
        when(registry.getStep(MyEventsFlowDef.FLOW, MyEventsFlowDef.STEP_GUESTS_PROMPT)).thenReturn(Optional.of(stubHandler));

        router.route(callbackCtx("/" + MyEventsFlowDef.CB_GUESTS_PFX + "reg-1"), s);

        assertThat(s.getFlow()).isEqualTo(MyEventsFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(MyEventsFlowDef.STEP_GUESTS_PROMPT);
    }

    @Test
    void callback_payApprovePrefix_routesToEventPaymentAdmin() {
        Session s = session();
        when(registry.getStep(EventPaymentFlowDef.FLOW, EventPaymentFlowDef.STEP_ADMIN)).thenReturn(Optional.of(stubHandler));

        router.route(callbackCtx("pay_approve_42"), s);

        assertThat(s.getFlow()).isEqualTo(EventPaymentFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(EventPaymentFlowDef.STEP_ADMIN);
    }

    @Test
    void callback_cwConfirmPrefix_delegatesDirectlyToCwLoginConfirmHandler() {
        Session s = session();
        when(cwLoginConfirmHandler.handle(any(), any())).thenReturn(StepResult.finish());

        StepResult result = router.route(callbackCtx("cw_confirm_" + UUID.randomUUID()), s);

        verify(cwLoginConfirmHandler).handle(any(), any());
        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void callback_digestUnsubPrefix_delegatesDirectlyToWeeklyDigestUnsubscribeHandler() {
        Session s = session();
        when(weeklyDigestUnsubscribeHandler.handle(any(), any())).thenReturn(StepResult.finish());

        StepResult result = router.route(callbackCtx("digest_unsub_661"), s);

        verify(weeklyDigestUnsubscribeHandler).handle(any(), any());
        assertThat(result).isEqualTo(StepResult.finish());
    }

    @Test
    void callback_evtRegPrefix_routesToEventsListRegisterStep() {
        Session s = session();
        when(registry.getStep(EventsListFlowDef.FLOW, EventsListFlowDef.STEP_REGISTER)).thenReturn(Optional.of(stubHandler));

        router.route(callbackCtx(EventsListFlowDef.CB_EVT_REG_PREFIX + "42"), s);

        assertThat(s.getFlow()).isEqualTo(EventsListFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(EventsListFlowDef.STEP_REGISTER);
    }

    @Test
    void callback_eventChangePrefix_routesToEventChangeWaitMessage() {
        Session s = session();
        when(registry.getStep(EventChangeFlowDef.FLOW, EventChangeFlowDef.STEP_WAIT_MESSAGE)).thenReturn(Optional.of(stubHandler));

        router.route(callbackCtx(EventChangeFlowDef.CB_PREFIX + "42"), s);

        assertThat(s.getFlow()).isEqualTo(EventChangeFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(EventChangeFlowDef.STEP_WAIT_MESSAGE);
    }

    @Test
    void plainCallbackMatchingRegisteredCommand_isTreatedAsCommand() {
        Session s = session();
        registerCommand("wifi", "wifi", "wifi:show");

        router.route(callbackCtx("wifi"), s);

        assertThat(s.getFlow()).isEqualTo("wifi");
        assertThat(s.getStep()).isEqualTo("wifi:show");
        verify(stubHandler).handle(any(), any());
    }

    @Test
    void restrictedCommand_whenNotAuthenticated_sendsAuthRequiredAndForcesStartShow() {
        Session s = session();
        registerCommand("profile", "secure", "secure:profile");
        when(guestService.findByChatId(100L)).thenReturn(Optional.empty());

        router.route(textCtx("/profile"), s);

        verify(telegramClient).sendHtml(eq(100L), any(), any());
        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
    }

    @Test
    void guestCommand_wifi_doesNotRequireAuthentication() {
        Session s = session();
        registerCommand("wifi", "wifi", "wifi:show");

        router.route(textCtx("/wifi"), s);

        verify(telegramClient, never()).sendHtml(anyLong(), any(), any());
        verify(stubHandler).handle(any(), any());
    }

    @Test
    void restrictedCommand_whenGuestFoundByChatId_marksSessionRegisteredAndProceeds() {
        Session s = session();
        registerCommand("profile", "secure", "secure:profile");
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(UUID.randomUUID());
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));

        router.route(textCtx("/profile"), s);

        assertThat(RegistrationSession.isRegistered(s)).isTrue();
        assertThat(RegistrationSession.getGuestId(s)).isNotBlank();
        assertThat(s.getFlow()).isEqualTo("secure");
        verify(telegramClient, never()).sendHtml(anyLong(), any(), any());
        verify(stubHandler).handle(any(), any());
    }

    @Test
    void alreadyRegisteredSession_missingGuestId_backfillsItViaGuestService() {
        Session s = session();
        registerCommand("profile", "secure", "secure:profile");
        RegistrationSession.markRegistered(s);
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        UUID id = UUID.randomUUID();
        when(guest.getId()).thenReturn(id);
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));

        router.route(textCtx("/profile"), s);

        assertThat(RegistrationSession.getGuestId(s)).isEqualTo(id.toString());
    }

    @Test
    void manualLogout_blocksAuthEvenIfPreviouslyRegistered() {
        Session s = session();
        registerCommand("profile", "secure", "secure:profile");
        RegistrationSession.setManualLogout(s);

        router.route(textCtx("/profile"), s);

        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
    }

    @Test
    void normalizeCommand_stripsBotSuffixAndLowercases() {
        Session s = session();
        registerCommand("start", StartFlowDef.FLOW, StartFlowDef.STEP_SHOW);

        router.route(textCtx("/START@my_coworking_bot"), s);

        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        verify(stubHandler).handle(any(), any());
    }

    @Test
    void blankTextAndCallback_fallsBackToStartShow() {
        Session s = session();

        router.route(textCtx(""), s);

        assertThat(s.getFlow()).isEqualTo(StartFlowDef.FLOW);
        assertThat(s.getStep()).isEqualTo(StartFlowDef.STEP_SHOW);
    }

    @Test
    void noHandlerRegisteredEvenForStartShow_returnsFinish() {
        Session s = session();
        when(registry.getStep(StartFlowDef.FLOW, StartFlowDef.STEP_SHOW)).thenReturn(Optional.empty());

        StepResult result = router.route(textCtx("/totally_unknown_command_xyz"), s);

        assertThat(result).isEqualTo(StepResult.finish());
    }
}
