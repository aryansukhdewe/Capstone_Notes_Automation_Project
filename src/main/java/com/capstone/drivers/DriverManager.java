package com.capstone.drivers;

import com.capstone.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * DriverManager — Thread-safe WebDriver lifecycle manager.
 *
 * WHY THREADLOCAL:
 * When TestNG runs tests in parallel (multiple threads simultaneously),
 * a simple static WebDriver variable would be shared — Thread A's test
 * would interfere with Thread B's browser. ThreadLocal gives each thread
 * its own isolated copy of the driver variable. Thread A gets its own
 * Chrome window, Thread B gets its own. They never collide.
 *
 * WHY NOT PageFactory:
 * PageFactory.initElements() uses lazy proxy initialization, which can
 * produce StaleElementReferenceException in dynamic SPAs. We use By
 * locators with explicit waits instead — more control, more reliable.
 *
 * ARCHITECTURE NOTE:
 * The driver is NOT instantiated in this class's constructor. It's created
 * only when initDriver() is called (typically in @BeforeMethod). This
 * "lazy initialization" pattern prevents drivers from starting unnecessarily.
 */
public class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    /**
     * ThreadLocal storage: each thread (parallel test) gets its own WebDriver.
     * Think of it as a Map<Thread, WebDriver> managed automatically by Java.
     */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {
        // Utility class — no instantiation allowed
        throw new UnsupportedOperationException("DriverManager is a static utility class");
    }

    /**
     * Initializes a new WebDriver for the current thread.
     * Called in @BeforeMethod of base test class.
     *
     * @param browser Browser name (chrome/firefox/edge) — can come from
     *                config.properties or CI -Dbrowser=chrome override
     */
    public static void initDriver(String browser) {
        WebDriver driver = createDriver(browser);
        configureDriver(driver);
        driverThreadLocal.set(driver);
        log.info("WebDriver initialized for browser: {} | Thread: {}",
                browser, Thread.currentThread().getId());
    }

    public static void initDriver() {
        initDriver(ConfigManager.getInstance().getBrowser());
    }

    /**
     * Retrieves the WebDriver for the currently running thread.
     * @throws IllegalStateException if driver hasn't been initialized for this thread
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                "WebDriver not initialized for thread: " + Thread.currentThread().getId() +
                ". Ensure initDriver() is called in @BeforeMethod.");
        }
        return driver;
    }

    /**
     * Quits the browser and removes the ThreadLocal binding.
     * WHY REMOVE: If we only call quit() without remove(), ThreadLocal retains
     * a reference to the dead driver object. In long test suites this causes
     * memory leaks. Always remove() after quit() to allow GC.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver quit for thread: {}", Thread.currentThread().getId());
            } catch (Exception e) {
                log.warn("Error quitting WebDriver: {}", e.getMessage());
            } finally {
                // CRITICAL: Always remove, even if quit throws
                driverThreadLocal.remove();
            }
        }
    }

    private static WebDriver createDriver(String browser) {
        boolean headless = ConfigManager.getInstance().isHeadless();

        switch (browser.toLowerCase().trim()) {

            case "chrome": {
                // WHY WebDriverManager: Automatically downloads the ChromeDriver binary
                // matching the installed Chrome version. No manual driver management needed.
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();

                // WHY THESE OPTIONS:
                // --no-sandbox: Required for CI/Docker environments
                // --disable-dev-shm-usage: Prevents Chrome crashes in containers
                //   (Docker default /dev/shm is 64MB; Chrome needs more)
                // --disable-gpu: Prevents GPU rendering issues in headless mode
                // window-size: Ensures consistent viewport for screenshot comparison
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--disable-extensions");
                // Block heavy/intrusive Google Ads to prevent overlays and speed up page loading
                options.addArguments("--host-resolver-rules=" +
                    "MAP pagead2.googlesyndication.com 127.0.0.1, " +
                    "MAP *.google-analytics.com 127.0.0.1, " +
                    "MAP googleads.g.doubleclick.net 127.0.0.1, " +
                    "MAP *.googlesyndication.com 127.0.0.1, " +
                    "MAP *.googletagmanager.com 127.0.0.1");

                if (headless) {
                    options.addArguments("--headless=new"); // "new" headless is more stable than old mode
                    log.info("Running Chrome in HEADLESS mode");
                }
                return new ChromeDriver(options);
            }

            case "firefox": {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (headless) options.addArguments("--headless");
                return new FirefoxDriver(options);
            }

            case "edge": {
                WebDriverManager.edgedriver().setup();
                EdgeOptions options = new EdgeOptions();
                if (headless) options.addArguments("--headless");
                return new EdgeDriver(options);
            }

            default:
                throw new IllegalArgumentException(
                    "Unsupported browser: '" + browser + "'. " +
                    "Supported values: chrome, firefox, edge");
        }
    }

    private static void configureDriver(WebDriver driver) {
        ConfigManager config = ConfigManager.getInstance();

        // WHY MAXIMIZE: Consistent viewport prevents locators from failing on
        // responsive layouts that hide elements below a breakpoint.
        driver.manage().window().maximize();

        // WHY IMPLICIT WAIT IS 0:
        // We use EXPLICIT waits (WebDriverWait) for every element interaction.
        // Mixing implicit + explicit waits causes unpredictable timeout behavior.
        // The Selenium team recommends using one or the other, never both.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        // Page load timeout: how long to wait for window.load event
        driver.manage().timeouts()
              .pageLoadTimeout(Duration.ofSeconds(config.getInt("page.load.timeout")));

        // Script timeout: how long to wait for document.readyState in JS executor
        driver.manage().timeouts()
              .scriptTimeout(Duration.ofSeconds(30));
    }
}
