package com.lan.app.telegram;

import com.lan.app.handler.UpdateHandler;
import com.lan.app.telegram.dto.TelegramUpdate;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class TelegramPollingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollingService.class);
    private static final String OFFSET_FILE = "data/polling-offset.txt";

    private final TelegramClient telegramClient;
    private final UpdateHandler updateHandler;
    private final AtomicLong offset = new AtomicLong(0);

    @Inject
    public TelegramPollingService(TelegramClient telegramClient, UpdateHandler updateHandler) {
        this.telegramClient = telegramClient;
        this.updateHandler = updateHandler;
        loadOffset(); // restore offset on startup
    }

    @Scheduled(every = "3s", concurrentExecution = ConcurrentExecution.SKIP)
    void poll() {
        List<TelegramUpdate> updates = telegramClient.getUpdates(offset.get(), 0);
        for (TelegramUpdate update : updates) {
            if (update.update_id != null) {
                offset.accumulateAndGet(update.update_id + 1, Math::max);
            }
        }
        if (!updates.isEmpty()) {
            saveOffset(offset.get()); // persist after each batch
        }
        for (TelegramUpdate update : updates) {
            try {
                updateHandler.handle(update);
            } catch (Exception e) {
                log.error("Error handling update {}: {}", update.update_id, e.getMessage(), e);
            }
        }
    }

    private void loadOffset() {
        try {
            var path = java.nio.file.Path.of(OFFSET_FILE);
            if (java.nio.file.Files.exists(path)) {
                String val = java.nio.file.Files.readString(path).trim();
                offset.set(Long.parseLong(val));
                log.info("Loaded polling offset: {}", offset.get());
            }
        } catch (Exception e) {
            log.warn("Could not load polling offset, starting from 0: {}", e.getMessage());
        }
    }

    private void saveOffset(long value) {
        try {
            var path = java.nio.file.Path.of(OFFSET_FILE);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, String.valueOf(value));
        } catch (Exception e) {
            log.warn("Could not save polling offset: {}", e.getMessage());
        }
    }
}
