package com.eeum.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HubRegisterRequest {
    @Schema(description = "허브 IP 주소 (PostgreSQL inet)", example = "192.168.0.101", format = "ipv4")
	private String deviceAddr;
    private Integer homeId;
}
