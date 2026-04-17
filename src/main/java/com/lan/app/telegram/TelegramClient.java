package com.lan.app.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lan.app.config.TelegramConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TelegramClient {

    private final TelegramConfig telegramConfig;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public TelegramClient(
        TelegramConfig telegramConfig
    ) {
        this.telegramConfig = telegramConfig;
    }

    public void sendHtml(Long chatId, String text, Object replyMarkup) {
        try {
            Map<String, Object> body = new HashMap<>(); // ← HashMap вместо Map.of()
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");
            if (replyMarkup != null) {
                body.put("reply_markup", replyMarkup); // ← добавляем только если не null
            }

            String url = telegramConfig.apiBaseUrl() + "/bot" + telegramConfig.botToken() + "/sendMessage";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new TelegramClientException("Telegram sendMessage failed: " + resp.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramClientException("HTTP request was interrupted", e);
        } catch (Exception e) {
            throw new TelegramClientException("Failed to send message to Telegram", e);
        }
    }

     public void sendPhoto(Long chatId, String photoPath, String caption) {
        try {
            java.nio.file.Path path = java.nio.file.Path.of(photoPath);
            if (!java.nio.file.Files.exists(path)) {
                return;
            }
            byte[] fileBytes = java.nio.file.Files.readAllBytes(path);
            String fileName  = path.getFileName().toString();
            String boundary  = "----FormBoundary" + System.currentTimeMillis();

            var baos = new java.io.ByteArrayOutputStream();
            writeFormField(baos, boundary, "chat_id", String.valueOf(chatId));
            if (caption != null && !caption.isBlank()) {
                writeFormField(baos, boundary, "caption", caption);
                writeFormField(baos, boundary, "parse_mode", "HTML");
            }
            // file part
            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write(("Content-Disposition: form-data; name=\"photo\"; filename=\"" + fileName + "\"\r\n").getBytes());
            baos.write("Content-Type: image/jpeg\r\n\r\n".getBytes());
            baos.write(fileBytes);
            baos.write(("\r\n--" + boundary + "--\r\n").getBytes());

            String url = telegramConfig.apiBaseUrl() + "/bot" + telegramConfig.botToken() + "/sendPhoto";
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                    .build();

            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                System.err.println("[TelegramClient] sendPhoto failed: " + resp.body());
            }
        } catch (Exception e) {
            System.err.println("[TelegramClient] sendPhoto error: " + e.getMessage());
        }
    }

    // ─── Добавить в TelegramClient.java ─────────────────────────────────────────
// Отправляет сообщение с ReplyKeyboard-кнопкой «Поделиться номером».
// После нажатия Telegram присылает update с contact.phone_number.

    public void sendPhoneRequest(Long chatId, String text, String buttonLabel) {
        try {
            var keyboard = Map.of(
                    "keyboard", List.of(List.of(
                            Map.of(
                                    "text", buttonLabel,
                                    "request_contact", true
                            )
                    )),
                    "resize_keyboard", true,
                    "one_time_keyboard", true
            );

            var body = Map.of(
                    "chat_id",      chatId,
                    "text",         text,
                    "parse_mode",   "HTML",
                    "reply_markup", keyboard
            );

            String url = telegramConfig.apiBaseUrl() + "/bot" + telegramConfig.botToken() + "/sendMessage";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new RuntimeException("sendPhoneRequest failed: " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // После получения контакта нужно убрать ReplyKeyboard:
    public void removeKeyboard(Long chatId, String text) {
        try {
            var body = Map.of(
                    "chat_id",      chatId,
                    "text",         text,
                    "parse_mode",   "HTML",
                    "reply_markup", Map.of("remove_keyboard", true)
            );
            String url = telegramConfig.apiBaseUrl() + "/bot" + telegramConfig.botToken() + "/sendMessage";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {}
    }


    private static void writeFormField(java.io.ByteArrayOutputStream baos,
                                       String boundary, String name, String value)
            throws java.io.IOException {
        baos.write(("--" + boundary + "\r\n").getBytes());
        baos.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes());
        baos.write((value + "\r\n").getBytes());
    }
}
