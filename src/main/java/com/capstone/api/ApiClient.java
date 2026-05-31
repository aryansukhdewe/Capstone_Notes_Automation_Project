package com.capstone.api;

import com.capstone.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;

/**
 * ApiClient — RestAssured base configuration and all Notes API operations.
 *
 * WHY STATIC METHODS:
 * All API operations are static so they can be called as ApiClient.login(...)
 * without creating an instance — useful in @BeforeClass setup where we just
 * need a token quickly.
 *
 * WHY ALSO INSTANTIABLE:
 * PerformanceTests does new ApiClient() and calls apiClient.loginAndGetToken()
 * so it can store state (token) in the instance. The instance methods below
 * simply delegate to the static ones — no duplicate logic.
 *
 * FIX APPLIED (compilation error):
 * Java does NOT allow a static method and an instance method with identical
 * signatures in the same class — it is a compile error regardless of the
 * static modifier. The previous version had:
 *   static login(String, String)  AND  instance login(String, String)  → ERROR
 * Solution: instance methods are renamed to avoid signature collision:
 *   loginAndGetToken()  instead of  login()
 *   createNoteInstance() instead of  createNote()
 *   deleteNoteInstance() instead of  deleteNote()
 * All instance callers (PerformanceTests) use the renamed variants.
 *
 * WHY REQUESTSPECIFICATION:
 * Shared base config (baseURI, Content-Type, Allure filter) defined once.
 * Every call inherits it. Changing the API URL means updating config.properties
 * — not 30 test methods.
 *
 * WHY ALLURERESTASSURED FILTER:
 * Automatically attaches every HTTP request + response to the Allure report.
 * Zero per-test logging code needed.
 */
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);
    private static final ConfigManager config = ConfigManager.getInstance();

    // Lazy-initialised — built once, reused across all calls
    private static RequestSpecification baseRequestSpec;

    static {
        RestAssured.baseURI = config.getApiBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // =========================================================================
    // SPEC BUILDERS
    // =========================================================================

    public static RequestSpecification getBaseSpec() {
        if (baseRequestSpec == null) {
            baseRequestSpec = new RequestSpecBuilder()
                .setBaseUri(config.getApiBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())   // auto-attaches req/resp to Allure
                .log(LogDetail.ALL)
                .build();
        }
        return baseRequestSpec;
    }

    /**
     * Auth spec: base spec + x-auth-token header.
     * Every authenticated call uses this instead of repeating the header.
     */
    public static RequestSpecification getAuthSpec(String token) {
        return new RequestSpecBuilder()
            .addRequestSpecification(getBaseSpec())
            .addHeader("x-auth-token", token)
            .build();
    }

    /**
     * Global response spec: status 200 + response time < SLA.
     * Tests that expect success call .then().spec(getOkResponseSpec()) to get
     * FR-08 performance assertion for free on every request.
     */
    public static ResponseSpecification getOkResponseSpec() {
        return new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectResponseTime(lessThan((long) config.getApiTimeout()))
            .build();
    }

    // =========================================================================
    // AUTH  — STATIC
    // =========================================================================

    public static Response login(String email, String password) {
        log.info("API POST /users/login — email: {}", email);
        return given()
            .spec(getBaseSpec())
            .body("{ \"email\": \"" + email + "\", \"password\": \"" + password + "\" }")
        .when()
            .post("/users/login")
        .then()
            .log().ifError()
            .extract().response();
    }

    public static Response register(String name, String email, String password) {
        log.info("API POST /users — register: {}", email);
        return given()
            .spec(getBaseSpec())
            .body("{ \"name\": \"" + name + "\", \"email\": \"" + email
                  + "\", \"password\": \"" + password + "\" }")
        .when()
            .post("/users")
        .then()
            .log().ifError()
            .extract().response();
    }

    // =========================================================================
    // NOTES  — STATIC
    // =========================================================================

    public static Response getAllNotes(String token) {
        log.info("API GET /notes");
        return given()
            .spec(getAuthSpec(token))
        .when()
            .get("/notes")
        .then()
            .log().ifError()
            .extract().response();
    }

    public static Response createNote(String token, String category,
                                      String title, String description) {
        log.info("API POST /notes — title: {}", title);
        String body = String.format(
            "{ \"category\": \"%s\", \"title\": \"%s\", \"description\": \"%s\", \"completed\": false }",
            category, title, description);
        return given()
            .spec(getAuthSpec(token))
            .body(body)
        .when()
            .post("/notes")
        .then()
            .log().ifError()
            .extract().response();
    }

    public static Response getNoteById(String token, String noteId) {
        log.info("API GET /notes/{}", noteId);
        return given()
            .spec(getAuthSpec(token))
        .when()
            .get("/notes/" + noteId)
        .then()
            .log().ifError()
            .extract().response();
    }

    public static Response updateNote(String token, String noteId,
                                      String title, String description, boolean completed) {
        log.info("API PUT /notes/{}", noteId);
        String body = String.format(
            "{ \"title\": \"%s\", \"description\": \"%s\", \"completed\": %b }",
            title, description, completed);
        return given()
            .spec(getAuthSpec(token))
            .body(body)
        .when()
            .put("/notes/" + noteId)
        .then()
            .log().ifError()
            .extract().response();
    }

    public static Response deleteNote(String token, String noteId) {
        log.info("API DELETE /notes/{}", noteId);
        return given()
            .spec(getAuthSpec(token))
        .when()
            .delete("/notes/" + noteId)
        .then()
            .log().ifError()
            .extract().response();
    }

    public static Response deleteAccount(String token) {
        log.warn("API DELETE /users/me — account deletion");
        return given()
            .spec(getAuthSpec(token))
        .when()
            .delete("/users/me")
        .then()
            .log().ifError()
            .extract().response();
    }

    // =========================================================================
    // INSTANCE CONVENIENCE METHODS
    // WHY RENAMED: Java prohibits instance + static methods with identical
    // signatures. These are renamed so they delegate to static methods without
    // any signature collision. PerformanceTests uses these instance versions.
    // =========================================================================

    /**
     * Instance login: calls static login(), extracts token, returns it.
     * Used by: PerformanceTests (@BeforeClass), any class that does new ApiClient().
     */
    public String loginAndGetToken(String email, String password) {
        Response response = ApiClient.login(email, password);
        return extractToken(response);
    }

    /**
     * Instance getNotes: delegates to static getAllNotes().
     * Named differently from static to avoid overloading confusion.
     */
    public Response getNotes(String token) {
        return ApiClient.getAllNotes(token);
    }

    /**
     * Instance createNote: delegates to static.
     * Renamed to avoid signature collision with static createNote().
     */
    public Response createNoteInstance(String token, String category,
                                       String title, String description) {
        return ApiClient.createNote(token, category, title, description);
    }

    /**
     * Instance deleteNote: delegates to static.
     * Renamed to avoid signature collision with static deleteNote().
     */
    public Response deleteNoteInstance(String token, String noteId) {
        return ApiClient.deleteNote(token, noteId);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Extracts the auth token from a login response.
     * Throws a clear RuntimeException if login failed — test fails with a
     * meaningful message instead of a NullPointerException 3 steps later.
     */
    public static String extractToken(Response loginResponse) {
        if (loginResponse.statusCode() != 200) {
            throw new RuntimeException(
                "Login failed — HTTP " + loginResponse.statusCode()
                + " | body: " + loginResponse.asString());
        }
        String token = loginResponse.jsonPath().getString("data.token");
        if (token == null || token.isBlank()) {
            throw new RuntimeException(
                "Token missing in login response: " + loginResponse.asString());
        }
        log.info("Token obtained (first 10): {}...",
            token.substring(0, Math.min(10, token.length())));
        return token;
    }

    /**
     * Extracts the note ID from a create-note response.
     */
    public static String extractNoteId(Response createResponse) {
        String id = createResponse.jsonPath().getString("data.id");
        if (id == null) {
            throw new RuntimeException(
                "Note ID missing in create response: " + createResponse.asString());
        }
        return id;
    }
}
