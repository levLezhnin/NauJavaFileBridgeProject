package ru.LevLezhnin.NauJava.controller.api;

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
@DisplayName("FileController REST API Integration Tests")
@ActiveProfiles("test")
class FileControllerRestAssuredTest extends RestAssuredBaseTest {

    private static final String FILES_PATH = "/api/v1/files";

    private static final String TEST_FILE_CONTENT = "Hello, this is a test file content!";
    private static final String TEST_FILE_NAME = "test_document.pdf";
    private static final String TEST_CONTENT_TYPE = "application/pdf";

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Позитивный: загрузка файла с валидными параметрами")
    void shouldReturn201_whenUploadFileWithValidData() {
        String username = "upload_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10,
                  "password": "SecurePass123"
                }
                """;

        given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("file_name", equalTo(TEST_FILE_NAME))
                .body("file_size_bytes", equalTo(TEST_FILE_CONTENT.length()))
                .body("has_password", equalTo(true))
                .body("upload_date", notNullValue())
                .body("expire_date", notNullValue());
    }

    @Test
    @DisplayName("Позитивный: загрузка файла без пароля")
    void shouldReturn201_whenUploadFileWithoutPassword() {
        String username = "upload_nopass_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 5
                }
                """;

        given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", "report.txt", "Simple text content".getBytes(), "text/plain")
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(201)
                .body("has_password", equalTo(false));
    }

    @Test
    @DisplayName("Негативный: загрузка файла без аутентификации - 401")
    void shouldReturn401_whenUploadFileWithoutAuth() {
        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10
                }
                """;

        given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: загрузка файла с невалидным TTL - 400")
    void shouldReturn400_whenUploadFileWithInvalidTTL() {
        String username = "invalid_ttl_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String payload = """
                {
                  "ttl_minutes": 10,
                  "max_downloads": 10
                }
                """;

        given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(400)
                .body("error", equalTo("Запрос не прошёл валидацию"))
                .body("fieldErrors", hasItem(hasEntry("field", "ttlMinutes")));
    }

    @Test
    @DisplayName("Негативный: загрузка файла с невалидным именем - 400")
    void shouldReturn400_whenUploadFileWithInvalidFileName() {
        String username = "invalid_name_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

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
                .multiPart("file", "CON.txt", TEST_FILE_CONTENT.getBytes(), "text/plain")
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(400)
                .body("error", equalTo("Запрос не прошёл валидацию"));
    }

    @Test
    @DisplayName("Негативный: загрузка файла с невалидным паролем - 400")
    void shouldReturn400_whenUploadFileWithInvalidPassword() {
        String username = "invalid_pass_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10,
                  "password": "short"
                }
                """;

        given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(400)
                .body("error", equalTo("Запрос не прошёл валидацию"))
                .body("fieldErrors", hasItem(hasEntry("field", "password")));
    }

    @Test
    @DisplayName("Позитивный: получение списка файлов пользователя")
    void shouldReturn200_whenGetUserFiles() {
        String username = "list_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        uploadTestFile(accessToken);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(FILES_PATH + "/my")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(is(not(empty())));
    }

    @Test
    @DisplayName("Позитивный: получение пустого списка файлов")
    void shouldReturn200_whenGetUserFilesEmpty() {
        String username = "empty_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(FILES_PATH + "/my")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Негативный: получение списка файлов без аутентификации - 401")
    void shouldReturn401_whenGetUserFilesWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("page_size", 10)
                .when()
                .get(FILES_PATH + "/my")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Негативный: получение списка файлов с некорректной пагинацией - 400")
    void shouldReturn400_whenGetUserFilesWithInvalidPagination() {
        String username = "pagination_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .queryParam("page", -1)
                .queryParam("page_size", 10)
                .when()
                .get(FILES_PATH + "/my")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Позитивный: получение метаданных файла по ID")
    void shouldReturn200_whenGetFileMetadata() {
        String username = "metadata_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String fileId = uploadTestFileAndGetId(accessToken);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("file_name", equalTo(TEST_FILE_NAME));
    }

    @Test
    @DisplayName("Негативный: получение метаданных несуществующего файла - 404")
    void shouldReturn404_whenGetNonExistentFile() {
        String username = "notfound_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String nonExistentId = UUID.randomUUID().toString();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/" + nonExistentId)
                .then()
                .statusCode(404)
                .body("message", containsString("не найден"));
    }

    @Test
    @DisplayName("Негативный: получение метаданных с невалидным UUID - 400")
    void shouldReturn400_whenGetFileWithInvalidUUID() {
        String username = "invalid_uuid_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/not-a-uuid")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Позитивный: получение ссылки на скачивание владельцем")
    void shouldReturn200_whenOwnerGetsDownloadLink() {
        String username = "link_owner_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String fileId = uploadTestFileAndGetId(accessToken);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/link/" + fileId)
                .then()
                .statusCode(200)
                .body("download_link", notNullValue())
                .body("download_link", containsString(fileId));
    }

    @Test
    @DisplayName("Негативный: получение ссылки на чужой файл - 403")
    void shouldReturn403_whenNonOwnerGetsDownloadLink() {
        String ownerUsername = "link_owner_" + UUID.randomUUID();
        String ownerEmail = ownerUsername + "@example.com";
        String ownerPassword = "StrongP@ssw0rd123";
        registerUser(ownerUsername, ownerEmail, ownerPassword);
        String ownerToken = loginWithResponse(ownerUsername, ownerPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        String fileId = uploadTestFileAndGetId(ownerToken);

        String otherUsername = "link_other_" + UUID.randomUUID();
        String otherEmail = otherUsername + "@example.com";
        String otherPassword = "OtherP@ssw0rd456";
        registerUser(otherUsername, otherEmail, otherPassword);
        String otherToken = loginWithResponse(otherUsername, otherPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, otherToken)
                .when()
                .get(FILES_PATH + "/link/" + fileId)
                .then()
                .statusCode(403)
                .body("message", containsString("только его владельцу"));
    }

    @Test
    @DisplayName("Позитивный: скачивание файла без пароля")
    void shouldReturn200_whenDownloadFileWithoutPassword() {
        String username = "download_user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String fileId = uploadTestFileAndGetId(accessToken);

        String downloadBody = """
                {
                  "file_id": "%s"
                }
                """.formatted(fileId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(downloadBody)
                .when()
                .post(FILES_PATH + "/download")
                .then()
                .statusCode(200)
                .header("Content-Disposition", containsString(TEST_FILE_NAME))
                .body(notNullValue());
    }

    @Test
    @DisplayName("Позитивный: скачивание файла с правильным паролем")
    void shouldReturn200_whenDownloadFileWithCorrectPassword() {
        String username = "download_pass_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10,
                  "password": "FilePass123"
                }
                """;
        String fileId = given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String downloadBody = """
                {
                  "file_id": "%s",
                  "password": "FilePass123"
                }
                """.formatted(fileId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(downloadBody)
                .when()
                .post(FILES_PATH + "/download")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Негативный: скачивание с неверным паролем - 400")
    void shouldReturn400_whenDownloadFileWithWrongPassword() {
        String username = "download_wrong_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String payload = """
                {
                  "ttl_minutes": 1440,
                  "max_downloads": 10,
                  "password": "CorrectPass123"
                }
                """;
        String fileId = given()
                .contentType(ContentType.MULTIPART)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .multiPart("payload", payload, "application/json")
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String downloadBody = """
                {
                  "file_id": "%s",
                  "password": "WrongPassword"
                }
                """.formatted(fileId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(downloadBody)
                .when()
                .post(FILES_PATH + "/download")
                .then()
                .statusCode(400)
                .body("message", equalTo("Указан неверный пароль для скачивания файла"));
    }

    @Test
    @DisplayName("Негативный: скачивание несуществующего файла - 404")
    void shouldReturn404_whenDownloadNonExistentFile() {
        String username = "download_404_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String downloadBody = """
                {
                  "file_id": "%s"
                }
                """.formatted(UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(downloadBody)
                .when()
                .post(FILES_PATH + "/download")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Позитивный: удаление файла владельцем")
    void shouldReturn204_whenOwnerDeletesFile() {
        String username = "delete_owner_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String fileId = uploadTestFileAndGetId(accessToken);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .delete(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Негативный: удаление чужого файла - 403")
    void shouldReturn403_whenDeletingOtherUserFile() {
        String ownerUsername = "delete_owner_" + UUID.randomUUID();
        String ownerEmail = ownerUsername + "@example.com";
        String ownerPassword = "StrongP@ssw0rd123";
        registerUser(ownerUsername, ownerEmail, ownerPassword);
        String ownerToken = loginWithResponse(ownerUsername, ownerPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
        String fileId = uploadTestFileAndGetId(ownerToken);

        String otherUsername = "delete_other_" + UUID.randomUUID();
        String otherEmail = otherUsername + "@example.com";
        String otherPassword = "OtherP@ssw0rd456";
        registerUser(otherUsername, otherEmail, otherPassword);
        String otherToken = loginWithResponse(otherUsername, otherPassword).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, otherToken)
                .when()
                .delete(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(403)
                .body("message", containsString("только его владельцу"));
    }

    @Test
    @DisplayName("Негативный: удаление без аутентификации - 401")
    void shouldReturn401_whenDeleteFileWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .delete(FILES_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Полный сценарий: загрузка, получение метаданных, скачивание, удаление")
    void shouldHandleFullFileLifecycle() {
        String username = "full_flow_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "StrongP@ssw0rd123";
        registerUser(username, email, password);
        String accessToken = loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);

        String fileId = uploadTestFileAndGetId(accessToken);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/link/" + fileId)
                .then()
                .statusCode(200)
                .body("download_link", notNullValue());

        String downloadBody = """
                {
                  "file_id": "%s"
                }
                """.formatted(fileId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .body(downloadBody)
                .when()
                .post(FILES_PATH + "/download")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .delete(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .cookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .when()
                .get(FILES_PATH + "/" + fileId)
                .then()
                .statusCode(404);
    }

    private void uploadTestFile(String accessToken) {
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
                .multiPart("file", TEST_FILE_NAME, TEST_FILE_CONTENT.getBytes(), TEST_CONTENT_TYPE)
                .when()
                .post(FILES_PATH)
                .then()
                .statusCode(201);
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
                .post(FILES_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}