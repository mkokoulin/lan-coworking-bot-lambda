package com.lan.app.flows.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.flows.news.dto.CoworkingNewsDto;
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
public class NewsHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(NewsHandler.class);
    private static final int MAX_ITEMS = 10;
    private static final int MAX_BODY_CHARS = 300;

    @ConfigProperty(name = "app.baserow-url", defaultValue = "")
    String baserowUrl;

    @ConfigProperty(name = "app.baserow-token", defaultValue = "")
    String baserowToken;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public NewsHandler(TelegramClient telegramClient, I18n i18n) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        List<CoworkingNewsDto> news = fetchNews();

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "news_btn_home"), "/start")
            )
        ));

        if (news == null) {
            telegramClient.sendHtml(session.getChatId(), i18n.t(lang, "news_error"), kb);
            return StepResult.finish();
        }

        if (news.isEmpty()) {
            telegramClient.sendHtml(session.getChatId(),
                i18n.t(lang, "news_title") + "\n\n" + i18n.t(lang, "news_empty"), kb);
            return StepResult.finish();
        }

        String text = buildMessage(lang, news);
        telegramClient.sendHtml(session.getChatId(), text, kb);
        return StepResult.finish();
    }

    private List<CoworkingNewsDto> fetchNews() {
        if (baserowUrl.isBlank()) {
            LOG.warn("app.baserow-url not set, cannot fetch news");
            return null;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baserowUrl + "/coworking/v1/blog"))
                .header("Authorization", "Bearer " + baserowToken)
                .GET()
                .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                LOG.warnf("blog endpoint returned %d", resp.statusCode());
                return null;
            }
            CoworkingNewsDto[] arr = mapper.readValue(resp.body(), CoworkingNewsDto[].class);
            return List.of(arr);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch news");
            return null;
        }
    }

    private String buildMessage(String lang, List<CoworkingNewsDto> news) {
        boolean isRu = "ru".equals(lang);

        var sb = new StringBuilder();
        sb.append(i18n.t(lang, "news_title")).append("\n\n");

        int limit = Math.min(news.size(), MAX_ITEMS);
        for (int i = 0; i < limit; i++) {
            CoworkingNewsDto item = news.get(i);
            String rawTitle = isRu ? item.titleRu : item.titleEn;
            String title = escapeHtml(rawTitle != null ? rawTitle : "—");

            sb.append("<b>").append(title).append("</b>\n");

            String rawBody = isRu ? item.bodyRu : item.bodyEn;
            if (rawBody != null && !rawBody.isBlank()) {
                String body = escapeHtml(rawBody.trim());
                if (body.length() > MAX_BODY_CHARS) {
                    body = body.substring(0, MAX_BODY_CHARS) + "…";
                }
                sb.append(body).append("\n");
            }

            if (item.link != null && !item.link.isBlank()) {
                sb.append(escapeHtml(item.link)).append("\n");
            }

            if (i < limit - 1) {
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
