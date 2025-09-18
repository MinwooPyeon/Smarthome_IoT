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
public class IrSignalIn {

    // 공통 메타
    @NotNull  private Long   ts;
    @NotBlank private String deviceId;
    @NotBlank private String msgId;
    @NotBlank private String schema;

    // 필수
    @NotBlank private String brand;    
    @NotBlank private String device;    

    @NotNull
    @JsonAlias("raw_data")
    private int[] rawData;              
            
}
