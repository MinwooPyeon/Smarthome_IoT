package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoutineDetailResponse {
    private Integer routineDetailId;
    private Integer deviceId;
    private Integer routineId;
    private String deviceDetail;
}
