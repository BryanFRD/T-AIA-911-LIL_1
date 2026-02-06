package com.epitech.travelorder.orchestrator.step;

import com.epitech.travelorder.common.db.Stop;
import com.epitech.travelorder.common.repository.StopRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class StopStep {

    private final StopRepository stopRepository;

    private List<Stop> resultList;

    @Given("the database is clean")
    public void clearDatabase() {
        stopRepository.deleteAll();
    }

    @Given("a stop exists with ID {string} and name {string}")
    @Transactional
    public void createStop(String id, String name) {
        Stop stop = new Stop(id, name, 43.2965, 5.3698, 1, 0, null);
        stopRepository.save(stop);
    }

    @When("I retrieve all stops")
    public void fetchAllStops() {
        resultList = stopRepository.findAll();
    }

    @Then("I should find {int} stop named {string}")
    public void verifyStopCountAndName(int expectedCount, String expectedName) {
        assertThat(resultList).hasSize(expectedCount);
        assertThat(resultList.getFirst().getStopName()).isEqualTo(expectedName);
    }
}