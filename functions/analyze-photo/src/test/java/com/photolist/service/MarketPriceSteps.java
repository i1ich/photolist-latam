package com.photolist.service;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarketPriceSteps {

    private final MercadoLibreService service = new MercadoLibreService();

    private List<Double> prices;
    private String inputSite;
    private double medianResult;
    private String resolvedSite;

    @Given("the price list {double}, {double}, {double}")
    public void thePriceListThree(double a, double b, double c) {
        prices = Arrays.asList(a, b, c);
    }

    @Given("the price list {double}, {double}, {double}, {double}")
    public void thePriceListFour(double a, double b, double c, double d) {
        prices = Arrays.asList(a, b, c, d);
    }

    @When("I calculate the median")
    public void iCalculateTheMedian() {
        medianResult = MercadoLibreService.computeMedian(prices);
    }

    @Then("the median is {double}")
    public void theMedianIs(double expected) {
        assertEquals(expected, medianResult, 0.001);
    }

    @Given("no site is specified")
    public void noSiteIsSpecified() {
        inputSite = null;
    }

    @Given("the site {string}")
    public void theSite(String site) {
        inputSite = site;
    }

    @When("I resolve the site")
    public void iResolveTheSite() {
        resolvedSite = service.resolveSite(inputSite);
    }

    @Then("the resolved site is {string}")
    public void theResolvedSiteIs(String expected) {
        assertEquals(expected, resolvedSite);
    }
}
