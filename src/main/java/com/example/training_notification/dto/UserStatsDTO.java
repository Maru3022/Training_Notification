package com.example.training_notification.dto;

import java.util.List;

public record UserStatsDTO(
        Long userId,
        String userName,
        String email,

        int totalTrainings, //Всего тренировок за определенный период
        int completedTrainings, //Завершенных тренировок
        long totalMinutes, //Общее время в минутах
        double caloriesBurned, //Сожженные калории

        String progressMessage, //Текст прогресса(напр. "Вы выполнили на 20% больше, чем на прошлой неделе")
        List<String> achievemets //Список достижений за неделю
) {
}