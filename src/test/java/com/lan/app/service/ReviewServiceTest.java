package com.lan.app.service;

import com.lan.app.client.baserow.api.ReviewsApi;
import com.lan.app.client.baserow.model.CreateReviewRequest;
import com.lan.app.client.baserow.model.ReviewResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ReviewServiceTest {

    @Inject
    ReviewService reviewService;

    @InjectMock
    @RestClient
    ReviewsApi reviewsApi;

    private static WebApplicationException httpError(int status) {
        return new WebApplicationException(Response.status(status).build());
    }

    @Test
    void createReview_success_returnsTrueAndSendsCorrectRequest() {
        when(reviewsApi.createReview(any())).thenReturn(mock(ReviewResponse.class));

        boolean result = reviewService.createReview("Ann", 5, "Great place");

        assertThat(result).isTrue();
        var captor = org.mockito.ArgumentCaptor.forClass(CreateReviewRequest.class);
        verify(reviewsApi).createReview(captor.capture());
        assertThat(captor.getValue().getAuthorName()).isEqualTo("Ann");
        assertThat(captor.getValue().getRating()).isEqualTo(5);
        assertThat(captor.getValue().getText()).isEqualTo("Great place");
    }

    @Test
    void createReview_httpError_returnsFalse() {
        when(reviewsApi.createReview(any())).thenThrow(httpError(500));

        assertThat(reviewService.createReview("Ann", 5, "Great place")).isFalse();
    }

    @Test
    void createReview_unexpectedException_returnsFalse() {
        when(reviewsApi.createReview(any())).thenThrow(new RuntimeException("boom"));

        assertThat(reviewService.createReview("Ann", 5, "Great place")).isFalse();
    }
}
