package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingGuestTariffsApi;
import com.lan.app.client.baserow.api.CoworkingGuestsApi;
import com.lan.app.client.baserow.api.CoworkingTariffsApi;
import com.lan.app.client.baserow.model.CoworkingGuestTariffResponse;
import com.lan.app.client.baserow.model.CoworkingTariffResponse;
import com.lan.app.client.baserow.model.CreateCoworkingGuestTariffRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TariffService {

    private static final Logger LOG = Logger.getLogger(TariffService.class);

    @Inject
    @RestClient
    CoworkingGuestTariffsApi guestTariffsApi;

    @Inject
    @RestClient
    CoworkingGuestsApi guestsApi;

    @Inject
    @RestClient
    CoworkingTariffsApi tariffsApi;

    public List<CoworkingGuestTariffResponse> getGuestTariffs(UUID guestId) {
        try {
            return guestTariffsApi.listGuestTariffs(guestId);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch guest tariffs for guestId=%s", guestId);
            return Collections.emptyList();
        }
    }

    public List<CoworkingGuestTariffResponse> getGuestTariffHistory(UUID guestId) {
        try {
            return guestsApi.getGuestTariffHistory(guestId);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch tariff history for guestId=%s", guestId);
            return Collections.emptyList();
        }
    }

    public Optional<CoworkingTariffResponse> getTariff(UUID tariffId) {
        try {
            return Optional.of(tariffsApi.getCoworkingTariffById(tariffId));
        } catch (WebApplicationException e) {
            LOG.warnf("Tariff not found: %s, HTTP %d", tariffId, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch tariff %s", tariffId);
            return Optional.empty();
        }
    }

    public Optional<String> getTariffName(UUID tariffId) {
        return getTariff(tariffId).map(CoworkingTariffResponse::getName);
    }

    public List<CoworkingTariffResponse> listAllTariffs() {
        try {
            return tariffsApi.listCoworkingTariffs();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch tariff list");
            return Collections.emptyList();
        }
    }

    public Optional<CoworkingGuestTariffResponse> requestGuestTariff(UUID guestId, UUID tariffId) {
        try {
            var req = new CreateCoworkingGuestTariffRequest()
                .guestId(guestId)
                .tariffId(tariffId);
            return Optional.of(guestTariffsApi.createGuestTariff(req));
        } catch (WebApplicationException e) {
            LOG.warnf("requestGuestTariff failed for guest=%s tariff=%s: HTTP %d",
                guestId, tariffId, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "requestGuestTariff unexpected error for guest=%s tariff=%s", guestId, tariffId);
            return Optional.empty();
        }
    }

    public Optional<CoworkingGuestTariffResponse> deductDay(UUID guestTariffId) {
        try {
            return Optional.of(guestTariffsApi.deductGuestTariffDay(guestTariffId));
        } catch (WebApplicationException e) {
            LOG.warnf("deductDay failed for %s: HTTP %d", guestTariffId, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "deductDay unexpected error for %s", guestTariffId);
            return Optional.empty();
        }
    }
}
