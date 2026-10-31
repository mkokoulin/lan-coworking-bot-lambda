package com.lan.app.service;

import com.lan.app.client.baserow.api.CoworkingMeetingRoomBookingsApi;
import com.lan.app.client.baserow.model.CreateCoworkingMeetingRoomBookingRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class MeetingRoomService {

    private static final Logger LOG = Logger.getLogger(MeetingRoomService.class);
    private static final ZoneId YEREVAN = ZoneId.of("Asia/Yerevan");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    @RestClient
    CoworkingMeetingRoomBookingsApi bookingsApi;

    @Inject
    GuestService guestService;

    public void tryCreateBooking(Long chatId, String date, String start, String end, String contact) {
        var guest = guestService.findByChatId(chatId);
        if (guest.isEmpty()) {
            LOG.infof("No registered guest for chatId=%d, skipping Baserow booking", chatId);
            return;
        }

        try {
            OffsetDateTime dateStart = toOffsetDateTime(date, start);
            OffsetDateTime dateEnd   = toOffsetDateTime(date, end);

            var req = new CreateCoworkingMeetingRoomBookingRequest()
                    .guestId(guest.get().getId())
                    .dateStart(dateStart)
                    .dateEnd(dateEnd)
                    .persons(1)
                    .comment(contact);

            bookingsApi.createCoworkingMeetingRoomBooking(req);
            LOG.infof("Meeting room booking saved to Baserow: guestId=%s %s %s–%s",
                    guest.get().getId(), date, start, end);
        } catch (WebApplicationException e) {
            LOG.warnf("createMeetingRoomBooking failed: HTTP %d", e.getResponse().getStatus());
        } catch (Exception e) {
            LOG.warnf(e, "createMeetingRoomBooking unexpected error");
        }
    }

    private OffsetDateTime toOffsetDateTime(String date, String time) {
        LocalDate d = LocalDate.parse(date, DATE_FMT);
        LocalTime t = LocalTime.parse(time, TIME_FMT);
        return d.atTime(t).atZone(YEREVAN).toOffsetDateTime();
    }
}
