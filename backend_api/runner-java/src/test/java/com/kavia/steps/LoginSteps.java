package com.kavia.steps;

import com.kavia.pages.InventoryPage;
import com.kavia.pages.LoginPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * Step definitions for SauceDemo login feature.
 *
 * Flow: LoginTestFlow
 * Entrypoint: Cucumber discovers these steps via glue configuration.
 *
 * Contract:
 * - Input: Gherkin steps from login.feature with parameterized credentials and expected outcomes
 * - Output: Pass/fail assertions for each scenario
 * - Side effects: WebDriver navigation, page interactions, screenshot on failure (via Hooks)
 *
 * Notes:
 * - These steps are written to be reusable across multiple scenarios
 *   (valid/invalid/locked/logout/problem_user/performance_glitch_user/error UI).
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

    // ────────────────────────────────────────────
    // NAVIGATION STEPS
    // ────────────────────────────────────────────

    @Given("user navigates to {string}")
    public void userNavigatesTo(String url) {
        LOG.info("Step: user navigates to '{}'", url);
        loginPage = new LoginPage(world.getDriver());
        inventoryPage = new InventoryPage(world.getDriver());
        loginPage.open(url);
    }

    // ────────────────────────────────────────────
    // LOGIN STEPS
    // ────────────────────────────────────────────

    @When("user logs in with username {string} and password {string}")
    public void userLogsInWithUsernameAndPassword(String username, String password) {
        LOG.info("Step: user logs in with username '{}' and password '{}'",
                username, password.isEmpty() ? "<empty>" : "****");
        loginPage.login(username, password);
    }

    // ────────────────────────────────────────────
    // SUCCESSFUL LOGIN ASSERTIONS
    // ────────────────────────────────────────────

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

    @Then("user should be logged in successfully within {int} seconds")
    public void userShouldBeLoggedInSuccessfullyWithinSeconds(int timeoutSeconds) {
        LOG.info("Step: verifying user is logged in successfully within {}s (performance_glitch_user)", timeoutSeconds);
        MatcherAssert.assertThat(
            "Expected Inventory page to load within " + timeoutSeconds + " seconds",
            inventoryPage.isLoadedWithinTimeout(timeoutSeconds),
            is(true)
        );
        LOG.info("Assertion passed: user logged in within {}s", timeoutSeconds);
    }

    // ────────────────────────────────────────────
    // INVENTORY PAGE ASSERTIONS
    // ────────────────────────────────────────────

    @And("user should see the inventory page with products")
    public void userShouldSeeTheInventoryPageWithProducts() {
        LOG.info("Step: verifying inventory page shows products");
        MatcherAssert.assertThat(
            "Expected at least one product image on inventory page",
            inventoryPage.getProductImageCount(),
            greaterThan(0)
        );
        LOG.info("Assertion passed: inventory page shows products");
    }

    @And("user should see broken product images on inventory page")
    public void userShouldSeeBrokenProductImagesOnInventoryPage() {
        LOG.info("Step: verifying problem_user sees broken product images");
        MatcherAssert.assertThat(
            "Expected broken product images for problem_user on inventory page",
            inventoryPage.hasBrokenProductImages(),
            is(true)
        );
        LOG.info("Assertion passed: broken product images detected for problem_user");
    }

    // ────────────────────────────────────────────
    // LOGOUT STEPS
    // ────────────────────────────────────────────

    @When("user opens the hamburger menu")
    public void userOpensTheHamburgerMenu() {
        LOG.info("Step: opening hamburger menu");
        inventoryPage.openBurgerMenu();
    }

    @And("user clicks the logout link")
    public void userClicksTheLogoutLink() {
        LOG.info("Step: clicking logout link");
        inventoryPage.clickLogout();
    }

    @Then("user should be on the login page")
    public void userShouldBeOnTheLoginPage() {
        LOG.info("Step: verifying user is redirected to login page after logout");
        MatcherAssert.assertThat(
            "Expected user to be on the login page after logout",
            loginPage.isLoginPage(),
            is(true)
        );
        LOG.info("Assertion passed: user is on the login page");
    }

    // ────────────────────────────────────────────
    // ERROR MESSAGE ASSERTIONS
    // ────────────────────────────────────────────

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

    @Then("user should see a login error message containing {string}")
    public void userShouldSeeALoginErrorMessageContaining(String expectedSubstring) {
        LOG.info("Step: verifying login error message contains '{}'", expectedSubstring);
        MatcherAssert.assertThat(
            "Expected error message to be visible",
            loginPage.isErrorVisible(),
            is(true)
        );

        String errorText = loginPage.getErrorText();
        LOG.info("Error text found: '{}'", errorText);
        MatcherAssert.assertThat(
            "Expected error message to contain: " + expectedSubstring,
            errorText,
            containsString(expectedSubstring)
        );
        LOG.info("Assertion passed: error message contains '{}'", expectedSubstring);
    }

    // ────────────────────────────────────────────
    // ERROR UI ASSERTIONS
    // ────────────────────────────────────────────

    @Then("user should see error icons on the input fields")
    public void userShouldSeeErrorIconsOnTheInputFields() {
        LOG.info("Step: verifying error icons are displayed on input fields");
        MatcherAssert.assertThat(
            "Expected error icons (red exclamation marks) to be visible on input fields",
            loginPage.areErrorIconsVisible(),
            is(true)
        );
        LOG.info("Assertion passed: error icons are visible on input fields");
    }

    @When("user clicks the error close button")
    public void userClicksTheErrorCloseButton() {
        LOG.info("Step: clicking error close button");
        loginPage.clickErrorCloseButton();
    }

    @Then("user should not see a login error message")
    public void userShouldNotSeeALoginErrorMessage() {
        LOG.info("Step: verifying login error message is no longer displayed");
        MatcherAssert.assertThat(
            "Expected error message to not be visible after dismissal",
            loginPage.isErrorVisible(),
            is(false)
        );
        LOG.info("Assertion passed: login error message is no longer visible");
    }
}
