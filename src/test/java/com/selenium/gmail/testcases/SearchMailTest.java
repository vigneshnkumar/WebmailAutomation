package com.selenium.gmail.testcases;

import static org.testng.Assert.assertTrue;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.selenium.gmail.base.BaseTest;

public class SearchMailTest extends BaseTest {

	@DataProvider(name = "searchTerms")
	public Object[][] dataProvider() {
		return excelData("SearchTests", "Search");
	}

	@Test(groups = "ui", dataProvider = "searchTerms")
	public void searchOpensResults(String searchText) {
		navigate("base_url");
		assertTrue(doLogin(), "Precondition: login should succeed");

		type("searchname", searchText);
		click("btn_search");

		// Gmail (and the mock) route search results to #search/<query>
		wait.until(ExpectedConditions.urlContains("#search"));
		assertTrue(driver.getCurrentUrl().contains("#search"), "Search results should be displayed");
		takeScreenShot();
	}
}
