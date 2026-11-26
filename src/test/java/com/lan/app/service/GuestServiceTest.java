package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingGuestsApi;
import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.client.baserow.model.CreateCoworkingGuestRequest;
import com.lan.app.client.baserow.model.LinkStatusResponse;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class GuestServiceTest {

    @Inject
    GuestService guestService;

    @InjectMock
    @RestClient
    CoworkingGuestsApi guestsApi;

    private static WebApplicationException httpError(int status) {
        return new WebApplicationException(Response.status(status).build());
    }

    // ===== getGuest =====

    @Test
    void getGuest_delegatesToApi() {
        UUID id = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guestsApi.getCoworkingGuestById(id)).thenReturn(guest);

        assertThat(guestService.getGuest(id)).isEqualTo(guest);
    }

    // ===== findByChatId =====

    @Test
    void findByChatId_found_returnsGuest() {
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guestsApi.getCoworkingGuestByChatId(100L)).thenReturn(guest);

        assertThat(guestService.findByChatId(100L)).contains(guest);
    }

    @Test
    void findByChatId_404_returnsEmpty() {
        when(guestsApi.getCoworkingGuestByChatId(100L)).thenThrow(httpError(404));

        assertThat(guestService.findByChatId(100L)).isEmpty();
    }

    @Test
    void findByChatId_otherHttpError_returnsEmpty() {
        when(guestsApi.getCoworkingGuestByChatId(100L)).thenThrow(httpError(500));

        assertThat(guestService.findByChatId(100L)).isEmpty();
    }

    @Test
    void findByChatId_unexpectedException_returnsEmpty() {
        when(guestsApi.getCoworkingGuestByChatId(100L)).thenThrow(new RuntimeException("boom"));

        assertThat(guestService.findByChatId(100L)).isEmpty();
    }

    // ===== linkChatById =====

    @Test
    void linkChatById_success_returnsLinkedOutcome() {
        UUID guestId = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guestsApi.linkCoworkingGuestChatById(eq(guestId), any())).thenReturn(guest);

        GuestService.LinkChatOutcome outcome = guestService.linkChatById(guestId, 100L);

        assertThat(outcome.result()).isEqualTo(GuestService.LinkChatResult.LINKED);
        assertThat(outcome.guest()).isEqualTo(guest);
    }

    @Test
    void linkChatById_404_returnsNotFound() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.linkCoworkingGuestChatById(eq(guestId), any())).thenThrow(httpError(404));

        GuestService.LinkChatOutcome outcome = guestService.linkChatById(guestId, 100L);

        assertThat(outcome.result()).isEqualTo(GuestService.LinkChatResult.NOT_FOUND);
        assertThat(outcome.guest()).isNull();
    }

    @Test
    void linkChatById_409_returnsConflict() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.linkCoworkingGuestChatById(eq(guestId), any())).thenThrow(httpError(409));

        GuestService.LinkChatOutcome outcome = guestService.linkChatById(guestId, 100L);

        assertThat(outcome.result()).isEqualTo(GuestService.LinkChatResult.CHAT_ID_CONFLICT);
    }

    @Test
    void linkChatById_otherHttpError_returnsError() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.linkCoworkingGuestChatById(eq(guestId), any())).thenThrow(httpError(500));

        GuestService.LinkChatOutcome outcome = guestService.linkChatById(guestId, 100L);

        assertThat(outcome.result()).isEqualTo(GuestService.LinkChatResult.ERROR);
    }

    @Test
    void linkChatById_unexpectedException_returnsError() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.linkCoworkingGuestChatById(eq(guestId), any())).thenThrow(new RuntimeException("boom"));

        GuestService.LinkChatOutcome outcome = guestService.linkChatById(guestId, 100L);

        assertThat(outcome.result()).isEqualTo(GuestService.LinkChatResult.ERROR);
    }

    // ===== linkChat (phone-based) =====

    @Test
    void linkChat_success_returnsGuest() {
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guestsApi.linkCoworkingGuestChat(any())).thenReturn(guest);

        assertThat(guestService.linkChat("+37491123456", 100L)).contains(guest);
    }

    @Test
    void linkChat_404_returnsEmpty() {
        when(guestsApi.linkCoworkingGuestChat(any())).thenThrow(httpError(404));

        assertThat(guestService.linkChat("+37491123456", 100L)).isEmpty();
    }

    @Test
    void linkChat_otherHttpError_returnsEmpty() {
        when(guestsApi.linkCoworkingGuestChat(any())).thenThrow(httpError(500));

        assertThat(guestService.linkChat("+37491123456", 100L)).isEmpty();
    }

    @Test
    void linkChat_unexpectedException_returnsEmpty() {
        when(guestsApi.linkCoworkingGuestChat(any())).thenThrow(new RuntimeException("boom"));

        assertThat(guestService.linkChat("+37491123456", 100L)).isEmpty();
    }

    // ===== confirmLink =====

    @Test
    void confirmLink_success_callsApi() {
        UUID guestId = UUID.randomUUID();

        guestService.confirmLink(guestId);

        verify(guestsApi).confirmCoworkingGuestLink(guestId);
    }

    @Test
    void confirmLink_httpError_swallowsException() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.confirmCoworkingGuestLink(guestId)).thenThrow(httpError(404));

        guestService.confirmLink(guestId);
        // no exception propagated
    }

    @Test
    void confirmLink_unexpectedException_swallowsException() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.confirmCoworkingGuestLink(guestId)).thenThrow(new RuntimeException("boom"));

        guestService.confirmLink(guestId);
        // no exception propagated
    }

    // ===== rejectLink =====

    @Test
    void rejectLink_success_callsApi() {
        UUID guestId = UUID.randomUUID();

        guestService.rejectLink(guestId);

        verify(guestsApi).rejectCoworkingGuestLink(guestId);
    }

    @Test
    void rejectLink_httpError_swallowsException() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.rejectCoworkingGuestLink(guestId)).thenThrow(httpError(404));

        guestService.rejectLink(guestId);
        // no exception propagated
    }

    @Test
    void rejectLink_unexpectedException_swallowsException() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.rejectCoworkingGuestLink(guestId)).thenThrow(new RuntimeException("boom"));

        guestService.rejectLink(guestId);
        // no exception propagated
    }

    // ===== getLinkStatus =====

    @Test
    void getLinkStatus_linked_returnsConfirmed() {
        UUID guestId = UUID.randomUUID();
        LinkStatusResponse resp = new LinkStatusResponse();
        resp.setLinked(true);
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenReturn(resp);

        assertThat(guestService.getLinkStatus(guestId)).isEqualTo(GuestService.LinkStatus.CONFIRMED);
    }

    @Test
    void getLinkStatus_rejected_returnsRejected() {
        UUID guestId = UUID.randomUUID();
        LinkStatusResponse resp = new LinkStatusResponse();
        resp.setRejected(true);
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenReturn(resp);

        assertThat(guestService.getLinkStatus(guestId)).isEqualTo(GuestService.LinkStatus.REJECTED);
    }

    @Test
    void getLinkStatus_conflict_returnsConflict() {
        UUID guestId = UUID.randomUUID();
        LinkStatusResponse resp = new LinkStatusResponse();
        resp.setConflict(true);
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenReturn(resp);

        assertThat(guestService.getLinkStatus(guestId)).isEqualTo(GuestService.LinkStatus.CONFLICT);
    }

    @Test
    void getLinkStatus_allFlagsFalseOrNull_returnsPending() {
        UUID guestId = UUID.randomUUID();
        LinkStatusResponse resp = new LinkStatusResponse();
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenReturn(resp);

        assertThat(guestService.getLinkStatus(guestId)).isEqualTo(GuestService.LinkStatus.PENDING);
    }

    @Test
    void getLinkStatus_404_returnsNull() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenThrow(httpError(404));

        assertThat(guestService.getLinkStatus(guestId)).isNull();
    }

    @Test
    void getLinkStatus_otherHttpError_returnsNull() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenThrow(httpError(500));

        assertThat(guestService.getLinkStatus(guestId)).isNull();
    }

    @Test
    void getLinkStatus_unexpectedException_returnsNull() {
        UUID guestId = UUID.randomUUID();
        when(guestsApi.getCoworkingGuestLinkStatus(guestId)).thenThrow(new RuntimeException("boom"));

        assertThat(guestService.getLinkStatus(guestId)).isNull();
    }

    // ===== unlinkChat =====

    @Test
    void unlinkChat_success_callsApi() {
        guestService.unlinkChat(100L);

        verify(guestsApi).unlinkCoworkingGuestChat(any());
    }

    @Test
    void unlinkChat_exception_swallowsException() {
        when(guestsApi.unlinkCoworkingGuestChat(any())).thenThrow(new RuntimeException("boom"));

        guestService.unlinkChat(100L);
        // no exception propagated
    }

    // ===== createGuest =====

    @Test
    void createGuest_buildsRequestAndDelegates() {
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guestsApi.createCoworkingGuest(any())).thenReturn(guest);

        CoworkingGuestResponse result = guestService.createGuest("Ann", "Smith", "annsmith", "+37491123456", 100L);

        assertThat(result).isEqualTo(guest);
        var captor = org.mockito.ArgumentCaptor.forClass(CreateCoworkingGuestRequest.class);
        verify(guestsApi).createCoworkingGuest(captor.capture());
        CreateCoworkingGuestRequest sent = captor.getValue();
        assertThat(sent.getFirstName()).isEqualTo("Ann");
        assertThat(sent.getLastName()).isEqualTo("Smith");
        assertThat(sent.getTelegram()).isEqualTo("annsmith");
        assertThat(sent.getPhone()).isEqualTo("+37491123456");
        assertThat(sent.getTelegramChatId()).isEqualTo("100");
    }

    @Test
    void createGuest_nullChatId_setsNullTelegramChatId() {
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guestsApi.createCoworkingGuest(any())).thenReturn(guest);

        guestService.createGuest("Ann", "Smith", "annsmith", "+37491123456", null);

        var captor = org.mockito.ArgumentCaptor.forClass(CreateCoworkingGuestRequest.class);
        verify(guestsApi).createCoworkingGuest(captor.capture());
        assertThat(captor.getValue().getTelegramChatId()).isNull();
    }
}
