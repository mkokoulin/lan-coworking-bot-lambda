package com.lan.app.flows.news;

import com.lan.app.client.baserow.api.CoworkingNewsApi;
import com.lan.app.client.baserow.model.CoworkingNewsResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepHandler;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class NewsHandler implements StepHandler {

    private static final Logger LOG = Logger.getLogger(NewsHandler.class);
    private static final int MAX_ITEMS = 10;
    private static final int MAX_BODY_CHARS = 300;

    private final TelegramClient telegramClient;
    private final I18n i18n;
    private final CoworkingNewsApi newsApi;

    @Inject
    public NewsHandler(
        TelegramClient telegramClient,
        I18n i18n,
        @RestClient CoworkingNewsApi newsApi
    ) {
        this.telegramClient = telegramClient;
        this.i18n = i18n;
        this.newsApi = newsApi;
    }

    @Override
    public StepResult handle(UpdateContext ctx, Session session) {
        String lang = session.getLang();

        List<CoworkingNewsResponse> news = fetchNews();

        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                KeyboardBuilder.cbCmd(i18n.t(lang, "news_btn_home"), "start")
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

    private List<CoworkingNewsResponse> fetchNews() {
        try {
            return newsApi.listCoworkingNews();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch news");
            return null;
        }
    }

    private String buildMessage(String lang, List<CoworkingNewsResponse> news) {
        boolean isRu = "ru".equals(lang);

        var sb = new StringBuilder();
        sb.append(i18n.t(lang, "news_title")).append("\n\n");

        int limit = Math.min(news.size(), MAX_ITEMS);
        for (int i = 0; i < limit; i++) {
            CoworkingNewsResponse item = news.get(i);
            String rawTitle = isRu ? item.getTitleRu() : item.getTitleEn();
            String title = escapeHtml(rawTitle != null ? rawTitle : "—");

            sb.append("<b>").append(title).append("</b>\n");

            String rawBody = isRu ? item.getBodyRu() : item.getBodyEn();
            if (rawBody != null && !rawBody.isBlank()) {
                String body = escapeHtml(rawBody.trim());
                if (body.length() > MAX_BODY_CHARS) {
                    body = body.substring(0, MAX_BODY_CHARS) + "…";
                }
                sb.append(body).append("\n");
            }

            if (item.getLink() != null && !item.getLink().isBlank()) {
                sb.append(escapeHtml(item.getLink())).append("\n");
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
