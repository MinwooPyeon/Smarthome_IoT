package com.eeum.dto.request;

import lombok.Getter; import lombok.Setter;
import java.util.Map;

@Getter @Setter
public class RoutineDetailRequest {
    private Integer deviceId;
    private Map<String, Object> deviceDetail;
}