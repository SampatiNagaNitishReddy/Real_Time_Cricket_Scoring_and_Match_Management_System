package com.nitish.cricket.cricket_platform.repository;

import com.nitish.cricket.cricket_platform.model.Tournament;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentRepository extends MongoRepository<Tournament, String> {
    Optional<Tournament> findByTournamentId(String tournamentId);
}
