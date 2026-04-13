package com.lan.app.domain;

public class IncomingUpdate {
    
    private Long updateId;
    private Long userId;
    private Long chatId;
    private String text;
    private String callbackData;
    private UpdateType type;
    private String userLanguageCode;
    private String firstName;
    private String username;

    public enum UpdateType {
        MESSAGE,
        CALLBACK
    }

    public Long getUpdateId() {
        return updateId;
    }

    public void setUpdateId(Long updateId) {
        this.updateId = updateId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    public UpdateType getType() {
        return type;
    }

    public void setType(UpdateType type) {
        this.type = type;
    }

    public String getUserLanguageCode() {
        return userLanguageCode;
    }

    public void setUserLanguageCode(String userLanguageCode) {
        this.userLanguageCode = userLanguageCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
