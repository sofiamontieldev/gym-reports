package com.epam.gymreports.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDurationSummaryTest {

    @Test
    void accumulatesDurationsByYearAndMonth() {
        TrainingDurationSummary summary = summary();

        summary.addTraining(LocalDate.of(2026, 10, 3), 60);
        summary.addTraining(LocalDate.of(2026, 10, 17), 30);
        summary.addTraining(LocalDate.of(2026, 11, 2), 45);
        summary.addTraining(LocalDate.of(2027, 1, 8), 20);

        assertEquals(2, summary.getYears().size());
        assertEquals(2026, summary.getYears().get(0).getYear());
        assertEquals(2, summary.getYears().get(0).getMonths().size());
        assertEquals(90, summary.getYears().get(0).getMonths().get(0)
                .getTrainingDurationSummary());
        assertEquals(45, summary.getYears().get(0).getMonths().get(1)
                .getTrainingDurationSummary());
        assertEquals(20, summary.getYears().get(1).getMonths().get(0)
                .getTrainingDurationSummary());
    }

    @Test
    void tracksProcessedEventsAndUpdatesTrainerProfile() {
        TrainingDurationSummary summary = summary();
        UUID eventId = UUID.randomUUID();

        summary.markProcessed(eventId);
        summary.updateTrainer(
                "jose.rojas",
                "Jose",
                "Rojas",
                TrainerStatus.INACTIVE);

        assertTrue(summary.hasProcessed(eventId));
        assertEquals("Jose", summary.getTrainerFirstName());
        assertEquals(TrainerStatus.INACTIVE, summary.getTrainerStatus());
    }

    @Test
    void rejectsInvalidTrainingData() {
        TrainingDurationSummary summary = summary();

        assertThrows(
                IllegalArgumentException.class,
                () -> summary.addTraining(null, 60));
        assertThrows(
                IllegalArgumentException.class,
                () -> summary.addTraining(LocalDate.of(2026, 10, 3), 0));
    }

    private static TrainingDurationSummary summary() {
        return new TrainingDurationSummary(
                "luis.rodriguez",
                "Luis",
                "Rodriguez",
                TrainerStatus.ACTIVE);
    }
}
