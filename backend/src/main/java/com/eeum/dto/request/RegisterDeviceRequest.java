package com.eeum.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterDeviceRequest {
    private Integer roomId;
    private Integer remoteId;
    private Integer irDeviceId;
    private Integer deviceConsumption;
    private String deviceName;
    private String type;
    private String brand;
    private String model;
}