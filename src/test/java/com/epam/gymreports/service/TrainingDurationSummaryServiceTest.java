package com.epam.gymreports.service;

import com.epam.gymreports.domain.TrainerStatus;
import com.epam.gymreports.domain.TrainingDurationSummary;
import com.epam.gymreports.messaging.EventType;
import com.epam.gymreports.messaging.TrainerWorkloadEvent;
import com.epam.gymreports.repository.TrainingDurationSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingDurationSummaryServiceTest {

    @Mock
    private TrainingDurationSummaryRepository repository;

    @InjectMocks
    private TrainingDurationSummaryService service;

    @Test
    void createsAndStoresMonthlySummaryForTrainingEvent() {
        TrainerWorkloadEvent event = trainingEvent(UUID.randomUUID(), 60);
        when(repository.findByTrainerUsername("juan.perez"))
                .thenReturn(Optional.empty());

        service.process(event);

        ArgumentCaptor<TrainingDurationSummary> captor =
                ArgumentCaptor.forClass(TrainingDurationSummary.class);
        verify(repository).save(captor.capture());

        TrainingDurationSummary saved = captor.getValue();
        assertEquals("juan.perez", saved.getTrainerUsername());
        assertEquals(60, saved.getYears().getFirst().getMonths().getFirst()
                .getTrainingDurationSummary());
        assertEquals(TrainerStatus.ACTIVE, saved.getTrainerStatus());
    }

    @Test
    void ignoresDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        TrainerWorkloadEvent event = trainingEvent(eventId, 60);
        TrainingDurationSummary existing = new TrainingDurationSummary(
                "juan.perez", "Juan", "Perez", TrainerStatus.ACTIVE);
        existing.markProcessed(eventId);
        when(repository.findByTrainerUsername("juan.perez"))
                .thenReturn(Optional.of(existing));

        service.process(event);

        verify(repository, never()).save(existing);
    }

    @Test
    void appliesStatusChangeWithoutAddingDuration() {
        TrainerWorkloadEvent event = new TrainerWorkloadEvent(
                UUID.randomUUID(),
                EventType.TRAINER_STATUS_CHANGED,
                "juan.perez",
                "Juan",
                "Perez",
                TrainerStatus.INACTIVE,
                null,
                null);
        when(repository.findByTrainerUsername("juan.perez"))
                .thenReturn(Optional.empty());

        service.process(event);

        ArgumentCaptor<TrainingDurationSummary> captor =
                ArgumentCaptor.forClass(TrainingDurationSummary.class);
        verify(repository).save(captor.capture());
        assertEquals(TrainerStatus.INACTIVE, captor.getValue().getTrainerStatus());
        assertTrue(captor.getValue().getYears().isEmpty());
    }

    @Test
    void rejectsInvalidTrainingEvent() {
        TrainerWorkloadEvent event = trainingEvent(UUID.randomUUID(), 0);
        when(repository.findByTrainerUsername("juan.perez"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.process(event));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsMissingTrainer() {
        when(repository.findByTrainerUsername("missing.trainer"))
                .thenReturn(Optional.empty());

        assertThrows(
                com.epam.gymreports.exception.ReportNotFoundException.class,
                () -> service.findByUsername("missing.trainer"));
    }

    private static TrainerWorkloadEvent trainingEvent(
            UUID eventId,
            int duration) {
        return new TrainerWorkloadEvent(
                eventId,
                EventType.TRAINING_ADDED,
                "juan.perez",
                "Juan",
                "Perez",
                TrainerStatus.ACTIVE,
                LocalDate.of(2026, 10, 20),
                duration);
    }
}
