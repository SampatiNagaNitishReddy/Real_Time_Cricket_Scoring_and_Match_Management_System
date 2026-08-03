package com.nitish.cricket.cricket_platform.repository;

import com.nitish.cricket.cricket_platform.dto.stats.*;
import com.nitish.cricket.cricket_platform.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email); // for login or checking duplicates

    Optional<User> findByUserId(String userId); // for fetching by public user ID

    boolean existsByEmail(String email); // for registration checks

    boolean existsByUserId(String userId); // optional, for conflict checks

    void deleteByUserId(String userId); //delete By UserId

    List<User> findAllByUserIdIn(List<String> userIds);

    // 🏏 BATTERS

    @Aggregation(pipeline = {
            "{ $match: { 'stats.batter.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.batter.matches', runs: '$stats.batter.totalRuns' } }",
            "{ $sort: { runs: -1 } }"
    })
    List<TopRunGettersResponse> findTopRunGetters(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.batter.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.batter.matches', strikeRate: '$stats.batter.strikeRate' } }",
            "{ $sort: { strikeRate: -1 } }"
    })
    List<HighestStrikeRateResponse> findHighestStrikeRate(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.batter.dismissals': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.batter.matches', average: '$stats.batter.average' } }",
            "{ $sort: { average: -1 } }"
    })
    List<HighestAverageResponse> findHighestAverage(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.batter.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.batter.matches', fours: '$stats.batter.fours' } }",
            "{ $sort: { fours: -1 } }"
    })
    List<MostFoursResponse> findMostFours(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.batter.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.batter.matches', sixes: '$stats.batter.sixes' } }",
            "{ $sort: { sixes: -1 } }"
    })
    List<MostSixesResponse> findMostSixes(Pageable pageable);


    // 🎯 BOWLERS

    @Aggregation(pipeline = {
            "{ $match: { 'stats.bowler.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.bowler.matches', wickets: '$stats.bowler.wickets' } }",
            "{ $sort: { wickets: -1 } }"
    })
    List<TopWicketTakersResponse> findTopWicketTakers(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.bowler.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.bowler.matches', economy: '$stats.bowler.economy' } }",
            "{ $sort: { economy: 1 } }"
    })
    List<LowestEconomyResponse> findLowestEconomy(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.bowler.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.bowler.matches', strikeRate: '$stats.bowler.strikeRate' } }",
            "{ $sort: { strikeRate: 1 } }"
    })
    List<LowestBowlingStrikeRateResponse> findLowestStrikeRate(Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { 'stats.bowler.innings': { $gt: 0 } } }",
            "{ $project: { _id: 0, userId: '$user_id', name: 1, profileImage: 1, teams: 1, matches: '$stats.bowler.matches', average: '$stats.bowler.average' } }",
            "{ $sort: { average: 1 } }"
    })
    List<LowestBowlingAverageResponse> findLowestAverage(Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByUserIdIn(List<String> userIds);

    List<User> findTop10ByNameContainingIgnoreCase(String keyword);
}
