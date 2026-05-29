package ru.LevLezhnin.NauJava.controller.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.controller.base.RestAssuredBaseTest;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserController REST API Integration Tests")
@ActiveProfiles("test")
class UserControllerRestAssuredTest extends RestAssuredBaseTest {

    @LocalServerPort
    private int port;

    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String USER_PATH = "/api/v1/users/me";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Позитивный: получение профиля текущего пользователя")
    void shouldReturn200_whenGetProfileWithValidToken() {
        String username = "profile_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(200)
                .body("username", equalTo(username))
                .body("email", equalTo(email))
                .body("role", notNullValue());
    }

    @Test
    @DisplayName("Негативный: получение профиля без аутентификации - 401")
    void shouldReturn401_whenGetProfileWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: получение профиля с невалидным токеном - 401")
    void shouldReturn401_whenGetProfileWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, "invalid_token_xyz")
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(401)
                .body("message", anyOf(
                        equalTo("Невалидный access токен"),
                        equalTo("Истёк срок действия access токена")
                ));
    }

    @Test
    @DisplayName("Позитивный: обновление логина пользователя")
    void shouldReturn200_whenUpdateUsername() {

        String username = "update_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String newUsername = "updated_user_" + UUID.randomUUID();
        String updateBody = """
                {
                  "new_username": "%s"
                }
                """.formatted(newUsername);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(200)
                .body("username", equalTo(newUsername));
    }

    @Test
    @DisplayName("Позитивный: обновление пароля пользователя")
    void shouldReturn200_whenUpdatePassword() {

        String username = "pass_update_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "OldP@ssw0rd123";
        String newPassword = "NewP@ssw0rd456";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String updateBody = """
                {
                  "current_password": "%s",
                  "new_password": "%s",
                  "confirm_new_password": "%s"
                }
                """.formatted(password, newPassword, newPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(200);

        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, newPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(loginBody)
                .when()
                .post(AUTH_PATH + "/login")
                .then()
                .statusCode(200)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, not(emptyOrNullString()));
    }

    @Test
    @DisplayName("Негативный: обновление с неверным текущим паролем - 400")
    void shouldReturn400_whenUpdatePasswordWithWrongCurrentPassword() {

        String username = "wrong_pass_update_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "CorrectP@ss123";
        String wrongPassword = "WrongP@ss456";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String updateBody = """
                {
                  "current_password": "%s",
                  "new_password": "NewP@ssw0rd789",
                  "confirm_new_password": "NewP@ssw0rd789"
                }
                """.formatted(wrongPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(400)
                .body("message", equalTo("Указан неверный текущий пароль"));
    }

    @Test
    @DisplayName("Негативный: обновление пароля на такой же - 400")
    void shouldReturn400_whenUpdatePasswordToSameValue() {

        String username = "same_pass_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "SameP@ssw0rd123";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String updateBody = """
                {
                  "current_password": "%s",
                  "new_password": "%s",
                  "confirm_new_password": "%s"
                }
                """.formatted(password, password, password);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(400)
                .body("message", equalTo("Новый пароль должен отличаться от старого"));
    }

    @Test
    @DisplayName("Негативный: confirm_new_password отличается от нового пароля - 400")
    void shouldReturn400_whenConfirmPasswordNotEqualToNewPassword() {

        String username = "confirm_pass_failed_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "C0nf1RmP@ssw0rd123";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String newPassword = "s0meN3wPas$w0rd";
        String failedNewPassword = "s0me4notherPas$w0rd";

        String updateBody = """
                {
                  "current_password": "%s",
                  "new_password": "%s",
                  "confirm_new_password": "%s"
                }
                """.formatted(password, newPassword, failedNewPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(400)
                .body("error", equalTo("Запрос не прошёл валидацию"))
                .body("fieldErrors[0].field", equalTo("confirm_new_password"))
                .body("fieldErrors[0].message", equalTo("Пароли не совпадают"));
    }

    @Test
    @DisplayName("Негативный: установка занятого логина - 409")
    void shouldReturn409_whenUpdateToTakenUsername() {

        String existingUsername = "taken_user_" + UUID.randomUUID();
        String existingEmail = existingUsername + "@example.com";
        registerUser(existingUsername, existingEmail, "StrongP@ssw0rd123");

        String username = "updater_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String updateBody = """
                {
                  "new_username": "%s"
                }
                """.formatted(existingUsername);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(409)
                .body("error", equalTo("Логин уже занят другим пользователем"));
    }

    @Test
    @DisplayName("Негативный: обновление без аутентификации - 401")
    void shouldReturn401_whenUpdateProfileWithoutAuth() {
        String updateBody = """
                {
                  "newUsername": "new_name"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Позитивный: удаление аккаунта текущего пользователя")
    void shouldReturn204_whenDeleteAccount() {

        String username = "delete_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);

        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .delete(USER_PATH)
                .then()
                .statusCode(204);

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
                .statusCode(401);

        assertThat(userRepository.findByUsername(username)).isEmpty();
    }

    @Test
    @DisplayName("Негативный: удаление аккаунта без аутентификации - 401")
    void shouldReturn401_whenDeleteAccountWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .delete(USER_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Полный сценарий: регистрация, получение профиля, обновление, удаление")
    void shouldHandleFullUserProfileFlow() {
        String username = "flow_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        String newUsername = "flow_updated_" + UUID.randomUUID();
        String newPassword = "NewFlowP@ss456";

        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(200)
                .body("username", equalTo(username));

        String updateUsernameBody = """
                {
                  "new_username": "%s"
                }
                """.formatted(newUsername);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updateUsernameBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(200);

        String updatePasswordBody = """
                {
                  "current_password": "%s",
                  "new_password": "%s",
                  "confirm_new_password": "%s"
                }
                """.formatted(password, newPassword, newPassword);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(updatePasswordBody)
                .when()
                .patch(USER_PATH)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(200)
                .body("username", equalTo(newUsername));

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .delete(USER_PATH)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(USER_PATH)
                .then()
                .statusCode(404);
    }
}