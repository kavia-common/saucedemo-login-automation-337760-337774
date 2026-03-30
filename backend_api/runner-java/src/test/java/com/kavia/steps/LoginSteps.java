package com.kavia.steps;

import com.kavia.pages.InventoryPage;
import com.kavia.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Step definitions for SauceDemo login feature.
 *
 * Notes:
 * - These steps are written to be reusable across multiple scenarios (valid/invalid/locked).
 * - Navigation is performed via an explicit URL step (as defined in login.feature Background).
 * - Uses the World object for shared WebDriver context between steps.
 */
public class LoginSteps {

    private static final Logger LOG = LoggerFactory.getLogger(LoginSteps.class);

    private final World world;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;

    public LoginSteps(World world) {
        this.world = world;
    }

    @Given("user navigates to {string}")
    public void userNavigatesTo(String url) {
        LOG.info("Step: user navigates to '{}'", url);
        loginPage = new LoginPage(world.getDriver());
        inventoryPage = new InventoryPage(world.getDriver());
        loginPage.open(url);
    }

    @When("user logs in with username {string} and password {string}")
    public void userLogsInWithUsernameAndPassword(String username, String password) {
        LOG.info("Step: user logs in with username '{}' and password '{}'",
                username, password.isEmpty() ? "<empty>" : "****");
        loginPage.login(username, password);
    }

    @Then("user should be logged in successfully")
    public void userShouldBeLoggedInSuccessfully() {
        LOG.info("Step: verifying user is logged in successfully");
        MatcherAssert.assertThat(
            "Expected Inventory page to be loaded after successful login",
            inventoryPage.isLoaded(),
            is(true)
        );
        LOG.info("Assertion passed: user is logged in successfully");
    }

    @Then("user should see a login error message")
    public void userShouldSeeALoginErrorMessage() {
        LOG.info("Step: verifying login error message is displayed");
        MatcherAssert.assertThat(
            "Expected error message to be visible",
            loginPage.isErrorVisible(),
            is(true)
        );

        // Assert message contains the standard SauceDemo prefix to avoid false positives.
        String errorText = loginPage.getErrorText();
        LOG.info("Error text found: '{}'", errorText);
        MatcherAssert.assertThat(
            "Expected standard SauceDemo error prefix",
            errorText,
            containsString("Epic sadface:")
        );
        LOG.info("Assertion passed: login error message is displayed correctly");
    }

    @Then("user should see a locked out error message")
    public void userShouldSeeALockedOutErrorMessage() {
        LOG.info("Step: verifying locked out error message is displayed");
        MatcherAssert.assertThat(
            "Expected error message to be visible",
            loginPage.isErrorVisible(),
            is(true)
        );

        String errorText = loginPage.getErrorText().toLowerCase();
        LOG.info("Error text found: '{}'", errorText);
        MatcherAssert.assertThat(
            "Expected locked-out message to mention 'locked'",
            errorText,
            containsString("locked")
        );
        LOG.info("Assertion passed: locked out error message is displayed correctly");
    }
}
