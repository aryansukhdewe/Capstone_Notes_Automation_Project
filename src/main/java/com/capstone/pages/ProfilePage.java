package com.capstone.pages;

import com.capstone.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProfilePage — Page Object for User Profile / Account management.
 * Covers account deletion scenario (FR validation).
 */
public class ProfilePage {

    private static final Logger log = LoggerFactory.getLogger(ProfilePage.class);
    private final WebDriver driver;

    private static final By DELETE_ACCOUNT_BTN    = By.cssSelector("[data-testid='delete-account-btn']");
    private static final By CONFIRM_DELETE_BTN    = By.cssSelector("[data-testid='confirm-delete-account']");
    private static final By PROFILE_HEADING       = By.cssSelector("[data-testid='profile-heading']");
    private static final By CHANGE_PASSWORD_BTN   = By.cssSelector("[data-testid='change-password-btn']");

    public ProfilePage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isProfilePageLoaded() {
        return WaitUtil.isElementPresent(driver, PROFILE_HEADING, 10);
    }

    public LoginPage deleteAccount() {
        log.warn("Deleting user account — this is irreversible in this session!");
        WaitUtil.click(driver, DELETE_ACCOUNT_BTN);
        WaitUtil.click(driver, CONFIRM_DELETE_BTN);
        log.info("Account deletion confirmed");
        return new LoginPage(driver);
    }
}
