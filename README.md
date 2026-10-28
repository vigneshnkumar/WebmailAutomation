# Webmail Automation

[![CI](https://github.com/vigneshnkumar/WebmailAutomation/actions/workflows/ci.yml/badge.svg)](https://github.com/vigneshnkumar/WebmailAutomation/actions/workflows/ci.yml)

A data-driven UI test framework for a web-based mail client (Gmail), built with **Selenium 4**, **TestNG**,
**Apache POI** (Excel test data) and **ExtentReports** (HTML reporting).

It automates the core mail flows (log in, search, compose with an attachment and send, log out) and
ships with a local **mock mailbox** that mirrors Gmail's page structure. The whole suite therefore runs
offline and in CI with one command, and the same tests can be pointed at the real Gmail.

## Quick start

Requirements: **Java 17+** and **Google Chrome**. Maven is not required; the included wrapper downloads it.

```bash
./mvnw test          # macOS / Linux
mvnw.cmd test        # Windows
```

This runs:

| Suite | What it covers | Browser |
|-------|----------------|---------|
| Unit (`-Dgroups=unit`) | Excel data reader (tables, blank cells, formulas, malformed input) and config loading | none |
| UI (`-Dgroups=ui`) | Login, search (data-driven), compose + attachment + send, logout | headless Chrome against `mock/mailbox.html` |

The HTML report, with screenshots, is written to `target/extent-report/index.html`.

## Common options

Any setting in [`config.properties`](src/test/resources/config.properties) can be overridden on the
command line (`-Dkey=value`) or with an environment variable (`KEY`, with dots replaced by underscores).

```bash
./mvnw test -Dgroups=unit               # fast checks, no browser needed
./mvnw test -Dheadless=false            # watch the browser
./mvnw test -Dbrowser=firefox           # chrome | firefox | edge
```

Selenium Manager (built into Selenium 4.6+) downloads the matching browser driver automatically, so
there is no chromedriver or geckodriver to install.

### Running against real Gmail

```bash
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD='your-password'
./mvnw test -Dgroups=ui -Dtarget=gmail -Dheadless=false
```

Without credentials, the Gmail tests are **skipped** rather than failed. Note that Google often blocks
sign-in from automated browsers ("This browser or app may not be secure") and changes its markup over
time. The default target is the mock mailbox for that reason. Use a dedicated test account, never a
personal one.

## Project structure

```
src/main/java/com/selenium/gmail/
├── config/Config.java              # properties loader with -D / env-var overrides
├── driver/DriverFactory.java       # Chrome / Firefox / Edge, headless option
└── utilities/
    ├── ExcelDataReader.java        # reads marker-delimited tables from .xlsx for @DataProvider
    └── ExtentManager.java          # shared ExtentReports instance
src/test/java/com/selenium/gmail/
├── base/BaseTest.java              # browser lifecycle, waits, actions, screenshots, reporting
├── testcases/                      # LoginTest, SearchMailTest, FileUploadTest, LogoutTest
├── config/ConfigTest.java          # unit tests
└── utilities/ExcelDataReaderTest.java
src/test/resources/
├── config.properties               # target, browser, URLs, XPath locators
├── mock/mailbox.html               # offline stand-in for Gmail
├── testdata/ExcelData.xlsx         # data-driven test inputs
└── testng.xml                      # suite file for running from an IDE
```

## Test data format

`ExcelDataReader` reads a table bounded by two cells containing the table name: one at the top-left
and one at the bottom-right. The cells strictly between them become rows for a TestNG `@DataProvider`,
and the marker row can hold column headers:

|            | B             | C            | D                     | E      |
|------------|---------------|--------------|-----------------------|--------|
| **Attach** | sendTo        | subject      | filePath              |        |
|            | a@example.com | Hello        | testdata/testfile.txt |        |
|            |               |              |                       | **Attach** |

Relative file paths are resolved from the test classpath (`src/test/resources`).

## Running from an IDE

Import the folder as a **Maven project** (IntelliJ IDEA, Eclipse, VS Code). Then run
`src/test/resources/testng.xml` or any test class with the TestNG runner.

## License

[MIT](LICENSE)
