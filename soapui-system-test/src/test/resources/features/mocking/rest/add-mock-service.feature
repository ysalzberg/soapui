@Automated @Regression
Feature: Add rest mock service

  Scenario: Add mock service option available in rest request context
    Given SoapUI is started
    And print Starting Scenario REST Mock
    And a new REST project is created
    When in rest request context
    Then “Add to REST mock service” option is available on REST Request
    And close SoapUI
    And print Finishing Scenario REST Mock
