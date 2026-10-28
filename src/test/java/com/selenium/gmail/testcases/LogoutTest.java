package com.selenium.gmail.testcases;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.Duration;

import org.testng.annotations.Test;

import com.selenium.gmail.base.BaseTest;

public class LogoutTest extends BaseTest {

	@Test(groups = "ui")
	public void logoutEndsSession() {
		navigate("base_url");
		assertTrue(doLogin(), "Precondition: login should succeed");

		test.info("Logging out");
		logout();

		// Re-opening the mailbox must not show the inbox again
		navigate("base_url");
		assertFalse(isElementVisible(locator("inbox_link"), Duration.ofSeconds(5)),
				"Inbox should not be accessible after logging out");
		takeScreenShot();
	}
}
