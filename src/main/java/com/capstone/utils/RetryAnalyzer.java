package com.capstone.utils;

import com.capstone.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer — Automatically retries failed tests.
 *
 * WHY RETRY LOGIC (AGENTIC CONCEPT):
 * In real-world test environments, tests can fail due to:
 * - Network latency spikes (API timeout)
 * - Browser startup delays in CI containers
 * - Transient JavaScript rendering issues
 * These are "flaky" failures — the test would pass on a second run.
 *
 * Retry logic is part of the "Agentic Automation" requirement:
 * the framework autonomously decides to re-run a test rather than
 * immediately marking it as failed. This reduces false negatives.
 *
 * HOW TO USE:
 * Add @Test(retryAnalyzer = RetryAnalyzer.class) to any test method,
 * or attach it globally via a TestNG Listener.
 *
 * IMPORTANT: Only retry on genuine flaky failures, not logical bugs.
 * Max retry is kept at 1 to avoid masking real issues.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);
    private int retryCount = 0;
    private static final int MAX_RETRY =
        ConfigManager.getInstance().getInt("max.retry.count");

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            log.warn("RETRY [{}/{}] for test: {} | Reason: {}",
                retryCount, MAX_RETRY,
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown");
            return true; // true = retry the test
        }
        log.error("Test PERMANENTLY FAILED after {} retries: {}",
            MAX_RETRY, result.getMethod().getMethodName());
        return false; // false = give up, mark as FAILED
    }
}
