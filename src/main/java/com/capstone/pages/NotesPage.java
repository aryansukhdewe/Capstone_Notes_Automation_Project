package com.capstone.pages;

import com.capstone.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * NotesPage — Page Object for the main Notes dashboard.
 *
 * This page handles:
 * - Viewing the list of notes
 * - Creating new notes via the "Add Note" modal
 * - Deleting notes
 * - Editing notes
 * - Filtering by category
 */
public class NotesPage {

    private static final Logger log = LoggerFactory.getLogger(NotesPage.class);
    private final WebDriver driver;

    // ===== LOCATORS — Notes Dashboard =====
    private static final By ADD_NOTE_BUTTON   = By.cssSelector("[data-testid='add-new-note'], [data-testid='add-note-btn'], [data-testid='add-note-button']");
    private static final By NOTE_CARDS        = By.cssSelector("[data-testid='note-card']");
    private static final By NOTE_TITLE_TEXT   = By.cssSelector("div[data-testid='note-card'] [data-testid='note-title']");
    private static final By LOGOUT_BUTTON     = By.cssSelector("[data-testid='logout']");
    private static final By PROFILE_LINK      = By.cssSelector("[data-testid='profile']");

    // ===== LOCATORS — Add/Edit Note Modal =====
    private static final By MODAL_CATEGORY    = By.cssSelector("[data-testid='note-category']");
    private static final By MODAL_TITLE       = By.cssSelector("input[data-testid='note-title']");
    private static final By MODAL_DESCRIPTION = By.cssSelector("[data-testid='note-description']");
    private static final By MODAL_SUBMIT      = By.cssSelector("[data-testid='note-submit']");
    private static final By MODAL_CLOSE       = By.cssSelector("[data-testid='note-close-btn']");
    private static final By SUCCESS_TOAST     = By.cssSelector(".toast-success, [data-testid='toast-message']");

    // ===== LOCATORS — Note Card Actions =====
    // Note: nth-of-type selectors used; adjust based on actual app DOM
    private static final By DELETE_NOTE_BTN   = By.cssSelector("[data-testid='note-delete']");
    private static final By EDIT_NOTE_BTN     = By.cssSelector("[data-testid='note-edit']");
    private static final By CONFIRM_DELETE    = By.cssSelector("[data-testid='note-delete-confirm']");
    private static final By COMPLETE_CHECKBOX = By.cssSelector("[data-testid='note-completed']");

    public NotesPage(WebDriver driver) {
        this.driver = driver;
    }

    // ===== NOTE CREATION =====

    /**
     * Full workflow to create a note via UI.
     * Clicks Add, fills the modal form, submits, waits for success.
     *
     * @return this (for fluent chaining)
     */
    public NotesPage createNote(String category, String title, String description) {
        log.info("Creating note — Category: {}, Title: {}", category, title);

        // Step 1: Open the modal
        WaitUtil.click(driver, ADD_NOTE_BUTTON);

        // Step 2: Select category from dropdown
        WebElement categoryDropdown = WaitUtil.waitForVisible(driver, MODAL_CATEGORY);
        new Select(categoryDropdown).selectByVisibleText(category);

        // Step 3: Enter title
        WebElement titleField = WaitUtil.waitForVisible(driver, MODAL_TITLE);
        titleField.clear();
        titleField.sendKeys(title);

        // Step 4: Enter description
        WebElement descField = WaitUtil.waitForVisible(driver, MODAL_DESCRIPTION);
        descField.clear();
        descField.sendKeys(description);

        // Step 5: Submit form
        WaitUtil.click(driver, MODAL_SUBMIT);

        // Step 6: Wait for success confirmation (toast message or card appearance)
        waitForSuccessToast();

        log.info("Note created successfully: '{}'", title);
        return this;
    }

    // ===== NOTE DELETION =====

    /**
     * Deletes the first note that matches the given title.
     * Searches all note cards for a title match, then clicks its delete button.
     */
    public NotesPage deleteNoteByTitle(String title) {
        log.info("Deleting note: '{}'", title);
        waitForSpinnerToDisappear();
        // Wait for any transient toast to disappear so it doesn't block clicking
        try {
            WaitUtil.waitForInvisible(driver, By.cssSelector(".toast, [data-testid='toast-message']"), 5);
        } catch (Exception e) {
            log.info("No toast blocking delete or timed out waiting: {}", e.getMessage());
        }

        // Find the specific note card containing this title
        List<WebElement> cards = driver.findElements(NOTE_CARDS);
        for (WebElement card : cards) {
            if (card.getText().contains(title)) {
                WebElement deleteBtn = card.findElement(DELETE_NOTE_BTN);
                try {
                    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(deleteBtn));
                } catch (Exception e) {
                    log.warn("Delete button was not clickable: {}", e.getMessage());
                }

                try {
                    deleteBtn.click();
                } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                    log.warn("Click intercepted for delete button of note '{}', using JS click fallback", title);
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteBtn);
                }

                // Confirm the deletion in the confirmation dialog
                WebElement confirmBtn = WaitUtil.waitForClickable(driver, CONFIRM_DELETE, 5);
                
                // Add a small delay (500ms) to allow the modal fade-in transition and event bindings to complete
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                try {
                    confirmBtn.click();
                } catch (org.openqa.selenium.ElementClickInterceptedException e) {
                    log.warn("Click intercepted for confirm delete button, using JS click fallback");
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
                }
                log.info("Deletion confirmed for note: '{}'", title);

                // Wait for the card to disappear from DOM. If it doesn't disappear in 4 seconds,
                // try clicking the confirm button again (using JS) in case the first click was missed!
                try {
                    WaitUtil.waitForInvisible(driver,
                        By.xpath("//div[@data-testid='note-card']//*[text()='" + title + "']"), 4);
                } catch (Exception e) {
                    log.warn("Note card did not disappear after 4s, checking if we need to retry...");
                    try {
                        List<WebElement> remaining = driver.findElements(By.xpath("//div[@data-testid='note-card']//*[text()='" + title + "']"));
                        if (!remaining.isEmpty()) {
                            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
                            WaitUtil.waitForInvisible(driver,
                                By.xpath("//div[@data-testid='note-card']//*[text()='" + title + "']"), 5);
                        }
                    } catch (org.openqa.selenium.StaleElementReferenceException staleEx) {
                        log.info("Confirm button or card is stale, meaning deletion completed: {}", staleEx.getMessage());
                        if (driver.findElements(By.xpath("//div[@data-testid='note-card']//*[text()='" + title + "']")).isEmpty()) {
                            log.info("Confirmed: Note card is gone.");
                        } else {
                            throw staleEx;
                        }
                    } catch (Exception ex) {
                        log.error("Failed to delete note even after retry: {}", ex.getMessage());
                        throw ex;
                    }
                }
                return this;
            }
        }
        throw new RuntimeException("Note not found for deletion: '" + title + "'");
    }

    // ===== NOTE QUERIES =====

    /**
     * Returns true if a note with the given title is visible on the page.
     * Used to verify FR-03: "Note should appear instantly in UI list"
     */
    public boolean isNoteVisible(String title) {
        waitForSpinnerToDisappear();
        try {
            WaitUtil.waitForInvisible(driver, By.cssSelector(".toast, [data-testid='toast-message']"), 3);
        } catch (Exception e) {
            // ignore
        }
        List<WebElement> cards = driver.findElements(NOTE_CARDS);
        boolean found = cards.stream()
            .anyMatch(card -> card.getText().contains(title));
        log.info("Note '{}' visible: {}", title, found);
        return found;
    }

    /**
     * Returns a list of all note titles currently displayed.
     * Used for UI ↔ API data consistency checks.
     */
    public List<String> getAllNoteTitles() {
        waitForSpinnerToDisappear();
        return driver.findElements(NOTE_TITLE_TEXT)
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * Gets note details (title + description) from a card by title.
     * Returns as String array: [title, description, category]
     */
    public String[] getNoteDetails(String title) {
        waitForSpinnerToDisappear();
        List<WebElement> cards = driver.findElements(NOTE_CARDS);
        for (WebElement card : cards) {
            if (card.getText().contains(title)) {
                String cardTitle = card.findElement(
                    By.cssSelector("[data-testid='note-title']")).getText();
                String cardDesc = card.findElement(
                    By.cssSelector("[data-testid='note-description']")).getText();
                String cardCategory = card.findElement(
                    By.cssSelector("[data-testid='note-category']")).getText();
                return new String[]{ cardTitle, cardDesc, cardCategory };
            }
        }
        throw new RuntimeException("Note not found on page: '" + title + "'");
    }

    public int getNoteCount() {
        waitForSpinnerToDisappear();
        return driver.findElements(NOTE_CARDS).size();
    }

    // ===== NAVIGATION =====

    public LoginPage logout() {
        WaitUtil.click(driver, LOGOUT_BUTTON);
        return new LoginPage(driver);
    }

    public ProfilePage goToProfile() {
        WaitUtil.click(driver, PROFILE_LINK);
        return new ProfilePage(driver);
    }

    // ===== HELPERS =====

    private void waitForSuccessToast() {
        try {
            // Wait for the success toast to appear (means server confirmed the action)
            WaitUtil.waitForVisible(driver, SUCCESS_TOAST, 3);
            // Then wait for it to disappear (don't interact while toast is blocking)
            WaitUtil.waitForInvisible(driver, SUCCESS_TOAST, 10);
        } catch (Exception e) {
            log.info("Success toast did not appear or was already gone, proceeding...");
        }
    }

    public void waitForSpinnerToDisappear() {
        WaitUtil.waitForPageLoad(driver);
        try {
            // Wait for Bootstrap loading spinner to become invisible/not present
            WaitUtil.waitForInvisible(driver, By.cssSelector("div[class*='spinner'], .spinner-border, .spinner-grow"), 10);
        } catch (Exception e) {
            log.warn("Timed out waiting for loading spinner to disappear, proceeding: {}", e.getMessage());
        }
    }

    public boolean isNotesPageLoaded() {
        return WaitUtil.isElementPresent(driver, ADD_NOTE_BUTTON, 10);
    }
}
