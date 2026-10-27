package com.epam.gymreports.messaging;

import com.epam.gymreports.service.TrainingDurationSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "gym.integrations.activemq.enabled",
        havingValue = "true")
public class TrainerWorkloadListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TrainerWorkloadListener.class);

    private final TrainingDurationSummaryService service;

    public TrainerWorkloadListener(
            TrainingDurationSummaryService service) {
        this.service = service;
    }

    @JmsListener(
            destination =
                    "${gym.messaging.trainer-workload-queue}",
            containerFactory =
                    "jmsListenerContainerFactory")
    public void consume(TrainerWorkloadEvent event) {
        log.info(
                "Received event {} of type {}",
                event.eventId(),
                event.eventType());

        service.process(event);
    }
}