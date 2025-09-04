package com.eeum.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonRawValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceItemResponse{
	    private Integer deviceId;
	    private Integer roomId;
	    private Integer remoteId;
	    private Integer irDeviceId;
	    private String  brand;
	    private String  model;
	    private String  deviceName;
	    private String  type;
	    private Instant registeredAt;
	    private @JsonRawValue String deviceDetail;
}