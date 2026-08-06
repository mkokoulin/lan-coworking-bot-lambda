package com.lan.app.flows.coworking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.FlowEntry;
import com.lan.app.engine.FlowRegistry;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.coworking.dto.CoworkingTariffDto;
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
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class CoworkingHomeHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(CoworkingHomeHandler.class);

    @ConfigProperty(name = "app.baserow-url", defaultValue = "")
    String baserowUrl;

    @ConfigProperty(name = "app.baserow-token", defaultValue = "")
    String baserowToken;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final FlowRegistry registry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public CoworkingHomeHandler(
        TelegramClient telegramClient,
        I18n i18n,
        FlowRegistry registry
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.registry = registry;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        if (ctx.hasCallback() && ctx.callbackData().startsWith("/")) {
            String command = ctx.callbackData().substring(1); // "booking", "meetingroom", ...
            FlowEntry entry = registry.getCommand(command).orElse(null);
            if (entry != null) {
                session.setFlow(entry.flow());
                session.setStep(entry.step());
                return new StepResult(entry.flow(), entry.step());
            }
        }

        String text = i18n.t(lang, "coworking_intro") + "\n\n"
                + buildPricesBlock(lang) + "\n\n"
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
                )
        ));

        telegramClient.sendHtml(session.getChatId(), text, kb);

        return StepResult.stay(CoworkingFlowDef.FLOW, CoworkingFlowDef.STEP_HOME);
    }

    private String buildPricesBlock(String lang) {
        List<CoworkingTariffDto> tariffs = fetchTariffs();
        if (tariffs == null || tariffs.isEmpty()) {
            return i18n.t(lang, "coworking_prices");
        }

        boolean isRu = "ru".equalsIgnoreCase(lang);
        Locale locale = isRu ? Locale.forLanguageTag("ru") : Locale.ENGLISH;
        NumberFormat priceFormat = NumberFormat.getIntegerInstance(locale);

        var sb = new StringBuilder();
        sb.append(isRu ? "💳 Тарифы коворкинга:" : "💳 Coworking prices:");

        for (CoworkingTariffDto tariff : tariffs) {
            sb.append("\n• ").append(escapeHtml(tariff.name)).append(" — ");

            boolean hasDiscount = tariff.discount != null && tariff.discount > 0 && tariff.price != null;
            if (hasDiscount) {
                int finalPrice = tariff.price - tariff.discount;
                sb.append("<s>").append(priceFormat.format(tariff.price)).append("֏</s> ")
                    .append(priceFormat.format(finalPrice)).append("֏");

                String description = isRu ? tariff.discountDescriptionRu : tariff.discountDescriptionEn;
                if (description != null && !description.isBlank()) {
                    sb.append(" <i>(").append(escapeHtml(description.trim())).append(")</i>");
                }
            } else if (tariff.price != null) {
                sb.append(priceFormat.format(tariff.price)).append("֏");
            }
        }

        return sb.toString();
    }

    private List<CoworkingTariffDto> fetchTariffs() {
        if (baserowUrl.isBlank()) {
            LOG.warn("app.baserow-url not set, cannot fetch tariffs");
            return null;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baserowUrl + "/coworking/v1/tariffs"))
                .header("Authorization", "Bearer " + baserowToken)
                .GET()
                .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOG.warnf("tariffs endpoint returned %d", resp.statusCode());
                return null;
            }
            CoworkingTariffDto[] arr = mapper.readValue(resp.body(), CoworkingTariffDto[].class);
            return List.of(arr);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch tariffs");
            return null;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}