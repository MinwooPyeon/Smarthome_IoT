package com.eeum.mqtt.outbound;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    // ── 공통 ─────────────────────────────────────
    @NotNull  private Long   ts;
    @NotBlank private String deviceId; // 대상 허브
    @NotBlank private String msgId;    // 서버가 생성
    @NotBlank private String schema;   // "order/1.x"
    private   String corrId;
    @NotBlank private String type = "ir";
    private   Integer priority;
    private   Long    expiresAt;
    private   RetrySpec retry;
    private   String  replyTo;

    // ── IR payload ───────────────────────────────
    @NotBlank private String encoding; // NEC/RC5/Samsung/...
    private Integer carrierHz;
    private Double  dutyCycle;
    private String  data;
    private List<Integer> rawData;
    private Timing timing;
    private Integer repeat;
    private String  remark;
}
