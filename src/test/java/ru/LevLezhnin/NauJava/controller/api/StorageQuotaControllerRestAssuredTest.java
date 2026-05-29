package ru.LevLezhnin.NauJava.controller.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.controller.base.RestAssuredBaseTest;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserBan;
import ru.LevLezhnin.NauJava.model.UserRole;
import ru.LevLezhnin.NauJava.repository.jpa.UserBanRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("StorageQuotaController REST API Integration Tests")
@ActiveProfiles("test")
class StorageQuotaControllerRestAssuredTest extends RestAssuredBaseTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserBanRepository userBanRepository;

    private static final String QUOTAS_PATH = "/api/v1/quotas";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Позитивный: получение квоты хранилища для аутентифицированного пользователя")
    void shouldReturn200_whenGetStorageQuotaWithValidToken() {
        String username = "quota_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(QUOTAS_PATH + "/my")
                .then()
                .statusCode(200)
                .body("used_storage_bytes", notNullValue())
                .body("max_storage_bytes", notNullValue());
    }

    @Test
    @DisplayName("Позитивный: проверка формата значений квоты (строки)")
    void shouldReturnQuotaValuesAsString() {
        String username = "quota_format_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(QUOTAS_PATH + "/my")
                .then()
                .statusCode(200)
                .body("used_storage_bytes", instanceOf(String.class))
                .body("max_storage_bytes", instanceOf(String.class));
    }

    @Test
    @DisplayName("Негативный: получение квоты без аутентификации - 401")
    void shouldReturn401_whenGetQuotaWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .get(QUOTAS_PATH + "/my")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: получение квоты с невалидным токеном - 401")
    void shouldReturn401_whenGetQuotaWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, "invalid_token_xyz")
                .when()
                .get(QUOTAS_PATH + "/my")
                .then()
                .statusCode(401)
                .body("message", anyOf(
                        equalTo("Невалидный access токен"),
                        equalTo("Истёк срок действия access токена")
                ));
    }

    @Test
    @DisplayName("Негативный: получение квоты заблокированным пользователем - 403")
    void shouldReturn403_whenUserIsBanned() {
        String username = "banned_quota_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String adminUsername = "admin_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        registerUser(adminUsername, adminEmail, password);

        User admin = userRepository.findByUsername(adminUsername).orElseThrow();
        admin.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(admin);
        assertEquals(UserRole.ADMIN, admin.getRole());

        User user = userRepository.findByUsername(username).orElseThrow();
        UserBan ban = userBanRepository.saveAndFlush(
                UserBan.builder()
                        .setBannedUser(user)
                        .setAdmin(admin)
                        .setBannedAt(Instant.now())
                        .setUnbannedAt(null)
                        .setReason("Тестовая блокировка")
                        .build()
        );
        assertNotNull(ban);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(QUOTAS_PATH + "/my")
                .then()
                .statusCode(403)
                .body("error", equalTo("Ваш аккаунт заблокирован"));
    }

    @Test
    @DisplayName("Полный сценарий: регистрация, получение квоты, проверка значений")
    void shouldHandleFullQuotaFlow() {
        String username = "full_quota_flow_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";

        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        var response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(QUOTAS_PATH + "/my")
                .then()
                .statusCode(200)
                .extract();

        String usedBytes = response.path("used_storage_bytes");
        String maxBytes = response.path("max_storage_bytes");

        assertThat(usedBytes).matches("\\d+");
        assertThat(maxBytes).matches("\\d+");

        assertThat(Long.parseLong(usedBytes)).isLessThanOrEqualTo(Long.parseLong(maxBytes));
    }
}