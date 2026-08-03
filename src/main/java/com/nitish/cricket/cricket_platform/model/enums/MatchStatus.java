package com.nitish.cricket.cricket_platform.model.enums;

public enum MatchStatus {

    // --- Pre-match ---
    SCHEDULED,          // Match is created but not started yet
    TOSS_COMPLETED,
    PLAYING_XI_SELECTED,

    // --- During match ---
    LIVE_FIRST_INNINGS, // First innings is currently in progress
    LIVE_SECOND_INNINGS,// Second innings is currently in progress

    // --- Between innings ---
    INNINGS_BREAK,      // Break between two innings

    // --- After match ---
    COMPLETED,          // Match is finished
    CANCELLED,          // Match cancelled (rain, technical, etc.)
    ABANDONED           // Match started but not completed due to issues
}
