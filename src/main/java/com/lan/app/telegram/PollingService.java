package com.lan.app.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.handler.UpdateHandler;
import com.lan.app.telegram.dto.TelegramUpdate;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class PollingService {

    // @ConfigProperty(name = "telegram.mode", defaultValue = "polling")
    @ConfigProperty(name = "app.mode", defaultValue = "polling")
    String mode;

    @ConfigProperty(name = "telegram.bot-token")
    String botToken;

    @ConfigProperty(name = "telegram.api-base-url")
    String apiBaseUrl;

    private final UpdateHandler updateHandler;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public PollingService(
        UpdateHandler updateHandler
    ) {
        this.updateHandler = updateHandler;
    }

    void onStart(@Observes StartupEvent event) {
        if (!"polling".equals(mode)) {
            System.out.println("⏭️ Polling disabled, running in webhook mode");
            return;
        }

        System.out.println("🚀 Starting polling mode...");
        Thread.ofVirtual().start(this::poll);
    }

    private void poll() {
        long offset = 0;

        while (true) {
            try {
                String url = apiBaseUrl + "/bot" + botToken
                        + "/getUpdates?offset=" + offset + "&timeout=30";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                var root = objectMapper.readTree(response.body());
                var results = root.get("result");

                if (results != null && results.isArray()) {
                    for (var node : results) {
                        TelegramUpdate update = objectMapper.treeToValue(node, TelegramUpdate.class);
                        updateHandler.handle(update);
                        offset = update.update_id + 1;
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Polling interrupted");
                break;
            } catch (Exception e) {
                System.out.println("Polling error: " + e.getMessage());
                try {
                    Thread.sleep(3000); // пауза перед повтором при ошибке
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
