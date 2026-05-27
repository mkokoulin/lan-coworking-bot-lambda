package com.lan.app.flows.start;

import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.TariffService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class StartDeductDoHandler implements StepHandler {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Asia/Yerevan"));

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final TariffService tariffService;

    @Inject
    public StartDeductDoHandler(TelegramClient telegramClient, I18n i18n, TariffService tariffService) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.tariffService = tariffService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        String tariffIdStr = RegistrationSession.getDeductTariffId(session);
        if (tariffIdStr == null) {
            var noTariffKb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start"))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "deduct_no_tariff"), noTariffKb);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            return StepResult.finish();
        }

        UUID tariffId = UUID.fromString(tariffIdStr);
        var result = tariffService.deductDay(tariffId);

        if (result.isEmpty()) {
            var errorKb = KeyboardBuilder.inline(List.of(
                KeyboardBuilder.row(KeyboardBuilder.cbCmd(i18n.t(lang, "start_btn_profile"), "profile"))
            ));
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "deduct_error"), errorKb);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_PROFILE);
            return StepResult.finish();
        }

        var updated = result.get();
        int daysUsed = updated.getDaysUsed() != null ? updated.getDaysUsed() : 0;
        Instant dateEnd = updated.getDateEnd() != null ? updated.getDateEnd().toInstant() : null;
        long daysLeft = dateEnd != null
            ? Math.max(0, ChronoUnit.DAYS.between(Instant.now(), dateEnd))
            : 0;
        String dateEndStr = dateEnd != null ? DATE_FMT.format(dateEnd) : "—";

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "profile")
            )
        ));

        telegramClient.sendHtml(session.getChatId(),
            i18n.t(lang, "deduct_success").formatted(daysUsed, daysLeft, dateEndStr), kb);

        session.setFlow(StartFlowDef.FLOW);
        session.setStep(StartFlowDef.STEP_PROFILE);
        return StepResult.finish();
    }
}
