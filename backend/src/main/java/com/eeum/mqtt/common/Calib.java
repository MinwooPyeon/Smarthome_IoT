package com.eeum.mqtt.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 센서 교정(offset) 값.
@Getter
@Setter
@NoArgsConstructor
public class Calib { 
    private Double tOffset; // 온도 보정값 (섭씨 °C)
    private Double hOffset; // 습도 보정값 (%RH)
}