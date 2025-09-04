package com.eeum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceRequest {
    private Integer deviceId;
    private String  deviceName;
    private String  roomName;
    private String  type;
    private Boolean active;
}