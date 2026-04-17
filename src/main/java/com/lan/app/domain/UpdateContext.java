package com.lan.app.domain;

public record UpdateContext(
        Long    chatId,
        Long    userId,
        Integer messageId,
        String  messageText,
        String  callbackData,
        boolean callback,
        String  username,
        String  sharedPhone
) {

    public static UpdateContext fromIncomingUpdate(IncomingUpdate update) {
        return new UpdateContext(
                update.getChatId(),
                update.getUserId(),
                null,
                update.getText(),
                update.getCallbackData(),
                update.getCallbackData() != null && !update.getCallbackData().isBlank(),
                update.getUsername(),
                update.getSharedPhone()
        );
    }

    // ===== helpers =====

    public boolean hasText() {
        return messageText != null && !messageText.isBlank();
    }

    public boolean hasCallback() {
        return callbackData != null && !callbackData.isBlank();
    }

    public boolean hasSharedPhone() {
        return sharedPhone != null && !sharedPhone.isBlank();
    }

    public boolean isCommand() {
        return hasText() && messageText.startsWith("/");
    }

    public String command() {
        if (hasCallback() && callbackData.startsWith("/")) {
            return callbackData.substring(1);
        }
        if (isCommand()) {
            return messageText.substring(1).split("\\s+")[0];
        }
        return null;
    }

    public String commandArgs() {
        if (!isCommand()) return null;
        String[] parts = messageText.split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    public String callbackPayload() {
        if (callbackData == null) return null;
        return callbackData.startsWith("/") ? callbackData.substring(1) : callbackData;
    }
}
