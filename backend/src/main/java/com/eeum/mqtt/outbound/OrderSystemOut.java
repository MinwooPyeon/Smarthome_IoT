package com.eeum.mqtt.outbound;

import com.eeum.mqtt.common.RetrySpec;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/order (type=system)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSystemOut {

    @NotNull  private Long   ts;
    @NotBlank private String deviceId;
    @NotBlank private String msgId;
    @NotBlank private String schema;
    private   String corrId;
    @NotBlank private String type = "system";
    private   Integer priority;
    private   Long    expiresAt;
    private   RetrySpec retry;
    private   String  replyTo;

    // payload.system
    @NotBlank private String action;
}
