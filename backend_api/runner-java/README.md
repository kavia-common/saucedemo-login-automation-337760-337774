# runner-java

Java/Maven Cucumber + Selenium runner for SauceDemo login scenarios.

## Running locally (conceptual)

```bash
mvn test
```

Filter tags:

```bash
mvn test -Dcucumber.filter.tags=@valid_login
```

Browser (best-effort):

```bash
mvn test -Dqa.browser=chrome
```

## Sauce Labs (optional)

Set:
- `SAUCE_USERNAME`
- `SAUCE_ACCESS_KEY`

Then `mvn test` will use Sauce Labs RemoteWebDriver.
