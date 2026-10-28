package com.selenium.gmail.testcases;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.selenium.gmail.base.BaseTest;

public class LoginTest extends BaseTest {

	@Test(groups = "ui")
	public void loginShowsInbox() {
		navigate("base_url");
		test.info("Enter username and password");
		assertTrue(doLogin(), "Inbox should be visible after logging in");
		takeScreenShot();
	}
}
