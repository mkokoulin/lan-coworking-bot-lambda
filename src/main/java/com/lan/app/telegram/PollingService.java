package com.lan.app.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.handler.UpdateHandler;
import com.lan.app.telegram.dto.TelegramUpdate;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@ApplicationScoped
public class PollingService {

    private static final Logger log = Logger.getLogger(PollingService.class);
    private static final Path OFFSET_FILE = Path.of("data/polling-offset.txt");

    @ConfigProperty(name = "telegram.bot-token")
    String botToken;

    @ConfigProperty(name = "telegram.api-base-url")
    String apiBaseUrl;

    private final UpdateHandler updateHandler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public PollingService(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }

    void onStart(@Observes StartupEvent event) {
        log.info("🚀 Starting polling...");
        Thread.ofVirtual().start(this::poll);
    }

    private void poll() {
        long offset = readOffset();
        log.infof("Polling starting with offset=%d", offset);

        while (true) {
            try {
                String url = apiBaseUrl + "/bot" + botToken
                        + "/getUpdates?offset=" + offset + "&timeout=20";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(40))
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
                        try {
                            updateHandler.handle(update);
                        } catch (Exception e) {
                            log.errorf(e, "Failed to handle update %d", update.update_id);
                        }
                        offset = update.update_id + 1;
                        saveOffset(offset);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Polling interrupted");
                break;
            } catch (Exception e) {
                log.error("Polling error", e);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private long readOffset() {
        try {
            if (Files.exists(OFFSET_FILE)) {
                return Long.parseLong(Files.readString(OFFSET_FILE).trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void saveOffset(long offset) {
        try {
            Files.createDirectories(OFFSET_FILE.getParent());
            Files.writeString(OFFSET_FILE, String.valueOf(offset),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warnf("Failed to persist polling offset: %s", e.getMessage());
        }
    }
}
