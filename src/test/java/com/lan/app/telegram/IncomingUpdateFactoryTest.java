package com.lan.app.telegram;

import com.lan.app.domain.IncomingUpdate;
import com.lan.app.telegram.dto.TelegramCallbackQuery;
import com.lan.app.telegram.dto.TelegramChat;
import com.lan.app.telegram.dto.TelegramContact;
import com.lan.app.telegram.dto.TelegramDocument;
import com.lan.app.telegram.dto.TelegramMessage;
import com.lan.app.telegram.dto.TelegramPhotoSize;
import com.lan.app.telegram.dto.TelegramUpdate;
import com.lan.app.telegram.dto.TelegramUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncomingUpdateFactoryTest {

    private final IncomingUpdateFactory factory = new IncomingUpdateFactory();

    private static TelegramUser user(long id) {
        TelegramUser u = new TelegramUser();
        u.id = id;
        u.username = "bob";
        u.first_name = "Bob";
        u.language_code = "ru";
        return u;
    }

    private static TelegramChat chat(long id) {
        TelegramChat c = new TelegramChat();
        c.id = id;
        c.type = "private";
        return c;
    }

    @Test
    void fromTelegram_null_returnsNull() {
        assertThat(factory.fromTelegram(null)).isNull();
    }

    @Test
    void fromTelegram_neitherMessageNorCallback_returnsNull() {
        TelegramUpdate update = new TelegramUpdate();
        update.update_id = 1L;

        assertThat(factory.fromTelegram(update)).isNull();
    }

    @Test
    void fromTelegram_message_mapsTextAndUser() {
        TelegramUpdate update = new TelegramUpdate();
        update.update_id = 5L;
        update.message = new TelegramMessage(10L, user(2L), chat(3L), "  hello  ", null, null, null);

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getType()).isEqualTo(IncomingUpdate.UpdateType.MESSAGE);
        assertThat(result.getUpdateId()).isEqualTo(5L);
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getChatId()).isEqualTo(3L);
        assertThat(result.getChatType()).isEqualTo("private");
        assertThat(result.getText()).isEqualTo("hello");
        assertThat(result.getUsername()).isEqualTo("bob");
    }

    @Test
    void fromTelegram_message_nullText_becomesEmptyString() {
        TelegramUpdate update = new TelegramUpdate();
        update.message = new TelegramMessage(10L, user(2L), chat(3L), null, null, null, null);

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getText()).isEmpty();
    }

    @Test
    void fromTelegram_message_withContact_setsSharedPhone() {
        TelegramUpdate update = new TelegramUpdate();
        TelegramContact contact = new TelegramContact("  +37410000000  ", "Bob", null, 2L);
        update.message = new TelegramMessage(10L, user(2L), chat(3L), "", contact, null, null);

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getSharedPhone()).isEqualTo("+37410000000");
    }

    @Test
    void fromTelegram_message_photoTakesPriorityOverDocument_usesLargest() {
        TelegramUpdate update = new TelegramUpdate();
        List<TelegramPhotoSize> photos = List.of(
            new TelegramPhotoSize("small", 10, 10),
            new TelegramPhotoSize("large", 100, 100)
        );
        TelegramDocument document = new TelegramDocument("doc1", "doc.pdf", "application/pdf");
        update.message = new TelegramMessage(10L, user(2L), chat(3L), "", null, photos, document);

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getFileId()).isEqualTo("large");
        assertThat(result.getFileName()).isNull();
    }

    @Test
    void fromTelegram_message_documentOnly_setsFileIdAndName() {
        TelegramUpdate update = new TelegramUpdate();
        TelegramDocument document = new TelegramDocument("doc1", "doc.pdf", "application/pdf");
        update.message = new TelegramMessage(10L, user(2L), chat(3L), "", null, null, document);

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getFileId()).isEqualTo("doc1");
        assertThat(result.getFileName()).isEqualTo("doc.pdf");
    }

    @Test
    void fromTelegram_message_noFromOrChat_leavesUserAndChatNull() {
        TelegramUpdate update = new TelegramUpdate();
        update.message = new TelegramMessage(10L, null, null, "hi", null, null, null);

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getUserId()).isNull();
        assertThat(result.getChatId()).isNull();
    }

    @Test
    void fromTelegram_callback_mapsDataAndTrimsIt() {
        TelegramUpdate update = new TelegramUpdate();
        TelegramCallbackQuery cb = new TelegramCallbackQuery();
        cb.id = "qid1";
        cb.from = user(2L);
        cb.data = "  tariff_list  ";
        cb.message = new TelegramMessage(55L, null, chat(3L), null, null, null, null);
        update.callback_query = cb;

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getType()).isEqualTo(IncomingUpdate.UpdateType.CALLBACK);
        assertThat(result.getCallbackData()).isEqualTo("tariff_list");
        assertThat(result.getCallbackQueryId()).isEqualTo("qid1");
        assertThat(result.getChatId()).isEqualTo(3L);
        assertThat(result.getCallbackMessageId()).isEqualTo(55);
    }

    @Test
    void fromTelegram_callback_nullData_becomesEmptyString() {
        TelegramUpdate update = new TelegramUpdate();
        TelegramCallbackQuery cb = new TelegramCallbackQuery();
        cb.id = "qid1";
        cb.from = user(2L);
        cb.data = null;
        update.callback_query = cb;

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getCallbackData()).isEmpty();
    }

    @Test
    void fromTelegram_callback_messagePresentButNoChat_leavesChatIdNull() {
        TelegramUpdate update = new TelegramUpdate();
        TelegramCallbackQuery cb = new TelegramCallbackQuery();
        cb.id = "qid1";
        cb.from = user(2L);
        cb.data = "x";
        cb.message = new TelegramMessage(55L, null, null, null, null, null, null);
        update.callback_query = cb;

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getChatId()).isNull();
    }

    @Test
    void fromTelegram_messageTakesPriorityOverCallback_ifBothSomehowPresent() {
        TelegramUpdate update = new TelegramUpdate();
        update.message = new TelegramMessage(10L, user(2L), chat(3L), "hi", null, null, null);
        TelegramCallbackQuery cb = new TelegramCallbackQuery();
        cb.id = "qid1";
        cb.data = "x";
        update.callback_query = cb;

        IncomingUpdate result = factory.fromTelegram(update);

        assertThat(result.getType()).isEqualTo(IncomingUpdate.UpdateType.MESSAGE);
    }
}
