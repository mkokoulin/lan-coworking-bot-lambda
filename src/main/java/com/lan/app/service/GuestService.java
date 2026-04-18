package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingGuestsApi;
import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.client.baserow.model.CreateCoworkingGuestRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@ApplicationScoped
public class GuestService {

    @Inject
    @RestClient
    CoworkingGuestsApi guestsApi;

    public CoworkingGuestResponse getGuest(UUID id) {
        return guestsApi.getCoworkingGuestById(id);
    }

    public CoworkingGuestResponse createGuest(String firstName, String lastName,
                                              String telegram, String phone) {
        var request = new CreateCoworkingGuestRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setTelegram(telegram);
        request.setPhone(phone);
        return guestsApi.createCoworkingGuest(request);
    }
}