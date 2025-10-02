package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class RoutineResponse {
    private Integer routineId;
    private String name;
    private Boolean triggerType;
    private Integer routineWeekday;
    private String routineDescription;
    private LocalTime actTime;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer iconId;
    private Boolean isAi;
    private String iconUrl;
    private List<RoutineDetailResponse> details;
}