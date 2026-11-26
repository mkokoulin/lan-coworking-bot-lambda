package com.lan.app.service;

import com.lan.app.client.baserow.api.BirthdayWishesApi;
import com.lan.app.client.baserow.model.CreateBirthdayWishRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BirthdayWishService {

    private static final Logger LOG = Logger.getLogger(BirthdayWishService.class);

    @Inject
    @RestClient
    BirthdayWishesApi birthdayWishesApi;

    public boolean createWish(String authorName, String telegramUserId, String text) {
        try {
            var req = new CreateBirthdayWishRequest()
                .authorName(authorName)
                .telegramUserId(telegramUserId)
                .text(text);
            birthdayWishesApi.createBirthdayWish(req);
            return true;
        } catch (WebApplicationException e) {
            LOG.warnf("createBirthdayWish failed: HTTP %d", e.getResponse().getStatus());
            return false;
        } catch (Exception e) {
            LOG.warnf(e, "createBirthdayWish unexpected error");
            return false;
        }
    }
}
