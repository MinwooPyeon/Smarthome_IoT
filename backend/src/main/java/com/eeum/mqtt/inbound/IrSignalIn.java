package com.eeum.mqtt.inbound;

import java.util.List;

import com.eeum.mqtt.common.Timing;
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

    // IR 프로토콜 기본정보
    @NotBlank private String encoding;  
    private Integer carrierHz;          
    private Double  dutyCycle;          

    private String  address;            
    private String  command;            

    // 데이터 표현 
    private Timing timing;             
    private List<Integer> rawData;      
    private String data;                

    // 제어 부가정보 
    private Integer repeat;             
    private Double  quality;            
    private String  remark;            
}

