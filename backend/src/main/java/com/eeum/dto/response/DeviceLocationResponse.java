package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLocationResponse {
    private Integer positionId;
    private Integer deviceId;
    private Integer homeId;
    private Integer roomId;
    private Double  x;
    private Double  y;
}
