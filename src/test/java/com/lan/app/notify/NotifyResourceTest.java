package com.lan.app.notify;

import com.lan.app.i18n.I18n;
import com.lan.app.telegram.TelegramClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@QuarkusTest
class NotifyResourceTest {

    private static final String VALID_AUTH = "Bearer test-notify-secret";

    @InjectMock
    TelegramClient telegramClient;

    @InjectMock
    I18n i18n;

    @BeforeEach
    void stubTranslations() {
        when(i18n.t(any(), any())).thenReturn("translated");
    }

    @Test
    void notifyAdmin_missingAuthHeader_returns401() {
        given()
            .contentType("application/json")
            .body("{\"message\":\"hi\"}")
            .when().post("/notify/admin")
            .then().statusCode(401);
    }

    @Test
    void notifyAdmin_wrongSecret_returns401() {
        given()
            .header("Authorization", "Bearer wrong-secret")
            .contentType("application/json")
            .body("{\"message\":\"hi\"}")
            .when().post("/notify/admin")
            .then().statusCode(401);
    }

    @Test
    void notifyAdmin_blankMessage_returns400() {
        given()
            .header("Authorization", VALID_AUTH)
            .contentType("application/json")
            .body("{\"message\":\"\"}")
            .when().post("/notify/admin")
            .then().statusCode(400);
    }

    @Test
    void notifyAdmin_validRequest_sendsAndReturns200() {
        given()
            .header("Authorization", VALID_AUTH)
            .contentType("application/json")
            .body("{\"message\":\"hello admin\"}")
            .when().post("/notify/admin")
            .then().statusCode(200)
            .body("ok", org.hamcrest.Matchers.is(true));
    }

    @Test
    void notifyAdmin_telegramClientThrows_returns500() {
        doThrow(new RuntimeException("boom")).when(telegramClient).sendHtml(anyLong(), any(), any());

        given()
            .header("Authorization", VALID_AUTH)
            .contentType("application/json")
            .body("{\"message\":\"hello admin\"}")
            .when().post("/notify/admin")
            .then().statusCode(500);
    }

    @Test
    void notifyLogin_missingAuthHeader_returns401() {
        given()
            .contentType("application/json")
            .body("{\"chatId\":1,\"guestId\":\"g1\"}")
            .when().post("/notify/login")
            .then().statusCode(401);
    }

    @Test
    void notifyLogin_missingChatId_returns400() {
        given()
            .header("Authorization", VALID_AUTH)
            .contentType("application/json")
            .body("{\"guestId\":\"g1\"}")
            .when().post("/notify/login")
            .then().statusCode(400);
    }

    @Test
    void notifyLogin_validRequest_sendsAndReturns200() {
        given()
            .header("Authorization", VALID_AUTH)
            .contentType("application/json")
            .body("{\"chatId\":1,\"guestId\":\"g1\",\"lang\":\"ru\"}")
            .when().post("/notify/login")
            .then().statusCode(200)
            .body("ok", org.hamcrest.Matchers.is(true));
    }
}
