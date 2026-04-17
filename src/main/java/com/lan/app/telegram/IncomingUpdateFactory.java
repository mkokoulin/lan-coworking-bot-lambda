package com.lan.app.telegram;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.telegram.dto.TelegramCallbackQuery;
import com.lan.app.telegram.dto.TelegramMessage;
import com.lan.app.telegram.dto.TelegramUpdate;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IncomingUpdateFactory {

    public IncomingUpdate fromTelegram(TelegramUpdate update) {
        if (update == null) return null;

        IncomingUpdate result = new IncomingUpdate();
        result.setUpdateId(update.update_id);

        if (update.message != null) {
            fillFromMessage(result, update.message);
            result.setType(IncomingUpdate.UpdateType.MESSAGE);
            return result;
        }

        if (update.callback_query != null) {
            fillFromCallback(result, update.callback_query);
            result.setType(IncomingUpdate.UpdateType.CALLBACK);
            return result;
        }

        return null;
    }

    private void fillFromMessage(IncomingUpdate target, TelegramMessage message) {
        if (message.from != null) {
            target.setUserId(message.from.id);
            target.setUserLanguageCode(message.from.language_code);
            target.setFirstName(message.from.first_name);
            target.setUsername(message.from.username);
        }
        if (message.chat != null) {
            target.setChatId(message.chat.id);
        }
        target.setText(message.text == null ? "" : message.text.trim());

        // Контакт (кнопка «Поделиться номером»)
        if (message.contact != null && message.contact.phone_number != null) {
            target.setSharedPhone(message.contact.phone_number);
        }
    }

    private void fillFromCallback(IncomingUpdate target, TelegramCallbackQuery callback) {
        if (callback.from != null) {
            target.setUserId(callback.from.id);
            target.setUserLanguageCode(callback.from.language_code);
            target.setFirstName(callback.from.first_name);
            target.setUsername(callback.from.username);
        }
        if (callback.message != null && callback.message.chat != null) {
            target.setChatId(callback.message.chat.id);
        }
        target.setCallbackData(callback.data == null ? "" : callback.data.trim());
    }
}
