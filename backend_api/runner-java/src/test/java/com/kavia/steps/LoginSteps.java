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
 */
public class LoginSteps {

    private final World world;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;

    public LoginSteps(World world) {
        this.world = world;
    }

    @Given("I am on the SauceDemo login page")
    public void iAmOnTheLoginPage() {
        loginPage = new LoginPage(world.getDriver());
        inventoryPage = new InventoryPage(world.getDriver());
        loginPage.open();
    }

    @When("I login with username {string} and password {string}")
    public void iLoginWithUsernameAndPassword(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("I should be logged in successfully")
    public void iShouldBeLoggedInSuccessfully() {
        MatcherAssert.assertThat("Expected Inventory page to be loaded", inventoryPage.isLoaded(), is(true));
    }

    @Then("I should see a login error message")
    public void iShouldSeeLoginErrorMessage() {
        MatcherAssert.assertThat("Expected error message to be visible", loginPage.isErrorVisible(), is(true));
    }

    @Then("I should see a locked out error message")
    public void iShouldSeeLockedOutErrorMessage() {
        MatcherAssert.assertThat("Expected error message to be visible", loginPage.isErrorVisible(), is(true));
        MatcherAssert.assertThat(loginPage.getErrorText().toLowerCase(), containsString("locked"));
    }
}
