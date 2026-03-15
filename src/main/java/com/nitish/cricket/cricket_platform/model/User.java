package com.nitish.cricket.cricket_platform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    private String name;
    private String email;
    private String password;
    private String profileImage;

    private String profileImageId; // store Cloudinary public ID

    private int age;
    private String place;

    @Field("date_of_register")
    private LocalDateTime dateOfRegister;

    private List<String> matches = new ArrayList<>();
    private List<String> teams = new ArrayList<>();

    private Role role;
    private Stats stats;

    //--------------------- Tournament Stats-----------------
    // inside your User class
    private Map<String, Stats> tournamentStats = new HashMap<>();
    private List<Award> awards = new ArrayList<>();
    private List<BadgeReference> badges = new ArrayList<>();

    // -------------------- Role --------------------
    public static class Role {
        private RoleType type;
        private BattingStyle batting;
        private BowlingStyle bowling;

        public enum RoleType { BATTER, BOWLER, ALL_ROUNDER, WICKET_KEEPER }
        public enum BattingStyle { RIGHT_HAND, LEFT_HAND }
        public enum BowlingStyle { OFF_BREAK, LEG_BREAK, FAST, MEDIUM_FAST, SLOW_LEFT_ARM , LEFT_ARM_LEGSPIN , LEFT_ARM_MEDIUM_FAST, LEFT_ARM_FAST,NONE}

        // Getters and Setters
        public RoleType getType() { return type; }
        public void setType(RoleType type) { this.type = type; }

        public BattingStyle getBatting() { return batting; }
        public void setBatting(BattingStyle batting) { this.batting = batting; }

        public BowlingStyle getBowling() { return bowling; }
        public void setBowling(BowlingStyle bowling) { this.bowling = bowling; }
    }

    // -------------------- Stats --------------------
    public static class Stats {
        private BatterStats batter;
        private BowlerStats bowler;

        public static class BatterStats {
            private int matches;        // total matches played
            private int innings;        // times batted
            private int dismissals;     // times got out

            private int ballsFaced;
            private int totalRuns;
            private double average;
            private double strikeRate;
            private int fours;
            private int sixes;

            // Getters and Setters

            public int getMatches() {
                return matches;
            }

            public void setMatches(int matches) {
                this.matches = matches;
            }

            public int getInnings() {
                return innings;
            }

            public void setInnings(int innings) {
                this.innings = innings;
            }

            public int getDismissals() {
                return dismissals;
            }

            public void setDismissals(int dismissals) {
                this.dismissals = dismissals;
            }

            public int getBallsFaced() {
                return ballsFaced;
            }

            public void setBallsFaced(int ballsFaced) {
                this.ballsFaced = ballsFaced;
            }

            public int getTotalRuns() { return totalRuns; }
            public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

            public double getAverage() { return average; }
            public void setAverage(double average) { this.average = average; }

            public double getStrikeRate() { return strikeRate; }
            public void setStrikeRate(double strikeRate) { this.strikeRate = strikeRate; }

            public int getFours() { return fours; }
            public void setFours(int fours) { this.fours = fours; }

            public int getSixes() { return sixes; }
            public void setSixes(int sixes) { this.sixes = sixes; }
            // New getter for display purposes
            public String getAverageDisplay() {
                if (dismissals == 0) {
                    return "–"; // batter never dismissed, average undefined
                }
                return String.valueOf(round(average)); // round() same as your utility
            }
            private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
            }
        }

        public static class BowlerStats {
            private int matches;        // total matches played as a bowler
            private int innings;        // total innings bowled
            private int ballsBowled;
            private int runsGiven;
            private int wickets;
            private double average;
            private double economy;
            private double strikeRate;
            private Extras extras;

            public static class Extras {
                private int wides;
                private int noBalls;
                private int byes;
                private int legByes;

                // Getters and Setters
                public int getWides() { return wides; }
                public void setWides(int wides) { this.wides = wides; }

                public int getNoBalls() { return noBalls; }
                public void setNoBalls(int noBalls) { this.noBalls = noBalls; }

                public int getByes() { return byes; }
                public void setByes(int byes) { this.byes = byes; }

                public int getLegByes() { return legByes; }
                public void setLegByes(int legByes) { this.legByes = legByes; }
            }

            // Getters and Setters


            public int getMatches() { return matches; }
            public void setMatches(int matches) { this.matches = matches; }

            public int getInnings() { return innings; }
            public void setInnings(int innings) { this.innings = innings; }

            public int getBallsBowled() { return ballsBowled; }
            public void setBallsBowled(int ballsBowled) { this.ballsBowled = ballsBowled; }

            public int getRunsGiven() { return runsGiven; }
            public void setRunsGiven(int runsGiven) { this.runsGiven = runsGiven; }

            public int getWickets() { return wickets; }
            public void setWickets(int wickets) { this.wickets = wickets; }

            public double getAverage() { return average; }
            public void setAverage(double average) { this.average = average; }

            public double getEconomy() { return economy; }
            public void setEconomy(double economy) { this.economy = economy; }

            public double getStrikeRate() { return strikeRate; }
            public void setStrikeRate(double strikeRate) { this.strikeRate = strikeRate; }

            public Extras getExtras() { return extras; }
            public void setExtras(Extras extras) { this.extras = extras; }

            // --- Display safe methods ---
            public String getAverageDisplay() {
                if (wickets == 0) return "–";
                return String.valueOf(round(average));
            }

            public String getStrikeRateDisplay() {
                if (wickets == 0) return "–";
                return String.valueOf(round(strikeRate));
            }

            private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
            }
        }


        // Getters and Setters
        public BatterStats getBatter() { return batter; }
        public void setBatter(BatterStats batter) { this.batter = batter; }

        public BowlerStats getBowler() { return bowler; }
        public void setBowler(BowlerStats bowler) { this.bowler = bowler; }
    }



    // -------------------- Award --------------------
    public static class Award {
        private String title;
        private String matchId;


        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }


    }

    // -------------------- Badge --------------------
    public static class BadgeReference {
        private String badgeId;
        private LocalDateTime achievedOn;

        public String getBadgeId() { return badgeId; }
        public void setBadgeId(String badgeId) { this.badgeId = badgeId; }

        public LocalDateTime getAchievedOn() { return achievedOn; }
        public void setAchievedOn(LocalDateTime achievedOn) { this.achievedOn = achievedOn; }
    }

    // -------------------- Getters and Setters --------------------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Map<String, Stats> getTournamentStats() {
        return tournamentStats;
    }

    public void setTournamentStats(Map<String, Stats> tournamentStats) {
        this.tournamentStats = tournamentStats;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(String profileImageId) {
        this.profileImageId = profileImageId;
    }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public LocalDateTime getDateOfRegister() { return dateOfRegister; }
    public void setDateOfRegister(LocalDateTime dateOfRegister) { this.dateOfRegister = dateOfRegister; }

    public List<String> getMatches() { return matches; }
    public void setMatches(List<String> matches) { this.matches = matches; }

    public List<String> getTeams() { return teams; }
    public void setTeams(List<String> teams) { this.teams = teams; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Stats getStats() { return stats; }
    public void setStats(Stats stats) { this.stats = stats; }

    public List<Award> getAwards() { return awards; }
    public void setAwards(List<Award> awards) { this.awards = awards; }

    public List<BadgeReference> getBadges() { return badges; }
    public void setBadges(List<BadgeReference> badges) { this.badges = badges; }
}
