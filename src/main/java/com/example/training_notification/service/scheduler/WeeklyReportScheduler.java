package com.example.training_notification.service.scheduler;

import com.example.training_notification.dto.NotificationRequest;
import com.example.training_notification.dto.NotificationType;
import com.example.training_notification.dto.UserStatsDTO;
import com.example.training_notification.factory.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final NotificationFactory notificationFactory;

    @Value("${weekly.report.demo.email:demo@example.com}")
    private String weeklyReportDemoEmail;

    @Scheduled(cron = "0 0 20 * * SUN")
    public void sendWeeklyReports() {
        log.info(">>> Starting weekly report distribution process...");

        List<UserStatsDTO> reports = collectStatistics();

        for (UserStatsDTO stats : reports) {
            String emailText = String.format(
                    "Hello, %s!\n\n" +
                            "Your weekly report is ready:\n" +
                            "- Trainings this week: %d\n" +
                            "- Active time: %d min.\n" +
                            "- Calories burned: %.2f kcal.\n\n" +
                            "Your status: %s\n\n" +
                            "Keep up the good work!",
                    stats.userName(),
                    stats.totalTrainings(),
                    stats.totalMinutes(),
                    stats.caloriesBurned(),
                    stats.progressMessage()
            );

            NotificationRequest request = new NotificationRequest(
                    stats.email(),
                    emailText,
                    NotificationType.EMAIL
            );

            notificationFactory.getSender(NotificationType.EMAIL).send(request);
        }

        log.info(">>> Distribution finished. Reports sent successfully: {}", reports.size());
    }

    private List<UserStatsDTO> collectStatistics() {
        return List.of(
                new UserStatsDTO(
                        java.util.UUID.randomUUID(),
                        "User",
                        weeklyReportDemoEmail,
                        5,
                        4,
                        250L,
                        1500.0,
                        "Great progress this week",
                        List.of("Early Bird", "Weekly Hero")
                )
        );
    }

}
