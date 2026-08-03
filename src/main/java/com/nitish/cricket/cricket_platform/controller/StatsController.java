package com.nitish.cricket.cricket_platform.controller;

import com.nitish.cricket.cricket_platform.dto.stats.*;
import com.nitish.cricket.cricket_platform.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stats")
@CrossOrigin
public class StatsController {

    @Autowired
    private StatsService statsService;

    // 🏏 Batters
    @GetMapping("/batters/top-runs")
    public List<TopRunGettersResponse> getTopRunGetters(@RequestParam(defaultValue = "0") int page) {
        return statsService.getTopRunGetters(page);
    }

    @GetMapping("/batters/strike-rate")
    public List<HighestStrikeRateResponse> getHighestStrikeRate(@RequestParam(defaultValue = "0") int page) {
        return statsService.getHighestStrikeRate(page);
    }

    @GetMapping("/batters/average")
    public List<HighestAverageResponse> getHighestAverage(@RequestParam(defaultValue = "0") int page) {
        return statsService.getHighestAverage(page);
    }

    @GetMapping("/batters/fours")
    public List<MostFoursResponse> getMostFours(@RequestParam(defaultValue = "0") int page) {
        return statsService.getMostFours(page);
    }

    @GetMapping("/batters/sixes")
    public List<MostSixesResponse> getMostSixes(@RequestParam(defaultValue = "0") int page) {
        return statsService.getMostSixes(page);
    }

    // 🎯 Bowlers
    @GetMapping("/bowlers/wickets")
    public List<TopWicketTakersResponse> getTopWicketTakers(@RequestParam(defaultValue = "0") int page) {
        return statsService.getTopWicketTakers(page);
    }

    @GetMapping("/bowlers/economy")
    public List<LowestEconomyResponse> getLowestEconomy(@RequestParam(defaultValue = "0") int page) {
        return statsService.getLowestEconomy(page);
    }

    @GetMapping("/bowlers/strike-rate")
    public List<LowestBowlingStrikeRateResponse> getLowestStrikeRate(@RequestParam(defaultValue = "0") int page) {
        return statsService.getLowestStrikeRate(page);
    }

    @GetMapping("/bowlers/average")
    public List<LowestBowlingAverageResponse> getLowestAverage(@RequestParam(defaultValue = "0") int page) {
        return statsService.getLowestAverage(page);
    }
}