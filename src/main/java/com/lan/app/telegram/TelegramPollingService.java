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

    private final TelegramClient telegramClient;
    private final UpdateHandler updateHandler;
    private final AtomicLong offset = new AtomicLong(0);

    @Inject
    public TelegramPollingService(TelegramClient telegramClient, UpdateHandler updateHandler) {
        this.telegramClient = telegramClient;
        this.updateHandler = updateHandler;
    }

    @Scheduled(every = "3s", concurrentExecution = ConcurrentExecution.SKIP)
    void poll() {
        List<TelegramUpdate> updates = telegramClient.getUpdates(offset.get(), 0);
        // advance offset before processing so repeated ticks don't re-fetch the same updates
        for (TelegramUpdate update : updates) {
            if (update.update_id != null) {
                offset.accumulateAndGet(update.update_id + 1, Math::max);
            }
        }
        for (TelegramUpdate update : updates) {
            try {
                updateHandler.handle(update);
            } catch (Exception e) {
                log.error("Error handling update {}: {}", update.update_id, e.getMessage(), e);
            }
        }
    }
}
