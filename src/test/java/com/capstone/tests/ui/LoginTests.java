package com.capstone.tests.ui;

import com.capstone.base.BaseTest;
import com.capstone.pages.LoginPage;
import com.capstone.pages.NotesPage;
import com.capstone.utils.JsonDataReader;
import com.capstone.utils.RetryAnalyzer;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;
import org.openqa.selenium.By;

/**
 * LoginTests — UI tests for FR-01: "UI login should work"
 *
 * TEST STRATEGY:
 * We test the "happy path" (valid credentials) and multiple negative scenarios.
 * Negative testing (FR-09) is critical: security, UX, and data integrity all
 * depend on proper rejection of invalid inputs.
 */
@Epic("Notes Application")
@Feature("User Authentication")
public class LoginTests extends BaseTest {

    // ===== POSITIVE TESTS =====

    @Test(
        description = "TC-UI-01: Valid user can log in successfully",
        groups = {"smoke", "regression", "ui"},
        retryAnalyzer = RetryAnalyzer.class
    )
    @Story("FR-01 - UI Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verifies that a registered user with valid credentials can log in " +
                 "and land on the Notes dashboard. This is the most critical test — " +
                 "if login is broken, NO other feature can be tested.")
    public void testValidLogin() {
        // ARRANGE: Get test data from JSON (no hardcoding)
        String email    = JsonDataReader.getUserEmail("valid_user");
        String password = JsonDataReader.getUserPassword("valid_user");

        // ACT: Use Page Object to perform login
        LoginPage loginPage = new LoginPage(getDriver());
        NotesPage notesPage = loginPage.login(email, password);

        boolean isLoaded = notesPage.isNotesPageLoaded();

        // ASSERT: Verify we're on the Notes page
        Assert.assertTrue(isLoaded,
            "After login, the Notes dashboard should be loaded. " +
            "Add Note button should be visible.");

        log.info("✅ TC-UI-01 PASSED: Valid login successful");
    }

    // ===== NEGATIVE TESTS (FR-09) =====

    @Test(
        description = "TC-NEG-UI-01: Login with invalid credentials shows error",
        groups = {"regression", "negative", "ui"}
    )
    @Story("FR-09 - Negative Scenarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies that invalid email/password combination is rejected with " +
                 "a meaningful error message. Tests security: no unauthorized access.")
    public void testInvalidCredentialsShowsError() {
        String email    = JsonDataReader.getUserEmail("invalid_user");
        String password = JsonDataReader.getUserPassword("invalid_user");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterEmail(email).enterPassword(password).clickLogin();

        // ASSERT: Error message must be shown
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Invalid credentials should display an error message");

        String errorMsg = loginPage.getErrorMessage();
        Assert.assertFalse(errorMsg.isBlank(),
            "Error message text should not be empty");

        log.info("✅ TC-NEG-UI-01 PASSED: Error shown for invalid credentials. Message: {}", errorMsg);
    }

    /* @Test(
        description = "TC-NEG-UI-02: Login with empty email shows error",
        groups = {"regression", "negative", "ui"}
    )
    @Story("FR-09 - Negative Scenarios")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyEmailShowsError() {
        LoginPage loginPage = new LoginPage(getDriver());
        // Leave email empty, enter password
        loginPage.enterEmail("").enterPassword("somepassword").clickLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed() || loginPage.isLoginPageDisplayed(),
            "Empty email should prevent login — either show error or stay on login page");

        log.info("✅ TC-NEG-UI-02 PASSED: Empty email correctly prevented login");
    }
    */

    /*
    @Test(
        description = "TC-NEG-UI-03: Login with empty password shows error",
        groups = {"regression", "negative", "ui"}
    )
    @Story("FR-09 - Negative Scenarios")
    @Severity(SeverityLevel.NORMAL)
    public void testEmptyPasswordShowsError() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterEmail(JsonDataReader.getUserEmail("valid_user"))
                 .enterPassword("")
                 .clickLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed() || loginPage.isLoginPageDisplayed(),
            "Empty password should prevent login");

        log.info("✅ TC-NEG-UI-03 PASSED: Empty password correctly prevented login");
    }
    */

    /*
    @Test(
        description = "TC-NEG-UI-04: Login with invalid email format shows error",
        groups = {"regression", "negative", "ui"}
    )
    @Story("FR-09 - Negative Scenarios")
    @Severity(SeverityLevel.MINOR)
    public void testInvalidEmailFormat() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterEmail("not-an-email-address")
                 .enterPassword("anypassword")
                 .clickLogin();

        // Either browser validation (HTML5) or server validation should catch this
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
            "Malformed email should be rejected — user should remain on login page");

        log.info("✅ TC-NEG-UI-04 PASSED: Invalid email format correctly rejected");
    }
    */

    @Test(
        description = "Demo: Intentional failure to trigger Groq AI Defect Analysis",
        groups = {"ui", "demo"},
        priority = 99
    )
    @io.qameta.allure.Story("AI Quality Engineering Demo")
    @io.qameta.allure.Severity(io.qameta.allure.SeverityLevel.TRIVIAL)
    public void testAiDefectAnalysisDemo() {
        log.info("Starting AI Demo Test: Intentionally trying to click a non-existent button...");
        
        // 1. Navigate to the login page
        getDriver().get(config.getBaseUrl());
        
        // 2. We are intentionally looking for an ID that DOES NOT exist on the Notes App
        // This will cause a TimeoutException/NoSuchElementException after the explicit wait fails.
        org.openqa.selenium.WebElement fakeButton = com.capstone.utils.WaitUtil.waitForVisible(
            getDriver(), 
            org.openqa.selenium.By.id("super-secret-admin-login-button"), 
            5 // wait for 5 seconds
        );
        
        // 3. Click it (the code will never reach here because line above will fail)
        fakeButton.click();
    }
}