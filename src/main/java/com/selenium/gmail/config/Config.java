package com.selenium.gmail.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * Reads test configuration from a classpath properties file.
 *
 * <p>Every key can be overridden without editing the file, in this order of precedence:
 * <ol>
 *   <li>JVM system property, e.g. {@code -Dmail.username=me@gmail.com}</li>
 *   <li>Environment variable, upper-cased with dots replaced by underscores, e.g. {@code MAIL_USERNAME}</li>
 *   <li>Target-specific entry in the file, e.g. {@code gmail.base_url} when {@code target=gmail}</li>
 *   <li>Plain entry in the file, e.g. {@code base_url}</li>
 * </ol>
 * This keeps real credentials out of source control.
 */
public class Config {

	public static final String DEFAULT_RESOURCE = "config.properties";
	private static final String CLASSPATH_PREFIX = "classpath:";

	private static Config instance;

	private final Properties props;

	public Config(Properties props) {
		this.props = props;
	}

	/** Shared configuration loaded from {@value #DEFAULT_RESOURCE}. */
	public static synchronized Config get() {
		if (instance == null) {
			instance = new Config(loadResource(DEFAULT_RESOURCE));
		}
		return instance;
	}

	public static Properties loadResource(String resource) {
		Properties props = new Properties();
		try (InputStream in = Config.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Config file not found on classpath: " + resource);
			}
			props.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read config file " + resource, e);
		}
		return props;
	}

	/** The application under test, e.g. {@code mock} or {@code gmail}. */
	public String target() {
		String fromOverrides = override("target");
		return fromOverrides != null ? fromOverrides : props.getProperty("target", "mock").trim();
	}

	/** Returns the value for {@code key}, or {@code null} if it is not set anywhere. */
	public String getOptional(String key) {
		String value = override(key);
		if (value == null) {
			value = props.getProperty(target() + "." + key);
		}
		if (value == null) {
			value = props.getProperty(key);
		}
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isEmpty() ? null : value;
	}

	/** Returns the value for {@code key}, failing fast with a helpful message if it is missing. */
	public String get(String key) {
		String value = getOptional(key);
		if (value == null) {
			throw new IllegalStateException("Missing config value '" + key + "' for target '" + target()
					+ "'. Set it in " + DEFAULT_RESOURCE + ", with -D" + key + "=..., or env var " + envName(key));
		}
		return value;
	}

	public String get(String key, String defaultValue) {
		String value = getOptional(key);
		return value != null ? value : defaultValue;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String value = getOptional(key);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	public int getInt(String key, int defaultValue) {
		String value = getOptional(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Config value '" + key + "' must be an integer but was '" + value + "'", e);
		}
	}

	/**
	 * Returns a URL value. A {@code classpath:} prefix is resolved to a {@code file:} URL so local
	 * HTML fixtures can be opened in the browser. A query string or fragment is preserved.
	 */
	public String getUrl(String key) {
		String value = get(key);
		if (!value.startsWith(CLASSPATH_PREFIX)) {
			return value;
		}
		String resource = value.substring(CLASSPATH_PREFIX.length());
		String suffix = "";
		int split = indexOfAny(resource, '?', '#');
		if (split >= 0) {
			suffix = resource.substring(split);
			resource = resource.substring(0, split);
		}
		return resourcePath(resource).toUri() + suffix;
	}

	private static int indexOfAny(String s, char a, char b) {
		int i = s.indexOf(a);
		int j = s.indexOf(b);
		if (i < 0) {
			return j;
		}
		return j < 0 ? i : Math.min(i, j);
	}

	/** Returns a file path value; relative paths are looked up on the classpath. */
	public Path getPath(String key) {
		return resolvePath(get(key));
	}

	/** Resolves an absolute path as-is, otherwise treats the value as a classpath resource. */
	public static Path resolvePath(String value) {
		Path path = Paths.get(value);
		return path.isAbsolute() ? path : resourcePath(value.replace('\\', '/'));
	}

	/** Resolves a classpath resource to a file on disk (test resources are always unpacked). */
	public static Path resourcePath(String resource) {
		URL url = Config.class.getClassLoader().getResource(resource);
		if (url == null) {
			throw new IllegalStateException("Resource not found on classpath: " + resource);
		}
		try {
			return Paths.get(url.toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid resource URL: " + url, e);
		}
	}

	static String envName(String key) {
		return key.toUpperCase(Locale.ROOT).replace('.', '_');
	}

	private String override(String key) {
		String value = System.getProperty(key);
		if (value == null || value.isBlank()) {
			value = System.getenv(envName(key));
		}
		return value == null || value.isBlank() ? null : value.trim();
	}
}
