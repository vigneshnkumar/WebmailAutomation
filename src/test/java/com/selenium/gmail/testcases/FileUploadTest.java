package com.selenium.gmail.testcases;

import static org.testng.Assert.assertTrue;

import java.nio.file.Path;
import java.time.Duration;

import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.selenium.gmail.base.BaseTest;
import com.selenium.gmail.config.Config;

public class FileUploadTest extends BaseTest {

	@DataProvider(name = "attachments")
	public Object[][] dataProvider() {
		return excelData("SendMailTests", "Attach");
	}

	@Test(groups = "ui", dataProvider = "attachments")
	public void sendMailWithAttachment(String sendTo, String subject, String filePath) {
		Path attachment = Config.resolvePath(filePath);

		navigate("base_url");
		assertTrue(doLogin(), "Precondition: login should succeed");

		click("btn_compose");
		type("mailbox", sendTo);
		type("subjectbox", subject);
		uploadFile("file_input", attachment);

		By attachmentChip = By.xpath("//*[contains(text()," + xpathLiteral(attachment.getFileName().toString()) + ")]");
		assertTrue(isElementVisible(attachmentChip, Duration.ofSeconds(30)),
				"Attachment " + attachment.getFileName() + " should appear in the draft");
		takeScreenShot();

		click("sendbtn");
		assertTrue(isElementVisible("message_sent"), "'Message sent' confirmation should be shown");
		takeScreenShot();
	}
}
