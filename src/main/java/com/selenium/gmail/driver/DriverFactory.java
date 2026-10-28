package com.selenium.gmail.driver;

import java.time.Duration;
import java.util.Locale;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Creates WebDriver instances. Selenium Manager (built into Selenium 4.6+) resolves the matching
 * driver binary automatically, so no chromedriver/geckodriver setup is required.
 */
public final class DriverFactory {

	private static final String WINDOW_SIZE = "1366,900";

	private DriverFactory() {
	}

	public static WebDriver create(String browser, boolean headless, int pageLoadTimeoutSeconds) {
		WebDriver driver;
		switch (browser.toLowerCase(Locale.ROOT)) {
		case "chrome":
			ChromeOptions chrome = new ChromeOptions();
			chrome.addArguments("--window-size=" + WINDOW_SIZE, "--disable-gpu", "--no-sandbox",
					"--disable-dev-shm-usage", "--allow-file-access-from-files");
			if (headless) {
				chrome.addArguments("--headless=new");
			}
			driver = new ChromeDriver(chrome);
			break;
		case "edge":
			EdgeOptions edge = new EdgeOptions();
			edge.addArguments("--window-size=" + WINDOW_SIZE, "--disable-gpu");
			if (headless) {
				edge.addArguments("--headless=new");
			}
			driver = new EdgeDriver(edge);
			break;
		case "firefox":
			FirefoxOptions firefox = new FirefoxOptions();
			if (headless) {
				firefox.addArguments("-headless");
			}
			driver = new FirefoxDriver(firefox);
			break;
		default:
			throw new IllegalArgumentException(
					"Unsupported browser '" + browser + "'. Use one of: chrome, firefox, edge");
		}
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeoutSeconds));
		return driver;
	}
}
