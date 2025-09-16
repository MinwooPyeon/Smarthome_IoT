package com.eeum.mqtt.inbound;

import java.util.Map;

import com.eeum.mqtt.common.Calib;
import com.eeum.mqtt.common.Units;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvIn {

    // 공통 메타
    @NotNull  private Long   ts;        
    @NotBlank private String deviceId;  
    @NotBlank private String msgId;     
    @NotBlank private String schema;    
    
    // 측정값
    private Double temperature;         
    private Double humidity;            
    private Double gasDensity;         
    
    // 보조정보
    private Units units;                
    private Calib calib;                
    private Double sampleRateHz;        
    private String status;              
    private Map<String, Object> meta;   
}
