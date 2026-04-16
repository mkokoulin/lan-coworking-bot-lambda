package com.lan.app.telegram;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.telegram.dto.TelegramCallbackQuery;
import com.lan.app.telegram.dto.TelegramMessage;
import com.lan.app.telegram.dto.TelegramUpdate;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IncomingUpdateFactory {

    public IncomingUpdate fromTelegram(TelegramUpdate update) {
        if (update == null) {
            return null;
        }

        IncomingUpdate result = IncomingUpdate.create();
        result = result.withUpdateId(update.update_id);

        if (update.message != null) {
            result = fillFromMessage(result, update.message);
            return result.withType(IncomingUpdate.UpdateType.MESSAGE);
        }

        if (update.callback_query != null) {
            result = fillFromCallback(result, update.callback_query);
            return result.withType(IncomingUpdate.UpdateType.CALLBACK);
        }

        return null;
    }

    private IncomingUpdate fillFromMessage(IncomingUpdate target, TelegramMessage message) {
        var output = target.copy();

        if (message.from != null) {
            output = output.withUserId(message.from.id);
            output = output.withUserLanguageCode(message.from.language_code);
            output = output.withFirstName(message.from.first_name);
            output = output.withUsername(message.from.username);
        }
        if (message.chat != null) {
            output = output.withChatId(message.chat.id);
        }
        output = output.withText(message.text == null ? "" : message.text.trim());

        return output;
    }

    private IncomingUpdate fillFromCallback(IncomingUpdate target, TelegramCallbackQuery callback) {
        var output = target.copy();

        if (callback.from != null) {
            output = output.withUserId(callback.from.id);
            output = output.withUserLanguageCode(callback.from.language_code);
            output = output.withFirstName(callback.from.first_name);
            output = output.withUsername(callback.from.username);
        }
        if (callback.message != null && callback.message.chat != null) {
            output = output.withChatId(callback.message.chat.id);
        }

        return output.withCallbackData(callback.data == null ? "" : callback.data.trim());
    }
}
