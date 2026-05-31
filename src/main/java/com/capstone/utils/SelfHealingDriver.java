package com.capstone.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * SelfHealingDriver — Agentic locator fallback mechanism.
 *
 * WHY SELF-HEALING LOCATORS (AGENTIC AUTOMATION):
 * Web applications frequently change their DOM structure during development.
 * A button's ID might change from "btn-add" to "addNoteBtn". Traditional
 * Selenium immediately throws NoSuchElementException.
 *
 * Self-healing locators try MULTIPLE strategies for the same element:
 * 1. Primary locator (e.g., By.id("btn-add"))
 * 2. If that fails → try By.cssSelector("[data-testid='add-btn']")
 * 3. If that fails → try By.xpath("//button[contains(text(),'Add Note')]")
 *
 * This is the "Agentic" pattern: the framework AUTONOMOUSLY decides how to
 * find the element without human intervention, mimicking intelligent behavior.
 *
 * REAL-WORLD USE: Applitools, Healenium, and TestIM all use this concept
 * commercially. We implement a lightweight version here.
 */
public class SelfHealingDriver {

    private static final Logger log = LoggerFactory.getLogger(SelfHealingDriver.class);
    private final WebDriver driver;
    private static final int FALLBACK_TIMEOUT = 5;

    public SelfHealingDriver(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Attempts to find element using multiple locator strategies.
     * Returns the first one that succeeds.
     *
     * @param elementName Human-readable name for logging
     * @param locators    Ordered list of fallback locators
     * @return WebElement found by any of the strategies
     */
    public WebElement findElement(String elementName, By... locators) {
        List<By> strategies = Arrays.asList(locators);

        for (int i = 0; i < strategies.size(); i++) {
            By locator = strategies.get(i);
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(FALLBACK_TIMEOUT));
                WebElement element = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(locator));
                if (i > 0) {
                    // Primary locator failed, fallback succeeded — log for maintenance awareness
                    log.warn("SELF-HEAL: Primary locator failed for '{}'. " +
                             "Used fallback strategy #{}: {}", elementName, i, locator);
                } else {
                    log.debug("Element '{}' found with primary locator: {}", elementName, locator);
                }
                return element;
            } catch (TimeoutException | NoSuchElementException e) {
                if (i < strategies.size() - 1) {
                    log.debug("Locator strategy #{} failed for '{}', trying next fallback...",
                        i, elementName);
                } else {
                    log.error("ALL {} locator strategies failed for element: '{}'",
                        strategies.size(), elementName);
                }
            }
        }
        throw new NoSuchElementException(
            "Self-healing failed: could not find element '" + elementName +
            "' with any of " + strategies.size() + " strategies.");
    }

    /**
     * Self-healing click: finds element with fallback strategies, then clicks.
     * Includes JavaScript click fallback if regular click fails (e.g. overlapping element).
     */
    public void click(String elementName, By... locators) {
        WebElement element = findElement(elementName, locators);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Regular click intercepted for '{}', using JS click fallback", elementName);
            // JS click bypasses overlay elements that intercept clicks
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    /**
     * Self-healing type: finds element, clears it, then sends keys.
     */
    public void type(String elementName, String text, By... locators) {
        WebElement element = findElement(elementName, locators);
        element.clear();
        element.sendKeys(text);
        log.debug("Typed '{}' into element '{}'", text, elementName);
    }

    /**
     * Scrolls element into view using JS before interaction.
     * WHY: Elements at bottom of page may be outside viewport and
     * cause ElementNotInteractableException even when found in DOM.
     */
    public WebElement scrollAndFind(String elementName, By... locators) {
        WebElement element = findElement(elementName, locators);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
        // Brief pause for smooth scroll animation
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return element;
    }
}
