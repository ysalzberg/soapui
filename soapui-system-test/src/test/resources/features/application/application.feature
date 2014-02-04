@Automated @Regression
Feature: Application

  Scenario: The main window is showing up without error when starting up SoapUI
    Given SoapUI is started
    And print Starting Scenario Application
    Then ensure that the main window is showing up without error
    And close SoapUI
    And print Finishing Scenario Application