package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingMeetingRoomBookingsApi;
import com.lan.app.client.baserow.model.CoworkingGuestResponse;
import com.lan.app.client.baserow.model.CreateCoworkingMeetingRoomBookingRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class MeetingRoomServiceTest {

    @Inject
    MeetingRoomService meetingRoomService;

    @InjectMock
    @RestClient
    CoworkingMeetingRoomBookingsApi bookingsApi;

    @InjectMock
    GuestService guestService;

    private static WebApplicationException httpError(int status) {
        return new WebApplicationException(Response.status(status).build());
    }

    @Test
    void tryCreateBooking_noRegisteredGuest_skipsBookingCreation() {
        when(guestService.findByChatId(100L)).thenReturn(Optional.empty());

        meetingRoomService.tryCreateBooking(100L, "2026-07-23", "10:00", "11:00", "contact");

        verify(bookingsApi, never()).createCoworkingMeetingRoomBooking(any());
    }

    @Test
    void tryCreateBooking_success_sendsCorrectRequest() {
        UUID guestId = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(guestId);
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));

        meetingRoomService.tryCreateBooking(100L, "2026-07-23", "10:00", "11:30", "some contact");

        var captor = org.mockito.ArgumentCaptor.forClass(CreateCoworkingMeetingRoomBookingRequest.class);
        verify(bookingsApi).createCoworkingMeetingRoomBooking(captor.capture());
        CreateCoworkingMeetingRoomBookingRequest sent = captor.getValue();

        assertThat(sent.getGuestId()).isEqualTo(guestId);
        assertThat(sent.getPersons()).isEqualTo(1);
        assertThat(sent.getComment()).isEqualTo("some contact");

        // Asia/Yerevan is UTC+4 (no DST) -> 2026-07-23T10:00 local becomes offset +04:00
        OffsetDateTime expectedStart = OffsetDateTime.parse("2026-07-23T10:00:00+04:00");
        OffsetDateTime expectedEnd = OffsetDateTime.parse("2026-07-23T11:30:00+04:00");
        assertThat(sent.getDateStart()).isEqualTo(expectedStart);
        assertThat(sent.getDateEnd()).isEqualTo(expectedEnd);
    }

    @Test
    void tryCreateBooking_httpError_isSwallowed() {
        UUID guestId = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(guestId);
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));
        when(bookingsApi.createCoworkingMeetingRoomBooking(any())).thenThrow(httpError(500));

        meetingRoomService.tryCreateBooking(100L, "2026-07-23", "10:00", "11:00", "contact");
        // no exception propagated
    }

    @Test
    void tryCreateBooking_unexpectedException_isSwallowed() {
        UUID guestId = UUID.randomUUID();
        CoworkingGuestResponse guest = mock(CoworkingGuestResponse.class);
        when(guest.getId()).thenReturn(guestId);
        when(guestService.findByChatId(100L)).thenReturn(Optional.of(guest));
        when(bookingsApi.createCoworkingMeetingRoomBooking(any())).thenThrow(new RuntimeException("boom"));

        meetingRoomService.tryCreateBooking(100L, "2026-07-23", "10:00", "11:00", "contact");
        // no exception propagated
    }
}
