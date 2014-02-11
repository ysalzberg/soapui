@Manual @Regression
Feature: Add SOAP mock service

  Scenario: Add mock service option available in soap operation context
    Given SoapUI is started
    And print Starting Scenario Soap Mock
    And a new SOAP project is created
    When in soap operation context
    Then “Add to MockService” option is available
    And close SoapUI
    And print Finishing Scenario Soap Mock
