package com.lan.app.flows.eventchange;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.myevents.MyEventsFlowDef;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class EventChangeMenuHandlerTest {

    @Inject
    EventChangeMenuHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext callbackCtx(String data) {
        IncomingUpdate u = new IncomingUpdate();
        u.setChatId(100L);
        u.setUserId(200L);
        u.setCallbackData(data);
        return UpdateContext.fromIncomingUpdate(u);
    }

    @SuppressWarnings("unchecked")
    private static List<String> buttonCallbackData(Object replyMarkup) {
        var rows = (List<?>) ((Map<String, Object>) replyMarkup).get("inline_keyboard");
        var data = new java.util.ArrayList<String>();
        for (Object row : rows) {
            for (Object button : (List<?>) row) {
                data.add((String) ((Map<String, String>) button).get("callback_data"));
            }
        }
        return data;
    }

    @Test
    void withRegId_showsCancelAndMessageButtons() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx(EventChangeFlowDef.CB_MENU_PREFIX + "reg-42"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(telegramClient).sendHtml(eq(100L), any(), captor.capture());
        assertThat(buttonCallbackData(captor.getValue())).containsExactly(
                MyEventsFlowDef.CB_CANCEL_PFX + "reg-42",
                EventChangeFlowDef.CB_PREFIX + "reg-42"
        );
    }

    @Test
    void withoutRecognizedPrefix_sendsErrorMessage() {
        Session s = session();

        StepResult result = handler.handle(callbackCtx("something_else"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(i18n).t(eq("en"), eq("event_change_menu_error"));
    }

    @Test
    void blankRegId_sendsErrorMessage() {
        Session s = session();

        handler.handle(callbackCtx(EventChangeFlowDef.CB_MENU_PREFIX), s);

        verify(i18n).t(eq("en"), eq("event_change_menu_error"));
    }
}
