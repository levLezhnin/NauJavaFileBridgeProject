package ru.LevLezhnin.NauJava.controller.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.controller.base.RestAssuredBaseTest;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("AuthController REST API Integration Tests")
@ActiveProfiles("test")
class AuthControllerRestAssuredTest extends RestAssuredBaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String PROTECTED_PATH = "/api/v1/users/me";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Позитивный: успешная регистрация нового пользователя")
    void shouldReturn200_whenRegisterValidUser() {
        String uniqueUsername = "test_user_" + UUID.randomUUID().toString().substring(0, 8);

        Response response = registerUserWithResponse(uniqueUsername, "%s@example.com".formatted(uniqueUsername), "StrongP@ssw0rd123");
        response
                .then()
                .statusCode(200)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, not(emptyOrNullString()))
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, not(emptyOrNullString()));

        Cookies allCookies = response.getDetailedCookies();
        assertAll(allCookies.asList().stream().map(cookie -> () -> assertTrue(cookie::isHttpOnly)));

        assertThat(userRepository.findByUsername(uniqueUsername)).isPresent();
    }

    @Test
    @DisplayName("Негативный: регистрация с уже существующим username - 409")
    void shouldReturn409_whenRegisterDuplicateUsername() {
        String username = "duplicate_user_" + UUID.randomUUID();
        String email = "unique_" + UUID.randomUUID() + "@example.com";
        String password = "StrongP@ssw0rd123";

        registerUser(username, email, password);

        String duplicateBody = """
                {
                  "username": "%s",
                  "email": "another_%s@example.com",
                  "password": "%s"
                }
                """.formatted(username, UUID.randomUUID(), password);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(duplicateBody)
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(409)
                .body("error", equalTo("Логин уже занят другим пользователем"));
    }

    @Test
    @DisplayName("Негативный: регистрация с уже существующим email - 409")
    void shouldReturn409_whenRegisterDuplicateEmail() {
        String email = "duplicate_" + UUID.randomUUID() + "@example.com";
        String password = "StrongP@ssw0rd123";

        registerUser("first_user_" + UUID.randomUUID(), email, password);

        String duplicateBody = """
                {
                  "username": "another_user_%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(UUID.randomUUID(), email, password);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(duplicateBody)
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(409)
                .body("error", equalTo("Email уже занят другим пользователем"));
    }

    @Test
    @DisplayName("Позитивный: успешный вход с выдачей токенов")
    void shouldReturn200_whenLoginWithValidCredentials() {
        String username = "login_test_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";

        registerUser(username, email, password);

        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(loginBody)
                .when()
                .post(AUTH_PATH + "/login")
                .then()
                .statusCode(200)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, not(emptyOrNullString()))
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, not(emptyOrNullString()));
    }

    @Test
    @DisplayName("Негативный: вход с неверным паролем - 401")
    void shouldReturn401_whenLoginWithWrongPassword() {
        String username = "wrong_pass_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "CorrectP@ss123";
        String wrongPassword = "WrongP@ss456";

        registerUser(username, email, password);

        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, wrongPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(loginBody)
                .when()
                .post(AUTH_PATH + "/login")
                .then()
                .statusCode(401)
                .body("error", equalTo("Ошибка аутентификации"))
                .body("message", equalTo("Неверный логин или пароль"));
    }

    @Test
    @DisplayName("Негативный: вход с несуществующим пользователем - 401")
    void shouldReturn401_whenLoginWithNonExistentUser() {
        String loginBody = """
                {
                  "username": "non_existent_%s",
                  "password": "any_password"
                }
                """.formatted(UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(loginBody)
                .when()
                .post(AUTH_PATH + "/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Позитивный: успешное обновление access-токена")
    void shouldReturn200_whenRefreshWithValidRefreshToken() {
        String username = "refresh_test_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";

        Response response = registerUserWithResponse(username, email, password);
        String refreshToken = response.getCookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(200)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, not(emptyOrNullString()));
    }

    @Test
    @DisplayName("Негативный: рефреш с отсутствующей кукой - 400")
    void shouldReturn400_whenRefreshWithoutCookie() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: рефреш с невалидным токеном - 401")
    void shouldReturn401_whenRefreshWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, "invalid_token_12345")
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(401)
                .body("error", anyOf(
                        equalTo("Невалидный токен"),
                        equalTo("Истёк срок действия токена")
                ));
    }

    @Test
    @DisplayName("Позитивный: успешный выход с очисткой токенов")
    void shouldReturn200_whenLogoutWithValidTokens() {
        String username = "logout_test_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";

        registerUser(username, email, password);

        String accessToken = extractCookieValue(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        String refreshToken = extractCookieValue(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .when()
                .post(AUTH_PATH + "/logout")
                .then()
                .statusCode(200)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, emptyOrNullString())
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, emptyOrNullString());

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Позитивный: логаут без токенов не вызывает ошибку")
    void shouldReturn200_whenLogoutWithoutTokens() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(AUTH_PATH + "/logout")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Полный поток: register, login, access protected, refresh, logout")
    void shouldHandleFullAuthFlow() {
        String username = "flow_test_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";

        registerUser(username, email, password);

        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(loginBody)
                .when()
                .post(AUTH_PATH + "/login");

        loginResponse.then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME,
                        loginResponse.getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME))
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(200);

        String refreshToken = loginResponse.getCookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME);
        Response refreshResponse = given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .when()
                .post(AUTH_PATH + "/refresh");

        refreshResponse.then().statusCode(200);
        String newAccessToken = refreshResponse.getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, newAccessToken)
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, newAccessToken)
                .cookie(JwtProperties.JWT_REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .when()
                .post(AUTH_PATH + "/logout")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, newAccessToken)
                .when()
                .get(PROTECTED_PATH)
                .then()
                .statusCode(401);
    }

    private Response registerUserWithResponse(String username, String email, String password) {
        String body = """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password);

        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post(AUTH_PATH + "/register");
    }

    private String extractCookieValue(String cookieName) {
        return RestAssured.given()
                .post(AUTH_PATH + "/register")
                .getCookie(cookieName);
    }
}