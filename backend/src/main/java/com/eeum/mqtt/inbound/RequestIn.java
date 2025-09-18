package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// MQTT 토픽: hub/{deviceId}/request
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestIn {

    @NotBlank
    @JsonAlias({"Type","type"})
    private String type;

    @JsonAlias("Streaming")
    private boolean streaming;
}