package com.lan.app.session;

import java.time.OffsetDateTime;

public class Session {

    private Long chatId;
    private Long userId;

    private String lang;
    private String flow;
    private String step;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private String payloadJson;
    private Long lastProcessedUpdateId;

    public Session() {
    }

    public static Session newDefault(Long chatId, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();

        Session s = new Session();
        s.chatId = chatId;
        s.userId = userId;
        s.lang = "en";
        s.flow = "";
        s.step = "";
        s.createdAt = now;
        s.updatedAt = now;

        return s;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getLastProcessedUpdateId() {
        return lastProcessedUpdateId;
    }

    public void setLastProcessedUpdateId(Long lastProcessedUpdateId) {
        this.lastProcessedUpdateId = lastProcessedUpdateId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}