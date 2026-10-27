package com.epam.gymreports.service;

import com.epam.gymreports.domain.TrainingDurationSummary;
import com.epam.gymreports.exception.ReportNotFoundException;
import com.epam.gymreports.messaging.EventType;
import com.epam.gymreports.messaging.TrainerWorkloadEvent;
import com.epam.gymreports.repository.TrainingDurationSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "gym.integrations.mongodb.enabled",
        havingValue = "true")
public class TrainingDurationSummaryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TrainingDurationSummaryService.class);

    private final TrainingDurationSummaryRepository repository;

    public TrainingDurationSummaryService(
            TrainingDurationSummaryRepository repository) {
        this.repository = repository;
    }

    public void process(TrainerWorkloadEvent event) {
        validateCommonFields(event);

        TrainingDurationSummary summary = repository
                .findByTrainerUsername(event.trainerUsername())
                .orElseGet(() -> new TrainingDurationSummary(
                        event.trainerUsername(),
                        event.trainerFirstName(),
                        event.trainerLastName(),
                        event.trainerStatus()));

        if (summary.hasProcessed(event.eventId())) {
            log.info(
                    "Ignoring duplicate event {} for trainer {}",
                    event.eventId(),
                    event.trainerUsername());
            return;
        }

        summary.updateTrainer(
                event.trainerUsername(),
                event.trainerFirstName(),
                event.trainerLastName(),
                event.trainerStatus());

        if (event.eventType() == EventType.TRAINING_ADDED) {
            validateTrainingAdded(event);

            summary.addTraining(
                    event.trainingDate(),
                    event.trainingDuration());
        }

        summary.markProcessed(event.eventId());
        repository.save(summary);

        log.info(
                "Processed event {} of type {} for trainer {}",
                event.eventId(),
                event.eventType(),
                event.trainerUsername());
    }

    public TrainingDurationSummary findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "trainerUsername must not be blank");
        }

        return repository.findByTrainerUsername(username)
                .orElseThrow(() -> new ReportNotFoundException(
                        "No workload report exists for trainer: "
                                + username));
    }

    private static void validateCommonFields(
            TrainerWorkloadEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                    "event must not be null");
        }

        if (event.eventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null");
        }

        if (event.eventType() == null) {
            throw new IllegalArgumentException(
                    "eventType must not be null");
        }

        requireText(event.trainerUsername(), "trainerUsername");
        requireText(event.trainerFirstName(), "trainerFirstName");
        requireText(event.trainerLastName(), "trainerLastName");

        if (event.trainerStatus() == null) {
            throw new IllegalArgumentException(
                    "trainerStatus must not be null");
        }
    }

    private static void validateTrainingAdded(
            TrainerWorkloadEvent event) {

        if (event.trainingDate() == null) {
            throw new IllegalArgumentException(
                    "trainingDate is required for TRAINING_ADDED");
        }

        if (event.trainingDuration() == null
                || event.trainingDuration() <= 0) {
            throw new IllegalArgumentException(
                    "trainingDuration must be greater than zero");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank");
        }
    }
}