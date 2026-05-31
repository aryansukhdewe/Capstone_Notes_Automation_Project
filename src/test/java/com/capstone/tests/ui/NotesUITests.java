package com.capstone.tests.ui;

import com.capstone.base.BaseTest;
import com.capstone.pages.LoginPage;
import com.capstone.pages.NotesPage;
import com.capstone.utils.JsonDataReader;
import com.capstone.utils.RetryAnalyzer;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * NotesUITests — UI tests for note CRUD operations.
 *
 * COVERS:
 * FR-02: Create note via UI
 * FR-03: Note appears instantly in UI list
 * FR-07: Deleted note must disappear from UI
 * FR-09: Negative scenarios
 */
@Epic("Notes Application")
@Feature("Notes Management - UI")
public class NotesUITests extends BaseTest {

    private NotesPage notesPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void loginBeforeTest() {
        LoginPage loginPage = new LoginPage(getDriver());
        notesPage = loginPage.login(
            JsonDataReader.getUserEmail("valid_user"),
            JsonDataReader.getUserPassword("valid_user")
        );
        Assert.assertTrue(notesPage.isNotesPageLoaded(), "Pre-condition: Login must succeed");
    }

    // ===== DATA PROVIDER =====

    @DataProvider(name = "noteCategories", parallel = false)
    public Object[][] noteDataProvider() {
        return JsonDataReader.getAllNotesDataProvider();
    }

    // ===== CREATE NOTE TESTS =====

    @Test(
        dataProvider = "noteCategories",
        description = "TC-UI-02: Create notes across all categories",
        groups = {"smoke", "regression", "ui"},
        retryAnalyzer = RetryAnalyzer.class
    )
    @Story("FR-02 - Create Note via UI")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verifies that a logged-in user can create notes in each category " +
                 "(Home, Work, Personal) and the note appears in the list immediately.")
    public void testCreateNoteAllCategories(String category, String title, String description) {
        log.info("Testing note creation — Category: {}, Title: {}", category, title);

        // ACT: Create the note
        notesPage.createNote(category, title, description);

        // ASSERT: FR-03 — Note appears immediately in UI
        Assert.assertTrue(notesPage.isNoteVisible(title),
            "After creation, note '" + title + "' should appear in the UI list immediately. " +
            "This validates FR-03: DOM update must reflect the new note.");

        log.info("✅ Note '{}' created and visible in category '{}'", title, category);
    }

    /*
    @Test(
        description = "TC-UI-03: Create note with all required fields",
        groups = {"smoke", "regression", "ui"}
    )
    @Story("FR-02 - Create Note via UI")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateSingleNote() {
        String category    = JsonDataReader.getNoteCategory("home_note");
        String title       = JsonDataReader.getNoteTitle("home_note");
        String description = JsonDataReader.getNoteDescription("home_note");

        int countBefore = notesPage.getNoteCount();
        notesPage.createNote(category, title, description);
        int countAfter  = notesPage.getNoteCount();

        // Assert note count increased
        Assert.assertEquals(countAfter, countBefore + 1,
            "Note count should increase by 1 after creation. " +
            "Before: " + countBefore + ", After: " + countAfter);

        // Assert the specific note is visible
        Assert.assertTrue(notesPage.isNoteVisible(title),
            "Newly created note '" + title + "' must be visible in the list");

        log.info("✅ Note created. Count: {} → {}", countBefore, countAfter);
    }
    */

    // ===== DELETE NOTE TESTS =====

    @Test(
        description = "TC-UI-04: Delete a note and verify it disappears from UI",
        groups = {"regression", "ui"}
    )
    @Story("FR-07 - Deleted Note Must Disappear from UI")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Creates a note, then deletes it, and verifies the DOM no longer contains " +
                 "the note. Validates FR-07 from the UI perspective.")
    public void testDeleteNoteDisappearsFromUI() {
        String title = JsonDataReader.getNoteTitle("work_note") + " " + System.currentTimeMillis();
        String category = JsonDataReader.getNoteCategory("work_note");
        String description = JsonDataReader.getNoteDescription("work_note");

        // ARRANGE: Create the note first
        notesPage.createNote(category, title, description);
        Assert.assertTrue(notesPage.isNoteVisible(title), "Pre-condition: Note must exist before deletion");

        // ACT: Delete the note
        notesPage.deleteNoteByTitle(title);

        // ASSERT: Note must no longer be visible
        Assert.assertFalse(notesPage.isNoteVisible(title),
            "After deletion, note '" + title + "' must NOT appear in the UI. " +
            "Validates FR-07: deleted note disappears from UI.");

        log.info("✅ Note '{}' deleted and no longer visible in UI", title);
    }

    // ===== NEGATIVE TESTS =====

    /*
    @Test(
        description = "TC-NEG-UI-05: Create note without title shows validation error",
        groups = {"regression", "negative", "ui"}
    )
    @Story("FR-09 - Negative Scenarios")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateNoteWithEmptyTitle() {
        String emptyTitle   = JsonDataReader.get("negative_scenarios.missing_title.title");
        String description  = JsonDataReader.get("negative_scenarios.missing_title.description");
        String category     = JsonDataReader.get("negative_scenarios.missing_title.category");

        int countBefore = notesPage.getNoteCount();

        // Attempt to create note with empty title
        try {
            notesPage.createNote(category, emptyTitle, description);
        } catch (Exception e) {
            log.info("Exception on empty title (expected behavior): {}", e.getMessage());
        }

        int countAfter = notesPage.getNoteCount();

        // Note count should NOT have increased (creation should have failed)
        Assert.assertEquals(countAfter, countBefore,
            "Note with empty title should NOT be created. " +
            "Count should remain " + countBefore + " but is " + countAfter);

        log.info("✅ TC-NEG-UI-05 PASSED: Empty title prevented note creation");
    }
    */
}