package ru.LevLezhnin.NauJava.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("AuthController REST API Tests")
@ActiveProfiles("test")
@Testcontainers
class AuthControllerRestAssuredTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    private static final String AUTH_PATH = "/api/v1/auth";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Негативный: пустое тело запроса → 400")
    void shouldReturn400_whenRegisterEmptyBody() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("")
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: отсутствие обязательных полей → 400")
    void shouldReturn400_whenRegisterMissingFields() {
        String body = """
                {
                  "username": "incomplete_user"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: неверные учетные данные → 401")
    void shouldReturn401_whenLoginWrongPassword() {
        String body = """
                {
                  "username": "non_existent_user",
                  "password": "wrong_password"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post(AUTH_PATH + "/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Пограничный случай: отсутствует кука refresh → 400/401")
    void shouldReturn400_whenMissingRefreshCookie() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(anyOf(is(400), is(401)));
    }

    @Test
    @DisplayName("Позитивный: выход из системы → 200")
    void shouldReturn200_whenLogout() {
        given()
                .when()
                .accept(ContentType.JSON)
                .post(AUTH_PATH + "/logout")
                .then()
                .statusCode(200);
    }
}