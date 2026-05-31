package com.capstone.base;

import com.capstone.api.GroqApiClient;
import com.capstone.config.ConfigManager;
import com.capstone.drivers.DriverManager;
import com.capstone.utils.ExtentReportManager;
import com.capstone.utils.ScreenshotUtil;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * BaseTest — Parent class for all UI test classes.
 *
 * WHY A BASE CLASS (Template Method Pattern):
 * Every UI test needs the same lifecycle:
 *   1. @BeforeSuite  → init reporting (once)
 *   2. @BeforeMethod → start browser, navigate to app
 *   3. [TEST RUNS]
 *   4. @AfterMethod  → capture screenshot if failed, quit browser
 *   5. @AfterSuite   → flush reports (once)
 *
 * Without a base class, all 5 test classes would duplicate this code.
 * With it, updating teardown logic means changing ONE file.
 *
 * WHY @Parameters("browser"):
 * testng.xml passes <parameter name="browser" value="chrome"/> per <test> block.
 * @Parameters("browser") injects that value into setUp(). This lets us
 * run UI tests in a different browser without changing Java code —
 * just change the XML or pass -Dbrowser=firefox to Maven.
 *
 * WHY optional=true:
 * API-only test blocks in testng.xml don't pass a "browser" parameter.
 * optional=true means "use the parameter if provided; otherwise use null
 * and fall back to config.properties". Without optional=true, TestNG would
 * throw an exception for test blocks that don't declare the parameter.
 */
/**
 * BaseTest — Parent class for all UI test classes.
 *
 * WHY A BASE CLASS (Template Method Pattern):
 * Every UI test needs the same lifecycle:
 * 1. @BeforeSuite  → init reporting (once)
 * 2. @BeforeMethod → start browser, navigate to app
 * 3. [TEST RUNS]
 * 4. @AfterMethod  → capture screenshot if failed, quit browser
 * 5. @AfterSuite   → flush reports (once)
 *
 * Without a base class, all 5 test classes would duplicate this code.
 * With it, updating teardown logic means changing ONE file.
 *
 * WHY @Parameters("browser"):
 * testng.xml passes <parameter name="browser" value="chrome"/> per <test> block.
 * @Parameters("browser") injects that value into setUp(). This lets us
 * run UI tests in a different browser without changing Java code —
 * just change the XML or pass -Dbrowser=firefox to Maven.
 *
 * WHY optional=true:
 * API-only test blocks in testng.xml don't pass a "browser" parameter.
 * optional=true means "use the parameter if provided; otherwise use null
 * and fall back to config.properties". Without optional=true, TestNG would
 * throw an exception for test blocks that don't declare the parameter.
 */
public class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected final ConfigManager config = ConfigManager.getInstance();

    @BeforeSuite(alwaysRun = true)
    public void globalSetup() {
        ExtentReportManager.initReports();
        log.info("╔══════════════════════════════════════╗");
        log.info("║      TEST SUITE STARTED              ║");
        log.info("║  App : {}  ║", config.getBaseUrl());
        log.info("╚══════════════════════════════════════╝");
    }

    /**
     * @BeforeMethod: runs before EACH @Test method.
     * @Parameters("browser"): receives browser type from testng.xml.
     * optional=true: safe for test blocks that don't pass "browser" param.
     * alwaysRun=true: runs even if the test is in an unselected group.
     */
    @Parameters("browser")
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional String browser, ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("▶ Starting: {}", testName);

        // Determine browser: testng.xml param → -Dbrowser system property → config file
        String resolvedBrowser = (browser != null && !browser.isBlank())
            ? browser
            : config.getBrowser();

        DriverManager.initDriver(resolvedBrowser);
        ExtentReportManager.createTest(testName);

        getDriver().get(config.getBaseUrl());
        log.info("  Browser: {} | URL: {}", resolvedBrowser, config.getBaseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        switch (result.getStatus()) {
            case ITestResult.FAILURE:
                log.error("✘ FAILED: {} | {}", testName,
                    result.getThrowable() != null ? result.getThrowable().getMessage() : "");
                
                // Capture screenshot for Allure (embedded) AND filesystem (Jenkins artifact)
                byte[] screenshot = ScreenshotUtil.captureScreenshot(getDriver());
                if (screenshot != null) {
                    attachScreenshotToAllure(screenshot);
                    ScreenshotUtil.saveScreenshot(getDriver(), testName);
                }
                ExtentReportManager.failTest(result.getThrowable());

                // 🤖 GROQ AI INTEGRATION: Analyze the failure
                Throwable exception = result.getThrowable();
                if (exception != null) {
                    String errorMessage = exception.getMessage();
                    
                    // Create a prompt for Groq
                    String prompt = "You are an expert QA Automation Engineer. My Selenium/RestAssured test named '" 
                                    + testName + "' just failed with this exact error: " 
                                    + errorMessage 
                                    + ". Briefly explain what this error means and suggest 2 possible ways to fix it in my Java code.";
                    
                    // Call our new Groq Client
                    String aiAnalysis = GroqApiClient.askAi(prompt);
                    
                    // Attach Groq's analysis directly to the Allure Report
                    Allure.addAttachment("🤖 Groq AI Failure Analysis", aiAnalysis);
                    log.info("AI Analysis attached to test report.");
                }
                break;

            case ITestResult.SUCCESS:
                log.info("✔ PASSED: {}", testName);
                ExtentReportManager.passTest("Test passed successfully");
                break;

            case ITestResult.SKIP:
                log.warn("⊘ SKIPPED: {}", testName);
                ExtentReportManager.skipTest("Test was skipped");
                break;
        }

        // CRITICAL: Always quit driver in finally-equivalent block.
        // If we only call quit() without remove(), ThreadLocal retains a dead
        // WebDriver reference, causing memory leaks in long parallel runs.
        DriverManager.quitDriver();
        log.info("◀ Finished: {}", testName);
    }

    @AfterSuite(alwaysRun = true)
    public void globalTearDown() {
        ExtentReportManager.flushReports();
        log.info("╔══════════════════════════════════════╗");
        log.info("║      TEST SUITE COMPLETED            ║");
        log.info("╚══════════════════════════════════════╝");
    }

    /** Convenience accessor — subclasses write getDriver() not DriverManager.getDriver() */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Allure attachment: embeds screenshot as PNG directly in the test report.
     * WHY: When a test fails in Jenkins, engineers click the Allure link and
     * see exactly what the browser displayed at the point of failure —
     * no need to SSH into the server or dig through filesystem paths.
     */
    @Attachment(value = "Failure Screenshot", type = "image/png")
    private byte[] attachScreenshotToAllure(byte[] screenshot) {
        return screenshot;
    }
}
