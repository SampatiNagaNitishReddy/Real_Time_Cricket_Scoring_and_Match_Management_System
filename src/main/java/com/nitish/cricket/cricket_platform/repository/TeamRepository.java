package com.nitish.cricket.cricket_platform.repository;

import com.nitish.cricket.cricket_platform.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TeamRepository extends MongoRepository<Team, String> {
    Optional<Team> findByTeamId(String teamId);
    void deleteByTeamId(String teamId);
}
