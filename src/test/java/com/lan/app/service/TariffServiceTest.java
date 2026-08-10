package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingGuestTariffsApi;
import com.lan.app.client.baserow.api.CoworkingGuestsApi;
import com.lan.app.client.baserow.api.CoworkingTariffsApi;
import com.lan.app.client.baserow.model.CoworkingGuestTariffResponse;
import com.lan.app.client.baserow.model.CoworkingTariffResponse;
import com.lan.app.client.baserow.model.CreateCoworkingGuestTariffRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TariffServiceTest {

    @Inject
    TariffService tariffService;

    @InjectMock
    @RestClient
    CoworkingGuestTariffsApi guestTariffsApi;

    @InjectMock
    @RestClient
    CoworkingGuestsApi guestsApi;

    @InjectMock
    @RestClient
    CoworkingTariffsApi tariffsApi;

    private static WebApplicationException httpError(int status) {
        return new WebApplicationException(Response.status(status).build());
    }

    // ===== getGuestTariffs =====

    @Test
    void getGuestTariffs_success_returnsList() {
        UUID guestId = UUID.randomUUID();
        List<CoworkingGuestTariffResponse> list = List.of(mock(CoworkingGuestTariffResponse.class));
        when(guestTariffsApi.listGuestTariffs(guestId)).thenReturn(list);

        assertThat(tariffService.getGuestTariffs(guestId)).isEqualTo(list);
    }

    @Test
    void getGuestTariffs_exception_returnsEmptyList() {
        UUID guestId = UUID.randomUUID();
        when(guestTariffsApi.listGuestTariffs(guestId)).thenThrow(new RuntimeException("boom"));

        assertThat(tariffService.getGuestTariffs(guestId)).isEmpty();
    }

    // ===== getGuestTariffHistory =====

    @Test
    void getGuestTariffHistory_success_returnsList() {
        UUID guestId = UUID.randomUUID();
        List<CoworkingGuestTariffResponse> list = List.of(mock(CoworkingGuestTariffResponse.class));
        when(guestsApi.getGuestTariffHistory(guestId)).thenReturn(list);

        assertThat(tariffService.getGuestTariffHistory(guestId)).isEqualTo(list);
    }

    @Test
    void getGuestTariffHistory_exception_returnsEmptyList() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.getGuestTariffHistory(guestId)).thenThrow(new RuntimeException("boom"));

        assertThat(tariffService.getGuestTariffHistory(guestId)).isEmpty();
    }

    // ===== getTariff / getTariffName =====

    @Test
    void getTariff_success_returnsTariff() {
        UUID tariffId = UUID.randomUUID();
        CoworkingTariffResponse tariff = mock(CoworkingTariffResponse.class);
        when(tariffsApi.getCoworkingTariffById(tariffId)).thenReturn(tariff);

        assertThat(tariffService.getTariff(tariffId)).contains(tariff);
    }

    @Test
    void getTariff_404_returnsEmpty() {
        UUID tariffId = UUID.randomUUID();
        when(tariffsApi.getCoworkingTariffById(tariffId)).thenThrow(httpError(404));

        assertThat(tariffService.getTariff(tariffId)).isEmpty();
    }

    @Test
    void getTariff_unexpectedException_returnsEmpty() {
        UUID tariffId = UUID.randomUUID();
        when(tariffsApi.getCoworkingTariffById(tariffId)).thenThrow(new RuntimeException("boom"));

        assertThat(tariffService.getTariff(tariffId)).isEmpty();
    }

    @Test
    void getTariffName_present_returnsName() {
        UUID tariffId = UUID.randomUUID();
        CoworkingTariffResponse tariff = mock(CoworkingTariffResponse.class);
        when(tariff.getName()).thenReturn("Day pass");
        when(tariffsApi.getCoworkingTariffById(tariffId)).thenReturn(tariff);

        assertThat(tariffService.getTariffName(tariffId)).contains("Day pass");
    }

    @Test
    void getTariffName_notFound_returnsEmpty() {
        UUID tariffId = UUID.randomUUID();
        when(tariffsApi.getCoworkingTariffById(tariffId)).thenThrow(httpError(404));

        assertThat(tariffService.getTariffName(tariffId)).isEmpty();
    }

    // ===== listAllTariffs =====

    @Test
    void listAllTariffs_success_returnsList() {
        List<CoworkingTariffResponse> list = List.of(mock(CoworkingTariffResponse.class));
        when(tariffsApi.listCoworkingTariffs()).thenReturn(list);

        assertThat(tariffService.listAllTariffs()).isEqualTo(list);
    }

    @Test
    void listAllTariffs_exception_returnsEmptyList() {
        when(tariffsApi.listCoworkingTariffs()).thenThrow(new RuntimeException("boom"));

        assertThat(tariffService.listAllTariffs()).isEmpty();
    }

    // ===== requestGuestTariff =====

    @Test
    void requestGuestTariff_success_returnsCreated() {
        UUID guestId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        CoworkingGuestTariffResponse created = mock(CoworkingGuestTariffResponse.class);
        when(guestTariffsApi.createGuestTariff(any())).thenReturn(created);

        Optional<CoworkingGuestTariffResponse> result = tariffService.requestGuestTariff(guestId, tariffId);

        assertThat(result).contains(created);
        var captor = org.mockito.ArgumentCaptor.forClass(CreateCoworkingGuestTariffRequest.class);
        verify(guestTariffsApi).createGuestTariff(captor.capture());
        assertThat(captor.getValue().getGuestId()).isEqualTo(guestId);
        assertThat(captor.getValue().getTariffId()).isEqualTo(tariffId);
    }

    @Test
    void requestGuestTariff_httpError_returnsEmpty() {
        UUID guestId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        when(guestTariffsApi.createGuestTariff(any())).thenThrow(httpError(409));

        assertThat(tariffService.requestGuestTariff(guestId, tariffId)).isEmpty();
    }

    @Test
    void requestGuestTariff_unexpectedException_returnsEmpty() {
        UUID guestId = UUID.randomUUID();
        UUID tariffId = UUID.randomUUID();
        when(guestTariffsApi.createGuestTariff(any())).thenThrow(new RuntimeException("boom"));

        assertThat(tariffService.requestGuestTariff(guestId, tariffId)).isEmpty();
    }

    // ===== deductDay =====

    @Test
    void deductDay_success_returnsUpdated() {
        UUID guestTariffId = UUID.randomUUID();
        CoworkingGuestTariffResponse updated = mock(CoworkingGuestTariffResponse.class);
        when(guestTariffsApi.deductGuestTariffDay(guestTariffId)).thenReturn(updated);

        assertThat(tariffService.deductDay(guestTariffId)).contains(updated);
    }

    @Test
    void deductDay_httpError_returnsEmpty() {
        UUID guestTariffId = UUID.randomUUID();
        when(guestTariffsApi.deductGuestTariffDay(guestTariffId)).thenThrow(httpError(404));

        assertThat(tariffService.deductDay(guestTariffId)).isEmpty();
    }

    @Test
    void deductDay_unexpectedException_returnsEmpty() {
        UUID guestTariffId = UUID.randomUUID();
        when(guestTariffsApi.deductGuestTariffDay(guestTariffId)).thenThrow(new RuntimeException("boom"));

        assertThat(tariffService.deductDay(guestTariffId)).isEmpty();
    }
}
