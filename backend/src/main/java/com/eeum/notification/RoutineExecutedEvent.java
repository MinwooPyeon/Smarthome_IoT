package com.eeum.notification;

import java.time.OffsetDateTime;

public record RoutineExecutedEvent(
        Integer homeId,
        Integer routineId,
        String routineName,
        OffsetDateTime executedAt
) {}