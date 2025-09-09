package com.eeum.dto.response;

import lombok.*;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceItemResponse {
    private Integer deviceId;
    private Integer roomId;
    private Integer remoteId;
    private Integer irDeviceId;
    private String  brand;
    private String  model;
    private String  type;
    private String  deviceName;
    private Instant registeredAt;
    private JsonNode deviceDetail;

}
