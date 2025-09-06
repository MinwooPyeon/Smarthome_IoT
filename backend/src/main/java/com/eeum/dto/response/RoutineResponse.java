package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.List;

import com.eeum.dto.request.RoutineDetailRequest;

@Getter
@AllArgsConstructor
public class RoutineResponse {
    private Integer routineId;
    private String  name;
    private Boolean triggerType;
    private Integer routineWeekday;
    private String  routineDescription;
    private OffsetDateTime actTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}