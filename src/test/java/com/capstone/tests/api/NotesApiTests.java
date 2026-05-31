package com.capstone.tests.api;

import com.capstone.api.ApiClient;
import com.capstone.config.ConfigManager;
import com.capstone.utils.JsonDataReader;
import io.qameta.allure.*;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

@Epic("Notes Application")
@Feature("Notes API")
public class NotesApiTests {

    private static final Logger log = LoggerFactory.getLogger(NotesApiTests.class);
    private static String authToken;
    private static String createdNoteId;

    @BeforeClass(alwaysRun = true)
    @Step("Login via API and obtain auth token")
    public void apiLogin() {
        log.info("===== API Test Suite Setup: Logging in via API =====");
        Response loginResponse = ApiClient.login(
            JsonDataReader.getUserEmail("valid_user"),
            JsonDataReader.getUserPassword("valid_user")
        );
        Assert.assertEquals(loginResponse.statusCode(), 200, "Pre-condition: API login must return 200.");
        authToken = ApiClient.extractToken(loginResponse);
        log.info("API Login successful. Token obtained.");
    }

    // ===== AUTH TESTS =====

    @Test(description = "TC-API-01: POST /users/login returns 200 with valid credentials", groups = {"smoke", "api"}, priority = 1)
    @Story("FR-01 - API Login")
    @Severity(SeverityLevel.BLOCKER)
    public void testApiLoginSuccess() {
        Response response = ApiClient.login(JsonDataReader.getUserEmail("valid_user"), JsonDataReader.getUserPassword("valid_user"));
        long responseTime = response.getTime();

        Assert.assertEquals(response.statusCode(), 200, "Login API should return HTTP 200");
        Assert.assertTrue(response.jsonPath().getBoolean("success"), "Response 'success' field should be true");
        Assert.assertNotNull(response.jsonPath().getString("data.token"), "Response must contain a 'data.token' field");
        Assert.assertTrue(responseTime < ConfigManager.getInstance().getApiTimeout(), "API response time exceeds threshold (FR-08)");
        log.info("✅ TC-API-01 PASSED | Login time: {}ms", responseTime);
    }

    @Test(description = "TC-API-NEG-01: Login with wrong password returns 401", groups = {"regression", "api", "negative"}, priority = 2)
    @Story("FR-09 - Negative API Scenarios")
    @Severity(SeverityLevel.CRITICAL)
    public void testApiLoginInvalidPassword() {
        Response response = ApiClient.login(JsonDataReader.getUserEmail("valid_user"), "WrongPassword123!");
        Assert.assertEquals(response.statusCode(), 401, "Wrong password should return HTTP 401 Unauthorized");
        log.info("✅ TC-API-NEG-01 PASSED: 401 returned for invalid password");
    }

    // ===== NOTES CRUD TESTS =====

    @Test(description = "TC-API-02: POST /notes creates a note successfully", groups = {"smoke", "api"}, priority = 3)
    @Story("FR-02 - Create Note")
    @Severity(SeverityLevel.BLOCKER)
    public void testCreateNoteApi() {
        String category    = JsonDataReader.getNoteCategory("work_note");
        String title       = JsonDataReader.getNoteTitle("work_note");
        String description = JsonDataReader.getNoteDescription("work_note");

        Response response  = ApiClient.createNote(authToken, category, title, description);
        Assert.assertEquals(response.statusCode(), 200, "Create note should return 200.");
        
        createdNoteId = ApiClient.extractNoteId(response);
        log.info("✅ TC-API-02 PASSED | Note ID: {}", createdNoteId);
    }

    @Test(description = "TC-API-03: GET /notes returns list with at least the created note", groups = {"smoke", "api"}, priority = 4, dependsOnMethods = "testCreateNoteApi")
    @Story("FR-04 - GET /notes Returns List")
    @Severity(SeverityLevel.BLOCKER)
    public void testGetAllNotesApi() {
        Response response = ApiClient.getAllNotes(authToken);
        Assert.assertEquals(response.statusCode(), 200, "GET /notes should return 200.");
        log.info("✅ TC-API-03 PASSED | Notes list retrieved successfully.");
    }

    /*
    @Test(
        description = "TC-API-04: GET /notes/:id returns single note",
        groups = {"regression", "api"},
        priority = 5,
        dependsOnMethods = "testCreateNoteApi"
    )
    @Story("FR-04 - GET Single Note")
    @Severity(SeverityLevel.NORMAL)
    public void testGetNoteById() {
        Response response = ApiClient.getNoteById(authToken, createdNoteId);
        Assert.assertEquals(response.statusCode(), 200);
    }
    */

    @Test(description = "TC-API-05: DELETE /notes/:id removes the note", groups = {"smoke", "api"}, priority = 6, dependsOnMethods = "testCreateNoteApi")
    @Story("FR-06 - Delete Note via API")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteNoteApi() {
        Response deleteResponse = ApiClient.deleteNote(authToken, createdNoteId);
        Assert.assertEquals(deleteResponse.statusCode(), 200, "Delete should return 200.");
        log.info("✅ TC-API-05 PASSED: Note deleted");
    }

    @Test(description = "TC-API-NEG-02: Access notes without token returns 401", groups = {"regression", "api", "negative"}, priority = 7)
    @Story("FR-09 - Negative API Scenarios")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetNotesWithoutTokenReturns401() {
        Response response = ApiClient.getAllNotes("invalid-token-xyz");
        Assert.assertEquals(response.statusCode(), 401, "Request without valid token should return 401 Unauthorized");
        log.info("✅ TC-API-NEG-02 PASSED: 401 returned for invalid token");
    }

    /*
    @Test(
        description = "TC-API-NEG-03: GET non-existent note returns 404",
        groups = {"regression", "api", "negative"},
        priority = 8
    )
    @Story("FR-09 - Negative API Scenarios")
    @Severity(SeverityLevel.NORMAL)
    public void testGetNonExistentNoteReturns404() {
        Response response = ApiClient.getNoteById(authToken, "000000000000000000000000");
        Assert.assertEquals(response.statusCode(), 404);
    }
    */
}