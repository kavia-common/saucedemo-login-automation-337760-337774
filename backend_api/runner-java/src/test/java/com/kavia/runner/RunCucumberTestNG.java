package com.kavia.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG-based Cucumber runner.
 *
 * This runner can be used as an alternative to the JUnit Platform runner
 * for environments that prefer TestNG (e.g., some CI configurations).
 *
 * Contract:
 * - Discovers features under classpath:features
 * - Uses glue under com.kavia.steps (step definitions, hooks)
 * - Emits pretty console output + JSON report + HTML report
 *
 * Usage (via testng.xml or Maven):
 *   mvn test -Dtest=RunCucumberTestNG
 */
@CucumberOptions(
    features = "classpath:features",
    glue = "com.kavia.steps",
    plugin = {
        "pretty",
        "summary",
        "json:target/cucumber-reports/cucumber-testng.json",
        "html:target/cucumber-reports/cucumber-testng.html"
    },
    monochrome = true
)
public class RunCucumberTestNG extends AbstractTestNGCucumberTests {

    /**
     * Override to enable parallel execution of scenarios.
     * Set parallel = true for concurrent scenario execution.
     *
     * @return Object[][] of scenario data for TestNG data provider
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
