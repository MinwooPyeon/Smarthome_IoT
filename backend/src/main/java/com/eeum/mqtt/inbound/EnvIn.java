package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonAlias("dew_point")    private Double dewPoint;    
    @JsonAlias("head_index")   private Double headIndex;    
    @JsonAlias("abs_humidity") private Double absHumidity; 
    private Double pmv;                                      
    private Double ppd;                                      
    private Double wbgt;                                     

}