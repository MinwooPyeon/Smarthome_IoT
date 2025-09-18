package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/error
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorIn {

	@NotNull
    @JsonProperty("tx_id")   
    private Integer txId;

    @NotBlank
    @JsonProperty("error")
    private String error;

    @NotBlank
    @JsonProperty("message")
    private String message;

}