Feature: Market price statistics

  Scenario: Median of an odd number of prices
    Given the price list 100.0, 200.0, 300.0
    When I calculate the median
    Then the median is 200.0

  Scenario: Median of an even number of prices
    Given the price list 100.0, 200.0, 300.0, 400.0
    When I calculate the median
    Then the median is 250.0

  Scenario: Site defaults to MLA when none is provided
    Given no site is specified
    When I resolve the site
    Then the resolved site is "MLA"

  Scenario: Provided site is normalized to uppercase
    Given the site "mla"
    When I resolve the site
    Then the resolved site is "MLA"
