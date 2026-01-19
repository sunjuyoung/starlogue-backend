package com.example.starlogue.domain.visualization;

import java.util.List;
import java.util.UUID;

public record Galaxy(
        UUID userId,
        List<Star> allStars,
        List<Constellation> constellations,
        GalaxyStats stats
) {
}
