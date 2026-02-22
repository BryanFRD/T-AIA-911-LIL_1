Feature: Orchestrator

  Background:
    Given the database is clean

  Scenario: Successfully persist and retrieve a train stop
    Given a stop exists with ID "STOP1" and name "Marseille"
    When I retrieve all stops
    Then I should find 1 stop named "Marseille"
