package com.eeum.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter @Setter
public class RoutineCreateRequest {
    private String name;
    private Integer routineWeekday;
    private String routineDescription;
    private LocalTime actTime;
    private List<RoutineDetailRequest> detail;
    private Integer iconId;
    private Boolean isAi;
}
