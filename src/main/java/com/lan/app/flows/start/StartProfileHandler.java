package com.lan.app.flows.start;

import com.lan.app.client.baserow.model.CoworkingGuestTariffResponse;
import com.lan.app.client.baserow.model.CoworkingTariffResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.registration.RegistrationSession;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
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

@ApplicationScoped
public class StartProfileHandler implements StepHandler {

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final GuestService guestService;
    private final TariffService tariffService;

    @Inject
    public StartProfileHandler(
        TelegramClient telegramClient,
        I18n i18n,
        GuestService guestService,
        TariffService tariffService
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.guestService = guestService;
        this.tariffService = tariffService;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        var guest = guestService.findByChatId(session.getChatId());
        if (guest.isEmpty()) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "profile_not_found"), null);
            session.setFlow(StartFlowDef.FLOW);
            session.setStep(StartFlowDef.STEP_SHOW);
            return StepResult.finish();
        }

        var g = guest.get();
        String name = g.getFirstName() + " " + g.getLastName();
        String telegram = (g.getTelegram() != null && !g.getTelegram().isBlank())
            ? "@" + g.getTelegram()
            : "—";
        String phone = (g.getPhone() != null && !g.getPhone().isBlank()) ? g.getPhone() : "—";

        List<CoworkingGuestTariffResponse> tariffs = g.getId() != null
            ? tariffService.getGuestTariffs(g.getId())
            : List.of();

        String tariffSection = buildTariffSection(lang, tariffs);

        boolean canDeduct = false;
        if (!tariffs.isEmpty()) {
            var first = tariffs.getFirst();
            RegistrationSession.setDeductTariffId(session, first.getId().toString());
            if (first.getTariffId() != null) {
                var tariff = tariffService.getTariff(first.getTariffId());
                String tName = tariff.map(t -> t.getName() != null ? t.getName() : "—").orElse("—");
                RegistrationSession.setDeductTariffName(session, tName);
                boolean isShort = tariff
                    .map(t -> t.getType() == CoworkingTariffResponse.TypeEnum.SHORT)
                    .orElse(false);
                canDeduct = !isShort;
            } else {
                canDeduct = true;
            }
        }

        String text = i18n.t(lang, "profile_info").formatted(name, telegram, phone)
            + tariffSection;

        var rows = new java.util.ArrayList<List<java.util.Map<String, String>>>();
        if (canDeduct) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_deduct"), "deduct_confirm")
            ));
        } else if (tariffs.isEmpty()) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_tariff"), "tariff_list")
            ));
        }
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start"),
            KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_logout"), "logout")
        ));

        var kb = KeyboardBuilder.inline(rows);
        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_PROFILE);
    }

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Asia/Yerevan"));

    private String buildTariffSection(String lang, List<CoworkingGuestTariffResponse> tariffs) {
        if (tariffs.isEmpty()) {
            return "\n\n" + i18n.t(lang, "profile_no_tariff");
        }

        var sb = new StringBuilder();
        for (CoworkingGuestTariffResponse t : tariffs) {
            String tariffName = t.getTariffId() != null
                ? tariffService.getTariffName(t.getTariffId()).orElse("—")
                : "—";
            Instant dateEnd = t.getDateEnd() != null ? t.getDateEnd().toInstant() : null;
            long daysLeft = dateEnd != null
                ? Math.max(0, ChronoUnit.DAYS.between(Instant.now(), dateEnd))
                : 0;
            int daysUsed = t.getDaysUsed() != null ? t.getDaysUsed() : 0;
            String dateEndStr = dateEnd != null ? DATE_FMT.format(dateEnd) : "—";

            sb.append("\n\n").append(
                i18n.t(lang, "profile_tariff_section").formatted(tariffName, dateEndStr, daysLeft, daysUsed)
            );
        }
        return sb.toString();
    }
}
