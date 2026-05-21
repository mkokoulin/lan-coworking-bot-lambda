package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingGuestTariffsApi;
import com.lan.app.client.baserow.api.CoworkingTariffsApi;
import com.lan.app.client.baserow.model.CoworkingGuestTariffResponse;
import com.lan.app.client.baserow.model.CoworkingTariffResponse;
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
    CoworkingTariffsApi tariffsApi;

    public List<CoworkingGuestTariffResponse> getGuestTariffs(UUID guestId) {
        try {
            return guestTariffsApi.listGuestTariffs(guestId);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch guest tariffs for guestId=%s", guestId);
            return Collections.emptyList();
        }
    }

    public Optional<String> getTariffName(UUID tariffId) {
        try {
            CoworkingTariffResponse tariff = tariffsApi.getCoworkingTariffById(tariffId);
            return Optional.ofNullable(tariff.getName());
        } catch (WebApplicationException e) {
            LOG.warnf("Tariff not found: %s, HTTP %d", tariffId, e.getResponse().getStatus());
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to fetch tariff %s", tariffId);
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
