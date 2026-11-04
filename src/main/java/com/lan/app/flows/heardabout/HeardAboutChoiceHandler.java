package com.lan.app.flows.heardabout;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/** Handles the Instagram/Google/Friends/Other button tap sent by {@link HeardAboutScheduler}. */
@ApplicationScoped
public class HeardAboutChoiceHandler implements StepHandler {

    private static final Logger log = Logger.getLogger(HeardAboutChoiceHandler.class);

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public HeardAboutChoiceHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();
        String raw = ctx.command();

        String prefix;
        String source;
        if (raw != null && raw.startsWith(HeardAboutFlowDef.PREFIX_INSTAGRAM)) {
            prefix = HeardAboutFlowDef.PREFIX_INSTAGRAM;
            source = "Instagram";
        } else if (raw != null && raw.startsWith(HeardAboutFlowDef.PREFIX_GOOGLE)) {
            prefix = HeardAboutFlowDef.PREFIX_GOOGLE;
            source = "Google";
        } else if (raw != null && raw.startsWith(HeardAboutFlowDef.PREFIX_FRIENDS)) {
            prefix = HeardAboutFlowDef.PREFIX_FRIENDS;
            source = "Friends";
        } else if (raw != null && raw.startsWith(HeardAboutFlowDef.PREFIX_OTHER)) {
            prefix = HeardAboutFlowDef.PREFIX_OTHER;
            source = "Other";
        } else {
            return StepResult.finish();
        }

        String guestRowId = raw.substring(prefix.length());
        if (guestRowId.isBlank()) {
            log.warnf("Malformed heard-about-source callback payload: %s", raw);
            return StepResult.finish();
        }

        HeardAboutSession.setSource(session, source);
        HeardAboutSession.setGuestRowId(session, guestRowId);

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "heard_about_btn_skip"), HeardAboutFlowDef.CB_SKIP))
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "heard_about_ask_comment"), kb);
        return StepResult.stay(HeardAboutFlowDef.FLOW, HeardAboutFlowDef.STEP_COMMENT);
    }
}
