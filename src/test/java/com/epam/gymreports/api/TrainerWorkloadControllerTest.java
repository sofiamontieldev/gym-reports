package com.epam.gymreports.api;

import com.epam.gymreports.domain.TrainerStatus;
import com.epam.gymreports.domain.TrainingDurationSummary;
import com.epam.gymreports.exception.GlobalExceptionHandler;
import com.epam.gymreports.exception.ReportNotFoundException;
import com.epam.gymreports.service.TrainingDurationSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainerWorkloadControllerTest {

    private TrainingDurationSummaryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TrainingDurationSummaryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TrainerWorkloadController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsFilteredMonthlyWorkload() throws Exception {
        TrainingDurationSummary summary = new TrainingDurationSummary(
                "juan.perez", "Juan", "Perez", TrainerStatus.ACTIVE);
        summary.addTraining(LocalDate.of(2026, 10, 20), 60);
        summary.addTraining(LocalDate.of(2026, 11, 20), 30);
        when(service.findByUsername("juan.perez")).thenReturn(summary);

        mockMvc.perform(get(
                        "/api/v1/reports/trainers/juan.perez/workload")
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("juan.perez"))
                .andExpect(jsonPath("$.unit").value("MINUTES"))
                .andExpect(jsonPath("$.periods.length()").value(1))
                .andExpect(jsonPath("$.periods[0].trainingDuration").value(60));
    }

    @Test
    void rejectsMonthWithoutYear() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/reports/trainers/juan.perez/workload")
                        .param("month", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("year is required when month is provided"));
    }

    @Test
    void returnsNotFoundForUnknownTrainer() throws Exception {
        when(service.findByUsername("missing.trainer"))
                .thenThrow(new ReportNotFoundException(
                        "No workload report exists for trainer: missing.trainer"));

        mockMvc.perform(get(
                        "/api/v1/reports/trainers/missing.trainer/workload"))
                .andExpect(status().isNotFound());
    }
}
