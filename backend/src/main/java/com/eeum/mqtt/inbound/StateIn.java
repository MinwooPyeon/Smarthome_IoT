package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/state
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateIn {

    @NotNull  private Long ts;
    @NotBlank private String deviceId;

    @NotBlank private String status;
}
