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
@DisplayName("UserAdminController REST API Integration Tests")
@ActiveProfiles("test")
class UserAdminControllerRestAssuredTest extends RestAssuredBaseTest {

    private static final String ADMIN_USERS_PATH = "/api/v1/admin/users";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Позитивный: администратор выполняет поиск пользователей по username")
    void shouldReturn200_whenAdminSearchUsersByUsername() {
        String adminUsername = "admin_users_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String targetUsername = "search_target_" + UUID.randomUUID();
        String targetEmail = targetUsername + "@example.com";
        String targetPassword = "TargetP@ss123";
        registerUser(targetUsername, targetEmail, targetPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "username")
                .queryParam("search", targetUsername)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(is(not(empty())))
                .body("[0].username", equalTo(targetUsername))
                .body("[0].email", equalTo(targetEmail))
                .body("[0].role", equalTo("USER"))
                .body("[0].is_active", equalTo(true));
    }

    @Test
    @DisplayName("Позитивный: администратор выполняет поиск пользователей по email")
    void shouldReturn200_whenAdminSearchUsersByEmail() {
        String adminUsername = "admin_email_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String targetEmail = "email_search_" + UUID.randomUUID() + "@example.com";
        String targetUsername = "user_by_email_" + UUID.randomUUID();
        registerUser(targetUsername, targetEmail, "UserP@ss123");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "email")
                .queryParam("search", targetEmail)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(200)
                .body("[0].email", equalTo(targetEmail))
                .body("[0].username", equalTo(targetUsername));
    }

    @Test
    @DisplayName("Позитивный: администратор выполняет поиск пользователей по role")
    void shouldReturn200_whenAdminSearchUsersByRole() {
        String adminUsername = "admin_role_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        for (int i = 0; i < 3; i++) {
            registerUser("role_user_" + i + "_" + UUID.randomUUID(),
                    "role" + i + "@example.com", "UserP@ss123");
        }

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "role")
                .queryParam("search", "USER")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("$", hasSize(greaterThanOrEqualTo(3)))
                .body("[0].role", equalTo("USER"));
    }

    @Test
    @DisplayName("Позитивный: администратор получает пустой список при отсутствии совпадений")
    void shouldReturn200WithEmptyList_whenAdminSearchUsersNoMatches() {
        String adminUsername = "admin_empty_users_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "username")
                .queryParam("search", "nonexistent_user_xyz_99999")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Позитивный: пагинация работает корректно при поиске пользователей")
    void shouldReturnPaginatedResults_whenAdminSearchUsersWithPagination() {
        String adminUsername = "admin_pag_users_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        for (int i = 0; i < 5; i++) {
            registerUser("pag_user_" + i + "_" + UUID.randomUUID(),
                    "pag" + i + "@example.com", "UserP@ss123");
        }

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "username")
                .queryParam("search", "pag_user")
                .queryParam("page", 0)
                .queryParam("page_size", 2)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(lessThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Негативный: поиск пользователей без аутентификации - 401")
    void shouldReturn401_whenSearchUsersWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .queryParam("searchBy", "username")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: поиск пользователей обычным пользователем - 403")
    void shouldReturn403_whenNonAdminSearchUsers() {
        String username = "regular_searcher_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "UserP@ssw0rd123";
        registerUser(username, email, password);
        String token = loginWithResponse(username, password)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, token)
                .queryParam("searchBy", "username")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(403)
                .body("message", equalTo("Запрещён доступ к ресурсу"));
    }

    @Test
    @DisplayName("Негативный: поиск пользователей с невалидным searchBy - 400")
    void shouldReturn400_whenSearchUsersWithInvalidSearchBy() {
        String adminUsername = "admin_invalid_search_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "unknownField_xyz")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(400)
                .body("message", containsString("search_by"));
    }

    @Test
    @DisplayName("Негативный: поиск пользователей с отрицательным page - 400")
    void shouldReturn400_whenSearchUsersWithNegativePage() {
        String adminUsername = "admin_neg_page_users_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "username")
                .queryParam("search", "test")
                .queryParam("page", -1)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: поиск пользователей с page_size = 0 - 400")
    void shouldReturn400_whenSearchUsersWithZeroPageSize() {
        String adminUsername = "admin_zero_size_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "username")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 0)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: поиск пользователей с невалидным токеном - 401")
    void shouldReturn401_whenSearchUsersWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, "expired_or_invalid_token")
                .queryParam("searchBy", "username")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Позитивный: проверка структуры ответа UserProfileAdminResponseDto")
    void shouldReturnCorrectResponseStructure_whenAdminSearchUsers() {
        String adminUsername = "admin_structure_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String targetUsername = "struct_user_" + UUID.randomUUID();
        registerUser(targetUsername, targetUsername + "@example.com", "UserP@ss123");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "username")
                .queryParam("search", targetUsername)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_USERS_PATH)
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].username", notNullValue())
                .body("[0].email", notNullValue())
                .body("[0].role", notNullValue())
                .body("[0].is_active", notNullValue())
                .body("[0].registered_at", notNullValue());
    }
}