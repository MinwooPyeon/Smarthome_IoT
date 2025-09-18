package com.eeum.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HubRegisterRequest {
	private String deviceAddr;
    private Integer homeId;
}
