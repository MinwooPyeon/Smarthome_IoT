package com.eeum.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IrRegisterRequest{
	    String deviceId;  
	    String brand;      
	    String device;     
	    String function;  
}