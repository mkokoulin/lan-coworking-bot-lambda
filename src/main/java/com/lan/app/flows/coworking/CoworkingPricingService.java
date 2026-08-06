package com.lan.app.flows.coworking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.flows.coworking.dto.CoworkingTariffDto;
import com.lan.app.i18n.I18n;
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

/** Fetches coworking tariffs from {baserowUrl}/coworking/v1/tariffs and renders the price list shown
 *  both inline on the coworking home screen and on the dedicated tariffs screen. */
@ApplicationScoped
public class CoworkingPricingService {

    private static final Logger LOG = Logger.getLogger(CoworkingPricingService.class);

    @ConfigProperty(name = "app.baserow-url", defaultValue = "")
    String baserowUrl = "";

    @ConfigProperty(name = "app.baserow-token", defaultValue = "")
    String baserowToken = "";

    @Inject I18n i18n;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /** For CDI. */
    public CoworkingPricingService() {}

    /** For manual wiring outside CDI (e.g. from another handler's unit test). */
    public CoworkingPricingService(I18n i18n) {
        this.i18n = i18n;
    }

    public String formatPricesBlock(String lang) {
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
