package ru.LevLezhnin.NauJava.controller.admin;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.controller.base.RestAssuredBaseTest;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("FileAdminController REST API Integration Tests")
@ActiveProfiles("test")
class FileAdminControllerRestAssuredTest extends RestAssuredBaseTest {

    private static final String ADMIN_FILES_PATH = "/api/v1/admin/files";
    private static final String TEST_FILE_CONTENT = "Admin test file content";
    private static final String TEST_FILE_NAME = "admin_test.pdf";
    private static final String TEST_CONTENT_TYPE = "application/pdf";

    @Autowired
    private FileRepository fileRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Позитивный: администратор выполняет поиск файлов по authorId")
    void shouldReturn200_whenAdminSearchFilesByAuthorId() {
        String adminUsername = "admin_files_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "file_author_" + UUID.randomUUID();
        String userEmail = userUsername + "@example.com";
        String userPassword = "UserP@ssw0rd123";
        registerUser(userUsername, userEmail, userPassword);
        String userToken = loginWithResponse(userUsername, userPassword)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String fileId = uploadTestFileAndGetId(userToken);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "authorId")
                .queryParam("search", extractAuthorIdFromFile(fileId))
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(is(not(empty())))
                .body("[0].author_username", equalTo(userUsername));
    }

    @Test
    @DisplayName("Позитивный: администратор выполняет поиск файлов по fileName")
    void shouldReturn200_whenAdminSearchFilesByFileName() {
        String adminUsername = "admin_search_name_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String uniqueFileName = "unique_search_" + UUID.randomUUID() + ".txt";
        String userUsername = "search_user_" + UUID.randomUUID();
        registerUser(userUsername, userUsername + "@example.com", "UserP@ss123");
        String userToken = loginWithResponse(userUsername, "UserP@ss123")
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        uploadTestFileWithName(userToken, uniqueFileName, "text/plain");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "fileNameContains")
                .queryParam("search", uniqueFileName)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("[0].file_name", equalTo(uniqueFileName));
    }

    @Test
    @DisplayName("Позитивный: администратор получает пустой список при отсутствии совпадений")
    void shouldReturn200WithEmptyList_whenAdminSearchFilesNoMatches() {
        String adminUsername = "admin_empty_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "fileNameContains")
                .queryParam("search", "nonexistent_file_xyz_12345")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Позитивный: пагинация работает корректно")
    void shouldReturnPaginatedResults_whenAdminSearchFilesWithPagination() {
        String adminUsername = "admin_pagination_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        String userUsername = "pagination_user_" + UUID.randomUUID();
        registerUser(userUsername, userUsername + "@example.com", "UserP@ss123");
        String userToken = loginWithResponse(userUsername, "UserP@ss123")
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        for (int i = 0; i < 5; i++) {
            uploadTestFileWithName(userToken, "page_test_" + i + ".txt", "text/plain");
        }

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "fileNameContains")
                .queryParam("search", "page_test")
                .queryParam("page", 0)
                .queryParam("page_size", 2)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(lessThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Негативный: поиск файлов без аутентификации - 401")
    void shouldReturn401_whenSearchFilesWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .queryParam("searchBy", "fileName")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: поиск файлов пользователем без роли ADMIN - 403")
    void shouldReturn403_whenNonAdminSearchFiles() {
        String username = "regular_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "UserP@ssw0rd123";
        registerUser(username, email, password);
        String token = loginWithResponse(username, password)
                .getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, token)
                .queryParam("searchBy", "fileNameContains")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(403)
                .body("message", equalTo("Запрещён доступ к ресурсу"));
    }

    @Test
    @DisplayName("Негативный: поиск файлов с невалидным searchBy - 400")
    void shouldReturn400_whenSearchFilesWithInvalidSearchBy() {
        String adminUsername = "admin_invalid_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "invalidCriteria_xyz")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(400)
                .body("message", containsString("search_by"));
    }

    @Test
    @DisplayName("Негативный: поиск файлов с отрицательным номером страницы - 400")
    void shouldReturn400_whenSearchFilesWithNegativePage() {
        String adminUsername = "admin_neg_page_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "fileName")
                .queryParam("search", "test")
                .queryParam("page", -1)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: поиск файлов с невалидным page_size - 400")
    void shouldReturn400_whenSearchFilesWithInvalidPageSize() {
        String adminUsername = "admin_neg_size_" + UUID.randomUUID();
        String adminEmail = adminUsername + "@example.com";
        String adminPassword = "AdminP@ssw0rd123";
        String adminToken = registerAndLoginAsAdmin(adminUsername, adminEmail, adminPassword);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, adminToken)
                .queryParam("searchBy", "fileName")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 0)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Негативный: поиск файлов с невалидным токеном - 401")
    void shouldReturn401_whenSearchFilesWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, "invalid_token_12345")
                .queryParam("searchBy", "fileName")
                .queryParam("search", "test")
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(ADMIN_FILES_PATH)
                .then()
                .statusCode(401);
    }

    private String uploadTestFileAndGetId(String accessToken) {
        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10
                }
                """;

        return given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post("/api/v1/files")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private void uploadTestFileWithName(String accessToken, String fileName, String contentType) {
        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10
                }
                """;

        given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", fileName, "Test content".getBytes(), contentType)
                .when()
                .post("/api/v1/files")
                .then()
                .statusCode(201);
    }

    private String extractAuthorIdFromFile(String fileId) {
        return fileRepository.findWithDetailsById(UUID.fromString(fileId)).orElseThrow().getAuthor().getId().toString();
    }
}