package com.lan.app.domain;

public record IncomingUpdate(
    Long updateId,
    Long userId,
    Long chatId,
    String text,
    String callbackData,
    UpdateType type,
    String userLanguageCode,
    String firstName,
    String username
) {
    public IncomingUpdate withUpdateId(Long updateId) {
        return new IncomingUpdate(
            updateId, 
            this.userId(), 
            this.chatId(),
            this.text(),
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    public IncomingUpdate withType(UpdateType type) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            this.text(),
            this.callbackData(),
            type,
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    public IncomingUpdate withUserId(Long userId) {
        return new IncomingUpdate(
            this.updateId(), 
            userId, 
            this.chatId(),
            this.text(),
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    
    public IncomingUpdate withChatId(Long chatId) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            chatId,
            this.text(),
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    public IncomingUpdate withText(String text) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            text,
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    public IncomingUpdate withCallbackData(String callbackData) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            this.text(),
            callbackData,
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    public IncomingUpdate withUserLanguageCode(String userLanguageCode) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            this.text(),
            this.callbackData(),
            this.type(),
            userLanguageCode,
            this.firstName(),
            this.username()
        );
    }

    public IncomingUpdate withFirstName(String firstName) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            this.text(),
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            firstName,
            this.username()
        );
    }

    public IncomingUpdate withUsername(String username) {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            this.text(),
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            username
        );
    }

    public IncomingUpdate copy() {
        return new IncomingUpdate(
            this.updateId(), 
            this.userId(), 
            this.chatId(),
            this.text(),
            this.callbackData(),
            this.type(),
            this.userLanguageCode(),
            this.firstName(),
            this.username()
        );
    }

    public static IncomingUpdate create() {
        return new IncomingUpdate(
            Long.MIN_VALUE,
            Long.MIN_VALUE,
            Long.MIN_VALUE,
            "",
            "",
            UpdateType.MESSAGE,
            "",
            "",
            ""
        );
    }

    public enum UpdateType {
        MESSAGE,
        CALLBACK
    }
}