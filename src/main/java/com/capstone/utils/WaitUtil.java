package com.capstone.utils;

import com.capstone.config.ConfigManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * WaitUtil — Centralized explicit wait library.
 *
 * WHY EXPLICIT WAITS (NOT IMPLICIT OR THREAD.SLEEP):
 *
 * Thread.sleep(3000):
 *   ❌ NEVER use this. It always waits the full 3 seconds even if the element
 *   is ready in 200ms. In a 500-test suite, these add up to hours of wasted time.
 *
 * Implicit Wait (driver.manage().timeouts().implicitlyWait()):
 *   ❌ Applies globally to every findElement() call. Can't be customized
 *   per-element. Also has unpredictable interactions when combined with
 *   explicit waits (known Selenium anti-pattern).
 *
 * Explicit Wait (WebDriverWait + ExpectedConditions):
 *   ✅ Polls every 500ms until the condition is true OR timeout is reached.
 *   Fails fast if condition never met. Customizable per-element.
 *   The CORRECT industry approach.
 *
 * JS Executor Wait:
 *   ✅ Used when Selenium's ExpectedConditions aren't enough — e.g. waiting
 *   for React/Angular to finish rendering (document.readyState == "complete").
 */
public class WaitUtil {

    private static final Logger log = LoggerFactory.getLogger(WaitUtil.class);
    private static final int DEFAULT_TIMEOUT = ConfigManager.getInstance().getTimeout();

    private WaitUtil() {}

    // ===== ELEMENT VISIBILITY WAITS =====

    /**
     * Waits until element is present in DOM AND visible (height/width > 0).
     * Use this before reading text or asserting element properties.
     */
    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return waitForVisible(driver, locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisible(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement element = wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator));
            log.debug("Element visible: {}", locator);
            return element;
        } catch (TimeoutException e) {
            log.error("TIMEOUT: Element not visible after {}s: {}", timeoutSeconds, locator);
            throw new TimeoutException(
                "Element did not become visible within " + timeoutSeconds + "s: " + locator);
        }
    }

    /**
     * Waits until element is clickable (visible + enabled).
     * Use this before .click() operations to prevent ElementNotInteractableException.
     */
    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return waitForClickable(driver, locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickable(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            log.error("TIMEOUT: Element not clickable after {}s: {}", timeoutSeconds, locator);
            throw new TimeoutException(
                "Element did not become clickable within " + timeoutSeconds + "s: " + locator);
        }
    }

    /**
     * Waits for an element to disappear (invisibility).
     * Use after delete actions to confirm the element is gone from DOM.
     */
    public static boolean waitForInvisible(WebDriver driver, By locator) {
        return waitForInvisible(driver, locator, DEFAULT_TIMEOUT);
    }

    public static boolean waitForInvisible(WebDriver driver, By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ===== TEXT WAITS =====

    /**
     * Waits until element contains specific text.
     * WHY: UI updates are asynchronous. After creating a note, the note title
     * may not appear immediately. This polls until the text is present.
     */
    public static boolean waitForText(WebDriver driver, By locator, String text) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public static boolean waitForPageTitle(WebDriver driver, String partialTitle) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.titleContains(partialTitle));
    }

    // ===== URL WAITS =====

    public static boolean waitForUrlContains(WebDriver driver, String partialUrl) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.urlContains(partialUrl));
    }

    // ===== JAVASCRIPT EXECUTOR WAITS =====

    /**
     * Waits for page to fully load using document.readyState.
     * WHY JS EXECUTOR: React/Angular/Vue apps update the DOM asynchronously
     * after the initial page load event. Selenium's pageLoad timeout fires on
     * the window.load event, but JS frameworks continue rendering after that.
     * Polling document.readyState catches the fully-hydrated state.
     */
    public static void waitForPageLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        wait.until(d -> {
            String readyState = (String)
                ((JavascriptExecutor) d).executeScript("return document.readyState");
            return "complete".equals(readyState);
        });
        log.debug("Page fully loaded (document.readyState == 'complete')");
    }

    /**
     * Waits for a specific number of elements to be present.
     * Useful when verifying that a list has the expected count after an operation.
     */
    public static void waitForElementCount(WebDriver driver, By locator, int expectedCount) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        wait.until(d -> d.findElements(locator).size() >= expectedCount);
    }

    /**
     * Safe element presence check: returns false instead of throwing.
     * Use for conditional logic: "if element exists, do X"
     */
    public static boolean isElementPresent(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public static boolean isElementPresent(WebDriver driver, By locator) {
        return isElementPresent(driver, locator, 3); // Short timeout for presence check
    }

    /**
     * Clicks on an element after waiting for it to be clickable.
     * If a standard click fails due to click interception or not interactable,
     * it falls back to a JavaScript click.
     */
    public static void click(WebDriver driver, By locator) {
        WebElement element = waitForClickable(driver, locator);
        try {
            element.click();
        } catch (ElementNotInteractableException e) {
            log.warn("Standard click failed or was intercepted, falling back to JS click for locator: {}", locator);
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception ex) {
                log.error("JS fallback click also failed for locator: {}", locator, ex);
                throw e; // throw the original click exception if both failed
            }
        }
    }
}
