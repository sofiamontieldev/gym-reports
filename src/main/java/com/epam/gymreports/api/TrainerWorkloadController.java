package com.epam.gymreports.api;

import com.epam.gymreports.domain.TrainingDurationSummary;
import com.epam.gymreports.service.TrainingDurationSummaryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Validated
@ConditionalOnProperty(
        name = "gym.integrations.mongodb.enabled",
        havingValue = "true")
public class TrainerWorkloadController {

    private final TrainingDurationSummaryService service;

    public TrainerWorkloadController(
            TrainingDurationSummaryService service) {
        this.service = service;
    }

    @GetMapping("/trainers/{trainerUsername}/workload")
    public TrainerWorkloadResponse find(
            @PathVariable String trainerUsername,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false)
            @Min(1) @Max(12) Integer month) {

        if (month != null && year == null) {
            throw new IllegalArgumentException(
                    "year is required when month is provided");
        }

        TrainingDurationSummary summary =
                service.findByUsername(trainerUsername);

        List<TrainerWorkloadResponse.WorkloadPeriodResponse>
                periods = summary.getYears().stream()
                .filter(existingYear ->
                        year == null
                                || existingYear.getYear() == year)
                .flatMap(existingYear ->
                        existingYear.getMonths().stream()
                                .filter(existingMonth ->
                                        month == null
                                                || existingMonth
                                                .getMonth() == month)
                                .map(existingMonth ->
                                        new TrainerWorkloadResponse
                                                .WorkloadPeriodResponse(
                                                existingYear.getYear(),
                                                existingMonth.getMonth(),
                                                existingMonth
                                                        .getTrainingDurationSummary())))
                .sorted(Comparator
                        .comparingInt(
                                TrainerWorkloadResponse
                                        .WorkloadPeriodResponse::year)
                        .thenComparingInt(
                                TrainerWorkloadResponse
                                        .WorkloadPeriodResponse::month))
                .toList();

        return new TrainerWorkloadResponse(
                summary.getTrainerUsername(),
                summary.getTrainerFirstName(),
                summary.getTrainerLastName(),
                summary.getTrainerStatus(),
                "MINUTES",
                periods);
    }
}