package com.capstone.tests.e2e;

import com.capstone.api.ApiClient;
import com.capstone.base.BaseTest;
import com.capstone.pages.LoginPage;
import com.capstone.pages.NotesPage;
import com.capstone.utils.JsonDataReader;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * HybridE2ETests — Cross-layer validation: UI ↔ API data consistency.
 *
 * WHY HYBRID (E2E) TESTS:
 * UI tests prove the interface works. API tests prove the backend works.
 * But NEITHER proves that data created in UI actually reaches the backend
 * correctly, or that data deleted via API actually disappears from the UI.
 *
 * Hybrid tests bridge this gap:
 * - Create note via UI → Verify same note via API (FR-05)
 * - Delete note via API → Verify note gone from UI (FR-07)
 * - This is "synchronized UI and API" testing as mentioned in the brief.
 *
 * ARCHITECTURE:
 * Hybrid tests use BOTH the WebDriver (via BaseTest) AND RestAssured (via ApiClient).
 * This tests the full stack in one test scenario.
 *
 * COVERS:
 * FR-05: UI-created note must appear in API
 * FR-07: API-deleted note must disappear from UI
 * TC-01 from the capstone doc: UI ↔ API Data Consistency Check
 */
@Epic("Notes Application")
@Feature("Hybrid E2E - UI & API Cross-Validation")
public class HybridE2ETests extends BaseTest {

    private NotesPage notesPage;
    private String apiToken;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void loginBothLayers() {
        // 1. UI Login (opens browser, navigates to app)
        LoginPage loginPage = new LoginPage(getDriver());
        notesPage = loginPage.login(
            JsonDataReader.getUserEmail("valid_user"),
            JsonDataReader.getUserPassword("valid_user")
        );
        Assert.assertTrue(notesPage.isNotesPageLoaded(), "UI Login pre-condition failed");

        // 2. API Login (same user, gets token for API calls)
        // WHY SAME USER: We must use the same account so that notes created in UI
        // are retrievable via API for the same user's token.
        Response loginResponse = ApiClient.login(
            JsonDataReader.getUserEmail("valid_user"),
            JsonDataReader.getUserPassword("valid_user")
        );
        apiToken = ApiClient.extractToken(loginResponse);
        log.info("Hybrid setup complete: UI logged in + API token obtained");
    }

    // ===== FR-05: UI-Created Note Appears in API =====

    @Test(
        description = "TC-E2E-01: Note created via UI appears in API GET /notes",
        groups = {"e2e", "smoke"},
        priority = 1
    )
    @Story("FR-05 - UI-Created Note Must Appear in API")
    @Severity(SeverityLevel.BLOCKER)
    @Description(
        "This is the core data synchronization test (TC-01 from capstone doc).\n\n" +
        "Flow:\n" +
        "1. Create note via UI (browser interaction)\n" +
        "2. Call GET /notes via API (HTTP call)\n" +
        "3. Find the note in API response\n" +
        "4. Compare EVERY field: Title, Description, Category\n\n" +
        "WHY THIS MATTERS: Proves the UI form submission correctly persists data " +
        "to the backend. If fields are silently dropped or transformed, this test catches it."
    )
    public void testUiCreatedNoteAppearsInApi() {
        // Test data
        String category    = JsonDataReader.getNoteCategory("personal_note");
        String title       = JsonDataReader.getNoteTitle("personal_note");
        String description = JsonDataReader.getNoteDescription("personal_note");

        // STEP 1: Create note via UI
        log.info("STEP 1: Creating note via UI — Title: {}", title);
        notesPage.createNote(category, title, description);

        // STEP 2: Verify note visible in UI first (FR-03 validation)
        Assert.assertTrue(notesPage.isNoteVisible(title),
            "STEP 2 FAILED: Note '" + title + "' not visible in UI after creation");
        log.info("STEP 2: Note visible in UI ✅");

        // STEP 3: Call GET /notes via API
        log.info("STEP 3: Fetching notes via API...");
        Response apiResponse = ApiClient.getAllNotes(apiToken);
        Assert.assertEquals(apiResponse.statusCode(), 200,
            "STEP 3 FAILED: GET /notes returned " + apiResponse.statusCode());

        // STEP 4: Find the note in API response by title
        List<String> apiTitles = apiResponse.jsonPath().getList("data.title");
        log.info("STEP 4: API returned {} notes. Checking for '{}'...", apiTitles.size(), title);

        Assert.assertTrue(apiTitles.contains(title),
            "FR-05 FAILED: Note '" + title + "' created via UI was NOT found in API response.\n" +
            "API returned these titles: " + apiTitles);

        // STEP 5: Field-level comparison (the core TC-01 assertion)
        int noteIndex = apiTitles.indexOf(title);
        String apiCategory    = apiResponse.jsonPath().getString("data[" + noteIndex + "].category");
        String apiTitle       = apiResponse.jsonPath().getString("data[" + noteIndex + "].title");
        String apiDescription = apiResponse.jsonPath().getString("data[" + noteIndex + "].description");

        log.info("STEP 5: Comparing fields — UI vs API:");
        log.info("  Title:       UI='{}' | API='{}'", title, apiTitle);
        log.info("  Category:    UI='{}' | API='{}'", category, apiCategory);
        log.info("  Description: UI='{}' | API='{}'", description, apiDescription);

        Assert.assertEquals(apiTitle, title,
            "DATA MISMATCH: Title — UI: '" + title + "' | API: '" + apiTitle + "'");
        Assert.assertEquals(apiCategory, category,
            "DATA MISMATCH: Category — UI: '" + category + "' | API: '" + apiCategory + "'");
        Assert.assertEquals(apiDescription, description,
            "DATA MISMATCH: Description — UI: '" + description + "' | API: '" + apiDescription + "'");

        log.info("✅ TC-E2E-01 PASSED: All fields match between UI and API");
    }

    // ===== FR-07: API-Deleted Note Disappears from UI =====

    @Test(
        description = "TC-E2E-02: Note deleted via API disappears from UI",
        groups = {"e2e", "regression"},
        priority = 2
    )
    @Story("FR-07 - Deleted Note Must Disappear from UI")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
        "Flow:\n" +
        "1. Create a note via API (faster setup than UI)\n" +
        "2. Verify the note is visible in UI (page refresh or real-time update)\n" +
        "3. Delete the note via API\n" +
        "4. Verify the note is GONE from UI\n\n" +
        "WHY DELETE VIA API: Tests the reverse sync — backend change reflected in frontend."
    )
    public void testApiDeletedNoteDisappearsFromUI() {
        String category    = JsonDataReader.getNoteCategory("home_note");
        String title       = "E2E-Delete-Test-" + System.currentTimeMillis(); // Unique title
        String description = JsonDataReader.getNoteDescription("home_note");

        // STEP 1: Create note via API (skip UI for faster setup)
        log.info("STEP 1: Creating note via API for deletion test — Title: {}", title);
        Response createResponse = ApiClient.createNote(apiToken, category, title, description);
        Assert.assertEquals(createResponse.statusCode(), 200, "Pre-condition: API create must succeed");
        String noteId = ApiClient.extractNoteId(createResponse);
        log.info("Note created via API. ID: {}", noteId);

        // STEP 2: Refresh UI and confirm note appears
        getDriver().navigate().refresh();
        Assert.assertTrue(notesPage.isNoteVisible(title),
            "STEP 2 FAILED: API-created note '" + title + "' not visible in UI after refresh");
        log.info("STEP 2: Note visible in UI after API creation ✅");

        // STEP 3: Delete via API
        log.info("STEP 3: Deleting note ID '{}' via API...", noteId);
        Response deleteResponse = ApiClient.deleteNote(apiToken, noteId);
        Assert.assertEquals(deleteResponse.statusCode(), 200,
            "STEP 3 FAILED: API delete returned " + deleteResponse.statusCode());

        // STEP 4: Refresh UI and confirm note is GONE
        getDriver().navigate().refresh();
        Assert.assertFalse(notesPage.isNoteVisible(title),
            "FR-07 FAILED: Note '" + title + "' deleted via API still appears in UI. " +
            "The frontend must sync with backend deletions.");

        log.info("✅ TC-E2E-02 PASSED: API-deleted note '{}' correctly disappeared from UI", title);
    }

    // ===== PERFORMANCE HYBRID TEST =====

    @Test(
        description = "TC-E2E-03: Measure UI page load and API response times",
        groups = {"performance", "e2e"},
        priority = 3
    )
    @Story("FR-08 - API Response < 2s")
    @Severity(SeverityLevel.NORMAL)
    @Description(
        "Performance Engineering (Section 3.5):\n" +
        "- Measures UI page navigation timing using JavaScript Performance API\n" +
        "- Measures API response time\n" +
        "- Logs a comparison for the performance trend report"
    )
    public void testPerformanceMeasurements() {
        // UI Performance via JavaScript Navigation Timing API
        // WHY JS EXECUTOR: Selenium doesn't expose page load time directly.
        // The browser's Performance API provides accurate millisecond-level metrics.
        Long domContentLoaded = (Long) ((org.openqa.selenium.JavascriptExecutor) getDriver())
            .executeScript(
                "return window.performance.timing.domContentLoadedEventEnd - " +
                "window.performance.timing.navigationStart;"
            );

        Long fullPageLoad = (Long) ((org.openqa.selenium.JavascriptExecutor) getDriver())
            .executeScript(
                "return window.performance.timing.loadEventEnd - " +
                "window.performance.timing.navigationStart;"
            );

        log.info("=== UI PERFORMANCE METRICS ===");
        log.info("  DOM Content Loaded: {}ms", domContentLoaded);
        log.info("  Full Page Load:     {}ms", fullPageLoad);

        // API Performance
        long apiStart = System.currentTimeMillis();
        Response response = ApiClient.getAllNotes(apiToken);
        long apiTime = System.currentTimeMillis() - apiStart;

        log.info("=== API PERFORMANCE METRICS ===");
        log.info("  GET /notes response time: {}ms", apiTime);
        log.info("  HTTP Status: {}", response.statusCode());

        // Assert both within thresholds
        Assert.assertTrue(apiTime < 2000,
            "API response time " + apiTime + "ms exceeds 2000ms limit (FR-08)");
        Assert.assertTrue(fullPageLoad < 15000,
            "UI page load " + fullPageLoad + "ms exceeds 15000ms acceptable threshold");

        log.info("✅ TC-E2E-03 PASSED | API: {}ms | UI: {}ms", apiTime, fullPageLoad);
    }
}
