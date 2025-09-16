package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/order/ack
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderAckIn {

    @NotNull  private Long ts;
    @NotBlank private String deviceId;
    @NotBlank private String schema;   
    @NotBlank private String corrId;    
    @NotBlank private String msgId;     

    @NotBlank private String status;    
    private Result result;              
    private Integer durationMs;
    private Integer retries;
    
    @Getter @Setter @NoArgsConstructor
    public static class Result {
        private Integer code;
        private String detail;
    }
}


