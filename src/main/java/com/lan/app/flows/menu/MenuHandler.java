package com.lan.app.flows.menu;

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
public class MenuHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;

    @Inject
    public MenuHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.urlBtn(i18n.t(lang, "menu_btn_open"), "https://lanmenu.my.canva.site/lan-mobile-menu")
            ),
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "menu_btn_back"), "start")
            )
        ));

        telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "menu_text"), kb);

        return StepResult.stay(MenuFlowDef.FLOW, MenuFlowDef.STEP_SHOW);
    }
}
