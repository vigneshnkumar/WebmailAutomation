package com.selenium.gmail.base;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.selenium.gmail.config.Config;
import com.selenium.gmail.driver.DriverFactory;
import com.selenium.gmail.utilities.ExcelDataReader;
import com.selenium.gmail.utilities.ExtentManager;

/**
 * Common setup for UI tests: opens a fresh browser before every test method, closes it afterwards,
 * records the result in the Extent report and attaches a screenshot when a test fails.
 */
public abstract class BaseTest {

	private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
	private static final AtomicInteger SCREENSHOT_COUNTER = new AtomicInteger();

	protected static final Config prop = Config.get();
	protected final ExtentReports rep = ExtentManager.getInstance();

	protected WebDriver driver;
	protected WebDriverWait wait;
	protected ExtentTest test;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method, Object[] params) {
		String name = getClass().getSimpleName() + "." + method.getName()
				+ (params.length > 0 ? " " + Arrays.toString(params) : "");
		test = rep.createTest(name);
		test.assignCategory(prop.target());

		requireCredentials();
		openBrowser();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown(ITestResult result) {
		try {
			switch (result.getStatus()) {
			case ITestResult.SUCCESS:
				test.pass("Test passed");
				break;
			case ITestResult.FAILURE:
				test.fail(result.getThrowable());
				takeScreenShot();
				break;
			default:
				test.skip(result.getThrowable() != null ? result.getThrowable().getMessage() : "Test skipped");
			}
		} finally {
			if (driver != null) {
				driver.quit();
				driver = null;
			}
		}
	}

	@AfterSuite(alwaysRun = true)
	public void flushReport() {
		rep.flush(); // write the HTML report
	}

	// Skip (rather than fail) live runs when no credentials were supplied
	protected void requireCredentials() {
		if (prop.getOptional("mail.username") == null || prop.getOptional("mail.password") == null) {
			throw new SkipException("No credentials for target '" + prop.target()
					+ "'. Set MAIL_USERNAME and MAIL_PASSWORD environment variables to run against it.");
		}
	}

	// Browser properties
	public void openBrowser() {
		driver = DriverFactory.create(prop.get("browser", "chrome"), prop.getBoolean("headless", true),
				prop.getInt("page_load_timeout_seconds", 60));
		wait = new WebDriverWait(driver, Duration.ofSeconds(prop.getInt("timeout_seconds", 15)));
	}

	// Navigate to Site property
	public void navigate(String urlKey) {
		driver.get(prop.getUrl(urlKey));
	}

	public By locator(String xpathEleKey) {
		return By.xpath(prop.get(xpathEleKey));
	}

	// Click properties
	public void click(String xpathEleKey) {
		wait.until(ExpectedConditions.elementToBeClickable(locator(xpathEleKey))).click();
	}

	// Type properties
	public void type(String xpathEleKey, String data) {
		WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator(xpathEleKey)));
		element.clear();
		element.sendKeys(data);
	}

	// Wait for Element Property; returns false instead of throwing when it never appears
	public boolean isElementVisible(By by, Duration timeout) {
		try {
			new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(by));
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public boolean isElementVisible(String xpathEleKey) {
		return isElementVisible(locator(xpathEleKey), Duration.ofSeconds(prop.getInt("timeout_seconds", 15)));
	}

	/**
	 * Attaches a file by typing its path into an {@code <input type="file">}. This replaces the old
	 * java.awt.Robot keyboard simulation, which only worked on macOS with a visible browser window.
	 */
	public void uploadFile(String fileInputKey, Path file) {
		if (!Files.isRegularFile(file)) {
			throw new IllegalArgumentException("Attachment not found: " + file.toAbsolutePath());
		}
		WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(locator(fileInputKey)));
		input.sendKeys(file.toAbsolutePath().toString());
	}

	// Screenshot feature: saved next to the report so the relative link keeps working
	public void takeScreenShot() {
		if (driver == null) {
			return;
		}
		String filename = LocalDateTime.now().format(FILE_TIMESTAMP) + "_" + SCREENSHOT_COUNTER.incrementAndGet()
				+ ".png";
		try {
			Path source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
			Path dir = Files.createDirectories(ExtentManager.screenshotDir());
			Files.copy(source, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			test.info("Screenshot", MediaEntityBuilder
					.createScreenCaptureFromPath(ExtentManager.SCREENSHOT_FOLDER + "/" + filename).build());
		} catch (IOException | WebDriverException e) {
			test.warning("Could not capture screenshot: " + e.getMessage());
		}
	}

	/** Reads a data table from the Excel file configured by {@code excel_path}. */
	protected static Object[][] excelData(String sheetName, String tableName) {
		return ExcelDataReader.read(prop.getPath("excel_path"), sheetName, tableName);
	}

	/** Builds an XPath string literal that is safe for any text, including quotes. */
	protected static String xpathLiteral(String text) {
		if (!text.contains("'")) {
			return "'" + text + "'";
		}
		if (!text.contains("\"")) {
			return "\"" + text + "\"";
		}
		return "concat('" + text.replace("'", "', \"'\", '") + "')";
	}

	// ----------------------------------- APP FUNCTIONALITY -----------------------------------

	// Login Method: returns true once the inbox is visible
	public boolean doLogin() {
		type("logintextbox", prop.get("mail.username"));
		click("btn_next");
		type("passwordtextbox", prop.get("mail.password"));
		click("btn_login");
		return isElementVisible("inbox_link");
	}

	// Logout Method
	public void logout() {
		navigate("logout_url");
	}
}
