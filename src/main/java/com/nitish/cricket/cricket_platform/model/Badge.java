package com.nitish.cricket.cricket_platform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "badges")
public class Badge {

    @Id
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private BadgeType type;
    private Milestone milestone;

    // -------------------- Enums --------------------
    public enum BadgeType {
        ACHIEVEMENT,
        SKILL,
        PARTICIPATION
    }

    public enum MilestoneField {
        MATCHES_PLAYED,
        RUNS_SCORED,
        WICKETS_TAKEN
        // Add more fields as needed
    }

    // -------------------- Milestone --------------------
    public static class Milestone {
        private MilestoneField field;
        private String condition; // Example: ">=10", ">50", etc.

        // Constructor
        public Milestone() {}

        public Milestone(MilestoneField field, String condition) {
            this.field = field;
            this.condition = condition;
        }

        // Getters and Setters
        public MilestoneField getField() {
            return field;
        }

        public void setField(MilestoneField field) {
            this.field = field;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }
    }

    // -------------------- Constructors --------------------
    public Badge() {
        this.milestone = new Milestone(); // Initialize to avoid null
    }

    public Badge(String name, String description, String iconUrl, BadgeType type, Milestone milestone) {
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.type = type;
        this.milestone = milestone != null ? milestone : new Milestone();
    }

    // -------------------- Getters and Setters --------------------
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public BadgeType getType() {
        return type;
    }

    public void setType(BadgeType type) {
        this.type = type;
    }

    public Milestone getMilestone() {
        return milestone;
    }

    public void setMilestone(Milestone milestone) {
        this.milestone = milestone != null ? milestone : new Milestone();
    }
}
