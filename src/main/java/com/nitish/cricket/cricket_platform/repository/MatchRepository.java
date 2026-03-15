package com.nitish.cricket.cricket_platform.repository;

import com.nitish.cricket.cricket_platform.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MatchRepository extends MongoRepository<Match, String> {
    Optional<Match> findByMatchId(String matchId);
    void deleteByMatchId(String matchId);
}
