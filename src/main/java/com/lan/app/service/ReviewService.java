package com.lan.app.service;

import com.lan.app.client.baserow.api.ReviewsApi;
import com.lan.app.client.baserow.model.CreateReviewRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReviewService {

    private static final Logger LOG = Logger.getLogger(ReviewService.class);

    @Inject
    @RestClient
    ReviewsApi reviewsApi;

    public boolean createReview(String authorName, int rating, String text) {
        return createReview(authorName, rating, text, null, null, null);
    }

    public boolean createReview(
        String authorName, int rating, String text,
        Integer eventRowId, Integer guestRowId, Integer registrationRowId
    ) {
        try {
            var req = new CreateReviewRequest()
                .authorName(authorName)
                .rating(rating)
                .text(text)
                .eventRowId(eventRowId)
                .guestRowId(guestRowId)
                .registrationRowId(registrationRowId);
            reviewsApi.createReview(req);
            return true;
        } catch (WebApplicationException e) {
            LOG.warnf("createReview failed: HTTP %d", e.getResponse().getStatus());
            return false;
        } catch (Exception e) {
            LOG.warnf(e, "createReview unexpected error");
            return false;
        }
    }
}
