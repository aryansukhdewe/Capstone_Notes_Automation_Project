package com.capstone.utils;

import com.capstone.config.ConfigManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ScreenshotUtil — Handles screenshot capture and storage.
 *
 * WHY SCREENSHOTS ARE MANDATORY:
 * Per the capstone requirements, screenshots must be attached to failed test
 * cases as evidence. Screenshots serve two purposes:
 * 1. Allure/Extent report embedding: base64 bytes displayed inline in HTML report
 * 2. Jenkins artifacts: saved PNG files that can be downloaded from build artifacts
 *
 * WHY TIMESTAMPED FILENAMES:
 * If you run tests 3 times on the same day, each run's screenshots need unique
 * names to prevent overwriting. Timestamp in filename solves this.
 */
public class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOTS_DIR =
        ConfigManager.getInstance().get("screenshots.dir", "reports/screenshots");

    static {
        // Create screenshots directory if it doesn't exist
        // WHY STATIC BLOCK: This runs once when the class is loaded, ensuring
        // the directory exists before any test tries to write to it.
        try {
            Files.createDirectories(Paths.get(SCREENSHOTS_DIR));
            log.info("Screenshots directory ready: {}", SCREENSHOTS_DIR);
        } catch (IOException e) {
            log.error("Failed to create screenshots directory: {}", e.getMessage());
        }
    }

    private ScreenshotUtil() {}

    /**
     * Captures screenshot as byte array for Allure report attachment.
     * @return PNG bytes, or null if capture fails (never throws)
     */
    public static byte[] captureScreenshot(WebDriver driver) {
        try {
            // TakesScreenshot is implemented by RemoteWebDriver (parent of all drivers)
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Saves screenshot to filesystem (for Jenkins artifacts collection).
     * @param testName Used as filename base
     * @return Path to saved file, or null if save fails
     */
    public static String saveScreenshot(WebDriver driver, String testName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            // WHY SANITIZE: Test names may contain characters invalid in filenames
            String safeName = testName.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = SCREENSHOTS_DIR + "/" + safeName + "_" + timestamp + ".png";

            byte[] screenshot = captureScreenshot(driver);
            if (screenshot != null) {
                Files.write(Paths.get(fileName), screenshot);
                log.info("Screenshot saved: {}", fileName);
                return fileName;
            }
        } catch (IOException e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Captures screenshot as File object (used by some report integrations).
     */
    public static File captureAsFile(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        } catch (Exception e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }
}
