package com.lan.app.flows.review;

import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.domain.UpdateContext;
import com.lan.app.engine.StepResult;
import com.lan.app.i18n.I18n;
import com.lan.app.service.GuestService;
import com.lan.app.service.ReviewService;
import com.lan.app.session.Session;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ReviewTextHandlerTest {

    @Inject
    ReviewTextHandler handler;

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @InjectMock
    ReviewService reviewService;

    @InjectMock
    GuestService guestService;

    @BeforeEach
    void setup() {
        lenient().when(i18n.t(any(), any())).thenReturn("translated");
        lenient().when(guestService.findByChatId(anyLong())).thenReturn(Optional.empty());
        lenient().when(reviewService.createReview(anyString(), anyInt(), any())).thenReturn(true);
    }

    private static Session session() {
        return Session.newDefault(100L, 200L);
    }

    private static UpdateContext textCtx(String text) {
        return new UpdateContext(100L, "private", 200L, null, text, null, null, false, "bob", null, null, null);
    }

    private static UpdateContext callbackCtx(String data) {
        return new UpdateContext(100L, "private", 200L, 55, null, data, "q1", true, "bob", null, null, null);
    }

    @Test
    void blankText_repromptsAndStaysOnText() {
        Session s = session();
        ReviewSession.setRating(s, "5");

        StepResult result = handler.handle(textCtx("   "), s);

        assertThat(result).isEqualTo(StepResult.stay(ReviewFlowDef.FLOW, ReviewFlowDef.STEP_TEXT));
        verify(reviewService, never()).createReview(any(), anyInt(), any());
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void validText_createsReviewAndFinishesWithSuccessMessage() {
        Session s = session();
        ReviewSession.setRating(s, "4");

        StepResult result = handler.handle(textCtx("Great place!"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        assertThat(ReviewSession.getRating(s)).isNull();
        verify(reviewService).createReview(eq("tg_200"), eq(4), eq("Great place!"));
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }

    @Test
    void skipCallback_createsReviewWithoutTextUsingDefaultRatingWhenMissing() {
        Session s = session(); // no rating stored

        StepResult result = handler.handle(callbackCtx("review_skip"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        verify(reviewService).createReview(eq("tg_200"), eq(5), eq(null));
    }

    @Test
    void guestFound_usesGuestNameAsAuthor() {
        Session s = session();
        ReviewSession.setRating(s, "3");
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getFirstName()).thenReturn("Ann");
        when(guest.getLastName()).thenReturn("Smith");
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));

        handler.handle(textCtx("Nice!"), s);

        verify(reviewService).createReview(eq("Ann Smith"), eq(3), eq("Nice!"));
    }

    @Test
    void reviewServiceFails_stillClearsSessionAndFinishesWithErrorMessage() {
        Session s = session();
        ReviewSession.setRating(s, "5");
        when(reviewService.createReview(any(), anyInt(), any())).thenReturn(false);

        StepResult result = handler.handle(textCtx("Nice!"), s);

        assertThat(result).isEqualTo(StepResult.finish());
        assertThat(s.getFlow()).isEmpty();
        assertThat(s.getStep()).isEmpty();
        verify(telegramClient).sendHtml(eq(100L), any(), any());
    }
}
