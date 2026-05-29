package ru.LevLezhnin.NauJava.controller.admin;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.controller.base.RestAssuredBaseTest;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserBanController REST API Integration Tests")
@ActiveProfiles("test")
class UserBanControllerRestAssuredTest extends RestAssuredBaseTest {

    private static final String ADMIN_BAN_PATH = "/api/v1/admin/users";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private String registerAndLoginAsUser(String username, String email, String password) {
        registerUser(username, email, password);
        return loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
    }

    private String createBanRequestPayload(Long targetUserId, String reason) {
        return """
                {
                  "ban_user_id": %d,
                  "reason": "%s"
                }
                """.formatted(targetUserId, reason);
    }

    @Test
    @DisplayName("Позитивный: администратор блокирует обычного пользователя")
    void shouldReturn200_whenAdminBansRegularUser() {
        String adminUsername = "admin_ban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "banned_user_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        String reason = "Игнорирование правил сообщества: отправка файла с вирусами";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("banned_user_id", equalTo(userId.toString()))
                .body("banned_user_username", equalTo(userUsername))
                .body("admin_username", equalTo(adminUsername))
                .body("reason", equalTo(reason))
                .body("banned_at", notNullValue())
                .body("unbanned_at", emptyOrNullString());
    }

    @Test
    @DisplayName("Негативный: попытка заблокировать самого себя - 403")
    void shouldReturn403_whenAdminTriesToBanSelf() {
        String adminUsername = "admin_selfban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);
        Long adminId = userRepository.findByUsername(adminUsername).orElseThrow().getId();

        String reason = "Тестовая причина для само-бана";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(adminId, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(403)
                .body("message", containsString("Нельзя заблокировать самого себя"));
    }

    @Test
    @DisplayName("Негативный: попытка заблокировать другого администратора - 403")
    void shouldReturn403_whenAdminTriesToBanAnotherAdmin() {
        String admin1Username = "admin1_" + UUID.randomUUID();
        String admin1Email = admin1Username + "@example.com";
        String admin1Password = "Admin1P@ssw0rd";
        String admin1Token = registerAndLoginAsAdmin(admin1Username, admin1Email, admin1Password);

        String admin2Username = "admin2_" + UUID.randomUUID();
        String admin2Email = admin2Username + "@example.com";
        String admin2Password = "Admin2P@ssw0rd";
        registerAndLoginAsAdmin(admin2Username, admin2Email, admin2Password);
        Long admin2Id = userRepository.findByUsername(admin2Username).orElseThrow().getId();

        String reason = "Страшный конфликт администраторов";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, admin1Token)
                .body(createBanRequestPayload(admin2Id, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(403)
                .body("message", containsString("Нельзя заблокировать администратора"));
    }

    @Test
    @DisplayName("Негативный: попытка заблокировать уже заблокированного пользователя - 409")
    void shouldReturn409_whenBanningAlreadyBannedUser() {
        String adminUsername = "admin_doubleban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "doublebanned_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        String reason1 = "Первое игнорирование правил использования сервиса";
        String reason2 = "Повторное игнорирование правил использования сервиса";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, reason1))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, reason2))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(409)
                .body("message", containsString("уже заблокирован"));
    }

    @Test
    @DisplayName("Негативный: попытка заблокировать несуществующего пользователя - 404")
    void shouldReturn404_whenBanningNonExistentUser() {
        String adminUsername = "admin_404ban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        Long nonExistentUserId = 999_999_999L;
        String reason = "Попытка бана несуществующего";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(nonExistentUserId, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(404)
                .body("message", containsString("не найден"));
    }

    @Test
    @DisplayName("Негативный: валидация - причина короче 20 символов - 400")
    void shouldReturn400_whenBanReasonTooShort() {
        String adminUsername = "admin_shortreason_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "user_shortreason_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        String shortReason = "Коротко";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, shortReason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(400)
                .body("error", equalTo("Запрос не прошёл валидацию"))
                .body("fieldErrors", hasItem(hasEntry("field", "reason")));
    }

    @Test
    @DisplayName("Негативный: валидация - причина длиннее 500 символов - 400")
    void shouldReturn400_whenBanReasonTooLong() {
        String adminUsername = "admin_longreason_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "user_longreason_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        String longReason = "A".repeat(501);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, longReason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(400)
                .body("error", equalTo("Запрос не прошёл валидацию"))
                .body("fieldErrors", hasItem(hasEntry("field", "reason")));
    }

    @Test
    @DisplayName("Негативный: попытка бана без аутентификации - 401")
    void shouldReturn401_whenBanWithoutAuth() {
        String userUsername = "unauth_ban_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        String reason = "Тест без авторизации";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(createBanRequestPayload(userId, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: обычный пользователь пытается заблокировать - 403")
    void shouldReturn403_whenRegularUserTriesToBan() {
        String regularUsername = "regular_ban_" + UUID.randomUUID();
        String regularEmail = regularUsername + "@example.com";
        String regularPassword = "RegularP@ss123";
        String regularToken = registerAndLoginAsUser(regularUsername, regularEmail, regularPassword);

        String targetUsername = "target_user_" + UUID.randomUUID();
        String targetEmail = targetUsername + "@example.com";
        String targetPassword = "TargetP@ss123";
        registerUser(targetUsername, targetEmail, targetPassword);
        Long targetUserId = userRepository.findByUsername(targetUsername).orElseThrow().getId();

        String reason = "Попытка бана обычным пользователем";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, regularToken)
                .body(createBanRequestPayload(targetUserId, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Позитивный: администратор разблокирует пользователя")
    void shouldReturn200_whenAdminUnbansUser() {
        String adminUsername = "admin_unban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "tobebanned_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, "Временная блокировка для теста"))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .post(ADMIN_BAN_PATH + "/unban/" + userId)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("banned_user_id", equalTo(userId.toString()))
                .body("banned_user_username", equalTo(userUsername))
                .body("unbanned_at", notNullValue());
    }

    @Test
    @DisplayName("Негативный: попытка разблокировать самого себя - 403")
    void shouldReturn403_whenAdminTriesToUnbanSelf() {
        String adminUsername = "admin_selfunban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);
        Long adminId = userRepository.findByUsername(adminUsername).orElseThrow().getId();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .post(ADMIN_BAN_PATH + "/unban/" + adminId)
                .then()
                .statusCode(403)
                .body("message", containsString("Нельзя разблокировать самого себя"));
    }

    @Test
    @DisplayName("Негативный: попытка разблокировать незаблокированного пользователя - 404")
    void shouldReturn404_whenUnbanningNotBannedUser() {
        String adminUsername = "admin_unban404_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "notbanned_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .post(ADMIN_BAN_PATH + "/unban/" + userId)
                .then()
                .statusCode(400)
                .body("message", containsString("не заблокирован"));
    }

    @Test
    @DisplayName("Негативный: попытка разблокировать несуществующего пользователя - 404")
    void shouldReturn404_whenUnbanningNonExistentUser() {
        String adminUsername = "admin_unban404ne_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        long nonExistentUserId = 999_999_998L;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .post(ADMIN_BAN_PATH + "/unban/" + nonExistentUserId)
                .then()
                .statusCode(404)
                .body("message", containsString("не найден"));
    }

    @Test
    @DisplayName("Позитивный: получение блокировки по корректному ID")
    void shouldReturn200_whenGetBanById() {
        String adminUsername = "admin_getban_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "getban_user_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        String reason = "Тестовая причина для получения по ID";

        String banId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, reason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .get(ADMIN_BAN_PATH + "/bans/" + banId)
                .then()
                .statusCode(200)
                .body("id", equalTo(banId))
                .body("reason", equalTo(reason))
                .body("banned_user_username", equalTo(userUsername))
                .body("admin_username", equalTo(adminUsername));
    }

    @Test
    @DisplayName("Негативный: получение несуществующей блокировки по ID - 404")
    void shouldReturn404_whenGetNonExistentBanById() {
        String adminUsername = "admin_getban404_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String nonExistentBanId = "999999";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .get(ADMIN_BAN_PATH + "/bans/" + nonExistentBanId)
                .then()
                .statusCode(404)
                .body("message", containsString("не найдена"));
    }

    @Test
    @DisplayName("Позитивный: получение активной блокировки заблокированного пользователя")
    void shouldReturn200_whenGetActiveBanOfBannedUser() {
        String adminUsername = "admin_getactive_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "activeban_user_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, "Активная блокировка для теста"))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/" + userId)
                .then()
                .statusCode(200)
                .body("banned_user_id", equalTo(userId.toString()))
                .body("unbanned_at", emptyOrNullString());
    }

    @Test
    @DisplayName("Негативный: получение активной блокировки незаблокированного пользователя - 404")
    void shouldReturn404_whenGetActiveBanOfNotBannedUser() {
        String adminUsername = "admin_getactive404_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "notbanned_active_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/" + userId)
                .then()
                .statusCode(404)
                .body("message", containsString("не заблокирован"));
    }

    @Test
    @DisplayName("Позитивный: получение истории блокировок пользователя с пагинацией")
    void shouldReturn200_whenGetUserBanHistoryWithPagination() {
        String adminUsername = "admin_history_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "history_user_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

        for (int i = 1; i <= 3; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                    .body(createBanRequestPayload(userId, "История блокировка #" + i))
                    .when()
                    .post(ADMIN_BAN_PATH + "/ban")
                    .then()
                    .statusCode(200);

            if (i < 3) {
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                        .when()
                        .post(ADMIN_BAN_PATH + "/unban/" + userId)
                        .then()
                        .statusCode(200);
            }
        }

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/history/" + userId)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(is(not(empty())));
    }

    @Test
    @DisplayName("Негативный: получение истории несуществующего пользователя - 404")
    void shouldReturn404_whenGetHistoryOfNonExistentUser() {
        String adminUsername = "admin_history404_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        long nonExistentUserId = 999_999_997L;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/history/" + nonExistentUserId)
                .then()
                .statusCode(404)
                .body("message", containsString("не найден"));
    }

    @Test
    @DisplayName("Позитивный: получение истории выданных блокировок администратором")
    void shouldReturn200_whenGetIssuedBansByAdmin() {
        String adminUsername = "admin_issued_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);
        Long adminId = userRepository.findByUsername(adminUsername).orElseThrow().getId();

        for (int i = 1; i <= 2; i++) {
            String userUsername = "issued_target_" + i + "_" + UUID.randomUUID();
            String userEmail = userUsername + "@example.com";
            String userPassword = "UserP@ssw0rd123";
            registerUser(userUsername, userEmail, userPassword);
            Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();

            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                    .body(createBanRequestPayload(userId, "Выдано админом " + adminUsername + " #" + i))
                    .when()
                    .post(ADMIN_BAN_PATH + "/ban")
                    .then()
                    .statusCode(200);
        }

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/issuedBans/" + adminId)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(is(not(empty())));
    }

    @Test
    @DisplayName("Негативный: получение истории выданных блокировок для обычного пользователя - 400")
    void shouldReturn400_whenGetIssuedBansForRegularUser() {
        String adminUsername = "admin_issued400_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String regularUsername = "regular_issued_" + UUID.randomUUID();
        String regularEmail = regularUsername + "@example.com";
        String regularPassword = "RegularP@ss123";
        registerUser(regularUsername, regularEmail, regularPassword);
        Long regularUserId = userRepository.findByUsername(regularUsername).orElseThrow().getId();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/issuedBans/" + regularUserId)
                .then()
                .statusCode(400)
                .body("message", matchesPattern("Нельзя посмотреть историю выданных блокировок, так как пользователь с id: [\\d+] - пользователь"));
    }

    @Test
    @DisplayName("Интеграция: заблокированный пользователь получает 403 при доступе к защищённым эндпоинтам")
    void shouldReturn403_whenBannedUserAccessesProtectedEndpoint() {
        String adminUsername = "admin_filter_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "banned_filter_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();
        String userToken = loginWithResponse(userUsername, userPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, "Блокировка для теста фильтра"))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, userToken)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(403)
                .body("error", equalTo("Ваш аккаунт заблокирован"))
                .body("message", containsString("был заблокирован"));
    }

    @Test
    @DisplayName("Интеграция: заблокированный пользователь может выйти из системы (logout)")
    void shouldAllowLogout_whenUserIsBanned() {
        String adminUsername = "admin_filter_logout_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "banned_logout_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();
        String userToken = loginWithResponse(userUsername, userPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, "Блокировка перед логаутом"))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, userToken)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Полный сценарий: бан пользователя, проверка блокировки, разблокировка")
    void shouldHandleFullBanLifecycle() {
        String adminUsername = "admin_fullcycle_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "fullcycle_user_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        Long userId = userRepository.findByUsername(userUsername).orElseThrow().getId();
        String userToken = loginWithResponse(userUsername, userPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String banReason = "Полный цикл тестирования блокировок";

        String banId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .body(createBanRequestPayload(userId, banReason))
                .when()
                .post(ADMIN_BAN_PATH + "/ban")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/" + userId)
                .then()
                .statusCode(200)
                .body("id", equalTo(banId))
                .body("reason", equalTo(banReason));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, userToken)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(403);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .when()
                .post(ADMIN_BAN_PATH + "/unban/" + userId)
                .then()
                .statusCode(200)
                .body("unbanned_at", notNullValue());

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, userToken)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("username", equalTo(userUsername));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_BAN_PATH + "/ban/history/" + userId)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].reason", equalTo(banReason))
                .body("[0].unbanned_at", notNullValue());
    }
}