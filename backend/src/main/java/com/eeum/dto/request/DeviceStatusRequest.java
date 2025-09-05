package com.eeum.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class DeviceStatusRequest {
	private Integer deviceId;
	private JsonNode deviceDetail;
}
