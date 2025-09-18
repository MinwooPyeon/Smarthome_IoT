package com.eeum.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IrSignalIn {

    // 공통 메타 (옵셔널)
    private Long ts;
    private String deviceId;
    private String msgId;
    private String schema;

    // 필수
    private String brand;
    private String device;

    @JsonAlias("raw_data")
    private int[] rawData;

    private String function;
}