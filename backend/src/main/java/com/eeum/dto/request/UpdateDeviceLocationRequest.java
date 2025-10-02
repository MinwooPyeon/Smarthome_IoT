package com.eeum.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class UpdateDeviceLocationRequest {

	private Integer homeId;
    private Integer roomId;
    private Double x;
    private Double y;
    
}
