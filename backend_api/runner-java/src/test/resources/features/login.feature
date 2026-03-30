Feature: SauceDemo Login

  As a user of SauceDemo
  I want to log in with valid or invalid credentials
  So that I can access the application only when authorized

  Background:
    Given user navigates to "https://www.saucedemo.com"

  @valid_login
  Scenario: Valid login with standard_user
    When user logs in with username "standard_user" and password "secret_sauce"
    Then user should be logged in successfully

  @login_negative
  Scenario Outline: Unsuccessful login attempts show an error message
    When user logs in with username "<username>" and password "<password>"
    Then user should see a login error message

    Examples:
      | case                 | username         | password        |
      | invalid_password     | standard_user    | wrong_password  |
      | empty_both_fields    |                  |                 |
      | empty_username       |                  | secret_sauce    |
      | empty_password       | standard_user    |                |

  @locked_user
  Scenario: Locked out user cannot login
    When user logs in with username "locked_out_user" and password "secret_sauce"
    Then user should see a locked out error message
