package com.eeum.mqtt.outbound;


import java.util.Map;

import com.eeum.mqtt.common.RetrySpec;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/order (type=matter)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderMatterOut {

    @NotNull  private Long   ts;
    @NotBlank private String deviceId;
    @NotBlank private String msgId;
    @NotBlank private String schema;   
    private   String corrId;
    @Builder.Default
    @NotBlank private String type = "matter";
    @Min(0) private Integer priority;
    @Min(0) private Long    expiresAt;
    private   RetrySpec retry;
    private   String  replyTo;

    // payload.matter
    @NotBlank private String nodeId;
    @NotNull  private Integer endpoint;
    @NotBlank private String cluster;
    @NotBlank private String command;
    private Map<String, Object> args;
}
