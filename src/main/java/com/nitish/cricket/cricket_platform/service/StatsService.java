package com.nitish.cricket.cricket_platform.service;

import com.nitish.cricket.cricket_platform.dto.stats.*;
import com.nitish.cricket.cricket_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsService {

    @Autowired
    private UserRepository userRepository;

    public List<TopRunGettersResponse> getTopRunGetters(int page) {
        return userRepository.findTopRunGetters(PageRequest.of(page, 20));
    }

    public List<HighestStrikeRateResponse> getHighestStrikeRate(int page) {
        return userRepository.findHighestStrikeRate(PageRequest.of(page, 20));
    }

    public List<HighestAverageResponse> getHighestAverage(int page) {
        return userRepository.findHighestAverage(PageRequest.of(page, 20));
    }

    public List<MostFoursResponse> getMostFours(int page) {
        return userRepository.findMostFours(PageRequest.of(page, 20));
    }

    public List<MostSixesResponse> getMostSixes(int page) {
        return userRepository.findMostSixes(PageRequest.of(page, 20));
    }

    public List<TopWicketTakersResponse> getTopWicketTakers(int page) {
        return userRepository.findTopWicketTakers(PageRequest.of(page, 20));
    }

    public List<LowestEconomyResponse> getLowestEconomy(int page) {
        return userRepository.findLowestEconomy(PageRequest.of(page, 20));
    }

    public List<LowestBowlingStrikeRateResponse> getLowestStrikeRate(int page) {
        return userRepository.findLowestStrikeRate(PageRequest.of(page, 20));
    }

    public List<LowestBowlingAverageResponse> getLowestAverage(int page) {
        return userRepository.findLowestAverage(PageRequest.of(page, 20));
    }
}
