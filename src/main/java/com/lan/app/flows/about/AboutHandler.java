package com.lan.app.flows.about;

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

@ApplicationScoped
public class AboutHandler implements StepHandler {

    private static final String PHOTO_PATH = "internal/assets/coworking_scheme.jpg";

    @Inject TelegramClient telegramClient;
    @Inject I18n i18n;

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        // 1) Фото карты (если файл есть — отправляем, ошибку не бросаем)
        telegramClient.sendPhoto(session.getChatId(), PHOTO_PATH, "");

        // 2) Текст с описанием и правилами
        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "about_btn_home"), "/start"))
        ));
        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "about_text"), kb);

        return StepResult.stay(AboutFlowDef.FLOW, AboutFlowDef.STEP_SEND);
    }
}
