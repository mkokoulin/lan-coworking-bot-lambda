package com.lan.app.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class HealthResourceTest {

    @Test
    void health_returns200WithBody() {
        given()
            .when().get("/health")
            .then()
            .statusCode(200)
            .body(equalTo("OK, Boomer!"));
    }
}
