package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingGuestsApi;
import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.client.baserow.model.CreateCoworkingGuestRequest;
import com.lan.app.client.baserow.model.LinkCoworkingGuestChatByIdRequest;
import com.lan.app.client.baserow.model.LinkCoworkingGuestChatRequest;
import com.lan.app.client.baserow.model.UnlinkCoworkingGuestChatRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GuestService {

    private static final Logger LOG = Logger.getLogger(GuestService.class);

    @Inject
    @RestClient
    CoworkingGuestsApi guestsApi;

    public CoworkingGuestResponse getGuest(UUID id) {
        return guestsApi.getCoworkingGuestById(id);
    }

    public Optional<CoworkingGuestResponse> findByChatId(Long chatId) {
        try {
            return Optional.ofNullable(guestsApi.getCoworkingGuestByChatId(chatId));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            LOG.warnf("Failed to find guest by chatId=%d: HTTP %d", chatId, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "Unexpected error finding guest by chatId=%d", chatId);
            return Optional.empty();
        }
    }

    public Optional<CoworkingGuestResponse> linkChatById(UUID guestId, Long chatId) {
        try {
            var req = new LinkCoworkingGuestChatByIdRequest();
            req.setChatId(chatId);
            return Optional.ofNullable(guestsApi.linkCoworkingGuestChatById(guestId, req));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) return Optional.empty();
            LOG.warnf("linkChatById failed guestId=%s: HTTP %d", guestId, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "Unexpected error in linkChatById guestId=%s", guestId);
            return Optional.empty();
        }
    }

    public Optional<CoworkingGuestResponse> linkChat(String phone, Long chatId) {
        try {
            var req = new LinkCoworkingGuestChatRequest();
            req.setPhone(phone);
            req.setChatId(chatId);
            return Optional.ofNullable(guestsApi.linkCoworkingGuestChat(req));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) return Optional.empty();
            LOG.warnf("linkChat failed phone=%s: HTTP %d", phone, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "Unexpected error in linkChat phone=%s", phone);
            return Optional.empty();
        }
    }

    public void confirmLink(UUID guestId) {
        try {
            guestsApi.confirmCoworkingGuestLink(guestId);
        } catch (WebApplicationException e) {
            LOG.warnf("confirmLink failed guestId=%s: HTTP %d", guestId, e.getResponse().getStatus());
        } catch (Exception e) {
            LOG.warnf(e, "Unexpected error in confirmLink guestId=%s", guestId);
        }
    }

    public void unlinkChat(Long chatId) {
        try {
            var req = new UnlinkCoworkingGuestChatRequest();
            req.setChatId(chatId);
            guestsApi.unlinkCoworkingGuestChat(req);
        } catch (Exception e) {
            LOG.warnf(e, "Unexpected error in unlinkChat chatId=%d", chatId);
        }
    }

    public CoworkingGuestResponse createGuest(
        String firstName, String lastName,
        String telegram,
        String phone,
        Long chatId
    ) {
        var request = new CreateCoworkingGuestRequest();

        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setTelegram(telegram);
        request.setPhone(phone);
        request.setTelegramChatId(chatId != null ? String.valueOf(chatId) : null);

        return guestsApi.createCoworkingGuest(request);
    }
}