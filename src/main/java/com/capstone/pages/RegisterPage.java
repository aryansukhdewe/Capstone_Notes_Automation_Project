package com.capstone.pages;

import com.capstone.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RegisterPage — Page Object for user registration screen.
 */
public class RegisterPage {

    private static final Logger log = LoggerFactory.getLogger(RegisterPage.class);
    private final WebDriver driver;

    private static final By NAME_FIELD       = By.cssSelector("input[data-testid='register-name']");
    private static final By EMAIL_FIELD      = By.cssSelector("input[data-testid='register-email']");
    private static final By PASSWORD_FIELD   = By.cssSelector("input[data-testid='register-password']");
    private static final By CONFIRM_PASSWORD = By.cssSelector("input[data-testid='register-confirm-password']");
    private static final By REGISTER_BUTTON  = By.cssSelector("button[data-testid='register-submit']");
    private static final By SUCCESS_MESSAGE  = By.cssSelector("[data-testid='register-success']");
    private static final By ERROR_MESSAGE    = By.cssSelector("[data-testid='alert-message']");

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
    }

    public RegisterPage enterName(String name) {
        WaitUtil.waitForVisible(driver, NAME_FIELD).sendKeys(name);
        return this;
    }

    public RegisterPage enterEmail(String email) {
        WaitUtil.waitForVisible(driver, EMAIL_FIELD).sendKeys(email);
        return this;
    }

    public RegisterPage enterPassword(String password) {
        WaitUtil.waitForVisible(driver, PASSWORD_FIELD).sendKeys(password);
        return this;
    }

    public RegisterPage confirmPassword(String password) {
        WaitUtil.waitForVisible(driver, CONFIRM_PASSWORD).sendKeys(password);
        return this;
    }

    public LoginPage submitRegistration() {
        WaitUtil.click(driver, REGISTER_BUTTON);
        WaitUtil.waitForVisible(driver, SUCCESS_MESSAGE, 10);
        log.info("Registration submitted successfully");
        return new LoginPage(driver);
    }

    public String getErrorMessage() {
        return WaitUtil.waitForVisible(driver, ERROR_MESSAGE).getText();
    }

    public boolean isErrorDisplayed() {
        return WaitUtil.isElementPresent(driver, ERROR_MESSAGE, 5);
    }
}
