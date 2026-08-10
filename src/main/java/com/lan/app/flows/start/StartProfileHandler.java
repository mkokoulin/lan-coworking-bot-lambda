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

import java.util.List;
import java.util.UUID;

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

        // Load all tariffs for this guest; split client-side by status
        List<CoworkingGuestTariffResponse> allTariffs = g.getId() != null
            ? tariffService.getGuestTariffs(g.getId())
            : List.of();

        List<CoworkingGuestTariffResponse> activeTariffs = allTariffs.stream()
            .filter(t -> t.getStatus() == CoworkingGuestTariffResponse.StatusEnum.ACTIVE)
            .toList();
        List<CoworkingGuestTariffResponse> pendingTariffs = allTariffs.stream()
            .filter(t -> t.getStatus() == CoworkingGuestTariffResponse.StatusEnum.PENDING)
            .toList();
        List<CoworkingGuestTariffResponse> historyTariffs = allTariffs.stream()
            .filter(t -> {
                var s = t.getStatus();
                return s == CoworkingGuestTariffResponse.StatusEnum.EXPIRED
                    || s == CoworkingGuestTariffResponse.StatusEnum.CANCELLED
                    || s == CoworkingGuestTariffResponse.StatusEnum.SUSPENDED;
            })
            .toList();

        String tariffSection = buildTariffSection(lang, activeTariffs, pendingTariffs, historyTariffs);

        boolean canDeduct = false;
        if (!activeTariffs.isEmpty()) {
            // Find the first ACTIVE LONG tariff for deduction.
            // A user may have both SHORT (day-pass) and LONG (monthly) tariffs active;
            // SHORT tariffs don't support day deduction, so we must skip them.
            for (var candidate : activeTariffs) {
                if (candidate.getTariffId() == null) {
                    // No tariff definition — treat as deductible (safe default)
                    RegistrationSession.setDeductTariffId(session, candidate.getId().toString());
                    RegistrationSession.setDeductTariffName(session, "—");
                    canDeduct = true;
                    break;
                }
                var tariff = tariffService.getTariff(candidate.getTariffId());
                boolean isShort = tariff
                    .map(t -> t.getType() == CoworkingTariffResponse.TypeEnum.SHORT)
                    .orElse(false);
                if (!isShort) {
                    String tName = tariff.map(t -> t.getName() != null ? t.getName() : "—").orElse("—");
                    RegistrationSession.setDeductTariffId(session, candidate.getId().toString());
                    RegistrationSession.setDeductTariffName(session, tName);
                    canDeduct = true;
                    break;
                }
            }
        }

        String text = i18n.t(lang, "profile_info").formatted(name, telegram, phone)
            + tariffSection;

        var rows = new java.util.ArrayList<List<java.util.Map<String, String>>>();
        if (canDeduct) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_deduct"), "deduct_confirm")
            ));
        } else if (activeTariffs.isEmpty() && pendingTariffs.isEmpty()) {
            rows.add(KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_tariff"), "/tariff_list")
            ));
        }
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_events"), "myevents")
        ));
        rows.add(KeyboardBuilder.row(
            KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_back"), "start"),
            KeyboardBuilder.cbCmd(i18n.t(lang, "profile_btn_logout"), "logout")
        ));

        var kb = KeyboardBuilder.inline(rows);
        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(StartFlowDef.FLOW, StartFlowDef.STEP_PROFILE);
    }

    private String statusLabel(String lang, CoworkingGuestTariffResponse.StatusEnum status) {
        if (status == null) return "—";
        return switch (status) {
            case ACTIVE    -> i18n.t(lang, "tariff_status_active");
            case PENDING   -> i18n.t(lang, "tariff_status_pending");
            case EXPIRED   -> i18n.t(lang, "tariff_status_expired");
            case CANCELLED -> i18n.t(lang, "tariff_status_cancelled");
            case SUSPENDED -> i18n.t(lang, "tariff_status_suspended");
        };
    }

    private String buildTariffSection(
        String lang,
        List<CoworkingGuestTariffResponse> activeTariffs,
        List<CoworkingGuestTariffResponse> pendingTariffs,
        List<CoworkingGuestTariffResponse> historyTariffs
    ) {
        var sb = new StringBuilder();

        if (activeTariffs.isEmpty() && pendingTariffs.isEmpty()) {
            sb.append("\n\n").append(i18n.t(lang, "profile_no_tariff"));
        }

        // Active tariffs
        for (CoworkingGuestTariffResponse t : activeTariffs) {
            String tariffName = t.getTariffId() != null
                ? tariffService.getTariffName(t.getTariffId()).orElse("—")
                : "—";
            int daysUsed = t.getDaysUsed() != null ? t.getDaysUsed() : 0;
            sb.append("\n\n").append(
                i18n.t(lang, "profile_tariff_section")
                    .formatted(tariffName, statusLabel(lang, t.getStatus()), daysUsed)
            );
        }

        // Pending tariffs (awaiting activation by admin)
        for (CoworkingGuestTariffResponse t : pendingTariffs) {
            String tariffName = t.getTariffId() != null
                ? tariffService.getTariffName(t.getTariffId()).orElse("—")
                : "—";
            sb.append("\n\n").append(
                i18n.t(lang, "profile_tariff_pending_section").formatted(tariffName)
            );
        }

        // History: expired / cancelled / suspended
        if (!historyTariffs.isEmpty()) {
            sb.append(i18n.t(lang, "profile_tariff_history_header"));
            for (CoworkingGuestTariffResponse t : historyTariffs) {
                String tariffName = t.getTariffId() != null
                    ? tariffService.getTariffName(t.getTariffId()).orElse("—")
                    : "—";
                sb.append("\n").append(
                    i18n.t(lang, "profile_tariff_expired_item")
                        .formatted(tariffName, statusLabel(lang, t.getStatus()))
                );
            }
        }

        return sb.toString();
    }
}
