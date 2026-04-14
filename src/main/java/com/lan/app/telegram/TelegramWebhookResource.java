package com.lan.app.telegram;

import com.lan.app.handler.UpdateHandler;
import com.lan.app.telegram.dto.TelegramUpdate;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class TelegramWebhookResource {

    private static final String EXPECTED_SECRET = "my_secret";

    private final UpdateHandler updateHandler;

    @Inject
    public TelegramWebhookResource(
        UpdateHandler updateHandler
    ) {
        this.updateHandler = updateHandler;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String root() {
        return "Server is running 🚀";
    }

    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response webhook(
            TelegramUpdate update,
            @HeaderParam("X-Telegram-Bot-Api-Secret-Token") String secretToken
    ) {
        if (!EXPECTED_SECRET.equals(secretToken)) {
            System.out.println("Forbidden: invalid secret token");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        System.out.println("🔥 UPDATE FROM TELEGRAM:");
        System.out.println(update);

        updateHandler.handle(update);

        return Response.ok("OK").build();
    }
}