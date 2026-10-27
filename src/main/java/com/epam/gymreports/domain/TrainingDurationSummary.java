package com.epam.gymreports.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Document(collection = "trainings_duration_summary")
public class TrainingDurationSummary {

    @Id
    private String id;

    @Indexed(name = "uk_trainer_username", unique = true)
    private String trainerUsername;

    private String trainerFirstName;
    private String trainerLastName;
    private TrainerStatus trainerStatus;

    private List<YearSummary> years = new ArrayList<>();

    /*
     * Se guarda como String para evitar problemas de representación
     * binaria de UUID entre diferentes clientes de MongoDB.
     */
    private Set<String> processedEventIds = new HashSet<>();

    private Instant updatedAt;

    protected TrainingDurationSummary() {
        // Required by Spring Data MongoDB.
    }

    public TrainingDurationSummary(
            String trainerUsername,
            String trainerFirstName,
            String trainerLastName,
            TrainerStatus trainerStatus) {

        updateTrainer(
                trainerUsername,
                trainerFirstName,
                trainerLastName,
                trainerStatus);
        this.updatedAt = Instant.now();
    }

    public void updateTrainer(
            String trainerUsername,
            String trainerFirstName,
            String trainerLastName,
            TrainerStatus trainerStatus) {

        this.trainerUsername = requireText(
                trainerUsername, "trainerUsername");
        this.trainerFirstName = requireText(
                trainerFirstName, "trainerFirstName");
        this.trainerLastName = requireText(
                trainerLastName, "trainerLastName");

        if (trainerStatus == null) {
            throw new IllegalArgumentException(
                    "trainerStatus must not be null");
        }

        this.trainerStatus = trainerStatus;
        this.updatedAt = Instant.now();
    }

    public void addTraining(LocalDate trainingDate, int duration) {
        if (trainingDate == null) {
            throw new IllegalArgumentException(
                    "trainingDate must not be null");
        }

        if (duration <= 0) {
            throw new IllegalArgumentException(
                    "trainingDuration must be greater than zero");
        }

        ensureYears();

        YearSummary yearSummary = years.stream()
                .filter(existing ->
                        existing.getYear() == trainingDate.getYear())
                .findFirst()
                .orElseGet(() -> {
                    YearSummary created =
                            new YearSummary(trainingDate.getYear());
                    years.add(created);
                    return created;
                });

        yearSummary.addDuration(
                trainingDate.getMonthValue(),
                duration);

        years.sort(Comparator.comparingInt(YearSummary::getYear));
        updatedAt = Instant.now();
    }

    public boolean hasProcessed(UUID eventId) {
        ensureProcessedEventIds();
        return eventId != null
                && processedEventIds.contains(eventId.toString());
    }

    public void markProcessed(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        ensureProcessedEventIds();
        processedEventIds.add(eventId.toString());
        updatedAt = Instant.now();
    }

    private void ensureYears() {
        if (years == null) {
            years = new ArrayList<>();
        }
    }

    private void ensureProcessedEventIds() {
        if (processedEventIds == null) {
            processedEventIds = new HashSet<>();
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank");
        }
        return value;
    }

    public String getId() {
        return id;
    }

    public String getTrainerUsername() {
        return trainerUsername;
    }

    public String getTrainerFirstName() {
        return trainerFirstName;
    }

    public String getTrainerLastName() {
        return trainerLastName;
    }

    public TrainerStatus getTrainerStatus() {
        return trainerStatus;
    }

    public List<YearSummary> getYears() {
        ensureYears();
        return List.copyOf(years);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}