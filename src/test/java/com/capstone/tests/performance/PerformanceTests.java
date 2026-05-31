package com.capstone.tests.performance;

import com.capstone.api.ApiClient;
import com.capstone.base.BaseTest;
import com.capstone.config.ConfigManager;
import com.capstone.mcp.MCPSeleniumIntegration;
import com.capstone.pages.LoginPage;
import com.capstone.utils.JsonDataReader;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Epic("Notes Application")
@Feature("Performance Engineering")
public class PerformanceTests extends BaseTest {

    private ApiClient apiClient;
    private String authToken;
    private final int API_THRESHOLD_MS = ConfigManager.getInstance().getApiTimeout();
    private final int UI_THRESHOLD_MS  = ConfigManager.getInstance().getInt("ui.page.load.time.ms");

    @BeforeClass
    public void setUpApi() {
        apiClient = new ApiClient();
        String email    = JsonDataReader.getTestData("users.valid_user.email");
        String password = JsonDataReader.getTestData("users.valid_user.password");
        authToken = apiClient.loginAndGetToken(email, password);
    }

    // ------------------------------------------------------------------
    @Test(description = "FR-08: Verify API response time is under 2000ms")
    @Story("API Response Time SLA")
    @Severity(SeverityLevel.CRITICAL)
    public void testApiResponseTimeUnderThreshold() {
        int iterations = 5;
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 1; i <= iterations; i++) {
            Response response = apiClient.getNotes(authToken);
            long elapsed = response.getTime();
            responseTimes.add(elapsed);
            Assert.assertTrue(elapsed < API_THRESHOLD_MS, "Request exceeded SLA");
        }

        long avg = responseTimes.stream().mapToLong(Long::longValue).sum() / iterations;
        log.info("API Average Response Time: {}ms", avg);
        Assert.assertTrue(avg < API_THRESHOLD_MS, "Average API response time exceeds threshold");
    }

    // ------------------------------------------------------------------
    @Test(description = "Measure UI page load timings using Navigation Timing API")
    @Story("UI Navigation Performance")
    @Severity(SeverityLevel.NORMAL)
    public void testUINavigationTimings() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(
            JsonDataReader.getTestData("users.valid_user.email"),
            JsonDataReader.getTestData("users.valid_user.password")
        );

        getDriver().get(ConfigManager.getInstance().getBaseUrl());
        Map<String, Long> timings = MCPSeleniumIntegration.captureNavigationTimings(getDriver());

        log.info("UI Full Page Load: {}ms", timings.get("pageLoad"));
        Assert.assertTrue(timings.get("pageLoad") < UI_THRESHOLD_MS, "Page load exceeds threshold");
    }

    // ------------------------------------------------------------------
    /*
    @Test(
        description = "API performance comparison across N sequential requests",
        groups = {"performance"}
    )
    @Story("API Load Baseline")
    @Severity(SeverityLevel.NORMAL)
    public void testApiPerformanceComparisonTable() {
        int[] batchSizes = {1, 5, 10};
        Map<Integer, Long> batchAverages = new HashMap<>();

        for (int batchSize : batchSizes) {
            List<Long> times = new ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                Response response = apiClient.getNotes(authToken);
                times.add(response.getTime());
            }
            long avg = times.stream().mapToLong(Long::longValue).sum() / batchSize;
            batchAverages.put(batchSize, avg);
        }
        log.info("Baseline generated.");
    }
    */
}