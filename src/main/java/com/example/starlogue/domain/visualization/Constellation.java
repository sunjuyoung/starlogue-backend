package com.example.starlogue.domain.visualization;

import java.util.List;

public record Constellation(
        List<Star> connectedStars,
        int streakDays,
        String name,
        List<Connection> connections
) {
    public record Connection(Position from, Position to) {
    }
}
