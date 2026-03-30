Feature: SauceDemo Login

  Background:
    Given I am on the SauceDemo login page

  @valid_login
  Scenario: Valid login with standard_user
    When I login with username "standard_user" and password "secret_sauce"
    Then I should be logged in successfully

  @invalid_login
  Scenario: Invalid login with wrong password
    When I login with username "standard_user" and password "wrong_password"
    Then I should see a login error message

  @empty_fields
  Scenario: Login attempt with both fields empty
    When I login with username "" and password ""
    Then I should see a login error message

  @empty_username
  Scenario: Login attempt with username only
    When I login with username "standard_user" and password ""
    Then I should see a login error message

  @empty_password
  Scenario: Login attempt with password only
    When I login with username "" and password "secret_sauce"
    Then I should see a login error message

  @locked_user
  Scenario: Locked out user cannot login
    When I login with username "locked_out_user" and password "secret_sauce"
    Then I should see a locked out error message
