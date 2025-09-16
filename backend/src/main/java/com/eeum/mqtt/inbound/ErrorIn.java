package com.eeum.mqtt.inbound;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    @NotNull  private Long ts;
    @NotBlank private String deviceId;
    @NotBlank private String schema; 

    @NotBlank private String level;  
    @NotBlank private String code;   
    private String detail;
    private Map<String, Object> ctx; 
}