# SauceDemo Login Automation - Cucumber Framework

Maven-based Cucumber BDD automation framework with Selenium WebDriver for testing
the [SauceDemo](https://www.saucedemo.com) login functionality.

## Project Structure

```
runner-java/
├── pom.xml                           # Maven project configuration & dependencies
├── README.md                         # This file
└── src/
    └── test/
        ├── java/
        │   └── com/
        │       └── kavia/
        │           ├── runner/        # Test runners (JUnit 5 + TestNG)
        │           │   ├── RunCucumberTest.java      # JUnit Platform runner
        │           │   └── RunCucumberTestNG.java     # TestNG runner
        │           ├── steps/         # Step definitions & hooks
        │           │   ├── Hooks.java        # Before/After lifecycle hooks
        │           │   ├── LoginSteps.java   # Login feature step definitions
        │           │   └── World.java        # Scenario-scoped state holder
        │           ├── pages/         # Page Object Model classes
        │           │   ├── LoginPage.java       # Login page interactions
        │           │   └── InventoryPage.java   # Inventory page verification
        │           ├── support/       # WebDriver support
        │           │   └── WebDriverFactory.java  # Driver creation & config
        │           └── utils/         # Utility classes
        │               ├── ConfigReader.java     # Configuration management
        │               ├── ScreenshotUtil.java   # Screenshot capture
        │               └── WaitUtil.java         # Explicit wait helpers
        └── resources/
            ├── features/              # Cucumber feature files (BDD scenarios)
            │   └── login.feature
            ├── config.properties      # Framework configuration defaults
            ├── junit-platform.properties  # JUnit Platform settings
            └── simplelogger.properties    # SLF4J logging configuration
```

## Dependencies

| Dependency       | Version  | Purpose                              |
|------------------|----------|--------------------------------------|
| Cucumber         | 7.18.1   | BDD test framework                   |
| Selenium         | 4.23.0   | Browser automation                   |
| JUnit 5          | 5.10.2   | Test runner (primary)                |
| TestNG           | 7.9.0    | Test runner (alternative)            |
| WebDriverManager | 5.9.2    | Automatic browser driver management  |
| SLF4J            | 2.0.13   | Logging framework                    |
| Hamcrest         | 2.2      | Assertion matchers                   |

## Running Tests

### Run all scenarios
```bash
mvn test
```

### Run by tag
```bash
mvn test -Dcucumber.filter.tags="@valid_login"
mvn test -Dcucumber.filter.tags="@smoke"
mvn test -Dcucumber.filter.tags="@regression"
mvn test -Dcucumber.filter.tags="@locked_user"
```

### Specify browser
```bash
mvn test -Dqa.browser=chrome
mvn test -Dqa.browser=firefox
```

### Disable headless mode (for local debugging)
```bash
mvn test -Dbrowser.headless=false
```

### Use TestNG runner
```bash
mvn test -Dtest=RunCucumberTestNG
```

## Reports

After test execution, reports are generated in:
- **JSON**: `target/cucumber-reports/cucumber.json`
- **HTML**: `target/cucumber-reports/cucumber.html`
- **Screenshots** (on failure): `target/screenshots/`

## Sauce Labs (Optional Remote Execution)

Set environment variables:
```bash
export SAUCE_USERNAME=your_username
export SAUCE_ACCESS_KEY=your_access_key
mvn test
```

## Configuration

Framework configuration is loaded from `src/test/resources/config.properties`.
All properties can be overridden by environment variables or system properties.

| Property               | Default                        | Description                  |
|------------------------|--------------------------------|------------------------------|
| `base.url`             | `https://www.saucedemo.com`    | Application URL              |
| `browser.name`         | `chrome`                       | Browser (chrome/firefox)     |
| `browser.headless`     | `true`                         | Headless mode                |
| `implicit.wait.seconds`| `2`                            | Implicit wait timeout        |
| `explicit.wait.seconds`| `10`                           | Explicit wait timeout        |
