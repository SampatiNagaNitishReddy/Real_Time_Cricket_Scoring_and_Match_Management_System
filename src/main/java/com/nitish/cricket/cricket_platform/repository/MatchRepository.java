package com.nitish.cricket.cricket_platform.repository;

import com.nitish.cricket.cricket_platform.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends MongoRepository<Match, String> {
    Optional<Match> findByMatchId(String matchId);
    void deleteByMatchId(String matchId);

    @Query("""
{
  "$and": [
    {
      "$or": [
        { "scorers.teamA": ?0 },
        { "scorers.teamB": ?0 }
      ]
    },
    {
      "status": {
        "$in": [
            "SCHEDULED",
          "LIVE_FIRST_INNINGS",
          "INNINGS_BREAK",
          "LIVE_SECOND_INNINGS"
        ]
      }
    }
  ]
}
""")
    List<Match> findMyLiveMatches(String userId);
}
