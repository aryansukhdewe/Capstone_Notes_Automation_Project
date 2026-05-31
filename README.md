# Notes App — Capstone Automation Suite
### Capgemini QA Engineering Capstone | Selenium + Java + RestAssured

---

## Project Overview

This is a complete end-to-end QA automation solution for the **Notes App** (ExpandTesting demo application). It covers all three capstone phases:

| Phase | Coverage |
|-------|----------|
| **Day 1** | Manual Test Plan, Test Cases, RTM, Defect Report |
| **Day 2** | Selenium UI + RestAssured API + Hybrid E2E Automation |
| **Day 3** | Parallel Execution, CI/CD (Jenkins), MCP, Performance |

---

## Framework Structure

```text
src/
├── main/java/com/capstone/
│   ├── base/          BaseTest.java           — TestNG lifecycle (@Before/@After)
│   ├── config/        ConfigManager.java      — Singleton config reader
│   ├── drivers/       DriverManager.java      — ThreadLocal WebDriver (parallel-safe)
│   ├── pages/         LoginPage, NotesPage... — Page Object Model
│   ├── api/           ApiClient.java          — RestAssured base + all API methods
│   ├── mcp/           MCPSeleniumIntegration  — AI/MCP locator generation
│   └── utils/         WaitUtil, ScreenshotUtil, RetryAnalyzer, SelfHealingDriver...
│
└── test/java/com/capstone/
    └── tests/
        ├── ui/        LoginTests, NotesUITests
        ├── api/       NotesApiTests
        ├── e2e/       HybridE2ETests
        └── performance/ PerformanceTests
```
---

##
How to Run
Prerequisites

Java 11+

Maven 3.6+

Google Chrome (latest)

---

Run full suite 

mvn clean test

---

Run specific layer

# UI only
mvn test -Dsurefire.suiteXmlFiles=testng.xml -Dgroups=ui

# API only
mvn test -Dsurefire.suiteXmlFiles=testng.xml -Dgroups=api

# E2E Hybrid only
mvn test -Dsurefire.suiteXmlFiles=testng.xml -Dgroups=e2e

---

Run cross-browser parallel

mvn test -Dsurefire.suiteXmlFiles=testng-parallel.xml

---

Generate Allure Report

allure serve target/allure-results

# Opens: target/allure-report/index.html

---

Override browser/headless from CLI

mvn test -Dbrowser=firefox -Dheadless=true

---

## Key Design Decisions

| Decision | Why |
|----------|-----|
| ThreadLocal WebDriver | Parallel tests each get isolated browser — no interference |
| No PageFactory | Factory proxies cause StaleElementException on SPAs; By locators + explicit waits are more reliable |
| External JSON test data | No hardcoded values; data changes don't require recompilation |
| Dual reporting (Allure + Extent) | Allure for CI/CD trends; Extent for standalone stakeholder HTML |
| RetryAnalyzer (1 retry) | Filters transient CI flakiness without masking real bugs |
| SelfHealingDriver | Multiple locator fallbacks — survives minor DOM changes |

---

CI/CD

Jenkins pipeline defined in Jenkinsfile:

Checkout → Compile → Run Tests → Generate Allure → Archive Artifacts

Artifacts collected per build:

reports/screenshots/ — PNG screenshots of failures

target/allure-report/ — Allure HTML report

reports/ExtentReport_*.html — Extent HTML report

target/surefire-reports/ — TestNG XML results

---

Requirements Coverage

| Req ID | Description | Test Class | Status |
|--------|-------------|------------|--------|
| FR-01 | UI login works | LoginTests | ✅ |
| FR-02 | Create note via UI | NotesUITests | ✅ |
| FR-03 | Note appears in UI list | NotesUITests | ✅ |
| FR-04 | API GET /notes returns list | NotesApiTests | ✅ |
| FR-05 | UI note appears in API | HybridE2ETests | ✅ |
| FR-06 | Delete note via API | NotesApiTests | ✅ |
| FR-07 | API deletion reflects in UI | HybridE2ETests | ✅ |
| FR-08 | API response < 2s | PerformanceTests + ApiTests | ✅ |
| FR-09 | Negative scenarios | All test classes | ✅ |
---
