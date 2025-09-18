package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽 : hub/{deviceId}/irProtocol
// hub → server <protocol DB>
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IrProtocolIn {

    @NotBlank private String brand;
    @NotBlank private String device;

    @NotBlank
    @JsonAlias("protocol_name")
    private String protocolName;

    @Min(1) private int unit;

    @NotNull @Size(min=2, max=2) private int[] header;
    @NotNull @Size(min=2, max=2) private int[] zero;
    @NotNull @Size(min=2, max=2) private int[] one;

    @Min(0) private int gap;

    @Min(1)
    @JsonAlias("avg_len")
    private int avgLen;
}
