package com.lan.app.notify;

import com.lan.app.config.TelegramConfig;
import com.lan.app.telegram.TelegramClient;
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

@Path("/notify")
@ApplicationScoped
public class NotifyResource {

    private static final Logger log = Logger.getLogger(NotifyResource.class);

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;

    @ConfigProperty(name = "notify.secret", defaultValue = "")
    String notifySecret;

    @Inject
    public NotifyResource(TelegramClient telegramClient, TelegramConfig telegramConfig) {
        this.telegramClient = telegramClient;
        this.telegramConfig = telegramConfig;
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
}
