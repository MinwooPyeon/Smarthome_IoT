package com.eeum.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HubRegisterRequest {
    @Schema(description = "허브 시리얼 번호", example = "test1_hub")
	private String hubDeviceId;
    private Integer homeId;
}
