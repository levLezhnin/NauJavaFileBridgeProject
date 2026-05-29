package ru.LevLezhnin.NauJava.controller.base;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.config.MinIOTestContainers;
import ru.LevLezhnin.NauJava.config.PostgresTestContainers;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserRole;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.security.properties.JwtProperties;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(MinIOTestContainers.class)
@ExtendWith(PostgresTestContainers.class)
public abstract class RestAssuredBaseTest {

    private static final String AUTH_PATH = "/api/v1/auth";

    @LocalServerPort
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    protected Response loginWithResponse(String username, String password) {
        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(loginBody)
                .when()
                .post(AUTH_PATH + "/login");
    }

    protected void registerUser(String username, String email, String password) {
        String body = """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .statusCode(200);
    }

    protected String registerAndLoginAsAdmin(String username, String email, String password) {
        registerUser(username, email, password);

        User admin = userRepository.findByUsername(username).orElseThrow();
        admin.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(admin);
        assertEquals(UserRole.ADMIN, admin.getRole());

        return loginWithResponse(username, password).getCookie(JwtProperties.JWT_ACCESS_TOKEN_COOKIE_NAME);
    }
}
