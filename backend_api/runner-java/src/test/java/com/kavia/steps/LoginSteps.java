package com.kavia.steps;

import com.kavia.pages.InventoryPage;
import com.kavia.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.hamcrest.MatcherAssert;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Step definitions for SauceDemo login feature.
 *
 * Notes:
 * - These steps are written to be reusable across multiple scenarios (valid/invalid/locked).
 * - Navigation is performed via an explicit URL step (as defined in login.feature Background).
 */
public class LoginSteps {

    private final World world;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;

    public LoginSteps(World world) {
        this.world = world;
    }

    @Given("user navigates to {string}")
    public void userNavigatesTo(String url) {
        loginPage = new LoginPage(world.getDriver());
        inventoryPage = new InventoryPage(world.getDriver());
        loginPage.open(url);
    }

    @When("user logs in with username {string} and password {string}")
    public void userLogsInWithUsernameAndPassword(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("user should be logged in successfully")
    public void userShouldBeLoggedInSuccessfully() {
        MatcherAssert.assertThat("Expected Inventory page to be loaded", inventoryPage.isLoaded(), is(true));
    }

    @Then("user should see a login error message")
    public void userShouldSeeALoginErrorMessage() {
        MatcherAssert.assertThat("Expected error message to be visible", loginPage.isErrorVisible(), is(true));

        // Assert message contains the standard SauceDemo prefix to avoid false positives.
        String errorText = loginPage.getErrorText();
        MatcherAssert.assertThat(
                "Expected standard SauceDemo error prefix",
                errorText,
                containsString("Epic sadface:")
        );
    }

    @Then("user should see a locked out error message")
    public void userShouldSeeALockedOutErrorMessage() {
        MatcherAssert.assertThat("Expected error message to be visible", loginPage.isErrorVisible(), is(true));

        String errorText = loginPage.getErrorText().toLowerCase();
        MatcherAssert.assertThat("Expected locked-out message to mention 'locked'", errorText, containsString("locked"));
    }
}
