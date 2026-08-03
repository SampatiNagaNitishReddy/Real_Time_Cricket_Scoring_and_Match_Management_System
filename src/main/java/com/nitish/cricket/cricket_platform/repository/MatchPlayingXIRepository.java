package com.nitish.cricket.cricket_platform.repository;

import com.nitish.cricket.cricket_platform.model.MatchPlayingXI;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MatchPlayingXIRepository extends MongoRepository<MatchPlayingXI, String> {

    Optional<MatchPlayingXI> findByMatchId(String matchId);

    boolean existsByMatchId(String matchId);

    void deleteByMatchId(String matchId);

}