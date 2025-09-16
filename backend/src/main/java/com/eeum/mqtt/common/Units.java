package com.eeum.mqtt.common;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 측정 단위 정보
@Getter
@Setter
@NoArgsConstructor
public class Units {
    private String temperature; // "C"
    private String humidity;    // "%RH"
    private String gasDensity;  // "ppm"
}