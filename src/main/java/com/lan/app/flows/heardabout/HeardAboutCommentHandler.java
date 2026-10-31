package com.lan.app.flows.heardabout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.heardabout.dto.HeardAboutSourceAnswerDto;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@ApplicationScoped
public class HeardAboutCommentHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(HeardAboutCommentHandler.class);

    @ConfigProperty(name = "app.backend-url", defaultValue = "")
    String backendUrl;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public HeardAboutCommentHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        boolean skip = ctx.hasCallback() && HeardAboutFlowDef.CB_SKIP.equals(ctx.callbackPayload());
        String text = skip ? null : ctx.messageText();

        if (!skip && (text == null || text.isBlank())) {
            var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "heard_about_btn_skip"), HeardAboutFlowDef.CB_SKIP))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "heard_about_ask_comment"), kb);
            return StepResult.stay(HeardAboutFlowDef.FLOW, HeardAboutFlowDef.STEP_COMMENT);
        }

        String source = HeardAboutSession.getSource(session);
        String guestRowId = HeardAboutSession.getGuestRowId(session);
        saveAnswer(guestRowId, source, text);

        HeardAboutSession.clear(session);
        session.setFlow("");
        session.setStep("");

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "heard_about_thanks"), null);
        return StepResult.finish();
    }

    private void saveAnswer(String guestRowId, String source, String comment) {
        if (backendUrl.isBlank() || guestRowId == null) return;
        try {
            var dto = new HeardAboutSourceAnswerDto(source, comment);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/events/v1/bot/heard-about-source/" + guestRowId + "/answer"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(dto)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warnf("heard-about-source answer endpoint returned %d for guestRowId=%s", resp.statusCode(), guestRowId);
            }
        } catch (Exception e) {
            log.warnf("Failed to save heard-about-source answer for guestRowId=%s: %s", guestRowId, e.getMessage());
        }
    }
}
