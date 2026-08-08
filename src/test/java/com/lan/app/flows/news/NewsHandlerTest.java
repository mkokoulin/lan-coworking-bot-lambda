package com.lan.app.flows.news;

import com.lan.app.client.baserow.api.CoworkingNewsApi;
import com.lan.app.client.baserow.model.CoworkingNewsResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class NewsHandlerTest {

    @Inject
    NewsHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    @RestClient
    CoworkingNewsApi newsApi;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext ctx() {
        return new UpdateContext(100L, "private", 200L, null, "/news", null, null, false, "bob", null, null, null);
    }

    @Test
    void apiThrows_sendsErrorAndFinishes() {
        when(newsApi.listCoworkingNews()).thenThrow(new RuntimeException("boom"));
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(100L), anyString(), any());
    }

    @Test
    void emptyNewsList_sendsEmptyMessageAndFinishes() {
        when(newsApi.listCoworkingNews()).thenReturn(Collections.emptyList());
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(telegramClient).sendHtml(eq(100L), anyString(), any());
    }

    @Test
    void nonEmptyNewsList_buildsMessageWithTitleBodyAndLinkAndFinishes() {
        // Session.newDefault() defaults lang to "en", so the handler renders the *En fields.
        CoworkingNewsResponse item = new CoworkingNewsResponse();
        item.setTitleEn("Big News");
        item.setTitleRu("Большая новость");
        item.setBodyEn("Something happened");
        item.setBodyRu("Что-то случилось");
        item.setLink("https://example.com/news");
        when(newsApi.listCoworkingNews()).thenReturn(List.of(item));
        Session s = session();

        StepResult result = handler.handle(ctx(), s);

        assertThat(result).isEqualTo(StepResult.finish());
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue())
                .contains("Big News")
                .contains("Something happened")
                .contains("https://example.com/news");
    }

    @Test
    void nonEmptyNewsList_escapesHtmlInTitleAndBody() {
        CoworkingNewsResponse item = new CoworkingNewsResponse();
        item.setTitleEn("title");
        item.setTitleRu("<script>alert(1)</script>");
        item.setBodyEn("body");
        item.setBodyRu("a & b < c");
        when(newsApi.listCoworkingNews()).thenReturn(List.of(item));
        Session s = session();

        handler.handle(ctx(), s);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendHtml(eq(100L), captor.capture(), any());
        assertThat(captor.getValue())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;")
                .contains("a &amp; b &lt; c");
    }
}
