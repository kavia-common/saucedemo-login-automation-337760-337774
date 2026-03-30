package com.kavia.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit Platform Cucumber runner.
 *
 * This is the primary test runner class that discovers and executes
 * all Cucumber feature files in the classpath:features directory.
 *
 * Contract:
 * - Discovers features under classpath:features
 * - Uses glue under com.kavia.steps (step definitions, hooks)
 * - Emits pretty console output + JSON report + HTML report for CI
 *
 * Usage:
 *   mvn test                                          -- run all scenarios
 *   mvn test -Dcucumber.filter.tags="@valid_login"    -- run only tagged scenarios
 *   mvn test -Dqa.browser=chrome                      -- specify browser
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.kavia.steps")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, summary, json:target/cucumber-reports/cucumber.json, html:target/cucumber-reports/cucumber.html"
)
public class RunCucumberTest {
    // This class serves as the Cucumber test runner entry point.
    // All configuration is handled via annotations above.
}
