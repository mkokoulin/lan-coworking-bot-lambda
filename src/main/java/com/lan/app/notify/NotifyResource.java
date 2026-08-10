package com.lan.app.notify;

import com.lan.app.config.TelegramConfig;
import com.lan.app.i18n.I18n;
import com.lan.app.telegram.TelegramClient;
import com.lan.app.ui.KeyboardBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/notify")
@ApplicationScoped
public class NotifyResource {

    private static final Logger log = Logger.getLogger(NotifyResource.class);

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;
    private final I18n i18n;

    @ConfigProperty(name = "notify.secret", defaultValue = "")
    String notifySecret;

    @Inject
    public NotifyResource(TelegramClient telegramClient, TelegramConfig telegramConfig, I18n i18n) {
        this.telegramClient = telegramClient;
        this.telegramConfig = telegramConfig;
        this.i18n = i18n;
    }

    @POST
    @Path("/admin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notifyAdmin(@HeaderParam("Authorization") String authHeader, NotifyRequest req) {
        if (notifySecret.isBlank() || !("Bearer " + notifySecret).equals(authHeader)) {
            return Response.status(401).build();
        }
        if (req == null || req.message() == null || req.message().isBlank()) {
            return Response.status(400).entity("{\"error\":\"message required\"}").build();
        }
        Long adminId = telegramConfig.adminChatId();
        if (adminId == null) {
            log.warn("adminChatId not configured, skipping notification");
            return Response.status(503).entity("{\"error\":\"admin chat not configured\"}").build();
        }
        try {
            telegramClient.sendHtml(adminId, req.message(), null);
            return Response.ok("{\"ok\":true}").build();
        } catch (Exception e) {
            log.errorf("Failed to send admin notification: %s", e.getMessage());
            return Response.serverError().entity("{\"error\":\"telegram error\"}").build();
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notifyLogin(@HeaderParam("Authorization") String authHeader, NotifyLoginRequest req) {
        if (notifySecret.isBlank() || !("Bearer " + notifySecret).equals(authHeader)) {
            return Response.status(401).build();
        }
        if (req == null || req.chatId() == null || req.guestId() == null) {
            return Response.status(400).entity("{\"error\":\"chatId and guestId required\"}").build();
        }
        String lang = req.lang() != null ? req.lang() : "ru";
        var kb = KeyboardBuilder.inline(List.of(
            KeyboardBuilder.row(
                Map.of("text", i18n.t(lang, "cw_login_confirm_yes"), "callback_data", "cw_confirm_" + req.guestId()),
                Map.of("text", i18n.t(lang, "cw_login_confirm_no"),  "callback_data", "cw_reject_"  + req.guestId())
            )
        ));
        try {
            telegramClient.sendHtml(req.chatId(), i18n.t(lang, "cw_login_confirm_text"), kb);
            return Response.ok("{\"ok\":true}").build();
        } catch (Exception e) {
            log.errorf("Failed to send login push chatId=%d: %s", req.chatId(), e.getMessage());
            return Response.serverError().entity("{\"error\":\"telegram error\"}").build();
        }
    }
}
