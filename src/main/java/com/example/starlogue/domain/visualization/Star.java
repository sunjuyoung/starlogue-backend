package com.example.starlogue.domain.visualization;

import com.example.starlogue.domain.studyday.StarType;
import com.example.starlogue.domain.studyday.StudyDay;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Star(
        LocalDate date,
        double brightness,
        double size,
        List<String> colors,
        StarType type,
        Position position,
        boolean hasNebulaEffect,
        UUID penaltyId
) {
    public static Star fromStudyDay(StudyDay day, Position position) {
        double hours = day.getTotalFocusTime().toHours();

        return new Star(
                day.getDate(),
                Math.min(1.0, hours / 8.0),
                Math.min(50.0, 10.0 + hours * 5.0),
                new ArrayList<>(day.getTagColors()),
                day.getStarType(),
                position,
                day.getStarType() == StarType.SUPERNOVA,
                null
        );
    }

    public Star withPenaltyId(UUID penaltyId) {
        return new Star(date, brightness, size, colors, type, position, hasNebulaEffect, penaltyId);
    }
}
