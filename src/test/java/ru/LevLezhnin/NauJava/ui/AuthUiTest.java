package ru.LevLezhnin.NauJava.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.LevLezhnin.NauJava.constants.ContainerVersionConstants;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("ui")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UI-тесты авторизации")
@ActiveProfiles("test")
@Testcontainers
class AuthUiTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(ContainerVersionConstants.POSTGRES_CONTAINER_VERSION);

    @LocalServerPort
    private int port;

    private static final String LOGIN_PAGE_PATH = "/login";
    private static final String FILES_PAGE_PATH = "/files";

    private static final String USERNAME_FIELD_ID = "loginUsername";
    private static final String PASSWORD_FIELD_ID = "loginPassword";
    private static final String LOGIN_BUTTON_XPATH = "//button[text() = 'Войти']";
    private static final String LOGOUT_BUTTON_XPATH = "//button[text() = 'Выйти']";

    // Тестовые данные
    private static final String TEST_USERNAME = "test_user_ui";
    private static final String TEST_PASSWORD = "UiTest123!";

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setUpClass(@LocalServerPort int port) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
            {
              "username": "%s",
              "email": "ui-test@example.com",
              "password": "%s"
            }
            """.formatted(TEST_USERNAME, TEST_PASSWORD))
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200);
    }

    @AfterAll
    static void tearDownClass() {
        if (driver != null) driver.quit();
    }

    @BeforeEach
    void setUp() {
        driver.manage().deleteAllCookies();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @Order(1)
    @DisplayName("Успешный вход в приложение")
    void shouldRedirectToHomePage_whenLoginSuccess() {
        driver.get(url(LOGIN_PAGE_PATH));
        wait.until(ExpectedConditions.urlContains(LOGIN_PAGE_PATH));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(USERNAME_FIELD_ID)))
                .sendKeys(TEST_USERNAME);
        driver.findElement(By.id(PASSWORD_FIELD_ID)).sendKeys(TEST_PASSWORD);
        driver.findElement(By.xpath(LOGIN_BUTTON_XPATH)).click();

        wait.until(ExpectedConditions.urlToBe(url(FILES_PAGE_PATH)));

        assertEquals(url(FILES_PAGE_PATH), driver.getCurrentUrl());
    }

    @Test
    @Order(2)
    @DisplayName("Выход из приложения")
    void shouldRedirectToHomePage_whenLogoutSuccess() {
        driver.get(url(LOGIN_PAGE_PATH));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(USERNAME_FIELD_ID)))
                .sendKeys(TEST_USERNAME);
        driver.findElement(By.id(PASSWORD_FIELD_ID)).sendKeys(TEST_PASSWORD);
        driver.findElement(By.xpath(LOGIN_BUTTON_XPATH)).click();
        wait.until(ExpectedConditions.urlToBe(url(FILES_PAGE_PATH)));

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(LOGOUT_BUTTON_XPATH))).click();
        wait.until(ExpectedConditions.urlContains(LOGIN_PAGE_PATH));

        assertEquals(url(LOGIN_PAGE_PATH), driver.getCurrentUrl());
    }
}