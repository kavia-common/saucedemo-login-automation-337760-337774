Feature: SauceDemo Login

  As a user of SauceDemo
  I want to log in with valid or invalid credentials
  So that I can access the application only when authorized

  Background:
    Given user navigates to "https://www.saucedemo.com"

  # ────────────────────────────────────────────
  # VALID LOGIN SCENARIOS
  # ────────────────────────────────────────────

  @valid_login @smoke
  Scenario: Valid login with standard_user
    When user logs in with username "standard_user" and password "secret_sauce"
    Then user should be logged in successfully

  @valid_login @regression
  Scenario: Valid login with problem_user
    When user logs in with username "problem_user" and password "secret_sauce"
    Then user should be logged in successfully
    And user should see the inventory page with products

  @valid_login @regression
  Scenario: Valid login with performance_glitch_user
    When user logs in with username "performance_glitch_user" and password "secret_sauce"
    Then user should be logged in successfully within 10 seconds

  # ────────────────────────────────────────────
  # LOGOUT AFTER LOGIN
  # ────────────────────────────────────────────

  @logout @smoke
  Scenario: Logout after successful login
    When user logs in with username "standard_user" and password "secret_sauce"
    Then user should be logged in successfully
    When user opens the hamburger menu
    And user clicks the logout link
    Then user should be on the login page

  @logout @regression
  Scenario: Logout after login with problem_user
    When user logs in with username "problem_user" and password "secret_sauce"
    Then user should be logged in successfully
    When user opens the hamburger menu
    And user clicks the logout link
    Then user should be on the login page

  # ────────────────────────────────────────────
  # NEGATIVE LOGIN SCENARIOS
  # ────────────────────────────────────────────

  @login_negative @regression
  Scenario Outline: Unsuccessful login attempts show an error message
    When user logs in with username "<username>" and password "<password>"
    Then user should see a login error message

    Examples:
      | case                 | username         | password        |
      | invalid_password     | standard_user    | wrong_password  |
      | empty_both_fields    |                  |                 |
      | empty_username       |                  | secret_sauce    |
      | empty_password       | standard_user    |                 |

  @locked_user @regression
  Scenario: Locked out user cannot login
    When user logs in with username "locked_out_user" and password "secret_sauce"
    Then user should see a locked out error message

  # ────────────────────────────────────────────
  # ERROR MESSAGE CONTENT VALIDATION
  # ────────────────────────────────────────────

  @error_validation @regression
  Scenario: Invalid credentials show correct error text
    When user logs in with username "standard_user" and password "wrong_password"
    Then user should see a login error message containing "Username and password do not match"

  @error_validation @regression
  Scenario: Empty username shows required field error
    When user logs in with username "" and password "secret_sauce"
    Then user should see a login error message containing "Username is required"

  @error_validation @regression
  Scenario: Empty password shows required field error
    When user logs in with username "standard_user" and password ""
    Then user should see a login error message containing "Password is required"

  @error_validation @regression
  Scenario: Both fields empty shows username required error
    When user logs in with username "" and password ""
    Then user should see a login error message containing "Username is required"

  # ────────────────────────────────────────────
  # ERROR UI VALIDATIONS
  # ────────────────────────────────────────────

  @error_ui @regression
  Scenario: Error icon is displayed on input fields after failed login
    When user logs in with username "standard_user" and password "wrong_password"
    Then user should see error icons on the input fields

  @error_ui @regression
  Scenario: Error message can be dismissed by clicking close button
    When user logs in with username "standard_user" and password "wrong_password"
    Then user should see a login error message
    When user clicks the error close button
    Then user should not see a login error message

  # ────────────────────────────────────────────
  # PROBLEM USER BEHAVIOR VALIDATION
  # ────────────────────────────────────────────

  @problem_user @regression
  Scenario: Problem user sees broken product images on inventory page
    When user logs in with username "problem_user" and password "secret_sauce"
    Then user should be logged in successfully
    And user should see broken product images on inventory page
