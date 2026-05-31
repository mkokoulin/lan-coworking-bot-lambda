package com.lan.app.flows.coworking;

import com.lan.app.client.baserow.model.CoworkingTariffResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.TariffService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class CoworkingHomeHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final FlowRegistry registry;
    private final TariffService tariffService;

    @Inject
    public CoworkingHomeHandler(
        TelegramClient telegramClient,
        I18n i18n,
        FlowRegistry registry,
        TariffService tariffService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.registry = registry;
        this.tariffService = tariffService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback() && ctx.callbackData().startsWith("/")) {
            String command = ctx.callbackData().substring(1);
            FlowEntry entry = registry.getCommand(command).orElse(null);
            if (entry != null) {
                session.setFlow(entry.flow());
                session.setStep(entry.step());
                return new StepResult(entry.flow(), entry.step());
            }
        }

        String pricesSection = buildPricesSection(lang);

        String text = i18n.t(lang, "coworking_intro") + "\n\n"
                + pricesSection + "\n\n"
                + i18n.t(lang, "coworking_meeting") + "\n\n"
                + i18n.t(lang, "coworking_options");

        var kb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_booking"), "booking"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_meetingroom"), "meetingroom")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_events"), "events")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_about"), "about"),
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_language"), "language")
                ),
                KeyboardBuilder.row(
                        KeyboardBuilder.cbCmd(i18n.t(lang, "coworking_btn_home"), "/start")
                )
        ));

        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(CoworkingFlowDef.FLOW, CoworkingFlowDef.STEP_HOME);
    }

    private String buildPricesSection(String lang) {
        List<CoworkingTariffResponse> tariffs = tariffService.listAllTariffs();
        if (tariffs.isEmpty()) {
            return i18n.t(lang, "coworking_prices");
        }

        StringBuilder sb = new StringBuilder(i18n.t(lang, "coworking_prices_header"));
        for (CoworkingTariffResponse t : tariffs) {
            sb.append("\n• ").append(t.getName())
              .append(" — ").append(formatPrice(t.getPrice())).append("֏");
        }
        return sb.toString();
    }

    private static String formatPrice(int price) {
        String s = String.valueOf(price);
        if (s.length() <= 3) return s;
        return s.substring(0, s.length() - 3) + " " + s.substring(s.length() - 3);
    }
}
