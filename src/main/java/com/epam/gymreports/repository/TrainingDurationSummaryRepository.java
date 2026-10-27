package com.epam.gymreports.repository;

import com.epam.gymreports.domain.TrainingDurationSummary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TrainingDurationSummaryRepository
        extends MongoRepository<TrainingDurationSummary, String> {

    Optional<TrainingDurationSummary> findByTrainerUsername(
            String trainerUsername);
}