package com.selenium.gmail.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/** Holds the single ExtentReports instance shared by the whole test run. */
public final class ExtentManager {

	/** Report directory; screenshots are written to a sub-folder so the report can be moved as a whole. */
	public static final Path REPORT_DIR = Paths.get("target", "extent-report");
	public static final String SCREENSHOT_FOLDER = "screenshots";

	private static ExtentReports extent;

	private ExtentManager() {
	}

	public static synchronized ExtentReports getInstance() {
		if (extent == null) {
			ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_DIR.resolve("index.html").toFile());
			spark.config().setTheme(Theme.STANDARD);
			spark.config().setDocumentTitle("Webmail Automation Report");
			spark.config().setReportName("Webmail Automation");
			spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

			extent = new ExtentReports();
			extent.attachReporter(spark);
			extent.setSystemInfo("Java", System.getProperty("java.version"));
			extent.setSystemInfo("OS", System.getProperty("os.name"));
		}
		return extent;
	}

	public static Path screenshotDir() {
		return REPORT_DIR.resolve(SCREENSHOT_FOLDER);
	}
}
