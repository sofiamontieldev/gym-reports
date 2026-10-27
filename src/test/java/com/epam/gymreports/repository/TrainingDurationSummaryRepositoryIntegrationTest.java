package com.epam.gymreports.repository;

import com.epam.gymreports.domain.TrainerStatus;
import com.epam.gymreports.domain.TrainingDurationSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@ActiveProfiles("integration-test")
@Testcontainers(disabledWithoutDocker = true)
class TrainingDurationSummaryRepositoryIntegrationTest {

    @Container
    static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
    }

    @Autowired
    private TrainingDurationSummaryRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void storesAndReadsNestedMonthlySummary() {
        TrainingDurationSummary summary = new TrainingDurationSummary(
                "sara.morales", "Sara", "Morales", TrainerStatus.ACTIVE);
        summary.addTraining(LocalDate.of(2026, 10, 20), 60);
        repository.save(summary);

        TrainingDurationSummary loaded = repository
                .findByTrainerUsername("sara.morales")
                .orElseThrow();

        assertEquals(60, loaded.getYears().getFirst().getMonths().getFirst()
                .getTrainingDurationSummary());
    }

    @Test
    void enforcesUniqueTrainerUsername() {
        repository.insert(new TrainingDurationSummary(
                "juan.perez", "Juan", "Perez", TrainerStatus.ACTIVE));

        assertThrows(
                DuplicateKeyException.class,
                () -> repository.insert(new TrainingDurationSummary(
                        "juan.perez", "Juan Carlos", "Perez", TrainerStatus.INACTIVE)));

        assertTrue(repository.findByTrainerUsername("juan.perez").isPresent());
    }
}
