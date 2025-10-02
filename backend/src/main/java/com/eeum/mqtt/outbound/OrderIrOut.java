package com.eeum.mqtt.outbound;

import java.util.List;

import com.eeum.mqtt.common.RetrySpec;
import com.eeum.mqtt.common.Timing;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/order (type=ir)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderIrOut {

    // 공통
    @NotNull  private Long   ts;
    @NotBlank private String deviceId;
    @NotBlank private String msgId;
    @NotBlank private String schema;
    private   String corrId;
    @Builder.Default
    @NotBlank private String type = "ir";
    private   Integer priority;
    private   Long    expiresAt;
    private   RetrySpec retry;
    private   String  replyTo;

    // ── IR payload 
    @NotBlank private String encoding;

 
    @Min(1)
    @JsonProperty("carrier_hz")          
    private Integer carrierHz;

    @DecimalMin(value = "0.0")           
    @DecimalMax(value = "1.0")           
    @JsonProperty("duty_cycle")          
    private Double  dutyCycle;

    private String  data;

    @JsonProperty("raw_data")            
    private List<Integer> rawData;

    private Timing timing;
    @Min(0)                               
    private Integer repeat;

    private String  remark;

    // 유효성
    @AssertTrue(message = "Either data or rawData must be provided") 
    public boolean isValidBody() {
        boolean hex = (data != null) && !data.isBlank();
        boolean raw = (rawData != null) && !rawData.isEmpty();
        return hex || raw;
    }
}