package com.eeum.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterDeviceRequest {
	private Integer homeId;
	private Integer irDeviceId;  // ir_device_id
	private String roomColor;
    private String model;
    private String brand;
    private String deviceType;
    private Double floorplansX;
    private Double floorplansY;
}