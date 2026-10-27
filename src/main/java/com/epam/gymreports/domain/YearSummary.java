package com.epam.gymreports.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class YearSummary {

    private int year;
    private List<MonthSummary> months = new ArrayList<>();

    protected YearSummary() {
    }

    public YearSummary(int year) {
        this.year = year;
    }

    public void addDuration(int month, int duration) {
        ensureMonths();

        MonthSummary monthSummary = months.stream()
                .filter(existing -> existing.getMonth() == month)
                .findFirst()
                .orElseGet(() -> {
                    MonthSummary created = new MonthSummary(month);
                    months.add(created);
                    return created;
                });

        monthSummary.addDuration(duration);
        months.sort(Comparator.comparingInt(MonthSummary::getMonth));
    }

    private void ensureMonths() {
        if (months == null) {
            months = new ArrayList<>();
        }
    }

    public int getYear() {
        return year;
    }

    public List<MonthSummary> getMonths() {
        ensureMonths();
        return List.copyOf(months);
    }
}