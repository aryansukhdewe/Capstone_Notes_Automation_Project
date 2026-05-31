package com.capstone.pages;

import com.capstone.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginPage — Page Object Model for the Login screen.
 *
 * WHY PAGE OBJECT MODEL (POM):
 * POM is the most widely-used design pattern in Selenium automation.
 * It separates:
 *   - WHERE elements are (this file — locators)
 *   - WHAT actions are performed (this file — methods)
 *   - WHAT is being verified (test classes — assertions)
 *
 * Benefits:
 * 1. If the login form's HTML changes, update only this file — not every test
 * 2. Test methods read like English: loginPage.enterEmail("...").clickLogin()
 * 3. Reusable: 10 tests can call loginPage.login() instead of duplicating 5 lines each
 *
 * WHY NO PAGE FACTORY:
 * PageFactory uses Java proxies that initialize elements lazily. On dynamic
 * React/SPA pages, elements can go stale between proxy init and actual use.
 * We use By locators with explicit waits instead — more predictable and debuggable.
 *
 * LOCATOR STRATEGY HIERARCHY (best → worst):
 * 1. By.id()           — fastest, most stable (if unique IDs exist)
 * 2. By.name()         — good for forms
 * 3. By.cssSelector()  — fast, readable, widely used
 * 4. By.xpath()        — use only when CSS can't do it (e.g., text matching)
 * 5. By.className()    — avoid if class names are generated (React CSS modules)
 * 6. By.tagName()      — too generic, rarely useful alone
 */
public class LoginPage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);
    private final WebDriver driver;

    // ===== LOCATORS =====
    // WHY PRIVATE FINAL: Locators are constants — they never change within a test run.
    // Private ensures test classes can't accidentally use raw locators (must use methods).
    private static final By EMAIL_FIELD      = By.cssSelector("input[data-testid='login-email']");
    private static final By PASSWORD_FIELD   = By.cssSelector("input[data-testid='login-password']");
    private static final By LOGIN_BUTTON     = By.cssSelector("button[data-testid='login-submit']");
    private static final By ERROR_MESSAGE    = By.cssSelector("[data-testid='alert-message']");
    private static final By REGISTER_LINK    = By.cssSelector("a[href*='register']");

    // Fallback locators for self-healing
    private static final By EMAIL_FALLBACK   = By.xpath("//input[@type='email']");
    private static final By PASSWORD_FALLBACK= By.xpath("//input[@type='password']");
    private static final By LOGIN_BTN_FALLBACK = By.xpath("//button[contains(text(),'Login')]");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // ===== PAGE ACTIONS =====

    /**
     * Enters email into the email field.
     * Returns 'this' for method chaining: loginPage.enterEmail("x").enterPassword("y").clickLogin()
     */
    public LoginPage enterEmail(String email) {
        log.info("Entering email: {}", email);
        WaitUtil.waitForVisible(driver, EMAIL_FIELD).clear();
        driver.findElement(EMAIL_FIELD).sendKeys(email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        log.info("Entering password: [MASKED]");
        WaitUtil.waitForVisible(driver, PASSWORD_FIELD).clear();
        driver.findElement(PASSWORD_FIELD).sendKeys(password);
        return this;
    }

    /**
     * Clicks the login button and returns NotesPage (expected destination).
     * WHY RETURN PAGE OBJECT: This establishes the page transition contract.
     * If login succeeds, you ARE on NotesPage. The return type enforces this.
     */
    public NotesPage clickLogin() {
        log.info("Clicking Login button");
        WaitUtil.click(driver, LOGIN_BUTTON);
        return new NotesPage(driver);
    }

    /**
     * Convenience method: full login flow in one call.
     * Used by tests that just need to be logged in (setup, not what's being tested).
     */
    public NotesPage login(String email, String password) {
        return enterEmail(email)
               .enterPassword(password)
               .clickLogin();
    }

    // ===== PAGE ASSERTIONS (return data for test verification) =====

    public boolean isErrorMessageDisplayed() {
        return WaitUtil.isElementPresent(driver, ERROR_MESSAGE, 5);
    }

    public String getErrorMessage() {
        return WaitUtil.waitForVisible(driver, ERROR_MESSAGE).getText();
    }

    public boolean isLoginPageDisplayed() {
        return WaitUtil.isElementPresent(driver, EMAIL_FIELD, 5) &&
               WaitUtil.isElementPresent(driver, PASSWORD_FIELD, 5);
    }
}
