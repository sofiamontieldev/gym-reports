package com.epam.gymreports.domain;

public class MonthSummary {

    private int month;
    private int trainingDurationSummary;

    protected MonthSummary() {
    }

    public MonthSummary(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        this.month = month;
    }

    public void addDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException(
                    "trainingDuration must be greater than zero");
        }

        trainingDurationSummary = Math.addExact(trainingDurationSummary, duration);
    }

    public int getMonth() {
        return month;
    }

    public int getTrainingDurationSummary() {
        return trainingDurationSummary;
    }
}