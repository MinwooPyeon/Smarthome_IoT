package com.eeum.mqtt.outbound;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/state
@Getter
@Setter
@NoArgsConstructor
public class StateOut {
    @NotNull  private Long ts;
    @NotBlank private String deviceId;
    @NotBlank private String status;
}