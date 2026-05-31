package com.capstone.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.capstone.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ExtentReportManager — Manages ExtentReports HTML report lifecycle.
 *
 * WHY TWO REPORTING SYSTEMS (Allure + Extent):
 * - Allure: Deep CI/CD integration, historical trends, rich timeline view.
 *   Requires Allure server or CLI to generate HTML from JSON results.
 *   Best for engineering teams.
 * - ExtentReports: Single self-contained HTML file. No server needed.
 *   Can be emailed or opened directly in any browser.
 *   Best for sharing results with business stakeholders.
 *
 * WHY THREADLOCAL FOR ExtentTest:
 * In parallel execution each thread runs its own test. If threads share
 * one ExtentTest object, logs from Thread A and Thread B get mixed into
 * the same test entry in the report. ThreadLocal gives each thread its
 * own ExtentTest instance.
 */
public class ExtentReportManager {

    private static final Logger log = LoggerFactory.getLogger(ExtentReportManager.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThreadLocal = new ThreadLocal<>();

    private ExtentReportManager() {}

    public static void initReports() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String reportPath = ConfigManager.getInstance().get("reports.dir", "reports")
                + "/ExtentReport_" + timestamp + ".html";

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("Notes App - Automation Test Report");
        spark.config().setReportName("Capstone QA Report - Capgemini Sprint");
        spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);

        // System info shown in the report's Environment section
        extent.setSystemInfo("Application", "Notes App - ExpandTesting");
        extent.setSystemInfo("Browser", ConfigManager.getInstance().getBrowser());
        extent.setSystemInfo("Base URL", ConfigManager.getInstance().getBaseUrl());
        extent.setSystemInfo("Tester", "Capstone QA Engineer");
        extent.setSystemInfo("Environment", "QA");

        log.info("ExtentReports initialized: {}", reportPath);
    }

    public static void createTest(String testName) {
        ExtentTest test = extent.createTest(testName);
        testThreadLocal.set(test);
    }

    public static void passTest(String message) {
        ExtentTest test = testThreadLocal.get();
        if (test != null) test.pass(message);
    }

    public static void failTest(Throwable throwable) {
        ExtentTest test = testThreadLocal.get();
        if (test != null) test.fail(throwable);
    }

    public static void skipTest(String message) {
        ExtentTest test = testThreadLocal.get();
        if (test != null) test.skip(message);
    }

    public static void logInfo(String message) {
        ExtentTest test = testThreadLocal.get();
        if (test != null) test.info(message);
    }

    public static ExtentTest getTest() {
        return testThreadLocal.get();
    }

    /**
     * Writes all results to disk. MUST be called at end of suite.
     * WHY: ExtentReports buffers results in memory. flush() serializes
     * them to the HTML file. Without flush(), the report file is incomplete.
     */
    public static void flushReports() {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed to disk.");
        }
    }
}
