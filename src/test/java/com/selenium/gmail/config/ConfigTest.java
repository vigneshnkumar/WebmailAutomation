package com.selenium.gmail.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.nio.file.Files;
import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;


@Test(groups = "unit")
public class ConfigTest {

	private static Config config(String... keyValues) {
		Properties props = new Properties();
		for (int i = 0; i < keyValues.length; i += 2) {
			props.setProperty(keyValues[i], keyValues[i + 1]);
		}
		return new Config(props);
	}

	@AfterMethod(alwaysRun = true)
	public void clearOverrides() {
		System.clearProperty("cfgtest.key");
		System.clearProperty("target");
	}

	@Test
	public void targetSpecificValueWinsOverPlainValue() {
		Config c = config("target", "gmail", "gmail.url", "https://mail.google.com", "url", "plain");
		assertEquals(c.get("url"), "https://mail.google.com");
	}

	@Test
	public void plainValueIsUsedWhenNoTargetSpecificValue() {
		assertEquals(config("target", "mock", "url", "plain").get("url"), "plain");
	}

	@Test
	public void systemPropertyOverridesFile() {
		System.setProperty("cfgtest.key", "from-jvm");
		assertEquals(config("cfgtest.key", "from-file").get("cfgtest.key"), "from-jvm");
	}

	@Test
	public void systemPropertyCanSwitchTarget() {
		System.setProperty("target", "gmail");
		Config c = config("target", "mock", "mock.url", "m", "gmail.url", "g");
		assertEquals(c.target(), "gmail");
		assertEquals(c.get("url"), "g");
	}

	@Test
	public void blankValuesCountAsMissing() {
		Config c = config("mail.username", "   ");
		assertNull(c.getOptional("mail.username"));
		IllegalStateException e = expectThrows(IllegalStateException.class, () -> c.get("mail.username"));
		assertTrue(e.getMessage().contains("MAIL_USERNAME"), e.getMessage());
	}

	@Test
	public void typedGettersUseDefaultsAndValidate() {
		Config c = config("timeout", "20", "headless", "false", "bad", "ten");
		assertEquals(c.getInt("timeout", 5), 20);
		assertEquals(c.getInt("absent", 5), 5);
		assertFalse(c.getBoolean("headless", true));
		assertThrows(IllegalStateException.class, () -> c.getInt("bad", 5));
	}

	@Test
	public void classpathUrlResolvesToLocalFileAndKeepsQuery() {
		Config c = config("page", "classpath:mock/mailbox.html?logout");
		String url = c.getUrl("page");
		assertTrue(url.startsWith("file:"), url);
		assertTrue(url.endsWith("mailbox.html?logout"), url);
	}

	@Test
	public void projectConfigPointsAtExistingFiles() {
		Config c = Config.get();
		assertTrue(Files.isRegularFile(c.getPath("excel_path")));
		for (String key : new String[] { "logintextbox", "btn_next", "passwordtextbox", "btn_login", "inbox_link",
				"btn_compose", "mailbox", "subjectbox", "file_input", "sendbtn", "message_sent", "searchname",
				"btn_search" }) {
			assertTrue(c.getOptional(key) != null, "Missing locator: " + key);
		}
	}
}
