package com.epam.gymreports.messaging;

import com.epam.gymreports.domain.TrainerStatus;
import com.epam.gymreports.service.TrainingDurationSummaryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrainerWorkloadListenerTest {

    @Test
    void delegatesReceivedEventToService() {
        TrainingDurationSummaryService service =
                mock(TrainingDurationSummaryService.class);
        TrainerWorkloadListener listener = new TrainerWorkloadListener(service);
        TrainerWorkloadEvent event = new TrainerWorkloadEvent(
                UUID.randomUUID(),
                EventType.TRAINING_ADDED,
                "juan.perez",
                "Juan",
                "Perez",
                TrainerStatus.ACTIVE,
                LocalDate.of(2026, 10, 20),
                60);

        listener.consume(event);

        verify(service).process(event);
    }
}
