package com.eeum.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceItemResponse {
    private Integer deviceId;
    private Integer roomId;
    private Integer irDeviceId;
    private String  brand;
    private String  model;
    private String  deviceType;
    private String  deviceName;
    private Instant registeredAt;
    private Double x;
    private Double y;
    private Map<String, Object> deviceDetail;
    
}
