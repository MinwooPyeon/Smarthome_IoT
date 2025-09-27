package com.eeum.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Setter
public class RoutineUpdateRequest {
    private String name;
    private Boolean triggerType;
    private Integer routineWeekday;
    private String routineDescription;
    private OffsetDateTime actTime;
    private List<RoutineDetailRequest> detail;
}