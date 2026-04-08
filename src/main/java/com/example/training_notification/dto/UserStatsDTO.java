package com.example.training_notification.dto;

import java.util.List;
import java.util.UUID;

public record UserStatsDTO(
        UUID userId,
        String userName,
        String email,

        int totalTrainings, // Total workouts for the period
        int completedTrainings, // Completed workouts
        long totalMinutes, // Total time in minutes
        double caloriesBurned, // Burned calories

        String progressMessage, // Progress text (e.g., "You completed 20% more than last week")
        List<String> achievements // List of achievements for the week
) {
}